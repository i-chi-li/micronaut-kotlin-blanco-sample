package micronaut.kotlin.blanco.sample

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.env.PropertySource
import io.micronaut.runtime.Micronaut

object Application {
    @JvmStatic
    fun main(args: Array<String>) {
        // DB 接続情報を取得
        val additionalProperties = getEscapedDbPasswordPropertySource()

        Micronaut.build()
            .packages("micronaut.kotlin.blanco.sample")
            .mainClass(Application.javaClass)
            // DB 接続情報を設定
            .propertySources(additionalProperties)
            // 不要な環境変数を除外
            .environmentVariableExcludes("DB_SECRETS")
            .start()
    }
}

/**
 * 環境変数に指定した Secrets Manager の DB 接続情報（JSON）を分解してプロパティソースに変換する。
 *
 * 2020/05/03 時点の ECS Fargate では、Secrets Manager の値（JSON）から個別の値（host など）を
 * 取り出すことができない。そのため、ここで JSON 文字列を解析する処理を行う。
 *
 * DB パスワードは、エスケープ処理をする
 * パスワードに "${" を含む場合、Micronaut のプロパティホルダーと認識されエラーとなる。
 * そのため、"${" を、プレースホルダ "${dummy:${}" に置換する。
 * プレースホルダ "${dummy:${}" は、「dummy」がプロパティ名で、コロン以降（「${」）がデフォルト値となる。
 * 「dummy」は、未設定とし、解決されないようにして、デフォルト値が利用されるようにする。
 * 最終的には、"${" になる。
 *
 * @return エスケープ済みパスワードを設定するためのプロパティソースを返す。
 */
private fun getEscapedDbPasswordPropertySource(): PropertySource {
    val map: MutableMap<String, Any> = mutableMapOf()
    System.getenv("DB_SECRETS")?.also { dbSecretsStr ->
        val objectMapper = ObjectMapper()
        objectMapper.readValue(dbSecretsStr, DbSecrets::class.java).let {dbSecrets ->
            dbSecrets.host?.also { host ->
                map["db.host"] = host
            }
            dbSecrets.hostReadOnly?.also { hostReadOnly ->
                map["db.hostRead"] = hostReadOnly
            }
            dbSecrets.port?.also {port ->
                map["db.port"] = port
            }
            dbSecrets.username?.also{ username ->
                map["db.username"] = username
            }
            dbSecrets.password?.also { password ->
                map["db.password"] = password
            }
        }
    }
    return PropertySource.of("escapedDbPassword", map)
}

/**
 * DB 接続情報
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class DbSecrets {
    /**
     * ホスト
     */
    var host: String? = null

    /**
     * ポート
     */
    var port: Int? = null

    /**
     * ユーザ名
     */
    var username: String? = null

    /**
     * パスワード
     */
    var password: String? = null
        get() = field?.replace("\${", "\${dummy:\${}")

    /**
     * 参照専用ホスト
     */
    val hostReadOnly: String?
        get() = host?.let { hostReadWrite ->
            Regex("""([^.]+\.cluster)(-[^.]+.ap-northeast-1.rds.amazonaws.com)""")
                .find(hostReadWrite)
                ?.groupValues
                ?.takeIf { it.size == 3 }
                ?.let { groupValues ->
                    // Aurora のエンドポイント形式に一致してる場合、
                    // 参照専用エンドポイントの形式に置換する
                    // groupValues[0] には、全体の文字列が格納されている。
                    "${groupValues[1]}-ro${groupValues[2]}"
                }
                // 一致しない場合は、そのまま返す。
                ?: host
        }
}
