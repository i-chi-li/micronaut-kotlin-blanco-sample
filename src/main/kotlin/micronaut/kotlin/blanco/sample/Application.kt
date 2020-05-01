package micronaut.kotlin.blanco.sample

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.env.PropertySource
import io.micronaut.runtime.Micronaut
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest

object Application {
    @JvmStatic
    fun main(args: Array<String>) {
        val additionalProperties = getAdditionalProperties()

        Micronaut.build()
            .packages("micronaut.kotlin.blanco.sample")
            .mainClass(Application.javaClass)
            .propertySources(additionalProperties)
            .start()
    }
}

/**
 * DB 接続設定を、Secrets Manager から取得して、
 * プロパティとして返す。
 */
private fun getAdditionalProperties(): PropertySource {
    val log = LoggerFactory.getLogger("getAdditionalProperties")
    val propertyMap = mutableMapOf<String, Any>()
    System.getenv("SECRETSMANAGER_SECRETID")?.let { secretId ->
        // 環境変数にシークレット ID が設定されている場合
        log.info("SECRETSMANAGER_SECRETID: $secretId")
        val secretsManager = SecretsManagerClient.builder()
            .build()
        val secretValueRequest = GetSecretValueRequest.builder()
            // シークレットの名前か、シークレットのARN を指定
            .secretId(secretId)
            .build()
        runCatching {
            // シークレットデータを取得
            secretsManager.getSecretValue(secretValueRequest)
        }
            .onSuccess { secretValue ->
                // シークレット取得に成功した場合
                log.info("get secret[$secretId] successful.")
                // シークレット文字列（JSON 形式）を取得
                val secretString = secretValue.secretString()
                log.info("secretString: $secretString")
                val mapper = ObjectMapper()
                mapper.readValue(secretString, DbSecrets::class.java)?.let { dbSecrets ->
                    // シークレット情報を、Bean に変換できた場合
                    log.info("dbSecrets: $dbSecrets")
                    // 接続 URL に変換
                    val url = "jdbc:mysql://${dbSecrets.host}:${dbSecrets.port}"
                    propertyMap["db.default.url"] = url
                    propertyMap["db.default.username"] = dbSecrets.username
                    dbSecrets.password?.let { password ->
                        propertyMap["db.default.password"] = password
                    }
                    dbSecrets.dbname?.let { dbname ->
                        propertyMap["db.default.dbname"] = dbname
                    }
                }
            }
            .onFailure { throwable ->
                // シークレット取得に失敗した場合
                log.warn("get secret[$secretId] failed.", throwable)
            }
            // 例外が発生していた場合は、スローする。
            .getOrThrow()
    }
    log.info("AdditionalProperties: $propertyMap")
    return PropertySource.of(propertyMap)
}

/**
 * シークレット情報を格納
 */
// Jackson でデシリアライズ時に、未知のフィールドがあった場合でも、無視するように設定
@JsonIgnoreProperties(ignoreUnknown = true)
class DbSecrets {
    val host: String = ""
    val port: String = ""
    val dbname: String? = null
    val username: String = ""
    val password: String? = null
        /**
         * 文字列中に"${"が含まれていると、プレースホルダーとして認識してしまい、
         * プレースホルダが不正であると判断されエラーとなる。
         * 「${」 が含まれている場合には、エスケープする処理をする。
         * dummy が、どこにも定義されていない場合、「${」に置き換わるようになる。
         * dummy を定義してはならない。
         */
        get() = field?.replace("\${", "\${dummy:\${}")
}
