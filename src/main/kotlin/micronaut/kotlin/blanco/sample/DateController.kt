package micronaut.kotlin.blanco.sample

import io.micronaut.core.convert.ConversionContext
import io.micronaut.core.convert.TypeConverter
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.OffsetDateTime
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

    /**
     * 現在時刻を UNIX EPOCH「エポック」（1970 年 1 月 1 日 00:00:00）からのミリ秒で取得する
     *
     * curl -i "http://localhost:8080/date/now"
     */
    @Get("/now")
    @Produces(MediaType.APPLICATION_JSON)
    fun now(): Date {
        return Date()
    }

    /**
     * LocalDateTimeConverter で、文字列を LocalDateTime に変換するサンプル
     *
     * curl -i "http://localhost:8080/date/localDateTime?localDateTime=2020-04-12T10:20:30"
     */
    @Get("/localDateTime")
    @Produces("${MediaType.APPLICATION_JSON}; ${MediaType.CHARSET_PARAMETER}=utf-8")
    fun localDateTime(@QueryValue localDateTime: LocalDateTime): String {
        return localDateTime.toString()
    }

    /**
     * 値を Bean で受け取る場合のサンプル
     * Date、 LocalDateTime および、ZonedDateTime フィールドが、変換されるサンプル
     * LocalDateTimeConverter 、ZonedDateTimeConverter および、ZoneIdConverter で変換される。
     *
     * curl -i "http://localhost:8080/date/bean" -X POST -H "Content-Type:application/json" -d '{"date": 1586827230, "localDateTime": "2020-04-14T10:20:30", "zonedDateTime": "2020-04-14T10:20:30+09:00[Asia/Tokyo]", "offsetDateTime": "2020-04-14T10:20:30+09:00", "zoneId": "Asia/Tokyo", "timeZone": "JST"}'
     */
    @Post("/bean")
    @Produces("${MediaType.APPLICATION_JSON}; ${MediaType.CHARSET_PARAMETER}=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    fun dateBean(@Body date: DateBean): DateBean {
        log.info("data: $date")
        return date
    }
}

/**
 * 日時格納用
 */
class DateBean(
    // サンプル値「1586846009893」
    val date: Date?,
    // サンプル値「2020-04-14T10:20:30」
    val localDateTime: LocalDateTime?,
    // サンプル値「2020-04-14T10:20:30+09:00[Asia/Tokyo]」
    val zonedDateTime: ZonedDateTime?,
    // サンプル値「2020-04-14T10:20:30+0900」
    val offsetDateTime: OffsetDateTime?,
    // サンプル値「Asia/Tokyo」。「JST」はエラーとなる
    val zoneId: ZoneId?,
    // サンプル値「JST」
    val timeZone: TimeZone?
) {
    fun localToZoned(): ZonedDateTime? {
        return zoneId?.let {
            localDateTime?.atZone(it)
        }
    }

    override fun toString(): String {
        return "date: $date" +
            ", localDateTime: $localDateTime" +
            ", zonedDateTime: $zonedDateTime" +
            ", offsetDateTime: $offsetDateTime" +
            ", zoneId: $zoneId" +
            ", timeZone: $timeZone" +
            ", localToZoned: ${localToZoned()}"
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
        log.info("Start convert String to ZonedDateTime")
        return Optional.ofNullable(str?.let {
            ZonedDateTime.parse(str, formatter)
        })
    }
}

/**
 * JSON 変換で、String 型を ZoneId 型に変換するために必要
 */
@Singleton
class ZoneIdConverter : TypeConverter<String, ZoneId> {
    private val log = LoggerFactory.getLogger(this.javaClass)

    override fun convert(
        str: String?, targetType: Class<ZoneId>?, context: ConversionContext?): Optional<ZoneId> {
        log.info("Start convert String to ZoneId")
        return Optional.ofNullable(str?.toZoneId())
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
        .atZone(zoneId ?: ZoneId.of("Asia/Tokyo"))
        .toLocalDateTime()
        .format(formatter)
}
