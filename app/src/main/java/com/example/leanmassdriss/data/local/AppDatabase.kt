package com.example.leanmassdriss.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.leanmassdriss.data.local.dao.LbmRecordDao
import com.example.leanmassdriss.data.local.entity.LbmRecordEntity
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(entities = [LbmRecordEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lbmRecordDao(): LbmRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context, passphrase: String): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                //  Cryptographic decoding: 64-char Hex -> 32-byte binary key
                val safeKey: ByteArray = passphrase.chunked(2)
                    .map { it.toInt(16).toByte() }
                    .toByteArray()

                val factory = SupportOpenHelperFactory(safeKey)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lean_mass_v4_secure"
                )
                .openHelperFactory(factory) // Enforces full AES-256 block encryption via SQLCipher
                .fallbackToDestructiveMigration()
                .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
