package micronaut.kotlin.blanco.sample.datasource

import micronaut.kotlin.blanco.sample.blanco.db.users.query.C00I00UsersInvoker
import micronaut.kotlin.blanco.sample.blanco.db.users.query.C00I01UsersFullInvoker
import micronaut.kotlin.blanco.sample.blanco.db.users.query.C00I02UsersInsertOrUpdateInvoker
import micronaut.kotlin.blanco.sample.blanco.db.users.query.C00S00UsersIterator
import micronaut.kotlin.blanco.sample.blanco.db.users.query.C00S01UsersIterator
import micronaut.kotlin.blanco.sample.blanco.db.users.query.C00S02UsersIterator
import micronaut.kotlin.blanco.sample.blanco.db.users.row.C00S00UsersRow
import micronaut.kotlin.blanco.sample.model.Users
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.SQLException
import java.util.*

class UserDatasource(private val con: Connection) {
    /** ロガー */
    private val log = LoggerFactory.getLogger(this.javaClass)

    /**
     * 古式ゆかしい記述方法
     */
    fun selectTraditional(): List<Users> {
        val s00UsersIterator = C00S00UsersIterator(con)
        val users: MutableList<Users> = mutableListOf()
        try {
            s00UsersIterator.executeQuery()
            while (s00UsersIterator.next()) {
                val row = s00UsersIterator.row
                users.add(Users(
                    userId = row.userId,
                    userName = row.userName,
                    password = row.password,
                    email = row.email,
                    createdAt = row.createdAt,
                    updatedAt = row.updatedAt
                ))
            }
        } finally {
            s00UsersIterator.close()
        }
        return users.toList()
    }

    /**
     * 近代的な記述方法
     *
     * この関数は、blancoDB オブジェクトを呼び出し毎に生成する。
     * 性能に影響するため、複数回呼び出す処理には利用しないこと。
     *
     * 定型コードを集約し、余分な変数の生成を抑制できる。
     * その結果、処理そのものに集中できる。
     * 適切な粒度で集積したコードに明確な名称を付与することで、利用する側でのコメントは、ほぼ不要となる。
     * コメント記載を強制すると、意味の無いコメントを増やす要因ともなる。
     * 以下のようなコードであれば、メソッド名が機能を表現しているため、コメントは無駄なノイズを増やす。
     * Kotlin では、if, try, when などは、構文ではなく、式なので、戻り値を返す。
     */
    fun selectModern(): List<Users> {
        // ユーザ全件検索
        val s00UsersIterator = C00S00UsersIterator(con)
        return runCatching {
            // レコードをすべて処理
            s00UsersIterator
                // レコードをシーケンス化
                .asRowSequence()
                // レコードを変換
                .map { row ->
                    println("createdAt: ${row.createdAt}")
                    // レコードを Users に変換
                    row.toUser()
                }
                // Users リスト型に変換
                .toList()
        }
            // catch 処理
            .onFailure { throwable ->
                // ここでは例外を握りつぶすことはできない。
                // 別の例外（アプリ固有の例外など）にラップして再スローするなどもできる。
                when (throwable) {
                    is SQLException -> {
                        log.error("Error in selectModern", throwable)
                    }
                }
            }
            // 例外発生時に、代わりの戻り値を生成する処理
            .recover { throwable ->
                when (throwable) {
                    is SQLException -> {
                        listOf()
                    }
                    else -> {
                        throw throwable
                    }
                }
            }
            // finally 処理
            .also {
                s00UsersIterator.close()
            }
            // 正常完了の場合、リストを返し、例外が発生していたら、スローする
            // その他にも、getOrNull()、getOrElse { ... } や、getOrDefault() などもある。
            .getOrThrow()
    }

    /**
     * Row をストリーム化する拡張関数
     *
     * 近代的な記述方法で利用する
     */
    private fun C00S00UsersIterator.asRowSequence(): Sequence<C00S00UsersRow> {
        // 検索実行
        this.executeQuery()
        // generateSequence は、戻り値が null の場合に完了するストリームを生成する。
        return generateSequence {
            // takeIf は、条件式が true の場合、C00S00UsersIterator を返し、それ以外は null を返す。
            // ?. 演算子は、null でないときだけ row の取得をする。
            this@asRowSequence.takeIf { it.next() }?.row
        }
    }

    /**
     * Row を Users に変換する拡張関数
     *
     * 近代的な記述方法で利用する
     *
     * Users の生成で、名前付きパラメータを利用することで、
     * 代入ミスの発生が抑制され、変更耐性も向上する。
     */
    private fun C00S00UsersRow.toUser() = Users(
        userId = this.userId,
        userName = this.userName,
        password = this.password,
        email = this.email,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    /**
     * 条件付きユーザ検索
     *
     * この関数は、blancoDB オブジェクトを呼び出し毎に生成する。
     * 性能に影響するため、複数回呼び出す処理には利用しないこと。
     *
     * @param userId ユーザID
     * @param userName ユーザ名
     * @param password パスワード
     * @param email E-mail
     * @param emailIncludeNull 空 E-mail を条件に含めるフラグ
     * @param createdAt 作成日時
     * @param updatedAt 更新日時
     * @param sort ソート句
     * @return ユーザリストを返す。
     */
    fun selectCondition(
        userId: Int?,
        userName: String?,
        password: String?,
        email: String?,
        emailIncludeNull: Boolean,
        createdAt: Date?,
        updatedAt: Date?,
        sort: String?
    ): List<Users> {
        log.info(
            "userId: $userId, userName: $userName, password: $password, email: $password," +
                " emailIncludeNull: $emailIncludeNull, createdAt: $createdAt, updatedAt: $updatedAt, sort: $sort")
        val s01UsersIterator = C00S01UsersIterator(con)
        return runCatching {
            if (sort != null) {
                // ソート条件が指定されている場合
                val sql = s01UsersIterator.query
                    // 実際に動的 SQL を利用する場合は、受け取ったパラメータをそのまま代入しないこと。
                    // SQL インジェクションの危険性があり、セキュリティリスクとなる。
                    // 推奨する実装は、パラメータのチェックを行い、想定した値のみを受け付けるようにする。
                    .replace("""/\*replace1\*/""".toRegex(), "ORDER BY $sort")
                log.info("sql: $sql")
                s01UsersIterator.prepareStatement(sql)
            }
            s01UsersIterator.setInputParameter(
                userId, userName, password, email, emailIncludeNull, createdAt, updatedAt
            )
            s01UsersIterator.executeQuery()
            s01UsersIterator.toUsersList()
        }.also {
            // finally 処理
            s01UsersIterator.close()
        }.getOrThrow()
    }

    /**
     * S01 ユーザリストに変換する拡張関数
     *
     * @return ユーザリストを返す。
     */
    private fun C00S01UsersIterator.toUsersList(): List<Users> = generateSequence {
        this.takeIf { it.next() }?.row
    }.map { row ->
        Users(
            userId = row.userId,
            userName = row.userName,
            password = row.password,
            email = row.email,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt
        )
    }.toList()

    /**
     * ユーザ情報登録
     *
     * この関数は、blancoDB オブジェクトを呼び出し毎に生成する。
     * 性能に影響するため、複数回呼び出す処理には利用しないこと。
     *
     * @param userId ユーザID
     * @param userName ユーザ名
     * @param password パスワード
     * @param email E-mail
     */
    fun insert(userId: Int, userName: String, password: String, email: String?) {
        val invoker = C00I00UsersInvoker(con)
        runCatching {
            invoker.setInputParameter(userId, userName, password, email)
            invoker.executeSingleUpdate()
        }.also {
            invoker.close()
        }
    }

    /**
     * ユーザ情報登録（全パラメータ指定）
     *
     * この関数は、blancoDB オブジェクトを呼び出し毎に生成する。
     * 性能に影響するため、複数回呼び出す処理には利用しないこと。
     *
     * @param userId ユーザID
     * @param userName ユーザ名
     * @param password パスワード
     * @param email E-mail
     * @param createdAt 作成日時
     * @param updatedAt 更新日時
     */
    fun insertFull(userId: Int, userName: String, password: String, email: String?, createdAt: Date?, updatedAt: Date?) {
        val i01UsersFullInvoker = C00I01UsersFullInvoker(con)
        i01UsersFullInvoker.setInputParameter(userId, userName, password, email, createdAt, updatedAt)
        i01UsersFullInvoker.executeSingleUpdate()
    }

    /**
     * ユーザ情報登録または、更新
     *
     * この関数は、blancoDB オブジェクトを呼び出し毎に生成する。
     * 性能に影響するため、複数回呼び出す処理には利用しないこと。
     *
     * @param userId ユーザID
     * @param userName ユーザ名
     * @param password パスワード
     * @param email E-mail
     * @return 登録・更新件数を返す。ただし、更新時には、1 件の更新が、2 件とカウントされるので注意。
     */
    fun insertOrUpdate(userId: Int, userName: String, password: String, email: String?): Int {
        val invoker = C00I02UsersInsertOrUpdateInvoker(con)
        return runCatching {
            invoker.setInputParameter(userId, userName, password, email)
            invoker.executeUpdate()
        }.also { invoker.close() }
            .getOrThrow()
    }

    /**
     * ユーザ情報登録または、更新
     *
     * この関数は、blancoDB オブジェクトを呼び出し毎に生成する。
     * 性能に影響するため、複数回呼び出す処理には利用しないこと。
     *
     * @param users ユーザ情報
     */
    fun insertOrUpdate(users: Users) {
        insertOrUpdate(
            userId = users.userId,
            userName = users.userName,
            password = users.password,
            email = users.email
        )
    }

    /**
     * ユーザ検索（条件付き）
     *
     * この関数は、ブロック処理が完了するまで、blancoDB オブジェクトを使い回すため、大量のアクセスに利用できる。
     *
     * @param block 検索条件を生成し、iterate 関数を呼び出し、検索結果を処理するブロック
     */
    suspend fun select(
        // ブロックの型を、インターフェースの拡張関数として定義していることに注目
        block: suspend BatchQueryIterateScope<C00S01UsersIteratorConditions, List<Users>>.() -> Unit
    ): Unit {
        val iterator = C00S01UsersIterator(con)
        runCatching {
            // 無名オブジェクトを定義し、iterate 関数を実装する。
            object : BatchQueryIterateScope<C00S01UsersIteratorConditions, List<Users>> {
                override fun iterate(conditions: C00S01UsersIteratorConditions): List<Users> {
                    if (conditions.sort != null) {
                        // ソート条件が指定されている場合
                        val sql = iterator.query
                            // 実際に動的 SQL を利用する場合は、受け取ったパラメータをそのまま代入しないこと。
                            // SQL インジェクションの危険性があり、セキュリティリスクとなる。
                            // 推奨する実装は、パラメータのチェックを行い、想定した値のみを受け付けるようにする。
                            .replace("""/\*replace1\*/""".toRegex(), "ORDER BY ${conditions.sort}")
                        log.info("sql: $sql")
                        iterator.prepareStatement(sql)
                    }
                    // 検索条件を設定
                    iterator.setInputParameter(
                        conditions.userId,
                        conditions.userName,
                        conditions.password,
                        conditions.email,
                        conditions.emailIncludeNull,
                        conditions.createdAt,
                        conditions.updatedAt
                    )
                    // 検索実行
                    iterator.executeQuery()
                    // 検索結果を返す
                    return iterator.toUsersList()
                }
            }
                // 引数の関数ブロックを、この無名オブジェクトの関数として呼び出す。
                .block()
        }.also {
            // finally 処理
            iterator.close()
        }
    }

    /**
     * ユーザ存在検索（条件付き）
     *
     * この関数は、ブロック処理が完了するまで、blancoDB オブジェクトを使い回すため、大量のアクセスに利用できる。
     *
     * @param block 検索条件を生成し、iterate 関数を呼び出し、検索結果を処理するブロック
     */
    suspend fun selectExists(
        // ブロックの型を、インターフェースの拡張関数として定義していることに注目
        block: suspend BatchQueryIterateScope<C00S02UsersIteratorConditions, Boolean>.() -> Unit
    ): Unit {
        val iterator = C00S02UsersIterator(con)
        iterator.runCatching {
            // 無名オブジェクトを定義し、iterate 関数を実装する。
            object : BatchQueryIterateScope<C00S02UsersIteratorConditions, Boolean> {
                override fun iterate(conditions: C00S02UsersIteratorConditions): Boolean {
                    // 検索条件を設定
                    iterator.setInputParameter(
                        conditions.userIdFrom,
                        conditions.userIdTo,
                        conditions.userName,
                        conditions.password,
                        conditions.email,
                        conditions.emailIncludeNull,
                        conditions.createdAtFrom,
                        conditions.createdAtTo,
                        conditions.updatedAtFrom,
                        conditions.updatedAtTo
                    )
                    // 検索実行
                    iterator.executeQuery()
                    // レコード存在の有無を返す
                    return iterator.next()
                }
            }
                // 引数の関数ブロックを、この無名オブジェクトの関数として呼び出す。
                .block()
        }.also {
            // finally 処理
            iterator.close()
        }
    }

    /**
     * ユーザ情報登録または、更新
     *
     * この関数は、ブロック処理が完了するまで、blancoDB オブジェクトを使い回すため、大量のアクセスに利用できる。
     *
     * @param block ユーザ情報を生成して、invoke 関数を呼び出す処理
     */
    fun insertOrUpdate(
        // ブロックの型を、インターフェースの拡張関数として定義していることに注目
        block: BatchQueryInvokeScope<Users>.() -> Unit
    ): Unit {
        // 登録・更新用オブジェクト生成
        val invoker = C00I02UsersInsertOrUpdateInvoker(con)
        log.info("Start insertOrUpdate")
        runCatching {
            // 無名オブジェクトを定義し、invoke 関数を実装する
            object : BatchQueryInvokeScope<Users> {
                override fun invoke(data: Users) {
                    invoker.setInputParameter(data.userId, data.userName, data.password, data.email)
                    invoker.executeUpdate()
                }
            }
                // 引数の関数ブロックを、この無名オブジェクトの関数として呼び出す。
                .block()
        }.also {
            // 登録・更新用オブジェクトクローズ
            invoker.close()
        }
        log.info("Finish insertOrUpdate")
    }
}

/**
 * 連続登録・更新クエリ実行用スコープ
 *
 * @param T 登録・更新データ
 */
interface BatchQueryInvokeScope<T> {
    fun invoke(data: T)
}

/**
 * 連続検索クエリ実行用スコープ
 *
 * @param T 条件データ
 * @param R 検索結果
 */
interface BatchQueryIterateScope<T, R> {
    fun iterate(conditions: T): R
}

/**
 * S01 ユーザ検索用条件
 *
 * 検索条件識別子は、業務処理で利用する想定。（実際には各種情報を格納する Bean 等になるかも）
 * 検索では利用せず、検索結果を処理する時の判定などに利用する。
 *
 * @property conditionId 検索条件識別子
 * @property userId ユーザID
 * @property userName ユーザ名
 * @property password パスワード
 * @property email E-mail
 * @property emailIncludeNull E-mail が null のレコードを検索結果に含めるフラグ
 * @property createdAt 作成日時
 * @property updatedAt 更新日時
 * @property sort ソート条件
 */
data class C00S01UsersIteratorConditions(
    val conditionId: Int,
    val userId: Int?,
    val userName: String?,
    val password: String?,
    val email: String?,
    val emailIncludeNull: Boolean,
    val createdAt: Date?,
    val updatedAt: Date?,
    val sort: String?
)

/**
 * S02 ユーザ検索用条件
 *
 * 検索条件識別子は、業務処理で利用する想定。（実際には各種情報を格納する Bean 等になるかも）
 * 検索では利用せず、検索結果を処理する時の判定などに利用する。
 *
 * @property conditionId 検索条件識別子
 * @property userIdFrom ユーザID From
 * @property userIdTo ユーザID To
 * @property userName ユーザ名
 * @property password パスワード
 * @property email E-mail
 * @property emailIncludeNull E-mail が null のレコードを検索結果に含めるフラグ
 * @property createdAtFrom 作成日時 From
 * @property createdAtTo 作成日時 To
 * @property updatedAtFrom 更新日時 From
 * @property updatedAtTo 更新日時 To
 */
data class C00S02UsersIteratorConditions(
    val conditionId: Int,
    val userIdFrom: Int?,
    val userIdTo: Int?,
    val userName: String?,
    val password: String?,
    val email: String?,
    val emailIncludeNull: Boolean,
    val createdAtFrom: Date?,
    val createdAtTo: Date?,
    val updatedAtFrom: Date?,
    val updatedAtTo: Date?
)
