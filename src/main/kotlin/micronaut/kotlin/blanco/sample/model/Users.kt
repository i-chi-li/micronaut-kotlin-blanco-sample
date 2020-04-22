package micronaut.kotlin.blanco.sample.model

import java.util.*

/**
 * ユーザ情報
 *
 * @property userId ユーザID
 * @property userName ユーザ名
 * @property password パスワード
 * @property email E-mail
 * @property createdAt 作成日時
 * @property updatedAt 更新日時
 */
data class Users(
    val userId: Int,
    val userName: String,
    val password: String,
    val email: String? = null,
    val createdAt: Date? = Date(Long.MIN_VALUE),
    val updatedAt: Date? = Date(Long.MIN_VALUE)
)
