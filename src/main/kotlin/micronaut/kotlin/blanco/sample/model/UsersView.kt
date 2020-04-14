package micronaut.kotlin.blanco.sample.model

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class UsersView(val userId: Int, val userName: String, val password: String, val email: String?,
                val createdAt: Date?, val updatedAt: Date?, val zoneId: ZoneId) {

    val createdAtDisplay: String?
        get() = createdAt?.let {
            DateTimeFormatter.ISO_ZONED_DATE_TIME
                .withZone(zoneId)
                .format(it.toInstant())
        }

    val updatedAtDisplay: String?
        get() = updatedAt?.let {
            DateTimeFormatter.ISO_ZONED_DATE_TIME
                .withZone(zoneId)
                .format(it.toInstant())
        }
}

fun Users.toUsersView(zoneId: ZoneId) = UsersView(userId, userName, password, email, createdAt, updatedAt, zoneId)
