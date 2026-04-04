package com.amlet.callblocker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ContactEntity::class, BlockedCallEntity::class],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contactDao(): ContactDao
    abstract fun blockedCallDao(): BlockedCallDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** v2 → v3: adds simSlot column. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE blocked_calls ADD COLUMN simSlot TEXT DEFAULT NULL"
                )
            }
        }

        /**
         * v3 → v4: normalises all existing phoneNumber values in blocked_calls
         * (strips +, spaces, dashes, then trims leading zeros) to fix the
         * "0 attempts" bug in CallDetailScreen.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    UPDATE blocked_calls
                    SET phoneNumber = LTRIM(
                        REPLACE(REPLACE(REPLACE(phoneNumber, '+', ''), ' ', ''), '-', ''),
                        '0'
                    )
                    WHERE phoneNumber IS NOT NULL
                    """.trimIndent()
                )
            }
        }

        /**
         * v4 → v5: adds callDirection and accountHandleId columns
         * for richer per-attempt info in CallDetailScreen.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE blocked_calls ADD COLUMN callDirection TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE blocked_calls ADD COLUMN accountHandleId TEXT DEFAULT NULL")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "callblocker.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
