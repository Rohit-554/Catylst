package io.jadu.catylst.data.local

import androidx.room3.Room
import androidx.room3.RoomDatabase

actual fun createAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = documentDirectory() + "/catylst.db"
    return Room.databaseBuilder<AppDatabase>(name = dbFile)
}
