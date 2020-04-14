package micronaut.kotlin.blanco.sample

import io.micronaut.core.convert.ConversionContext
import io.micronaut.core.convert.TypeConverter
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.http.annotation.QueryValue
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Singleton

/**
 * 日時変換サンプル
 */
@Controller("/date")
class DateController {
    private val log = LoggerFactory.getLogger(this.javaClass)

    @Get("/now")
    @Produces(MediaType.APPLICATION_JSON)
    fun now(): Date {
        return Date()
    }

    @Get("/localDateTime")
    @Produces("${MediaType.APPLICATION_JSON}; ${MediaType.CHARSET_PARAMETER}=utf-8")
    fun localDateTime(@QueryValue localDateTime: LocalDateTime): String {
        return localDateTime.toString()
    }

    @Get("/localDateTime")
    @Produces("${MediaType.APPLICATION_JSON}; ${MediaType.CHARSET_PARAMETER}=utf-8")
    fun dateBean(@QueryValue date: DateBean): DateBean {
        log.info("data: $date")
        return date
    }
}

/**
 * 日時格納用
 */
class DateBean(
    val date: Date?,
    val localDateTime: LocalDateTime?,
    val zonedDateTime: ZonedDateTime?,
    val timeZone: TimeZone?
) {
    fun localToZoned(): ZonedDateTime? {
        return timeZone?.let {
            localDateTime?.atZone(it.toZoneId())
        }
    }

    override fun toString(): String {
        return "date: $date, localDateTime: $localDateTime, zonedDateTime: $zonedDateTime, timeZone: $timeZone, localToZoned: ${localToZoned()}"
    }
}

/**
 * JSON 変換で、String 型を LocalDateTime 型に変換するために必要
 */
@Singleton
class LocalDateTimeConverter : TypeConverter<String, LocalDateTime> {
    private val log = LoggerFactory.getLogger(this.javaClass)
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun convert(
        str: String?, targetType: Class<LocalDateTime>?, context: ConversionContext?): Optional<LocalDateTime> {
        log.info("Start convert String to LocalDateTime")
        return Optional.ofNullable(str?.let {
            LocalDateTime.parse(str, formatter)
        })
    }
}

/**
 * JSON 変換で、String 型を ZonedDateTime 型に変換するために必要
 */
@Singleton
class ZonedDateTimeConverter : TypeConverter<String, ZonedDateTime> {
    private val log = LoggerFactory.getLogger(this.javaClass)
    private val formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME

    override fun convert(
        str: String?, targetType: Class<ZonedDateTime>?, context: ConversionContext?): Optional<ZonedDateTime> {
        log.info("Start convert String to LocalDateTime")
        return Optional.ofNullable(str?.let {
            ZonedDateTime.parse(str, formatter)
        })
    }
}


/**
 * 文字列を ZoneId に変換
 *
 * @return ZoneId インスタンスを返す。
 */
fun String.toZoneId(): ZoneId {
    return TimeZone.getTimeZone(this).toZoneId()
}

/**
 * 日時文字列を Date 型に変換
 *
 * @param zoneId ゾーンID。null または、未指定の場合は、「Asia/Tokyo」となる。
 * @param formatter 日時パースフォーマット
 * @return Date 型日時インスタンスを返す。
 */
fun String.toDate(
    zoneId: ZoneId? = null,
    formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
): Date {
    // Date 型に変換
    return Date.from(
        // 入力文字列をタイムゾーン無しの日時にパースする
        LocalDateTime.parse(this, formatter)
            // タイムゾーンありの日時に変換
            .atZone(zoneId ?: ZoneId.of("Asia/Tokyo"))
            // Instant 型に変換
            .toInstant()
    )
}

/**
 * Date 型をタイムゾーン指定の文字列に変換
 *
 * @param zoneId タイムゾーンID
 * @param formatter フォーマッタ
 * @return 文字列を返す.
 */
fun Date.toZonedString(
    zoneId: ZoneId? = null,
    formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
): String {
    return formatter
        .withZone(zoneId ?: ZoneId.of("Asia/Tokyo"))
        .format(this.toInstant())
}

/**
 * Date 型をローカル日時文字列に変換
 *
 * @param zoneId タイムゾーンID
 * @param formatter フォーマッタ
 * @return 文字列を返す.
 */
fun Date.toLocalString(
    zoneId: ZoneId? = null,
    formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
): String {
    return toInstant()
        .atZone(zoneId?:ZoneId.of("Asia/Tokyo"))
        .toLocalDateTime()
        .format(formatter)
}
