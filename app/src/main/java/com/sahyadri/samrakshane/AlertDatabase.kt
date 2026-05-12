package com.sahyadri.samrakshane

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AlertEntity::class], version = 1, exportSchema = false)
abstract class AlertDatabase : RoomDatabase() {

    abstract fun alertDao(): AlertDao

    companion object {
        @Volatile
        private var instance: AlertDatabase? = null

        fun getInstance(context: Context): AlertDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AlertDatabase::class.java,
                    "alert_database"
                ).fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
        }
    }
}