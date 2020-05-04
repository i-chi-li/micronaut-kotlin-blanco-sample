package micronaut.kotlin.blanco.sample

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Property
import io.micronaut.core.convert.exceptions.ConversionErrorException
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Error
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.http.hateoas.JsonError
import io.micronaut.http.hateoas.Link
import io.micronaut.scheduling.annotation.Scheduled
import org.slf4j.LoggerFactory

@Controller("/handler")
class GlobalHandlerController(
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    /**
     * グローバル例外ハンドラ
     * 未処理例外が発生した場合に処理をする。
     * 任意の例外のみを処理することもできる。
     *
     * ハンドラは、グローバルであっても、必ず Controller クラス内に定義する必要がある。
     *
     * @param request HTTP リクエスト
     * @param e 例外
     * @return HTTP レスポンスを返す。
     */
    @Error(global = true)
    fun globalErrorHandler(request: HttpRequest<*>, e: Throwable): HttpResponse<JsonError> {
        log.info("Start globalErrorHandler()", e)
        logRequest(request)
        val error = JsonError("Uncatched Exception: ${e.message}")
            .path(e.javaClass.canonicalName)
            .link(Link.SELF, Link.of(request.uri))
        return HttpResponse.serverError<JsonError>()
            .body(error)
    }

    /**
     * グローバル例外ハンドラ
     * リクエスト時のパラメータ変換例外を処理する。
     *
     * @param request HTTP リクエスト
     * @param e パラメータ変換例外
     * @return HTTP レスポンスを返す。
     */
    @Error(global = true)
    fun globalConversionErrorExceptionHandler(request: HttpRequest<*>, e: ConversionErrorException): HttpResponse<JsonError> {
        log.info("Start globalConversionErrorExceptionHandler()", e)
        logRequest(request)
        val error = JsonError("ConversionErrorException: ${e.message}")
            .path(e.javaClass.canonicalName)
            .link(Link.SELF, Link.of(request.uri))
        return HttpResponse.serverError<JsonError>()
            .body(error)
    }

    /**
     * リクエストをロギング
     *
     * @param request リクエスト
     */
    private fun logRequest(request: HttpRequest<*>) {
        // request を丸ごと変換すると、循環参照が原因で「JsonMappingException: Infinite recursion」 が発生する。
        log.info("Request: {" +
            "uri: ${objectMapper.writeValueAsString(request.uri)}" +
            ", path: ${objectMapper.writeValueAsString(request.path)}" +
            ", method: ${objectMapper.writeValueAsString(request.methodName)}" +
            ", headers: ${objectMapper.writeValueAsString(request.headers)}" +
            ", body: ${objectMapper.writeValueAsString(request.body)}" +
            ", parameters: ${objectMapper.writeValueAsString(request.parameters)}" +
            ", cookies: ${objectMapper.writeValueAsString(request.cookies)}" +
            "}")
    }

    /**
     * グローバルステータスハンドラ
     * 処理で、任意のステータスを返した場合に処理をする。
     * 未処理例外時の INTERNAL_SERVER_ERROR は、このハンドラでは処理できない。
     * 処理から、明示的に INTERNAL_SERVER_ERROR を返した場合のみ、このハンドラで処理できる。
     *
     * @param request
     * @return
     */
    @Error(global = true, status = HttpStatus.INTERNAL_SERVER_ERROR)
    fun globalStatusHandler(request: HttpRequest<*>): HttpResponse<JsonError> {
        log.info("Start globalStatusHandler()")
        val error = JsonError("Global Status Handler")
            .link(Link.SELF, Link.of(request.uri))
        return HttpResponse.notFound<JsonError>()
            .body(error)
    }

    @Get("/status")
    @Produces(MediaType.APPLICATION_JSON)
    fun status(): HttpResponse<JsonError> {
        return HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error.")
    }
}

@Controller
class ScheduleController() {
    private val log = LoggerFactory.getLogger(this.javaClass)

    // この値は、環境変数の値を除外している。
    // Application.kt で除外指定をしている。
    @field:Property(name = "db.secrets")
    protected var secrets: String? = null

    @field:Property(name = "db.host")
    protected var host: String? = null

    @field:Property(name = "db.hostRead")
    protected var hostRead: String? = null

    @field:Property(name = "db.port")
    protected var port: String? = null

    @field:Property(name = "db.dbname")
    protected var dbname: String? = null

    @field:Property(name = "db.username")
    protected var username: String? = null

    @field:Property(name = "db.password")
    protected var password: String? = null

    @Scheduled(initialDelay = "0m")
    internal fun showDbInfo(
    ) {
        log.info("Start showDbInfo")
        log.info("secrets: [$secrets], host: [$host], hostRead: [$hostRead], port: [$port], dbname: [$dbname], username: [$username], password: [$password]")
        log.info("CPU: ${Runtime.getRuntime().availableProcessors()}," +
            " maxMemory, ${Runtime.getRuntime().maxMemory()}," +
            " totalMemory: ${Runtime.getRuntime().totalMemory()}," +
            " freeMemory: ${Runtime.getRuntime().freeMemory()}")
    }
}
