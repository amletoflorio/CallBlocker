package com.amlet.callblocker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ContactEntity::class, BlockedCallEntity::class],
    version = 9,
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
         * v6 → v7: ricrea blocked_calls con la struttura finale attesa da Room.
         *
         * Il problema: ALTER TABLE ADD COLUMN lascia un DEFAULT fisso nel
         * catalogo SQLite (es. DEFAULT 'incoming', DEFAULT NULL) che Room
         * confronta come stringa e trova diverso da 'undefined'.
         * L'unico modo per eliminare i DEFAULT è ricreare la tabella.
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Crea la nuova tabella con la struttura esatta che Room si aspetta
                //    (nessun DEFAULT sulle colonne nullable, isSmsBlocked NOT NULL senza default
                //     esplicito nel catalogo → Room lo vede come 'undefined').
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

                // 2. Copia i dati esistenti, fornendo valori di default per le colonne nuove
                db.execSQL("""
                    INSERT INTO blocked_calls_new
                        (id, phoneNumber, blockedAt, simSlot, callDirection, accountHandleId,
                         callType, isSmsBlocked, smsBodySnippet)
                    SELECT
                        id, phoneNumber, blockedAt, simSlot, callDirection, accountHandleId,
                        'incoming', 0, NULL
                    FROM blocked_calls
                """.trimIndent())

                // 3. Elimina la vecchia tabella e rinomina la nuova
                db.execSQL("DROP TABLE blocked_calls")
                db.execSQL("ALTER TABLE blocked_calls_new RENAME TO blocked_calls")
            }
        }

        /**
         * v7 → v8: nessuna modifica strutturale necessaria — le colonne callType,
         * isSmsBlocked e smsBodySnippet sono già state create nella migration 6→7.
         * Questa migration è un no-op che serve solo ad allineare il numero di versione.
         */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // no-op: struttura già corretta dalla migration 6→7
            }
        }

        /**
         * v8 → v9: rimuove le colonne isSmsBlocked e smsBodySnippet dalla tabella
         * blocked_calls, non più necessarie ora che la protezione SMS è stata rimossa.
         * SQLite non supporta DROP COLUMN (pre-3.35.0), quindi si ricrea la tabella.
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
                        MIGRATION_8_9
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
