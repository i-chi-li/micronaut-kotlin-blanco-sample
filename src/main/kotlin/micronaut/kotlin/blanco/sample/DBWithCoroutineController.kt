package micronaut.kotlin.blanco.sample

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.http.annotation.QueryValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import micronaut.kotlin.blanco.sample.datasource.C00S01UsersIteratorConditions
import micronaut.kotlin.blanco.sample.datasource.UserDatasource
import micronaut.kotlin.blanco.sample.model.Users
import org.slf4j.LoggerFactory
import javax.inject.Named
import javax.sql.DataSource
import kotlin.system.measureTimeMillis

/**
 * DB 接続を Coroutine で行う場合のサンプル
 *
 * @property dataSource 読み書き用 DB コネクションソース
 * @property readDataSource 読み込み専用 DB コネクションソース
 */
@Controller("/db-coroutine")
class DBWithCoroutineController(
    private val dataSource: DataSource,
    @Named("readonly")
    private val readDataSource: DataSource,
    private val mapper: ObjectMapper
) {
    /** ロガー */
    private val log = LoggerFactory.getLogger(this.javaClass)

    /**
     * 大量のユーザデータを生成する
     *
     * プライマリキーがが重複している場合は、更新となる。
     * DB アクセスの性能を向上させるには、DB コネクションを分けて、並行処理をする必要がある。
     * 同一 コネクションで、ステートメントを分けても、並行処理されない。
     * そもそも、MySQL Connector/J の Connection は、スレッドセーフではないようだ。
     *
     * curl -i http://localhost:8080/db-coroutine/generate
     * curl -i "http://localhost:8080/db-coroutine/generate?total=1000000&parallel=10"
     *
     * 以下の計測値は、テーブルにインデックスを適用する前の値となる。
     * インデックス適用後では、大体 2 倍強の時間が掛かる。
     * ## 10 万件登録
     *
     * ### スレッドで実行
     * - 8 thread: 6643 ms
     * - 10 thread: 6503 ms
     *
     * ### Coroutine で実行
     * - 1 coroutine: 28072 ms
     * - 8 coroutine: 7035 ms
     * - 10 coroutine: 6227 ms
     * - 12 coroutine: 6266 ms
     *
     * ## 1000 万件登録
     * - 12 coroutine: 653048 ms
     *
     */
    @Get("/generate")
    @Produces(MediaType.APPLICATION_JSON)
    fun generate(
        @QueryValue("total")
        totalParam: Int?,
        @QueryValue("parallel")
        parallelParam: Int?
    ): String = runBlocking {
        // 全登録ユーザ数
        // 並行処理数で割り切れないと、登録数に誤差が発生する。
        val totalUsersCount = totalParam ?: 1_000_000
        // 並行処理数
        // 確認した限りでは、仮想 CPU 数 × 1.5 程度で最も効果があった
        val parallelCount = parallelParam ?: 10
        log.info("Start generate [totalUsersCount: $totalUsersCount]")
        val baseData = getBaseData(mapper)
        val time = measureTimeMillis {
            // 1 処理あたりの登録数
            val repeatCount = totalUsersCount / parallelCount
            // 内部の Coroutine がすべて完了するまで待機する
            // 一つでも失敗すると終了する
            coroutineScope {
                repeat(parallelCount) { count ->
                    log.info("Start count: $count")
                    // Dispatchers を指定しない場合、
                    // このリクエスト処理が、開始したスレッドのみで、直列に実行をするため、並行処理されない。
                    launch(Dispatchers.IO) {
                        // ユーザデータを登録
                        insertUsers(count, repeatCount, baseData)
                    }
                }
            }
        }

        log.info("Finish generate [$time ms]")
        "OK [$time ms]"
    }

    /**
     * ユーザを登録
     *
     * @param count 並行処理番号
     * @param repeatCount 登録数
     * @param baseData ユーザデータ生成時の元データ
     */
    private fun insertUsers(count: Int, repeatCount: Int, baseData: List<User>) {
        // DB コネクションを自動的に Close する定義
        dataSource.connection.use { connection ->
            val userDataSource = UserDatasource(connection)
            log.info("Start launch $count")
            // insertOrUpdate 関数に処理ブロックを指定している
            // blancoDB オブジェクトは、自動的に生成および、破棄される。
            userDataSource.insertOrUpdate {
                runCatching {
                    repeat(repeatCount) { count2 ->
                        val userId = count * repeatCount + count2
                        val user = generateUser(baseData, userId)
                        // insertOrUpdate 関数内の無名クラスで定義している invoke 関数を呼び出している。
                        invoke(user)
                    }
                }.onFailure { log.error("in generate", it) }
            }
            log.info("Finish launch $count")
            // コミット
            connection.commit()
        }
    }

    /**
     * ランダムなユーザデータを生成
     */
    private fun generateUser(baseData: List<User>, userId: Int): Users {
        val userNameLast = baseData.random().userNameLast
        val userNameFirst = baseData.random().userNameFirst
        // パスワードは、文字列の順番をランダムに入れ替える
        val password = baseData.random().password.toList().shuffled().joinToString("")
        // E-mail は、@ の前に 3 桁のランダムな数字を付加する
        val emailAddString = "%03d@".format((0 until 1000).random())
        val email = baseData.random().email.replace("@", emailAddString)
        return Users(
            userId = userId,
            userName = "$userNameLast $userNameFirst",
            password = password,
            email = email
        )
    }

    /**
     * ユーザを検索
     *
     * @param count 並行処理番号
     * @param conditionsChannel ユーザデータ検索条件生成チャネル
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun selectUsers(
        coroutineScope: CoroutineScope,
        count: Int,
        conditionsChannel: ReceiveChannel<C00S01UsersIteratorConditions>
    ) = coroutineScope.produce<List<Users>> {
        // DB コネクションを自動的に Close する定義
        dataSource.connection.use { connection ->
            val userDataSource = UserDatasource(connection)
            log.info("Start launch $count")
            // select 関数に処理ブロックを指定している
            // blancoDB オブジェクトは、自動的に生成および、破棄される。
            userDataSource.select {
                this.runCatching {
                    for (condition in conditionsChannel) {
                        // select 関数内の無名クラスで定義している iterate 関数を呼び出している。
                        val result = this.iterate(condition)
                        send(result)
                    }
                }.onFailure { log.error("in selectUsers", it) }
            }
            log.info("Finish launch $count")
        }
    }
}

/**
 * Json テキストファイルからのデータ読み込み用
 * 引数無しコンストラクタを生成するアノテーションを付与
 *
 * 本来、Jackson で利用する Bean は、引数なしコンストラクタが必須となる。
 * しかし、no-arg プラグインを利用すれば、任意のアノテーションを付与した Bean に
 * 自動的に引数無しコンストラクタを生成する。
 */
@CustomBean
data class User(
    val userId: Int,
    val userNameLast: String,
    val userNameFirst: String,
    val password: String,
    val email: String
)

/**
 * 引数無しコンストラクタを生成する Bean に付与するアノテーション
 * no-arg プラグインの設定で、このアノテーションを指定している。
 * build.gradle ファイルの noArg の設定を参照。
 */
annotation class CustomBean

/**
 * ユーザデータ生成用データを取得
 */
fun getBaseData(mapper: ObjectMapper): List<User> =
    ClassLoader.getSystemResourceAsStream("personal_information.txt").use { usersDataStream ->
        mapper.readValue(usersDataStream, object : TypeReference<List<User>>() {})
    }
