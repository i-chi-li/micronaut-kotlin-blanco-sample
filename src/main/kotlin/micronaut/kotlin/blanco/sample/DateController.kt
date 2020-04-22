package micronaut.kotlin.blanco.sample

import io.micronaut.core.convert.ConversionContext
import io.micronaut.core.convert.TypeConverter
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Produces
import io.micronaut.http.annotation.QueryValue
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
     * curl -i http://localhost:8080/date/now
     */
    @Get("/now")
    @Produces(MediaType.APPLICATION_JSON)
    fun now(): Date {
        // 返る値は、UNIX エポック・ミリ秒
        // この値は、タイムゾーンに影響されない。
        return Date()
    }

    /**
     * LocalDateTimeConverter で、文字列を LocalDateTime に変換するサンプル
     *
     * curl -i "http://localhost:8080/date/localDateTime?localDateTime=2020-04-12T10:20:30"
     */
    @Get("/localDateTime")
    @Produces(MediaType.APPLICATION_JSON)
    fun localDateTime(@QueryValue localDateTime: LocalDateTime): LocalDateTime {
        // 戻り値は、「[2020,4,14,10,20,30]」（年月日時分秒）の、数値配列となる。
        return localDateTime
    }

    /**
     * ZonedDateTimeConverter で、文字列を ZonedDateTime に変換するサンプル
     *
     * curl -i "http://localhost:8080/date/zonedDateTime?zonedDateTime=2020-04-12T10:20:30%2B09:00[Asia/Tokyo]" --globoff
     * 「--globoff」 は、大括弧を含む場合にエスケープするオプション
     * %2B は、「+」記号。そのまま送るとスペースに置き換わってしまう
     */
    @Get("/zonedDateTime")
    @Produces(MediaType.APPLICATION_JSON)
    fun zonedDateTime(@QueryValue zonedDateTime: ZonedDateTime): ZonedDateTime {
        // 戻り値は、「1586827230.000000000」のような、UNIX エポック・ミリ秒と小数点以下（ピコ秒）
        return zonedDateTime
    }

    /**
     * OffsetDateTimeConverter で、文字列を OffsetDateTime に変換するサンプル
     *
     * curl -i "http://localhost:8080/date/offsetDateTime?offsetDateTime=2020-04-12T10:20:30%2B09:00"
     * %2B は、「+」記号。そのまま送るとスペースに置き換わってしまう
     */
    @Get("/offsetDateTime")
    @Produces(MediaType.APPLICATION_JSON)
    fun offsetDateTime(@QueryValue offsetDateTime: OffsetDateTime): OffsetDateTime {
        return offsetDateTime
    }

    /**
     * 値を Bean で受け取る場合のサンプル
     * Date、 LocalDateTime および、ZonedDateTime フィールドが、変換されるサンプル
     * LocalDateTimeConverter 、ZonedDateTimeConverter および、ZoneIdConverter で変換される。
     *
     * curl -i "http://localhost:8080/date/bean" -X POST -H "Content-Type:application/json" -d '{"date": 1586827230, "localDateTime": "2020-04-14T10:20:30", "zonedDateTime": "2020-04-14T10:20:30+09:00[Asia/Tokyo]", "offsetDateTime": "2020-04-14T10:20:30+09:00", "zoneId": "Asia/Tokyo", "timeZone": "JST"}'
     */
    @Post("/bean")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun dateBean(@Body date: DateBean): DateBean {
        // Bean への変換には、ここで定義したコンバータが利用されないようだ。
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
 * JSON 変換で、String 型を OffsetDateTime 型に変換するために必要
 */
@Singleton
class OffsetDateTimeConverter : TypeConverter<String, OffsetDateTime> {
    private val log = LoggerFactory.getLogger(this.javaClass)
    private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    override fun convert(
        str: String?, targetType: Class<OffsetDateTime>?, context: ConversionContext?): Optional<OffsetDateTime> {
        log.info("Start convert String to OffsetDateTime")
        return Optional.ofNullable(str?.let {
            OffsetDateTime.parse(str, formatter)
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

