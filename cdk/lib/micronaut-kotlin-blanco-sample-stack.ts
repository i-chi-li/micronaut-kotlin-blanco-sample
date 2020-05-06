import * as cdk from '@aws-cdk/core';
import * as ec2 from "@aws-cdk/aws-ec2";
import * as ecr from "@aws-cdk/aws-ecr";
import * as ecs from "@aws-cdk/aws-ecs";
import * as elb from "@aws-cdk/aws-elasticloadbalancingv2";
import * as iam from "@aws-cdk/aws-iam";
import * as rds from "@aws-cdk/aws-rds";

export class MicronautKotlinBlancoSampleStack extends cdk.Stack {
    constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);

        let port = 80;

        // ACM Certificate ARN
        const certArn = this.node.tryGetContext("cert");
        let certificate = null;
        if(certArn) {
            port = 443;
            certificate = elb.ListenerCertificate.fromArn(
                "arn:aws:acm:ap-northeast-1:801303654280:certificate/39b67626-84e3-4230-8071-e93b4928ce4c");
        }

        // NAT Gateway Instance KeyName
        const keyName = this.node.tryGetContext("keyName");

        // NAT Gateway Provider
        const natGatewayProvider = ec2.NatProvider.instance({
            // キーペア名を指定しないと、SSH ログインできないインスタンスとなる。
            keyName: keyName,
            instanceType: new ec2.InstanceType("t3a.nano"),
            // デフォルトでは、全受信許可となるので注意
            //allowAllTraffic: false
        });

        // VPC
        const vpc = new ec2.Vpc(this, "EcsVpc", {
            cidr: "10.20.1.0/24",
            maxAzs: 2,
            natGatewayProvider: natGatewayProvider,
            natGateways: 1,
            natGatewaySubnets: {
                // 指定必須。デフォルトでは、PRIVATE になり、NAT が作成できずエラーとなる。
                subnetType: ec2.SubnetType.PUBLIC,
                // 現時点で t3a.nano は、1b か 1d でのみサポートしている
                availabilityZones: ["ap-northeast-1d"]
            }
        });

        // ELB2
        const alb2 = new elb.ApplicationLoadBalancer(this, "EcsAlb2", {
            vpc,
            // 外部公開 ELB
            internetFacing: true
        });

        // ECS タスク実行ロール
        const role = new iam.Role(this, "Role", {
            assumedBy: new iam.ServicePrincipal("ecs-tasks.amazonaws.com")
        });

        // Aurora クラスタパラメータグループ
        const auroraClusterParameterGroup = new rds.ClusterParameterGroup(this, "clusterParameterGroup", {
            family: "aurora-mysql5.7",
            parameters: {
                time_zone: "UTC",
                character_set_server: "utf8mb4",
                collation_server: "utf8mb4_bin"
            }
        });

        // Aurora クラスタ
        const auroraCluster = new rds.DatabaseCluster(this, "Database", {
            engine: rds.DatabaseClusterEngine.AURORA_MYSQL,
            parameterGroup: auroraClusterParameterGroup,
            engineVersion: "5.7.mysql_aurora.2.07.2",
            instances: 1,
            removalPolicy: cdk.RemovalPolicy.DESTROY,
            masterUser: {
                username: "app"
            },
            instanceProps: {
                instanceType: ec2.InstanceType.of(ec2.InstanceClass.BURSTABLE3, ec2.InstanceSize.SMALL),
                vpc
            }
        });

        // VPC 内部からの Aurora アクセスを許可
        auroraCluster.connections.allowDefaultPortFrom(
            {
                connections: new ec2.Connections({
                    peer: ec2.Peer.ipv4(vpc.vpcCidrBlock)
                })
            }
        );

        // Secrets Manager に格納した Aurora 接続情報へのアクセス権限を ECS タスク実行ロールに付与
        const secret = auroraCluster.secret!!;
        secret.grantRead(role);

        // ECR リポジトリ
        // イメージが残っていると削除失敗するので、スタック削除後に手動で消す必要がある。
        const repository = new ecr.Repository(this, "Ecr", {
            lifecycleRules: [{maxImageCount: 1}]
        });
        // ECR リポジトリから取得する権限を ECS タスク実行ロールに付与
        repository.grantPull(role)

        // ECS クラスタ
        const ecsCluster = new ecs.Cluster(this, "EcsCluster", {
            vpc: vpc
        });

        // ECS タスクの CPU 数
        const ecsCpu = 4096;

        // Java に指定する CPU 数
        const javaCpu = Math.ceil(ecsCpu / 1024);

        // ECS タスク定義
        const taskDefinition = new ecs.FargateTaskDefinition(this, "TaskDefinition", {
            memoryLimitMiB: 8192,
            cpu: ecsCpu,
            executionRole: role
        });

        // ECS タスクコンテナ定義
        const container = taskDefinition.addContainer("MainContainer", {
            image: ecs.ContainerImage.fromEcrRepository(repository),
            // 環境変数
            environment: {
                // JVM の設定
                // -Dnocolor は、logback のフォーマット定義（logback.xml）の条件判定に利用している
                // -XX:ActiveProcessorCount は、Java で認識する CPU 数を指定する。
                // 指定しない場合、Java で認識する CPU 数は、常に 1 となる。
                // この問題は、ECS 特有の問題となる。
                // https://www.databasesandlife.com/java-docker-aws-ecs-multicore/
                // Java 8，9 の場合、-XX:ActiveProcessorCount オプションは、
                // -XX:+UnlockExperimentalVMOptions とセットで指定する。
                // ここでは、Docker イメージで定義しているので、以下のオプションのみで問題なし。
                JAVA_TOOL_OPTIONS: `-Xms386m -Xmx386m -Dnocolor -XX:ActiveProcessorCount=${javaCpu}`,
                PROXY_URL: `https://${alb2.loadBalancerDnsName}`
            },
            secrets: {
                // 2020/05/03 時点の Fargate では、Secrets Manager の JSON 値から、
                // 任意のパラメータ(host など)を指定して取得できない。
                DB_SECRETS: ecs.Secret.fromSecretsManager(secret)
            },
            logging: ecs.LogDrivers.awsLogs({streamPrefix: "ecs-"}),
            stopTimeout: cdk.Duration.seconds(10)
        });

        // ECS タスクコンテナポートマッピング
        container.addPortMappings({
            containerPort: 8080
        });

        // ECS サービス用セキュリティグループ
        const securityGroupEcsService = new ec2.SecurityGroup(this, "SecurityGroupEcsService", {
            vpc: vpc,
            description: "for ECS Service"
        });
        // VPC 内から、ECS サービスへの TCP 8080 ポートアクセスを許可
        securityGroupEcsService.addIngressRule(ec2.Peer.ipv4(vpc.vpcCidrBlock), ec2.Port.tcp(8080));

        // ECS サービス
        const ecsService = new ecs.FargateService(this, "Service", {
            cluster: ecsCluster,
            taskDefinition: taskDefinition,
            desiredCount: 0,
            securityGroup: securityGroupEcsService
        });

        // ECS サービスに複数のセキュリティグループを定義する
        // 現時点のハイコンストラクトでは、1 つのみ設定可能
        // よって、ローレベルコンストラクトをオーバーライドする。
        // const cfnEcsService = ecsService.node.findChild("Service") as ecs.CfnService;
        // cfnEcsService.addOverride(
        //     "Properties.NetworkConfiguration.AwsvpcConfiguration.SecurityGroups",
        //     [
        //         // Aurora のセキュリティグループを追加
        //         auroraCluster.securityGroupId
        //     ]
        // )

        // ELB
        const alb = new elb.ApplicationLoadBalancer(this, "EcsAlb", {
            vpc,
            // 外部公開 ELB
            internetFacing: true
        });

        // ELB リスナ
        const listener = alb.addListener("Listener", {
            port: port
        });
        if(certificate) {
            // 証明書を追加
            listener.addCertificates("Cert", [certificate])
        }

        // ELB ターゲットグループ
        const targetGroup = listener.addTargets("Ecs", {
            port: 8080,
            // ターゲットを指定。
            // 指定した ECS サービスのセキュリティグループには、ELB からの入力ルールが自動追加される。
            targets: [ecsService],
            healthCheck: {
                path: "/date/now",
                // チェック間隔(default 30 seconds)
                interval: cdk.Duration.seconds(10)
            },
            // ELB がターゲットを登録解除する前に待機する時間(0 - 3600 seconds) default 300
            deregistrationDelay: cdk.Duration.seconds(5)
        });

        // --------------------------------------------------------

        // ECS サービス2
        const ecsService2 = new ecs.FargateService(this, "Service2", {
            cluster: ecsCluster,
            taskDefinition: taskDefinition,
            desiredCount: 0,
            securityGroup: securityGroupEcsService
        });

        // ELB リスナ2
        const listener2 = alb2.addListener("Listener2", {
            port: port
        });
        if(certificate) {
            // 証明書を追加
            listener2.addCertificates("Cert", [certificate])
        }

        // ELB ターゲットグループ2
        const targetGroup2 = listener2.addTargets("Ecs2", {
            port: 8080,
            // ターゲットを指定。
            // 指定した ECS サービスのセキュリティグループには、ELB からの入力ルールが自動追加される。
            targets: [ecsService2],
            healthCheck: {
                path: "/date/now",
                // チェック間隔(default 30 seconds)
                interval: cdk.Duration.seconds(10)
            },
            // ELB がターゲットを登録解除する前に待機する時間(0 - 3600 seconds) default 300
            deregistrationDelay: cdk.Duration.seconds(5)
        });

    }
}
