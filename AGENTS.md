# WanderVault – Agent Guidelines

## Project Overview

WanderVault is an Android travel planning app written in **Kotlin** with a **Jetpack Compose** UI. It uses:

- **Clean Architecture** (Data / Domain / Presentation layers)
- **Room** for local persistence
- **Koin** for dependency injection
- **ML Kit** for intelligent features (e.g. smart text recognition, language detection, translation)

---

## Architecture

Follow a strict three-layer Clean Architecture. Every feature lives under `app/src/main/java/cat/company/wandervault/<feature>/`.

```
cat.company.wandervault/
├── core/                  # Shared utilities, extensions, base classes
├── data/
│   ├── local/             # Room DAOs, entities, database definition
│   ├── remote/            # Retrofit services, DTOs (if any)
│   └── repository/        # Repository implementations
├── domain/
│   ├── model/             # Pure Kotlin domain models
│   ├── repository/        # Repository interfaces
│   └── usecase/           # One class per use-case (single `invoke` operator)
└── presentation/
    ├── navigation/        # NavHost, routes, NavController helpers
    ├── theme/             # MaterialTheme, colours, typography, shapes
    └── <feature>/
        ├── <Feature>Screen.kt      # @Composable screen
        ├── <Feature>ViewModel.kt   # ViewModel (extends ViewModel())
        └── <Feature>UiState.kt     # Sealed class / data class for UI state
```

### Layer rules

| Layer        | May depend on       | Must NOT depend on          |
|--------------|---------------------|-----------------------------|
| Presentation | Domain              | Data, Room, Koin internals  |
| Domain       | –                   | Presentation, Data, Android |
| Data         | Domain (interfaces) | Presentation                |

---

## Tech Stack Details

### Jetpack Compose

- Use `@Composable` functions for all UI. No XML layouts.
- State hoisting: keep state in the ViewModel; screens are stateless.
- Use `collectAsStateWithLifecycle()` (from `androidx.lifecycle:lifecycle-runtime-compose`) to collect `StateFlow` from ViewModels.
- Preview every screen with `@Preview` using a fake/stub UiState.

### Room

- Define one `@Database` class (`WanderVaultDatabase`) in `data/local/`.
- Each entity has a corresponding `@Dao` interface.
- Expose `Flow<List<T>>` from DAOs for reactive queries.
- Use `@TypeConverters` for complex types (e.g. `LocalDate`, lists).
- Migrations must be provided for every schema change. `fallbackToDestructiveMigration()` may be used in debug builds during active development but must **never** appear in release/production builds.

### Koin

- Declare modules in `data/di/`, `domain/di/`, and `presentation/di/`, one module file per layer.
- Start Koin in the custom `Application` class (`WanderVaultApp`) using `startKoin { }`.
- Inject ViewModels with `koinViewModel()` (from `koin-androidx-compose`).
- Use `single { }` for repositories and the database; use `factory { }` for use-cases.

### ML Kit

- Feature modules live in `data/mlkit/` and expose simple suspending functions or `Flow`.
- Keep ML Kit dependencies isolated behind interfaces defined in `domain/`.
- Currently planned capabilities:
  - **Text Recognition** – scan travel documents / signs
  - **Language Detection** – auto-detect language of notes
  - **Translation** – translate notes or place descriptions on-device

---

## Coding Conventions

- **Language**: Kotlin only. No Java source files.
- **Formatting**: follow the official [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html). Run `./gradlew ktlintCheck` before every commit.
- **Naming**:
  - Composables: `PascalCase`. Screen-level composables use the `Screen` suffix (`TripListScreen`); reusable component composables use no suffix (`TripCard`, `DateRangePicker`).
  - ViewModels: `<Feature>ViewModel` (e.g. `TripListViewModel`).
  - Use-cases: verb + noun + `UseCase` (e.g. `GetTripsUseCase`, `SaveTripUseCase`).
  - Room entities: `<Name>Entity` (e.g. `TripEntity`).
  - Room DAOs: `<Name>Dao` (e.g. `TripDao`).
- **Coroutines**: use `viewModelScope` in ViewModels and `Dispatchers.IO` for data operations.
- **Error handling**: model errors in UiState (e.g. `sealed class TripListUiState { data class Error(...) }`). Do not swallow exceptions silently.
- **No magic strings**: define route names, database names, and preference keys as constants in a `core/` object.

---

## Build & Test

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

- Unit tests live in `app/src/test/` and should cover use-cases and ViewModels.
- Instrumented tests live in `app/src/androidTest/` and cover Room DAOs and critical UI flows.
- Use **Turbine** for testing `Flow` emissions.
- Mock dependencies with **MockK**.

---

## Pull Request Checklist

- [ ] New code follows the layering rules above.
- [ ] Every new public function/class has a KDoc comment if its purpose is not obvious.
- [ ] Room migrations are included for schema changes.
- [ ] Koin modules are updated for new dependencies.
- [ ] Unit tests added or updated.
- [ ] `./gradlew lint` passes with no new warnings.

---

## Code Review Comments

When addressing code review comments (from reviewers or automated tools), always reply to each individual comment thread once the fix has been committed, including the short commit hash that resolves it.
