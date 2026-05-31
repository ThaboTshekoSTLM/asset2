package za.gov.municipal.ictasset.data.local

import java.security.MessageDigest

object PasswordHasher {
    private const val SALT = "municipal-ict-asset-register"

    fun hash(rawPassword: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("$SALT:$rawPassword".toByteArray(Charsets.UTF_8))
        return bytes.joinToString(separator = "") { "%02x".format(it) }
    }
}
