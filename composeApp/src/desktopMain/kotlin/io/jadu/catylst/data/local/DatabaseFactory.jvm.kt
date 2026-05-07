package io.jadu.catylst.data.local

import androidx.room3.Room
import androidx.room3.RoomDatabase
import java.io.File

actual fun createAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "catylst.db")
    return Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
}
