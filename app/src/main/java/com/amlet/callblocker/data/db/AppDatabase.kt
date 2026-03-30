package com.amlet.callblocker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ContactEntity::class, BlockedCallEntity::class],
    version = 2,           // Incrementato da 1 a 2 per la nuova tabella
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contactDao(): ContactDao
    abstract fun blockedCallDao(): BlockedCallDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "callblocker.db"
                )
                    // fallbackToDestructiveMigration: in dev è ok, ricrea il DB
                    // se vai in produzione sostituisci con una Migration vera
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
