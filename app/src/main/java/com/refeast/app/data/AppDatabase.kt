package com.refeast.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [User::class, FoodListing::class, Reservation::class, StatusLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun foodDao(): FoodDao
    abstract fun reservationDao(): ReservationDao

    companion object {
        private var instance: AppDatabase? = null

        /** Returns the one shared database (it is only created the first time). */
        fun getInstance(context: Context): AppDatabase {
            if (instance == null) {
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "refeast.db"
                )
                    // Our data is small, so we keep the code simple and run queries
                    // directly on the main thread. A bigger app would use coroutines.
                    .allowMainThreadQueries()
                    .build()
            }
            return instance!!
        }
    }
}
