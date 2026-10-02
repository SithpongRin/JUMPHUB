package com.example.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SessionEntity::class,
        RoundRecordEntity::class,
        UserPlanEntity::class,
        PlanDefinitionEntity::class,
        ReminderScheduleEntity::class,
        BodyMetricEntity::class,
        PersonalRecordEntity::class,
        DailyStatEntity::class,
        WeeklyStatEntity::class,
        MonthlyStatEntity::class,
        SyncMetadataEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun roundRecordDao(): RoundRecordDao
    abstract fun planDao(): PlanDao
    abstract fun reminderScheduleDao(): ReminderScheduleDao
    abstract fun bodyMetricDao(): BodyMetricDao
    abstract fun personalRecordDao(): PersonalRecordDao
    abstract fun statsDao(): StatsDao
    abstract fun syncMetadataDao(): SyncMetadataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jumphub_database"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
