---
name: bloom-build
description: Build new screens end-to-end in your generated project: composable UI, navigation wiring, ViewModel, Room Entity/DAO/Repository, and Koin DI — all scaffolded in one pass
---

# kmp-build

Use this skill when adding anything new to the Catylst project — a new screen, a new data feature, or both together.

Replace `Xxx` / `xxx` throughout with your feature name.

---

## Adding a Screen

### Step 1 — Create the composable

Create `composeApp/src/commonMain/kotlin/io/jadu/catylst/ui/screens/XxxScreen.kt`:

```kotlin
package io.jadu.catylst.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun XxxScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Xxx Screen")
    }
}
```

### Step 2 — Add the Screen object

Open `composeApp/src/commonMain/kotlin/io/jadu/catylst/navigation/Screen.kt`.
Add inside the `sealed class Screen` body:

```kotlin
@Serializable
data object Xxx : Screen()
```

### Step 3 — Register in AppNavigation

Open `composeApp/src/commonMain/kotlin/io/jadu/catylst/navigation/AppNavigation.kt`.

3a. Add to the `SerializersModule`:
```kotlin
subclass(Screen.Xxx::class)
```

3b. Add a branch in the `Crossfade` block:
```kotlin
is Screen.Xxx -> XxxScreen(onBack = { backStack.removeLastOrNull() })
```

### Step 4 — Add navigation trigger in HomeScreen

Open `composeApp/src/commonMain/kotlin/io/jadu/catylst/ui/screens/HomeScreen.kt`.

4a. Add a lambda parameter:
```kotlin
onNavigateToXxx: () -> Unit,
```

4b. Add a button in the content:
```kotlin
OutlinedButton(onClick = onNavigateToXxx) { Text("Open Xxx") }
```

4c. In `AppNavigation.kt` where `HomeScreen(...)` is called, pass the lambda:
```kotlin
onNavigateToXxx = { backStack.add(Screen.Xxx) },
```

### Step 5 — Add a ViewModel (optional)

If the screen needs state, create `XxxViewModel.kt` and register it:

```kotlin
// composeApp/src/commonMain/.../ui/viewmodel/XxxViewModel.kt
class XxxViewModel : ViewModel() {
    // StateFlow, etc.
}
```

Register in `AppModule.kt`:
```kotlin
viewModel { XxxViewModel() }
```

Inject in the composable:
```kotlin
val viewModel: XxxViewModel = koinViewModel()
```

---

## Adding a Full Data Feature (Entity → DAO → Repository → ViewModel → Screen)

Follow Steps 1–5 above for the screen, then add the data layer below.

### Step 6 — Entity

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

### Step 7 — DAO

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

### Step 8 — Register DAO in AppDatabase

Open `data/local/AppDatabase.kt`. Add:

```kotlin
abstract fun xxxDao(): XxxDao
```

### Step 9 — Repository

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

### Step 10 — Register in AppModule

Open `di/AppModule.kt`. Add:

```kotlin
single { XxxRepository(get<AppDatabase>().xxxDao()) }
viewModel { XxxViewModel(get()) }
```

### Step 11 — ViewModel with Repository

Update `XxxViewModel.kt` to inject the repository:

```kotlin
class XxxViewModel(private val repo: XxxRepository) : ViewModel() {
    val items = repo.getAll().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun add(name: String) { viewModelScope.launch { repo.add(name) } }
    fun remove(id: Long)  { viewModelScope.launch { repo.remove(id) } }
}
```

### Step 12 — Regenerate Room code

```bash
./gradlew :composeApp:kspAndroidMain
./gradlew :androidApp:assembleDebug
```

This must run after any Entity or DAO change.
