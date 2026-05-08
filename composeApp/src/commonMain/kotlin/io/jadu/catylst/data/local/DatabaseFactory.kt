package io.jadu.catylst.data.local

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

expect fun createAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>

fun createAppDatabase(): AppDatabase {
    return createAppDatabaseBuilder()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
