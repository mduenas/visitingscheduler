# GEMINI.md - VisitScheduler Project Context

## Project Overview
VisitScheduler (VisiScheduler) is a comprehensive **Kotlin Multiplatform (KMP)** proxy scheduling platform designed for caregivers, family members, or coordinators to manage and schedule visits on behalf of individuals (e.g., hospital patients, elderly in care facilities).

### Core Technologies
- **Language:** Kotlin 2.2.20
- **UI:** Compose Multiplatform 1.10.0 (Android & iOS)
- **Networking:** Ktor Client 3.1.1
- **Local Persistence:** SQLDelight 2.0.2 (`VisiSchedulerDatabase`)
- **Dependency Injection:** Koin 4.0.4
- **Navigation:** Voyager 1.1.0-beta03
- **Firebase:** Integrated via GitLive SDK for KMP (Firestore, Auth)
- **Settings:** Multiplatform Settings 1.1.1
- **Testing:** JUnit, MockK, Turbine, Kotest, Kover (coverage)

## Architecture
The project follows **Clean Architecture** with a **Unidirectional Data Flow (UDF)**.

### Module Structure
- `/VisiScheduler/shared`: KMP Shared Module
    - `commonMain`: Shared business logic, domain entities, repositories, and UI (Compose).
    - `androidMain`: Android-specific implementations (e.g., SQLDelight driver, Biometrics).
    - `iosMain`: iOS-specific implementations (e.g., Native driver, Keychain).
- `/VisiScheduler/androidApp`: Android Application (thin layer over `shared`).
- `/VisiScheduler/iosApp`: iOS Application (Swift project consuming `shared` framework).

### Package Structure (`shared/src/commonMain/kotlin/...`)
- `common`: Shared utilities and constants.
- `domain`:
    - `entities`: Business models (e.g., `Visit`, `User`, `Rule`).
    - `repository`: Repository interfaces.
    - `usecase`: Encapsulated business logic.
- `data`:
    - `local`: SQLDelight database, DAOs, and entities.
    - `remote`: Ktor API clients, DTOs, and Firebase implementations.
    - `repository`: Implementations of domain repository interfaces.
- `presentation`:
    - `viewmodel`: Voyager `ScreenModel` implementations.
    - `state`: UI State and Event models.
    - `navigation`: Screens and navigation graph.
- `di`: Koin modules.

## Building and Running

### Prerequisites
- JDK 17
- Android Studio (Koala or later)
- Xcode (for iOS)
- CocoaPods (`brew install cocoapods`)

### Gradle Commands (Run from `/VisiScheduler`)
- **Build All:** `./gradlew build`
- **Run Android:** `./gradlew :androidApp:installDebug`
- **Run iOS (Simulator):** Open `iosApp/iosApp.xcworkspace` in Xcode and run.
- **Run Tests:** `./gradlew test` (runs all unit tests)
- **Lint:** `./gradlew lint`
- **Coverage (Kover):** `./gradlew koverHtmlReport` (output in `shared/build/reports/kover/html`)

## Development Conventions
- **Clean Architecture:** Strictly separate layers. Domain must not depend on Data or Presentation.
- **UDF:** Use `StateFlow` for UI state and `SharedFlow` for one-time events.
- **Testing:**
    - Use `MockK` for mocking in tests.
    - Use `Turbine` for testing `Flow` emissions.
    - Minimum coverage target: 80% (enforced by Kover).
- **Expect/Actual:** Use the `expect`/`actual` pattern only for platform-specific APIs that cannot be abstracted via libraries.
- **DI:** Add new dependencies to `AppModule.kt` or specialized modules in the `di` package.
- **Styles:** Adhere to Kotlin Coding Conventions and Compose best practices.

## Primary Documentation
- `visitschedule_spec.md`: Detailed product specification and features.
- `docs/architecture/ARCHITECTURE.md`: Technical design and data flow diagrams.
- `README.md`: Root project overview.
