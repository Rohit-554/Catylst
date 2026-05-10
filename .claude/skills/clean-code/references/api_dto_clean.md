# API DTO Design Skills (KMP)

This guide defines how to design and manage Request and Response data classes (DTOs) in a clean, scalable way across a Kotlin Multiplatform project.

---

## Core Principle

DTOs are data carriers — nothing more.

- No business logic
- No UI logic
- No formatting logic
- Only serialization and deserialization

---

## Project Structure

Organize DTOs by feature and type:

```text
data/
 └── remote/
      └── auth/
           ├── request/
           ├── response/
           └── mapper/
```

---

## Naming Conventions

**Requests** — always end with `Request`
- `LoginRequest`
- `RegisterRequest`
- `GoogleLoginRequest`

**Responses** — always end with `Response`
- `AuthTokensResponse`
- `UserProfileResponse`

---

## DTO Rules

### 1. Keep DTOs Minimal

Only include fields required by the API contract.

```kotlin
@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)
```

---

### 2. Match Backend Naming

If the backend uses the same field names, no annotation is needed. If names differ, use `@SerialName`.

```kotlin
@SerialName("id_token")
val idToken: String
```

---

### 3. Nullable Fields

Use `?` only when the API can actually return null. Avoid adding nullability as a precaution.

---

### 4. Default Values

Use defaults only when:
- The API allows missing fields
- Safe parsing is required

```kotlin
val profilePictureUrl: String? = null
```

---

## Mapping

Never use DTOs directly in business logic. Always map to a domain model first.

```kotlin
fun AuthTokensResponse.toDomain() = AuthTokens(
    accessToken = accessToken,
    refreshToken = refreshToken
)
```

This is the most important rule in DTO design. The mapper is the boundary between the API layer and the rest of the application.

---

## What Not to Do

- Do not add validation logic inside DTOs
- Do not add UI formatting or utility functions
- Do not reuse the same DTO across multiple layers
- Do not expose DTOs to the UI layer

---

## Separation of Concerns

| Layer | Responsibility |
|-------|---------------|
| DTO | API contract only |
| Domain | Business logic |
| UI | Presentation |

---

## KMP Best Practices

- Annotate all DTOs with `@Serializable`
- Keep DTOs platform-independent
- Avoid platform-specific types in shared DTO classes

---

## Versioning and Safety

- Add new fields as nullable to avoid crashes on older API versions
- Never remove fields without coordinating with the backend team

---

## Example Flow

```text
UI → Domain Model → Mapper → DTO → API
API → DTO → Mapper → Domain Model → UI
```

---

## Pre-Implementation Checklist

Before adding a new DTO:

- [ ] Is it API-specific and nothing else?
- [ ] Does the name end with `Request` or `Response`?
- [ ] Are there any unnecessary fields?
- [ ] Is there any logic inside? (there should be none)
- [ ] Is the mapper implemented?

---

## The Final Rule

> DTOs should be boring. If they feel smart, you are doing it wrong.
