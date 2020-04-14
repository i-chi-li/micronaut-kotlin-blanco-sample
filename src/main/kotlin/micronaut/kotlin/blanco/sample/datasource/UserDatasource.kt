package micronaut.kotlin.blanco.sample.datasource

import micronaut.kotlin.blanco.sample.blanco.db.users.query.*
import micronaut.kotlin.blanco.sample.blanco.db.users.row.C00S00UsersRow
import micronaut.kotlin.blanco.sample.model.Users
import org.slf4j.LoggerFactory
import java.sql.Connection
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
        return try {
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
        } finally {
            s00UsersIterator.close()
        }
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
        return try {
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
            generateSequence {
                s01UsersIterator.takeIf { it.next() }?.row
            }.map { row ->
                println("createdAt: ${row.createdAt}")
                Users(
                    userId = row.userId,
                    userName = row.userName,
                    password = row.password,
                    email = row.email,
                    createdAt = row.createdAt,
                    updatedAt = row.updatedAt
                )
            }.toList()
        } finally {
            s01UsersIterator.close()
        }
    }

    /**
     * ユーザ情報登録
     *
     * @param userId ユーザID
     * @param userName ユーザ名
     * @param password パスワード
     * @param email E-mail
     */
    fun insert(userId: Int, userName: String, password: String, email: String?) {
        val i00UsersInvoker = C00I00UsersInvoker(con)
        i00UsersInvoker.setInputParameter(userId, userName, password, email)
        i00UsersInvoker.executeSingleUpdate()
    }

    /**
     * ユーザ情報登録（全パラメータ指定）
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
     * @param userId ユーザID
     * @param userName ユーザ名
     * @param password パスワード
     * @param email E-mail
     */
    fun insertOrUpdate(userId: Int, userName: String, password: String, email: String?) {
        val i02UsersInsertOrUpdateInvoker = C00I02UsersInsertOrUpdateInvoker(con)
        i02UsersInsertOrUpdateInvoker.setInputParameter(userId, userName, password, email)
        val count = i02UsersInsertOrUpdateInvoker.executeUpdate()
        log.info("count: $count")
    }
}
