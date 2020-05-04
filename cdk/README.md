# CDK
このサンプルを AWS 環境上で動作させるためのリソース定義。

## CDK コマンド実行前に
以下の環境変数を定義し、AWS アカウントIDと、リージョンをそれぞれ設定する。

- CDK_DEFAULT_ACCOUNT
- CDK_DEFAULT_REGION

このファイルと同じディレクトリに、以下のファイルを作成する。
XXXXXXXXXXXX は、AWS アカウントID に置き換えること。

`cdk.context.json`
```
{
  "availability-zones:account=XXXXXXXXXXXX:region=ap-northeast-1": [
    "ap-northeast-1c",
    "ap-northeast-1d"
  ]
}
```

## CDK コマンド実行

```
cdk deploy
```

NAT Gateway インスタンスに SSH ログインする場合

```
cdk deploy -c keyName=キーペア名
```

## CDK スタック削除後に
以下のリソースを手動で削除する。

- ECR Repository

## Useful commands

 * `npm run build`   compile typescript to js
 * `npm run watch`   watch for changes and compile
 * `npm run test`    perform the jest unit tests
 * `cdk deploy`      deploy this stack to your default AWS account/region
 * `cdk diff`        compare deployed stack with current state
 * `cdk synth`       emits the synthesized CloudFormation template

