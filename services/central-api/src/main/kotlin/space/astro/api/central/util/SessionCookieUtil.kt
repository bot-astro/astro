package space.astro.api.central.util

import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

private const val ITERATIONS = 1
private const val KEY_LENGTH = 256
private const val ALGORITHM = "PBKDF2WithHmacSHA1"
private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"

object SessionCookieUtil {
    fun unseal(cookie: String, password: String): String {
        val parts = cookie.split("*")

        val salt = parts[2]
        val ivBase64 = parts[3]
        val dataBase64 = parts[4]

        return decrypt(dataBase64, password, salt, ivBase64)
    }

    private fun decrypt(dataBase64: String, password: String, salt: String, ivBase64: String): String {
        val decodedData = Base64.getUrlDecoder().decode(dataBase64)
        val decodedIv = Base64.getUrlDecoder().decode(ivBase64)

        val ivSpec = IvParameterSpec(decodedIv)

        val secretKey = pbkdf2(password, salt)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)

        return cipher.doFinal(decodedData).decodeToString()
    }

    private fun pbkdf2(
        password: String,
        salt: String
    ): SecretKey {
        return SecretKeySpec(
            SecretKeyFactory.getInstance(ALGORITHM)
                .generateSecret(
                    PBEKeySpec(
                        password.toCharArray(),
                        salt.toByteArray(),
                        ITERATIONS,
                        KEY_LENGTH
                    )
                )
                .encoded,
            "AES"
        )
    }
}
