package com.amlet.callblocker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ContactEntity::class, BlockedCallEntity::class, ScheduleRuleEntity::class, CategoryEntity::class],
    version = 12,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contactDao(): ContactDao
    abstract fun blockedCallDao(): BlockedCallDao
    abstract fun scheduleRuleDao(): ScheduleRuleDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** v2 → v3: adds simSlot column. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE blocked_calls ADD COLUMN simSlot TEXT DEFAULT NULL")
            }
        }

        /** v3 → v4: normalises existing phoneNumber values. */
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

        /** v4 → v5: adds callDirection and accountHandleId columns. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE blocked_calls ADD COLUMN callDirection TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE blocked_calls ADD COLUMN accountHandleId TEXT DEFAULT NULL")
            }
        }

        /**
         * v5 → v6: adds expiresAt to allowed_contacts for temporary whitelist entries.
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE allowed_contacts ADD COLUMN expiresAt INTEGER DEFAULT NULL")
            }
        }

        /**
         * v6 → v7: recreates blocked_calls with the exact schema Room expects.
         *
         * Problem: ALTER TABLE ADD COLUMN leaves a fixed DEFAULT in the SQLite
         * catalogue (e.g. DEFAULT 'incoming', DEFAULT NULL) that Room compares as
         * a string and finds different from 'undefined'. The only way to remove
         * those DEFAULT markers is to recreate the table from scratch.
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create the new table with the exact structure Room expects
                //    (no DEFAULT on nullable columns; isSmsBlocked NOT NULL without an explicit
                //     default in the catalogue → Room sees it as 'undefined').
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS blocked_calls_new (
                        id               INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        phoneNumber      TEXT    NOT NULL,
                        blockedAt        INTEGER NOT NULL,
                        simSlot          TEXT,
                        callDirection    TEXT,
                        accountHandleId  TEXT,
                        callType         TEXT    NOT NULL,
                        isSmsBlocked     INTEGER NOT NULL,
                        smsBodySnippet   TEXT
                    )
                """.trimIndent())

                // 2. Copy existing rows, supplying default values for the new columns.
                db.execSQL("""
                    INSERT INTO blocked_calls_new
                        (id, phoneNumber, blockedAt, simSlot, callDirection, accountHandleId,
                         callType, isSmsBlocked, smsBodySnippet)
                    SELECT
                        id, phoneNumber, blockedAt, simSlot, callDirection, accountHandleId,
                        'incoming', 0, NULL
                    FROM blocked_calls
                """.trimIndent())

                // 3. Drop the old table and rename the new one.
                db.execSQL("DROP TABLE blocked_calls")
                db.execSQL("ALTER TABLE blocked_calls_new RENAME TO blocked_calls")
            }
        }

        /**
         * v7 → v8: no structural changes needed — callType, isSmsBlocked and
         * smsBodySnippet were already created in migration 6→7.
         * This is a no-op migration that only bumps the version number.
         */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // no-op: schema is already correct after migration 6→7
            }
        }

        /**
         * v8 → v9: removes the isSmsBlocked and smsBodySnippet columns from
         * blocked_calls, no longer needed since SMS protection was removed.
         * SQLite does not support DROP COLUMN (pre-3.35.0), so the table is recreated.
         */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS blocked_calls_new (
                        id               INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        phoneNumber      TEXT    NOT NULL,
                        blockedAt        INTEGER NOT NULL,
                        simSlot          TEXT,
                        callDirection    TEXT,
                        accountHandleId  TEXT,
                        callType         TEXT    NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO blocked_calls_new
                        (id, phoneNumber, blockedAt, simSlot, callDirection, accountHandleId, callType)
                    SELECT
                        id, phoneNumber, blockedAt, simSlot, callDirection, accountHandleId, callType
                    FROM blocked_calls
                """.trimIndent())

                db.execSQL("DROP TABLE blocked_calls")
                db.execSQL("ALTER TABLE blocked_calls_new RENAME TO blocked_calls")
            }
        }

        /**
         * v9 → v10: adds verificationStatus and allowReason to blocked_calls;
         * creates the schedule_rules table.
         */
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE blocked_calls ADD COLUMN verificationStatus TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE blocked_calls ADD COLUMN allowReason TEXT DEFAULT NULL")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS schedule_rules (
                        id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        mode        TEXT    NOT NULL,
                        days        TEXT    NOT NULL,
                        startTime   TEXT    NOT NULL,
                        endTime     TEXT    NOT NULL,
                        enabled     INTEGER NOT NULL DEFAULT 1,
                        simTarget   TEXT    NOT NULL DEFAULT 'both'
                    )
                """.trimIndent())
            }
        }

        /**
         * v10 → v11:
         * - Creates the contact_categories table.
         * - Adds categoryId to allowed_contacts (nullable, no FK constraint).
         * - Adds targetType and targetId to schedule_rules for per-category / per-contact scheduling.
         */
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // New table: contact categories
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS contact_categories (
                        id        INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name      TEXT    NOT NULL,
                        emoji     TEXT    NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // Allow contacts to reference a category
                db.execSQL("ALTER TABLE allowed_contacts ADD COLUMN categoryId INTEGER DEFAULT NULL")

                // Schedule rules can now target a specific category or contact
                db.execSQL("ALTER TABLE schedule_rules ADD COLUMN targetType TEXT NOT NULL DEFAULT 'all'")
                db.execSQL("ALTER TABLE schedule_rules ADD COLUMN targetId INTEGER DEFAULT NULL")
            }
        }

        /** v11 → v12: adds optional user-defined name to schedule rules. */
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE schedule_rules ADD COLUMN name TEXT DEFAULT NULL")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "callblocker.db"
                )
                    .addMigrations(
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
