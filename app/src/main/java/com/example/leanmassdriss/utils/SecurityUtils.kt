package com.example.leanmassdriss.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

object SecurityUtils {

    /**
     * Retrieves the 64-character Hexadecimal string representation of the 256-bit key.
     * Secured via Android Keystore.
     */
    fun getDatabaseKeyHex(context: Context): String {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "secure_db_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        var keyHex = sharedPreferences.getString("db_key", null)
        if (keyHex == null) {
            val keyBytes = ByteArray(32) // 256-bit encryption key
            SecureRandom().nextBytes(keyBytes)
            keyHex = keyBytes.joinToString("") { "%02x".format(it) }
            sharedPreferences.edit().putString("db_key", keyHex).apply()
        }
        return keyHex!!
    }
}
