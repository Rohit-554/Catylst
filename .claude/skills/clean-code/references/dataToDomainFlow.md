# Clean Code: Data Layer to Domain Layer (KMP)

This guide covers how clean code principles apply across the Data and Domain layers in a Kotlin Multiplatform project — from raw API responses all the way to business logic.

---

## The Mental Model

Every piece of data in your app follows a single direction:

```text
API / Database
     |
   [ DTO ]          ← Data Layer: what the backend speaks
     |
  [ Mapper ]        ← Data Layer: translation boundary
     |
[ Domain Model ]    ← Domain Layer: what your app speaks
     |
  [ Use Case ]      ← Domain Layer: what your app does
     |
  [ ViewModel ]     ← UI Layer: what the user sees
```

Each layer only knows about itself and the layer directly below it. Nothing leaks upward.

---

## Layer Responsibilities

| Layer | What it owns | What it must NOT contain |
|-------|-------------|--------------------------|
| Data | DTOs, API calls, mappers, repository implementations | Business logic, UI logic |
| Domain | Domain models, use cases, repository interfaces | API models, UI logic, framework dependencies |
| UI | ViewModels, UI state, screens | Business logic, direct API calls |

---

## Data Layer

### What belongs here

- DTOs (Request / Response classes)
- Retrofit / Ktor API service definitions
- Repository implementations
- Mappers (DTO → Domain Model)
- Local data sources (Room / SQLDelight)

### DTO Design Rules

- Annotate with `@Serializable`
- No logic of any kind
- Name ends with `Request` or `Response`
- Fields match the API contract exactly

```kotlin
@Serializable
data class UserProfileResponse(
    val id: String,
    val fullName: String,
    @SerialName("profile_pic_url")
    val profilePictureUrl: String? = null,
)
```

### Repository Implementation

The repository implementation lives in the Data layer. It depends on the API service and the mapper, and returns domain models — not DTOs.

```kotlin
class UserRepositoryImpl(
    private val apiService: UserApiService,
) : UserRepository {

    override suspend fun getUserProfile(userId: String): UserProfile {
        val response = apiService.getUserProfile(userId)
        return response.toDomain()
    }
}
```

Rules:
- The function signature uses domain models, not DTOs
- All mapping happens inside the repository or a dedicated mapper
- No business logic — only fetch, map, and return

### Mapper Rules

- One mapper per DTO, defined as an extension function
- Lives in the `mapper/` folder inside the feature package
- Only maps fields — no conditions, no calculations

```kotlin
fun UserProfileResponse.toDomain() = UserProfile(
    id = id,
    fullName = fullName,
    profilePictureUrl = profilePictureUrl,
)
```

If the mapping grows complex (nested objects, lists), break it into smaller extension functions rather than one large mapper.

---

## Domain Layer

### What belongs here

- Domain models (plain Kotlin data classes)
- Repository interfaces
- Use cases

The Domain layer has zero dependencies on Android, KMP platform code, or any framework. It is pure Kotlin.

### Domain Model Design Rules

- Plain data classes only
- No annotations (`@Serializable`, `@Entity`, etc.)
- No nullable fields unless the business domain genuinely allows null
- Named after what they represent in the real world, not after the API

```kotlin
data class UserProfile(
    val id: String,
    val fullName: String,
    val profilePictureUrl: String?,
)
```

The difference between a DTO and a domain model:
- DTO reflects what the API sends
- Domain model reflects what your application needs

These are often similar but should never be the same class.

### Repository Interface

The interface lives in the Domain layer. The implementation lives in the Data layer. This is the dependency inversion principle in practice.

```kotlin
interface UserRepository {
    suspend fun getUserProfile(userId: String): UserProfile
}
```

Rules:
- Only domain models in signatures
- No implementation details (no Retrofit, no Ktor, no database references)
- One interface per feature aggregate

### Use Case Design Rules

A use case represents one business action. It sits between the repository and the ViewModel.

```kotlin
class GetUserProfileUseCase(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(userId: String): UserProfile {
        return repository.getUserProfile(userId)
    }
}
```

Rules:
- One use case = one action
- Class name is a verb phrase: `GetUserProfile`, `SubmitOrder`, `ValidatePayment`
- Use `operator fun invoke` so it reads as a function call at the call site
- No UI logic, no formatting, no string resources
- Business validation and rules live here, not in the repository

When use cases grow:

```kotlin
class GetUserProfileUseCase(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(userId: String): UserProfile {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val profile = repository.getUserProfile(userId)
        return profile
    }
}
```

Validation of business inputs belongs in the use case. Validation of UI inputs (empty text fields, format checks) belongs in the ViewModel.

---

## Common Mistakes

**Returning DTOs from repositories**
The repository returns `UserProfileResponse` instead of `UserProfile`. This leaks the API contract into the domain layer.

**Putting logic inside DTOs**
A DTO that formats a date or checks a condition is no longer a data carrier. Move it to the use case.

**Skipping the mapper**
Using the same class as both DTO and domain model saves time initially but creates tight coupling that is painful to change.

**Fat repositories**
A repository that filters, sorts, and combines data is doing use case work. Move that logic up.

**Anemic use cases**
A use case that only delegates with no logic at all can be reasonable, but if every use case is just a passthrough, your business logic is probably scattered elsewhere.

---

## Clean Structure Reference

```text
feature/
 ├── data/
 │    ├── remote/
 │    │    ├── request/
 │    │    │    └── LoginRequest.kt
 │    │    ├── response/
 │    │    │    └── UserProfileResponse.kt
 │    │    └── mapper/
 │    │         └── UserProfileMapper.kt
 │    ├── api/
 │    │    └── UserApiService.kt
 │    └── repository/
 │         └── UserRepositoryImpl.kt
 │
 └── domain/
      ├── model/
      │    └── UserProfile.kt
      ├── repository/
      │    └── UserRepository.kt
      └── usecase/
           └── GetUserProfileUseCase.kt
```

---

## Pre-Commit Checklist

**Data layer**
- [ ] DTO contains no logic
- [ ] DTO fields match the API contract
- [ ] Mapper is a simple extension function
- [ ] Repository implementation returns domain models only

**Domain layer**
- [ ] Domain model has no framework annotations
- [ ] Repository interface uses only domain models
- [ ] Use case name is a clear verb phrase
- [ ] Business validation lives in the use case

**General**
- [ ] No DTO exposed beyond the Data layer
- [ ] No domain model has API-specific field names
- [ ] Dependencies point inward (Domain has no dependency on Data)

---

## The Final Rules

> The Data layer knows the API. The Domain layer knows the business. Neither should know the other's language directly — the mapper is the translator.

> If your use case is empty, your business logic is hiding somewhere it should not be.