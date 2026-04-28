package com.example.royalfreshapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ScheduleEntity::class], version = 3, exportSchema = false)
abstract class ScheduleDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: ScheduleDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE schedules_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, timeRange TEXT NOT NULL, frequency TEXT NOT NULL, deviceId TEXT NOT NULL, workingTime INTEGER NOT NULL DEFAULT 60, pauseTime INTEGER NOT NULL DEFAULT 20, isOn INTEGER NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO schedules_new (id, timeRange, frequency, deviceId, workingTime, pauseTime, isOn) SELECT id, timeRange, frequency, deviceId, 60 as workingTime, 20 as pauseTime, isOn FROM schedules"
                )
                db.execSQL("DROP TABLE schedules")
                db.execSQL("ALTER TABLE schedules_new RENAME TO schedules")
            }
        }

        fun getDatabase(context: Context): ScheduleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScheduleDatabase::class.java,
                    "schedule_database"
                ).addMigrations(MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
