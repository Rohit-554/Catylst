---
name: kmp-ai-provider
description: Swap or extend the AI provider in Catylst KMP — Claude, Groq, Gemini, or a custom provider — with one-line DI change
---

# kmp-ai-provider

The AI layer uses a strategy pattern — swap providers by changing one line in `AppModule.kt`.

## Swapping the Active Provider

Open `composeApp/src/commonMain/kotlin/io/jadu/catylst/di/AppModule.kt`.

Find the provider binding and change it to the desired provider:

```kotlin
/* Claude (Anthropic) */
single<AiProvider> { ClaudeProvider(get(), AppConfig.claudeApiKey) }

/* Groq (Llama, Gemma, Mixtral, etc.) */
single<AiProvider> { GroqProvider(get(), AppConfig.groqApiKey) }

/* Google Gemini */
single<AiProvider> { GeminiProvider(get(), AppConfig.geminiApiKey) }
```

Only one line should be active (uncommented) at a time.

## API Key Setup

```bash
cp local.properties.example local.properties
```

Edit `local.properties` and fill in the key for your active provider:

```properties
claude.api.key=sk-ant-...
groq.api.key=gsk_...
gemini.api.key=AIza...
```

Keys are read at build time into `BuildConfig` and injected into `AppConfig` in
`MainActivity.onCreate` before `startKoin` runs — no cross-module BuildConfig needed.

## Provider Auth Patterns

| Provider | Auth method              | Base URL |
|----------|--------------------------|----------|
| Claude   | `x-api-key` header + `anthropic-version: 2023-06-01` | `https://api.anthropic.com/v1/messages` |
| Groq     | `Authorization: Bearer <key>` (OpenAI-compatible) | `https://api.groq.com/openai/v1/chat/completions` |
| Gemini   | `?key=<key>` query param | `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent` |

All providers share the same `HttpClient` singleton from Koin — no extra HTTP setup needed.

## Adding a Custom Provider

1. Create `composeApp/src/commonMain/kotlin/io/jadu/catylst/ai/providers/MyProvider.kt`:

```kotlin
class MyProvider(private val client: HttpClient, private val apiKey: String) : AiProvider {
    override suspend fun chat(prompt: String): Result<String> = runCatching {
        /* implement your HTTP call here */
        "response text"
    }
}
```

2. Add your API key to `AppConfig.kt`:
```kotlin
var myApiKey: String = ""
```

3. Inject in `MainActivity.onCreate`:
```kotlin
AppConfig.myApiKey = BuildConfig.MY_API_KEY
```

4. Add `buildConfigField` to `androidApp/build.gradle.kts`:
```kotlin
buildConfigField("String", "MY_API_KEY", "\"${localProps.getProperty("my.api.key", "demo-key")}\"")
```

5. Register in `AppModule.kt`:
```kotlin
single<AiProvider> { MyProvider(get(), AppConfig.myApiKey) }
```

## Removing AI Services Entirely

Use the `kmp-remove-feature` skill.
