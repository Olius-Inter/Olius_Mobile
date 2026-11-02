package com.olius.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Banco SQLite local do app (Room). */
@Database(entities = [UserProfileEntity::class], version = 1, exportSchema = false)
abstract class OliusDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        private const val NAME = "olius.db"

        /** `allowMainThreadQueries`: a tabela tem uma linha só. */
        fun create(context: Context): OliusDatabase =
            Room.databaseBuilder(context.applicationContext, OliusDatabase::class.java, NAME)
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
