package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TravelAllDetailsEntity::class,
        TripEntity::class,
        ItineraryEntity::class,
        ActivityEntity::class,
        BudgetEntity::class,
        ExpenseEntity::class,
        JournalEntity::class,
        PackingItemEntity::class,
        SettlementEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TripMateDatabase : RoomDatabase() {
    abstract fun dao(): TripMateDao

    companion object {
        @Volatile
        private var INSTANCE: TripMateDatabase? = null

        fun getInstance(context: Context): TripMateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TripMateDatabase::class.java,
                    "tripmate_local.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
