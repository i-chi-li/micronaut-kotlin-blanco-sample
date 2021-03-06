package micronaut.kotlin.blanco.sample

import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Produces
import micronaut.kotlin.blanco.sample.datasource.UserDatasource
import micronaut.kotlin.blanco.sample.model.Users
import micronaut.kotlin.blanco.sample.model.UsersView
import micronaut.kotlin.blanco.sample.model.toUsersView
import org.slf4j.LoggerFactory
import java.time.ZoneId
import javax.sql.DataSource

/**
 * ユーザ管理コントローラ
 *
 * @property dataSource DB データソース
 */
@Controller("/users")
class UsersController(
    private val dataSource: DataSource
) {
    /** ロガー */
    private val log = LoggerFactory.getLogger(this.javaClass)

    /**
     * ユーザ全件取得
     *
     * curl -i http://localhost:8080/users/list
     *
     * @return 全ユーザのリストを返す。
     */
    @Get("/list")
    @Produces(MediaType.APPLICATION_JSON)
    fun listAllUsers(): List<Users> {
        return dataSource.connection.use { connection ->
            val userDatasource = UserDatasource(connection)
            userDatasource.selectModern()
        }
    }

    /**
     * 条件付きユーザ検索
     *
     * curl -i "http://localhost:8080/users?emailIncludeNull=true&sort=user_id%20desc"
     *
     * @param userId ユーザID
     * @param userName ユーザ名
     * @param password パスワード
     * @param email E-mail
     * @param emailIncludeNull 空 E-mail を条件に含めるフラグ
     * @param sort ソート句
     * @return ユーザのリストを返す。
     */
    @Get("/{?userId,userName,password,email,emailIncludeNull,createdAt,updatedAt,sort}")
    @Produces(MediaType.APPLICATION_JSON)
    fun findUsers(
        userId: Int?, userName: String?, password: String?, email: String?, emailIncludeNull: String?,
        createdAt: String?, updatedAt: String?, timeZone: String?, sort: String?
    ): List<UsersView> {
        log.info(
            "userId: $userId," + " userName: $userName," + " password: $password," + " email: $email," +
                " emailIncludeNull: $emailIncludeNull, createdAt: $createdAt, updatedAt: $updatedAt, timeZone: $timeZone, sort: $sort")
        // 空 E-mail を条件に含めるフラグ
        val emailIncludeNullFlag = emailIncludeNull != null
        val zoneId = timeZone?.toZoneId() ?: ZoneId.of("Asia/Tokyo")
        // 作成日時
        val createdAtDate = createdAt?.toDate(zoneId)
        // 更新日時
        val updatedAtDate = updatedAt?.toDate(zoneId)
        return dataSource.connection.use { connection ->
            val userDatasource = UserDatasource(connection)
            userDatasource.selectCondition(
                userId, userName, password, email, emailIncludeNullFlag, createdAtDate, updatedAtDate, sort)
                .map { it.toUsersView(zoneId) }
        }
    }

    /**
     * ユーザ登録（登録日時自動設定）
     *
     * curl -i "http://localhost:8080/users" -X POST -H "Content-Type:Application/json" -d '{"userId":4,"userName":"foo","password":"pass005"}'
     *
     * @param users ユーザ情報
     * @return 結果を返す。
     */
    @Post("/")
    fun post(@Body users: Users): String {
        log.info("users: $users")
        dataSource.connection.use { connection ->
            val userDatasource = UserDatasource(connection)
            userDatasource.insert(users.userId, users.userName, users.password, users.email)
            connection.commit()
        }
        return "OK"
    }

    /**
     * ユーザ登録（全カラム指定）
     *
     * curl -i "http://localhost:8080/users/full" -X POST -H "Content-Type:Application/json" -d '{"userId":5,"userName":"full","password":"pass006", "createdAt": "2020-04-12T10:20:30", "updatedAt": "2020-04-12T20:30:40"}'
     *
     * @param users ユーザ情報
     * @return 結果を返す。
     */
    @Post("/full")
    fun postFull(@Body users: Users): String {
        log.info("users: $users")
        dataSource.connection.use { connection ->
            val userDatasource = UserDatasource(connection)
            userDatasource.insertFull(
                users.userId, users.userName, users.password, users.email, users.createdAt, users.updatedAt)
            connection.commit()
        }
        return "OK"
    }

    /**
     * ユーザ情報を登録または、更新する
     *
     * curl -i "http://localhost:8080/users/insert_update" -X POST -H "Content-Type:Application/json" -d '{"userId":6,"userName":"bar","password":"pass007"}'
     *
     * @param users ユーザ情報
     * @return 結果を返す。
     */
    @Post("/insert_update")
    fun postInsertUpdate(@Body users: Users): String {
        log.info("users: $users")
        dataSource.connection.use { connection ->
            val userDatasource = UserDatasource(connection)
            userDatasource.insertOrUpdate(
                users.userId, users.userName, users.password, users.email)
            connection.commit()
        }
        return "OK"
    }
}
