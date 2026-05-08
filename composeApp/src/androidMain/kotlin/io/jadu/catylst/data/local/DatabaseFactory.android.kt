package io.jadu.catylst.data.local

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import org.koin.java.KoinJavaComponent.getKoin

actual fun createAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val context: Context = getKoin().get()
    val dbFile = context.getDatabasePath("catylst.db")
    return Room.databaseBuilder(context, AppDatabase::class.java, dbFile.absolutePath)
}
