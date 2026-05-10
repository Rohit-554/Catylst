---
name: kmp-add-screen
description: Add a new screen to the Catylst KMP starter kit end-to-end (Screen object, navigation wiring, composable, optional ViewModel)
---

# kmp-add-screen

Adds a new screen to the Catylst app end-to-end. Follow every step in order.

## Step 1 — Create the composable

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

Replace `Xxx` with the feature name (PascalCase).

## Step 2 — Add the Screen object

Open `composeApp/src/commonMain/kotlin/io/jadu/catylst/navigation/Screen.kt`.
Add inside the `sealed class Screen` body:

```kotlin
@Serializable
data object Xxx : Screen()
```

## Step 3 — Register in AppNavigation

Open `composeApp/src/commonMain/kotlin/io/jadu/catylst/navigation/AppNavigation.kt`.

3a. Add to the `SerializersModule`:
```kotlin
subclass(Screen.Xxx::class)
```

3b. Add a branch in the `Crossfade` block:
```kotlin
is Screen.Xxx -> XxxScreen(onBack = { backStack.removeLastOrNull() })
```

## Step 4 — Add navigation trigger in HomeScreen

Open `composeApp/src/commonMain/kotlin/io/jadu/catylst/ui/screens/HomeScreen.kt`.

4a. Add a lambda parameter to `HomeScreen`:
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

## Step 5 — Add a ViewModel (optional)

If the screen needs state:

5a. Create `composeApp/src/commonMain/kotlin/io/jadu/catylst/ui/viewmodel/XxxViewModel.kt`:
```kotlin
class XxxViewModel : ViewModel() {
    // StateFlow, etc.
}
```

5b. Register in `AppModule.kt`:
```kotlin
viewModel { XxxViewModel() }
```

5c. Inject in the composable:
```kotlin
val viewModel: XxxViewModel = koinViewModel()
```

## Verify

```bash
./gradlew :androidApp:assembleDebug
```

No compilation errors means the screen is correctly wired.
