package micronaut.kotlin.blanco.sample

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.http.annotation.QueryValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import micronaut.kotlin.blanco.sample.datasource.C00S02UsersIteratorConditions
import micronaut.kotlin.blanco.sample.datasource.UserDatasource
import org.slf4j.LoggerFactory
import java.sql.SQLException
import java.time.Instant
import java.util.*
import javax.sql.DataSource
import kotlin.random.Random

/**
 * キュー（Channel）による処理の連携
 */
@Controller("/cooperation")
class ProcessingBlockCooperationController(
    private val dataSource: DataSource,
    private val mapper: ObjectMapper
) {
    /** ロガー */
    private val log = LoggerFactory.getLogger(this.javaClass)

    /**
     * 並列検索処理
     *
     * curl -i http://localhost:8080/cooperation
     * curl -i "http://localhost:8080/cooperation?timeout=2000&backgroundTimeout=5000&selectCount=1000&parallelSize=4&slowQueryTime=200"
     *
     * @param timeoutParam リクエスト処理のタイムアウト（ms）
     * @param backgroundTimeoutParam バックグラウンド処理（検索結果判定処理など）のタイムアウト（ms）
     * @param selectCountParam 検索数
     * @param parallelSizeParam 並行数
     * @param slowQueryTimeParam 遅延クエリ時間
     * @return 検索の集計結果を返す。
     */
    @Get("/")
    @Produces(MediaType.APPLICATION_JSON)
    fun cooperation(
        @QueryValue("timeout")
        timeoutParam: Long?,
        @QueryValue("backgroundTimeout")
        backgroundTimeoutParam: Long?,
        @QueryValue("selectCount")
        selectCountParam: Int?,
        @QueryValue("parallelSize")
        parallelSizeParam: Int?,
        @QueryValue("slowQueryTime")
        slowQueryTimeParam: Long?
    ): RequestResult {
        // リクエストタイムアウト
        val timeout = timeoutParam ?: 2000
        // バックグラウンド処理タイムアウト
        val backgroundTimeout = backgroundTimeoutParam ?: 5000
        // 検索回数
        val selectCount = selectCountParam ?: 10
        // 並行数
        val parallelSize = parallelSizeParam ?: 3
        // 遅延クエリ時間
        val slowQueryTime = slowQueryTimeParam ?: 200

        log.info("Start cooperation [timeout: $timeout, selectCount: $selectCount, parallelSize: $parallelSize]")

        val time = System.currentTimeMillis()
        val processResult: QueryResultProcessor.ProcessResult = runCatching {
            // 検索結果判定処理をグローバルスコープで実行する。
            // よって、リクエスト処理が完了しても、検索結果判定処理までは、バックグラウンドで処理を継続する。
            val resultProcessorJob: Deferred<QueryResultProcessor.ProcessResult> =
                getResultProcessorJobAsync(selectCount, parallelSize, backgroundTimeout, slowQueryTime)

            // 検索結果判定処理の結果を取得
            getProcessResult(timeout, resultProcessorJob)
        }.onSuccess {
            // 処理が完了した場合
            log.info("Successful getProcessResult")
        }.onFailure {
            // 例外が発生した場合
            log.warn("Failed getProcessResult")
        }.recover { throwable ->
            QueryResultProcessor.ProcessResult.FailedProcessResult("Unhandled exception", throwable)
        }.getOrThrow()

        val elapsedTime = System.currentTimeMillis() - time
        log.info("Finish cooperation [$elapsedTime ms]")
        return getRequestResult(processResult, elapsedTime)
    }

    /**
     * 検索結果判定処理のバックグラウンドジョブを取得
     *
     * @param selectCount 合計検索件数
     * @param parallelSize 並行数
     * @param backgroundTimeout バックグラウンド処理のタイムアウト値（ms）
     * @param slowQueryTime 遅延クエリ時間
     * @return 検索結果判定処理のバックグラウンドジョブを返す。
     */
    private fun getResultProcessorJobAsync(
        selectCount: Int, parallelSize: Int, backgroundTimeout: Long, slowQueryTime: Long
    ): Deferred<QueryResultProcessor.ProcessResult> {
        // グローバルスコープでジョブを生成
        return GlobalScope.async {
            log.info("Create ConditionsProducer")
            // 条件生成処理を生成
            val conditionsProducer = ConditionsProducer(getBaseData(mapper))
            // 条件受信用 Channel を取得
            // 生成した、グローバルの Coroutine スコープを引数に渡すことで、その子 Coroutine となるため、
            // リクエスト処理のスコープとは処理継続期間が分離される。
            val conditionsChannel = conditionsProducer.getConditionsChannel(selectCount, this)

            log.info("Create QueryExecutor")
            // 検索実行処理を生成
            val queryExecutor = QueryExecutor(dataSource, slowQueryTime)
            // 検索結果受信用 Channel を取得
            val resultChannel = queryExecutor.getResultChannel(this, conditionsChannel, parallelSize)

            log.info("Create ResultProcessor")
            // 検索結果判定処理の結果を返す。
            QueryResultProcessor(backgroundTimeout).doProcess(resultChannel)
        }
    }

    /**
     * 検索結果判定処理の結果を取得する
     *
     * この処理では、判定処理結果を分析し、レスポンスを返すために必要な情報を生成する。
     * 例外を含む、多様な処理結果を分類し、レスポンスで返す種類に集約する役割を持つ。
     *
     * @param timeout タイムアウト（ms）
     * @param resultProcessorJob 検索結果判定処理ジョブ
     * @return 判定結果を返す。
     */
    private fun getProcessResult(
        timeout: Long, resultProcessorJob: Deferred<QueryResultProcessor.ProcessResult>
    ): QueryResultProcessor.ProcessResult {
        return runCatching {
            // withTimeout は、Suspend 関数なので、runBlocking を利用する。
            // 現在のスレッドをブロックするため、順次処理となる。
            runBlocking {
                // タイムアウトまで、処理を待機
                withTimeout(timeout) {
                    log.info("Waiting for processing ...")
                    // 検索結果判定の結果を結果取得
                    resultProcessorJob.await()
                }
            }
        }.onSuccess {
            // 処理が成功した場合
            log.info("Successful cooperation")
        }.onFailure { throwable ->
            // 例外が発生した場合
            log.warn("Failed cooperation [${throwable.message}]")
        }.recover { throwable ->
            // 例外が発生した場合の代替戻り値を生成
            when (throwable) {
                is TimeoutCancellationException -> {
                    // タイムアウトが発生した場合
                    log.warn("Timed out at getProcessResult")
                    QueryResultProcessor.ProcessResult.TimeoutProcessResult("Recovered", throwable)
                }
                else -> {
                    // タイムアウト以外が発生した場合
                    log.warn("Throw exception at getProcessResult")
                    QueryResultProcessor.ProcessResult.FailedProcessResult("Recovered", throwable)
                }
            }
        }.getOrThrow()
    }

    /**
     * リクエストの処理結果を取得する。
     *
     * この処理は、判定処理結果に応じて、
     * 各種データ書き込みや、ログの出力、代替レスポンス作成などを行う役割を持つ。
     *
     * @param processResult 検索結果判定処理結果
     * @param elapsedTime 経過時間
     * @return リクエスト処理結果を返す。
     */
    private fun getRequestResult(processResult: QueryResultProcessor.ProcessResult, elapsedTime: Long): RequestResult {
        return when (processResult) {
            is QueryResultProcessor.ProcessResult.SuccessfulProcessResult -> {
                // 処理が成功した場合
                RequestResult("$processResult", elapsedTime)
            }
            is QueryResultProcessor.ProcessResult.FailedProcessResult -> {
                // 処理が失敗した場合
                RequestResult("$processResult", elapsedTime)
            }
            is QueryResultProcessor.ProcessResult.TimeoutProcessResult -> {
                // タイムアウトした場合
                RequestResult("$processResult", elapsedTime)
            }
        }
    }

    /**
     * レスポンス返却用
     *
     * @property result 処理結果
     */
    data class RequestResult(
        val result: String,
        val elapsedTime: Long
    )
}

/**
 * 検索結果判定
 *
 * @property timeout 検索結果判定処理のタイムアウト（ms）
 */
class QueryResultProcessor(
    private val timeout: Long
) {
    /** ロガー */
    private val log = LoggerFactory.getLogger(this.javaClass)

    /**
     * 検索結果を処理する
     * @param resultChannel 検索結果受信 Channel
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun doProcess(
        resultChannel: ReceiveChannel<QueryExecutor.QueryResult>
    ): ProcessResult {
        val time = System.currentTimeMillis()
        return runCatching {
            log.info("Start doProcess [timeout: $timeout]")
            // バックグラウンドで動作する検索結果判定処理のタイムアウト設定
            withTimeout(timeout) {
                // 検索結果の集計結果を取得
                doProcessResult(resultChannel)
            }
        }.onSuccess {
            // 処理が完了した場合
            log.info("Successful doProcess [time: ${System.currentTimeMillis() - time}]")
        }.onFailure {
            // 例外が発生した場合
            log.error("Failure doProcess [time: ${System.currentTimeMillis() - time}]", it)
            when (it) {
                is CancellationException -> {
                    log.info("Canceling queryResultChannel")
                    resultChannel.cancel(it)
                }
            }
        }.recover {
            // 代替の戻り値を生成
            log.warn("Recover doProcess", it)
            ProcessResult.FailedProcessResult("Recover doProcess", it)
        }.getOrThrow()
    }

    /**
     * 検索結果を処理する
     *
     * @param resultChannel 検索結果受信 Channel
     * @return 検索結果の集計結果を返す。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun doProcessResult(
        resultChannel: ReceiveChannel<QueryExecutor.QueryResult>
    ): ProcessResult.SuccessfulProcessResult {
        // 検索成功数
        var querySuccessCount = 0L
        // 検索失敗数
        var queryFailedCount = 0L
        // 該当ユーザ数
        var totalExistsCount = 0L

        // 検索数
        var totalQueryCount = 0L

        // 検索結果を受信し、集計する
        resultChannel.consumeEach { result ->
            totalQueryCount++
            if (totalQueryCount % 1000 == 0L) log.info("totalQueryCount: $totalQueryCount")
            // 検索結果を受信した場合
            // log.info("Receive result [$result]")
            when (result) {
                is QueryExecutor.QueryResult.SuccessQueryResult -> {
                    // 検索が正常に完了した場合
                    // log.info("result: $result")
                    querySuccessCount++
                    if (result.userExists) totalExistsCount++
                    // ここで、受信した検索結果を利用して、各種判定処理を行う。
                }
                is QueryExecutor.QueryResult.FailedQueryResult -> {
                    // 検索で例外が発生した場合
                    // log.warn("result: $result")
                    queryFailedCount++
                }
            }
        }
        // 検索結果の集計を返す
        return ProcessResult.SuccessfulProcessResult(totalQueryCount, querySuccessCount, queryFailedCount, totalExistsCount)
    }

    /**
     * 検索結果判定処理結果の返却用シールドクラス
     */
    sealed class ProcessResult {
        /**
         * 検索結果処理結果の成功返却用
         *
         * @property totalQueryCount クエリ総数
         * @property querySuccessCount クエリ成功回数
         * @property queryFailedCount クエリ失敗回数
         * @property totalExistsCount ユーザ数合計
         */
        data class SuccessfulProcessResult(
            val totalQueryCount: Long,
            val querySuccessCount: Long,
            val queryFailedCount: Long,
            val totalExistsCount: Long
        ) : ProcessResult()

        /**
         * 検索結果処理結果の失敗返却用
         *
         * @property message 失敗の説明
         * @property throwable 発生した例外
         */
        data class FailedProcessResult(
            val message: String = "",
            val throwable: Throwable = IllegalStateException("FailedProcessResult")
        ) : ProcessResult()

        /**
         * 検索結果処理結果のタイムアウト返却用
         *
         * @property message 失敗の説明
         * @property throwable 発生した例外
         */
        data class TimeoutProcessResult(
            val message: String = "",
            val throwable: Throwable = IllegalStateException("TimeoutProcessResult")
        ) : ProcessResult()
    }
}

/**
 * 検索条件生成
 */
class ConditionsProducer(
    private val baseData: List<User>
) {
    /** ロガー */
    private val log = LoggerFactory.getLogger(this.javaClass)

    /**
     * 検索条件を受信する Channel を取得
     *
     * @param generateCount 条件生成数
     * @param coroutineScope Coroutine スコープ
     * @return 検索条件を受信するための Channel を返す。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getConditionsChannel(
        generateCount: Int,
        coroutineScope: CoroutineScope
    ): ReceiveChannel<C00S02UsersIteratorConditions> = coroutineScope.produce(capacity = 1000) {
        log.info("Start getConditionsChannel [repeat: $generateCount]")
        repeat(generateCount) {
            var userIdFrom: Int? = null
            var userIdTo: Int? = null
            var userName: String? = null
            var password: String? = null
            var email: String? = null
            var emailIncludeNull = false
            var createdAtFrom: Date? = null
            var createdAtTo: Date? = null
            var updatedAtFrom: Date? = null
            var updatedAtTo: Date? = null
            val fromDateMillis = Instant.parse("2020-01-01T00:00:00.00Z").toEpochMilli()
            val toDateMillis = Instant.parse("2020-04-30T00:00:00.00Z").toEpochMilli()

            // 必ず、いずれか一つ検索条件を設定
            when ((0 until 600).random()) {
                in 0 until 1 -> userName = "${baseData.random().userNameLast}%"
                in 100 until 101 -> password = "${('a'..'z').random()}%"
                in 200 until 201 -> {
                    val emailName = baseData.random().email.replace("""\d*@.*$""".toRegex(), "")
                    email = "$emailName${(0..9).random()}%"
                }
                in 300 until 400 -> emailIncludeNull = true
                in 400 until 500 -> {
                    val dateFromMillis = (fromDateMillis..toDateMillis).random()
                    val dateToMillis = (dateFromMillis..toDateMillis).random()
                    createdAtFrom = Date.from(Instant.ofEpochMilli(dateFromMillis))
                    createdAtTo = Date.from(Instant.ofEpochMilli(dateToMillis))
                }
                in 500 until 600 -> {
                    val dateFromMillis = (fromDateMillis..toDateMillis).random()
                    val dateToMillis = (dateFromMillis..toDateMillis).random()
                    updatedAtFrom = Date.from(Instant.ofEpochMilli(dateFromMillis))
                    updatedAtTo = Date.from(Instant.ofEpochMilli(dateToMillis))
                }
                else -> {
                    userIdFrom = Random.nextInt(0, Int.MAX_VALUE)
                    userIdTo = Random.nextInt(userIdFrom, Int.MAX_VALUE)
                }
            }

            val conditions = C00S02UsersIteratorConditions(
                conditionId = Random.nextInt(),
                userIdFrom = userIdFrom,
                userIdTo = userIdTo,
                userName = userName,
                password = password,
                email = email,
                emailIncludeNull = emailIncludeNull,
                createdAtFrom = createdAtFrom,
                createdAtTo = createdAtTo,
                updatedAtFrom = updatedAtFrom,
                updatedAtTo = updatedAtTo
            )
//            log.info("conditions: $conditions")
            send(conditions)
//            log.info("Send condition [conditionId: ${conditions.conditionId}]")
        }
        log.info("Finish getConditionsChannel")
    }
}

/**
 * 検索実行
 * @param dataSource データストア
 * @param slowQueryTime 遅延クエリ時間
 */
@Introspected
class QueryExecutor(
    private val dataSource: DataSource,
    private val slowQueryTime: Long
) {
    /** ロガー */
    private val log = LoggerFactory.getLogger(this.javaClass)

    /**
     * 検索結果を受信する Channel を取得
     *
     * 検索条件は、キューに格納し、各検索実行スレッドが、順次受け取る。
     * 重い SQL が偏らず、各検索実行スレッドを効率的に稼働することができる。
     * たとえば、検索条件をリストで検索実行スレッド毎に分割して渡すケースでは、
     * 重い SQL が偏ると、一部のスレッドが空き状態となり非効率となる。
     *
     * @param coroutineScope Coroutine スコープ
     * @param conditionsChannel 検索条件を受信する Channel
     * @param parallelSize 並行数
     * @return 検索結果を受信する Channel を返す。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getResultChannel(
        coroutineScope: CoroutineScope,
        conditionsChannel: ReceiveChannel<C00S02UsersIteratorConditions>,
        parallelSize: Int
    ): ReceiveChannel<QueryResult> = coroutineScope.produce(capacity = Channel.UNLIMITED) {
        // 各検索処理での想定外の例外処理用
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            log.error("cached unhandled exception", throwable)
        }

        // SupervisorScope で実行することで、子のキャンセルが現在のスコープおよび、他の子に伝播しない。
        // よって、一部検索に失敗しても、その他の検索は続行できる。
        supervisorScope {
            log.info("Start supervisorScope [parallelSize: $parallelSize]")
            // 並行実行数だけ起動する
            repeat(parallelSize) { num ->
                log.info("Start repeat $num")
                // 非同期に検索処理起動
                launch(Dispatchers.IO + exceptionHandler) {
                    log.info("Start launch-$num")
                    var count = 0
                    // DB コネクションを取得
                    dataSource.connection.use { connection ->
                        // ユーザデータアクセスオブジェクトを生成
                        val userDataSource = UserDatasource(connection)
                        // ユーザ数検索を実行
                        userDataSource.selectExists {
                            conditionsChannel.consumeEach { conditions ->
                                // クエリ開始時間
                                val queryStartTime = System.currentTimeMillis()
                                ++count
                                if (count % 1000 == 0) {
                                    log.info("launch-$num process count: $count")
                                }
                                // 検索条件を受信した場合
                                //log.info("Executing query [conditionId: ${conditions.conditionId}]")
                                // 例外は、発生する箇所で処理を行う。
                                runCatching {
                                    // 0.1 % の確率で SQLException をスローする
                                    //(0..1000).random().takeIf { it == 0 }?.run { throw SQLException("Too bad") }

//                                    log.info("execute query launch-$num [count: $count, conditions: $conditions]")
                                    // DB 検索結果から、ユーザ数を取得
                                    iterate(conditions)
                                }.onSuccess { userCount ->
                                    // DB 検索が正常に完了した場合
                                    // 検索結果を生成
                                    val result = QueryResult.SuccessQueryResult(conditions, userCount)
                                    // 検索結果を Channel に送信
                                    send(result)
                                }.onFailure { throwable ->
                                    // DB 検索で例外が発生した場合
                                    log.warn("Failed launch-$num: ${throwable.message}")
                                    val result = when (throwable) {
                                        is TimeoutCancellationException -> {
                                            // SQL でタイムアウトした場合
                                            QueryResult.FailedQueryResult(
                                                conditions, "selectCount",
                                                SQLException("Query timed out", throwable)
                                            )
                                        }
                                        // それ以外の例外の場合
                                        else -> QueryResult.FailedQueryResult(conditions, "selectCount", throwable)
                                    }
                                    send(result)
                                }
                                // クエリ実行時間を表示
                                val elapsedTime = System.currentTimeMillis() - queryStartTime
                                // 遅延 SQL の場合ログに出力
                                if (elapsedTime >= slowQueryTime) {
                                    log.info("Query elapsedTime launch-$num $elapsedTime ms, conditions: $conditions")
                                }
                            }
                        }
                    }
                    log.info("Finish launch-$num [query count: $count]")
                }
                log.info("Finish repeat $num")
            }
            log.info("Finish supervisorScope")
        }
    }

    /**
     * 検索結果格納用シールドクラス
     */
    sealed class QueryResult {
        /**
         * 成功時検索結果格納用
         *
         * 検索条件を含める理由は、検索結果判定処理で、検索結果との対応付けができるようにするため。
         * ただし、もっと良い方法があるはず。
         *
         * @property conditions 検索条件
         * @property userExists ユーザ存在
         */
        data class SuccessQueryResult(
            val conditions: C00S02UsersIteratorConditions,
            val userExists: Boolean
        ) : QueryResult()

        /**
         * 失敗時検索結果格納用
         *
         * @property conditions 検索条件
         * @property reason 失敗の理由
         * @property throwable 発生した例外
         */
        data class FailedQueryResult(
            val conditions: C00S02UsersIteratorConditions,
            val reason: String,
            val throwable: Throwable
        ) : QueryResult()
    }
}
