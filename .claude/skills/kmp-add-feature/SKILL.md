---
name: kmp-add-feature
description: Add a full feature to Catylst KMP — Entity, DAO, Room database, Repository, ViewModel, and Screen — end-to-end with Koin wiring
---

# kmp-add-feature

Adds a complete feature following the MVVM + Repository pattern used across Catylst.
All files go in `composeApp/src/commonMain/` unless noted.

Replace `Xxx` / `xxx` throughout with the feature name.

## Step 1 — Entity

Create `data/local/XxxEntity.kt`:

```kotlin
package io.jadu.catylst.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "xxx")
data class XxxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
```

## Step 2 — DAO

Create `data/local/XxxDao.kt`:

```kotlin
package io.jadu.catylst.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface XxxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: XxxEntity)

    @Query("SELECT * FROM xxx ORDER BY createdAt DESC")
    fun getAll(): Flow<List<XxxEntity>>

    @Query("DELETE FROM xxx WHERE id = :id")
    suspend fun delete(id: Long)
}
```

## Step 3 — Register DAO in AppDatabase

Open `data/local/AppDatabase.kt`. Add:

```kotlin
abstract fun xxxDao(): XxxDao
```

## Step 4 — Repository

Create `data/repository/XxxRepository.kt`:

```kotlin
package io.jadu.catylst.data.repository

import io.jadu.catylst.data.local.XxxDao
import io.jadu.catylst.data.local.XxxEntity
import kotlinx.coroutines.flow.Flow

class XxxRepository(private val dao: XxxDao) {
    fun getAll(): Flow<List<XxxEntity>> = dao.getAll()
    suspend fun add(name: String) = dao.insert(XxxEntity(name = name))
    suspend fun remove(id: Long) = dao.delete(id)
}
```

## Step 5 — Register in AppModule

Open `di/AppModule.kt`. Add:

```kotlin
single { XxxRepository(get<AppDatabase>().xxxDao()) }
```

## Step 6 — ViewModel

Create `ui/viewmodel/XxxViewModel.kt`:

```kotlin
package io.jadu.catylst.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.jadu.catylst.data.repository.XxxRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class XxxViewModel(private val repo: XxxRepository) : ViewModel() {
    val items = repo.getAll().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun add(name: String) { viewModelScope.launch { repo.add(name) } }
    fun remove(id: Long)  { viewModelScope.launch { repo.remove(id) } }
}
```

Register in `AppModule.kt`:
```kotlin
viewModel { XxxViewModel(get()) }
```

## Step 7 — Screen

Follow the `kmp-add-screen` skill to create the composable, register in `Screen.kt`,
wire navigation in `AppNavigation.kt`, and add the button in `HomeScreen.kt`.

Inject the ViewModel in the composable:
```kotlin
val viewModel: XxxViewModel = koinViewModel()
val items by viewModel.items.collectAsState()
```

## Step 8 — Regenerate Room code

```bash
./gradlew :composeApp:kspAndroidMain
```

This must run after any Entity or DAO change. Then build:

```bash
./gradlew :androidApp:assembleDebug
```
