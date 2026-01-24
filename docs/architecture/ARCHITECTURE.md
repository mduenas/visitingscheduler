# VisiScheduler - KMP Architecture Design Document

**Bundle Identifier**: `com.markduenas.visischeduler`
**Version**: 1.0.0
**Last Updated**: 2026-01-23

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Project Structure](#2-project-structure)
3. [Module Architecture](#3-module-architecture)
4. [Data Flow](#4-data-flow)
5. [API Layer Design](#5-api-layer-design)
6. [Database Schema](#6-database-schema)
7. [Security Architecture](#7-security-architecture)
8. [Technology Stack](#8-technology-stack)
9. [Deployment Architecture](#9-deployment-architecture)

---

## 1. System Overview

### 1.1 High-Level Architecture Diagram

```
+------------------------------------------------------------------+
|                        CLIENT LAYER                               |
|  +---------------------------+  +---------------------------+     |
|  |       Android App         |  |        iOS App            |     |
|  |   (Compose Multiplatform) |  |   (Compose Multiplatform) |     |
|  +-------------+-------------+  +-------------+-------------+     |
|                |                              |                   |
|                +-------------+----------------+                   |
|                              |                                    |
|  +---------------------------v---------------------------+        |
|  |                    SHARED MODULE                      |        |
|  |  +------------------------------------------------+  |        |
|  |  |              PRESENTATION LAYER                |  |        |
|  |  |  ViewModels | UI State | Screen Controllers    |  |        |
|  |  +------------------------+-----------------------+  |        |
|  |                           |                          |        |
|  |  +------------------------v-----------------------+  |        |
|  |  |                DOMAIN LAYER                    |  |        |
|  |  |  Use Cases | Entities | Repository Interfaces |  |        |
|  |  +------------------------+-----------------------+  |        |
|  |                           |                          |        |
|  |  +------------------------v-----------------------+  |        |
|  |  |                 DATA LAYER                     |  |        |
|  |  |  Repositories | Data Sources | DTOs | Mappers  |  |        |
|  |  +------------------------------------------------+  |        |
|  +-------------------------------------------------------+        |
+------------------------------------------------------------------+
                               |
                               | HTTPS/TLS 1.3
                               v
+------------------------------------------------------------------+
|                       BACKEND SERVICES                            |
|  +------------+  +------------+  +------------+  +------------+   |
|  |   Auth     |  | Scheduling |  |   Rules    |  |   Notify   |   |
|  |  Service   |  |  Service   |  |  Engine    |  |  Service   |   |
|  +-----+------+  +-----+------+  +-----+------+  +-----+------+   |
|        |               |               |               |          |
|  +-----v---------------v---------------v---------------v------+   |
|  |                    API GATEWAY                             |   |
|  |              (Rate Limiting, Auth, Routing)                |   |
|  +------------------------------------------------------------+   |
|                               |                                   |
|  +----------------------------v-------------------------------+   |
|  |                    DATA STORES                             |   |
|  |  +----------+  +----------+  +----------+  +----------+    |   |
|  |  |PostgreSQL|  |  Redis   |  |   S3     |  | Twilio/  |    |   |
|  |  |  (HIPAA) |  |  Cache   |  | Storage  |  | SendGrid |    |   |
|  |  +----------+  +----------+  +----------+  +----------+    |   |
|  +------------------------------------------------------------+   |
+------------------------------------------------------------------+
```

### 1.2 Module Dependency Graph

```
                    +-------------------+
                    |      :iosApp      |
                    +---------+---------+
                              |
          +-------------------+-------------------+
          |                                       |
+---------v---------+                   +---------v---------+
|    :androidApp    |                   |                   |
+---------+---------+                   |                   |
          |                             |                   |
          +-------------+---------------+                   |
                        |                                   |
              +---------v---------+                         |
              |     :shared       |<------------------------+
              +---------+---------+
                        |
        +---------------+---------------+
        |               |               |
+-------v------+ +------v------+ +------v-------+
| :shared:core | | :shared:ui  | | :shared:data |
+--------------+ +-------------+ +--------------+
        |               |               |
        +-------+-------+-------+-------+
                |               |
        +-------v-------+ +-----v--------+
        |:shared:domain | |:shared:common|
        +---------------+ +--------------+
```

### 1.3 Core Feature Modules

```
+------------------------------------------------------------------+
|                     FEATURE MODULES                               |
|                                                                   |
|  +-------------+  +-------------+  +-------------+                |
|  |    Auth     |  |  Schedule   |  |    Rules    |                |
|  |   Module    |  |   Module    |  |   Module    |                |
|  | - Login     |  | - Calendar  |  | - Time      |                |
|  | - MFA       |  | - Booking   |  | - Visitor   |                |
|  | - Session   |  | - Buffer    |  | - Location  |                |
|  | - Biometric |  | - Fatigue   |  | - Capacity  |                |
|  +-------------+  +-------------+  +-------------+                |
|                                                                   |
|  +-------------+  +-------------+  +-------------+                |
|  |   Notify    |  |  Calendar   |  |  Analytics  |                |
|  |   Module    |  |   Sync      |  |   Module    |                |
|  | - Push      |  | - Google    |  | - Dashboard |                |
|  | - SMS       |  | - Outlook   |  | - Reports   |                |
|  | - Email     |  | - Apple     |  | - Metrics   |                |
|  | - In-App    |  | - iCal      |  | - Export    |                |
|  +-------------+  +-------------+  +-------------+                |
|                                                                   |
|  +-------------+  +-------------+  +-------------+                |
|  |   Video     |  |  CareCircle |  |  Check-In   |                |
|  |   Module    |  |   Module    |  |   Module    |                |
|  | - WebRTC    |  | - Members   |  | - QR Code   |                |
|  | - Recording |  | - Roles     |  | - Location  |                |
|  | - Screen    |  | - Sharing   |  | - Time Log  |                |
|  +-------------+  +-------------+  +-------------+                |
+------------------------------------------------------------------+
```

---

## 2. Project Structure

### 2.1 Gradle Module Hierarchy

```
VisiScheduler/
├── build.gradle.kts                    # Root build configuration
├── settings.gradle.kts                 # Module includes
├── gradle.properties                   # Gradle properties
├── gradle/
│   ├── libs.versions.toml             # Version catalog
│   └── wrapper/
│
├── shared/                            # KMP Shared Module
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/
│       │   └── kotlin/com/markduenas/visischeduler/
│       │       ├── common/            # Shared utilities
│       │       ├── domain/            # Business logic
│       │       ├── data/              # Data layer
│       │       ├── presentation/      # ViewModels
│       │       └── di/                # Dependency injection
│       │
│       ├── androidMain/
│       │   └── kotlin/com/markduenas/visischeduler/
│       │       ├── platform/          # Android-specific
│       │       └── di/                # Android DI modules
│       │
│       └── iosMain/
│           └── kotlin/com/markduenas/visischeduler/
│               ├── platform/          # iOS-specific
│               └── di/                # iOS DI modules
│
├── androidApp/                        # Android Application
│   ├── build.gradle.kts
│   └── src/main/
│       ├── kotlin/com/markduenas/visischeduler/
│       │   ├── MainActivity.kt
│       │   ├── VisiSchedulerApp.kt
│       │   └── ui/                    # Android UI components
│       └── AndroidManifest.xml
│
└── iosApp/                           # iOS Application
    ├── VisiScheduler/
    │   ├── VisiSchedulerApp.swift
    │   └── ContentView.swift
    └── VisiScheduler.xcodeproj/
```

### 2.2 Package Structure (commonMain)

```
com.markduenas.visischeduler/
│
├── common/
│   ├── di/
│   │   └── CommonModule.kt
│   ├── util/
│   │   ├── DateTimeUtils.kt
│   │   ├── ValidationUtils.kt
│   │   ├── CryptoUtils.kt
│   │   └── Result.kt
│   ├── constants/
│   │   └── AppConstants.kt
│   └── extensions/
│       ├── StringExtensions.kt
│       └── FlowExtensions.kt
│
├── domain/
│   ├── model/
│   │   ├── user/
│   │   │   ├── User.kt
│   │   │   ├── UserRole.kt
│   │   │   └── CareCircle.kt
│   │   ├── schedule/
│   │   │   ├── Visit.kt
│   │   │   ├── TimeSlot.kt
│   │   │   ├── Recurrence.kt
│   │   │   └── VisitStatus.kt
│   │   ├── rules/
│   │   │   ├── Rule.kt
│   │   │   ├── TimeRestriction.kt
│   │   │   ├── VisitorRestriction.kt
│   │   │   └── CapacityLimit.kt
│   │   ├── notification/
│   │   │   ├── Notification.kt
│   │   │   └── NotificationType.kt
│   │   └── analytics/
│   │       ├── VisitMetrics.kt
│   │       └── DashboardData.kt
│   │
│   ├── repository/
│   │   ├── AuthRepository.kt
│   │   ├── UserRepository.kt
│   │   ├── ScheduleRepository.kt
│   │   ├── RulesRepository.kt
│   │   ├── NotificationRepository.kt
│   │   ├── CalendarRepository.kt
│   │   └── AnalyticsRepository.kt
│   │
│   └── usecase/
│       ├── auth/
│       │   ├── LoginUseCase.kt
│       │   ├── LogoutUseCase.kt
│       │   ├── RefreshTokenUseCase.kt
│       │   ├── VerifyMfaUseCase.kt
│       │   └── BiometricAuthUseCase.kt
│       ├── schedule/
│       │   ├── CreateVisitUseCase.kt
│       │   ├── UpdateVisitUseCase.kt
│       │   ├── CancelVisitUseCase.kt
│       │   ├── GetAvailableSlotsUseCase.kt
│       │   ├── CheckBufferTimeUseCase.kt
│       │   └── ValidateFatigueRulesUseCase.kt
│       ├── rules/
│       │   ├── CreateRuleUseCase.kt
│       │   ├── EvaluateRulesUseCase.kt
│       │   └── GetActiveRulesUseCase.kt
│       ├── visitor/
│       │   ├── RequestVisitorAccessUseCase.kt
│       │   ├── ApproveVisitorUseCase.kt
│       │   └── RevokeVisitorAccessUseCase.kt
│       ├── checkin/
│       │   ├── CheckInUseCase.kt
│       │   ├── CheckOutUseCase.kt
│       │   └── GenerateQrCodeUseCase.kt
│       └── video/
│           ├── InitiateVideoCallUseCase.kt
│           └── EndVideoCallUseCase.kt
│
├── data/
│   ├── remote/
│   │   ├── api/
│   │   │   ├── AuthApi.kt
│   │   │   ├── ScheduleApi.kt
│   │   │   ├── RulesApi.kt
│   │   │   ├── NotificationApi.kt
│   │   │   └── CalendarApi.kt
│   │   ├── dto/
│   │   │   ├── auth/
│   │   │   │   ├── LoginRequestDto.kt
│   │   │   │   ├── LoginResponseDto.kt
│   │   │   │   └── TokenDto.kt
│   │   │   ├── schedule/
│   │   │   │   ├── VisitDto.kt
│   │   │   │   ├── TimeSlotDto.kt
│   │   │   │   └── CreateVisitRequestDto.kt
│   │   │   └── user/
│   │   │       ├── UserDto.kt
│   │   │       └── CareCircleDto.kt
│   │   └── interceptor/
│   │       ├── AuthInterceptor.kt
│   │       └── LoggingInterceptor.kt
│   │
│   ├── local/
│   │   ├── database/
│   │   │   ├── VisiSchedulerDatabase.kt
│   │   │   └── dao/
│   │   │       ├── UserDao.kt
│   │   │       ├── VisitDao.kt
│   │   │       └── RuleDao.kt
│   │   ├── datastore/
│   │   │   ├── PreferencesDataStore.kt
│   │   │   └── SecureStorage.kt
│   │   └── entity/
│   │       ├── UserEntity.kt
│   │       ├── VisitEntity.kt
│   │       └── RuleEntity.kt
│   │
│   ├── repository/
│   │   ├── AuthRepositoryImpl.kt
│   │   ├── UserRepositoryImpl.kt
│   │   ├── ScheduleRepositoryImpl.kt
│   │   ├── RulesRepositoryImpl.kt
│   │   ├── NotificationRepositoryImpl.kt
│   │   ├── CalendarRepositoryImpl.kt
│   │   └── AnalyticsRepositoryImpl.kt
│   │
│   └── mapper/
│       ├── UserMapper.kt
│       ├── VisitMapper.kt
│       └── RuleMapper.kt
│
├── presentation/
│   ├── viewmodel/
│   │   ├── AuthViewModel.kt
│   │   ├── DashboardViewModel.kt
│   │   ├── ScheduleViewModel.kt
│   │   ├── VisitorManagementViewModel.kt
│   │   ├── RulesViewModel.kt
│   │   ├── NotificationsViewModel.kt
│   │   ├── AnalyticsViewModel.kt
│   │   └── VideoCallViewModel.kt
│   │
│   ├── state/
│   │   ├── AuthUiState.kt
│   │   ├── ScheduleUiState.kt
│   │   ├── DashboardUiState.kt
│   │   └── UiState.kt
│   │
│   ├── event/
│   │   ├── AuthEvent.kt
│   │   ├── ScheduleEvent.kt
│   │   └── UiEvent.kt
│   │
│   └── navigation/
│       ├── NavigationGraph.kt
│       ├── Screen.kt
│       └── NavigationActions.kt
│
└── di/
    ├── AppModule.kt
    ├── NetworkModule.kt
    ├── DatabaseModule.kt
    ├── RepositoryModule.kt
    ├── UseCaseModule.kt
    └── ViewModelModule.kt
```

---

## 3. Module Architecture

### 3.1 Auth Module

```
+------------------------------------------------------------------+
|                        AUTH MODULE                                |
|                                                                   |
|  +---------------------------+   +---------------------------+    |
|  |     AuthViewModel         |   |     AuthUiState           |    |
|  | - login()                 |   | - isLoading               |    |
|  | - logout()                |   | - isAuthenticated         |    |
|  | - verifyMfa()             |   | - mfaRequired             |    |
|  | - biometricAuth()         |   | - error                   |    |
|  +-------------+-------------+   +---------------------------+    |
|                |                                                  |
|  +-------------v------------------------------------------------+ |
|  |                      USE CASES                               | |
|  | +------------+ +-------------+ +------------+ +------------+ | |
|  | |   Login    | |   Logout    | | VerifyMfa  | | Biometric  | | |
|  | |  UseCase   | |  UseCase    | |  UseCase   | |  UseCase   | | |
|  | +------------+ +-------------+ +------------+ +------------+ | |
|  +--------------------------------------------------------------+ |
|                |                                                  |
|  +-------------v------------------------------------------------+ |
|  |                    AuthRepository                            | |
|  | - login(credentials): Result<AuthToken>                      | |
|  | - logout(): Result<Unit>                                     | |
|  | - refreshToken(): Result<AuthToken>                          | |
|  | - verifyMfa(code): Result<AuthToken>                         | |
|  | - getCurrentUser(): Flow<User?>                              | |
|  +--------------------------------------------------------------+ |
|                |                                                  |
|  +-------------v------------------------------------------------+ |
|  |                     DATA SOURCES                             | |
|  | +------------------+  +-------------------+                   | |
|  | |    AuthApi       |  |  SecureStorage    |                   | |
|  | | (Remote)         |  |  (Local)          |                   | |
|  | +------------------+  +-------------------+                   | |
|  +--------------------------------------------------------------+ |
+------------------------------------------------------------------+
```

#### Auth Flow Sequence

```
+--------+    +----------+    +----------+    +--------+    +--------+
| Client |    | ViewModel|    | UseCase  |    |  Repo  |    |  API   |
+---+----+    +----+-----+    +----+-----+    +---+----+    +---+----+
    |              |               |              |              |
    | login(creds) |               |              |              |
    |------------->|               |              |              |
    |              | execute(creds)|              |              |
    |              |-------------->|              |              |
    |              |               | login(creds) |              |
    |              |               |------------->|              |
    |              |               |              | POST /auth   |
    |              |               |              |------------->|
    |              |               |              |              |
    |              |               |              | AuthResponse |
    |              |               |              |<-------------|
    |              |               | Result<Token>|              |
    |              |               |<-------------|              |
    |              | AuthUiState   |              |              |
    |              |<--------------|              |              |
    | UI Update    |               |              |              |
    |<-------------|               |              |              |
    |              |               |              |              |
    |   [If MFA Required]         |              |              |
    |              |               |              |              |
    | verifyMfa()  |               |              |              |
    |------------->|               |              |              |
    |              | ... MFA flow  |              |              |
```

### 3.2 Scheduling Module

```
+------------------------------------------------------------------+
|                     SCHEDULING MODULE                             |
|                                                                   |
|  +--------------------------+  +-----------------------------+    |
|  |   ScheduleViewModel      |  |      ScheduleUiState        |    |
|  | - loadVisits()           |  | - visits: List<Visit>       |    |
|  | - createVisit()          |  | - availableSlots: List<Slot>|    |
|  | - updateVisit()          |  | - selectedDate: LocalDate   |    |
|  | - cancelVisit()          |  | - isLoading: Boolean        |    |
|  | - getAvailableSlots()    |  | - conflicts: List<Conflict> |    |
|  +-----------+--------------+  +-----------------------------+    |
|              |                                                    |
|  +-----------v--------------------------------------------------+ |
|  |                    SCHEDULING ENGINE                         | |
|  |                                                              | |
|  | +----------------------------------------------------------+ | |
|  | |              Smart Scheduling Rules                      | | |
|  | | +---------------+ +---------------+ +------------------+ | | |
|  | | | Buffer Time   | | Fatigue Mgmt  | | Capacity Control | | | |
|  | | | Calculator    | | Validator     | | Enforcer         | | | |
|  | | | - 15min min   | | - Max visits  | | - Max concurrent | | | |
|  | | | - Travel time | | - Rest period | | - Room limits    | | | |
|  | | +---------------+ +---------------+ +------------------+ | | |
|  | +----------------------------------------------------------+ | |
|  |                                                              | |
|  | +----------------------------------------------------------+ | |
|  | |              Conflict Resolution                         | | |
|  | | - Time overlap detection                                 | | |
|  | | - Visitor conflict checking                              | | |
|  | | - Resource availability validation                       | | |
|  | +----------------------------------------------------------+ | |
|  +--------------------------------------------------------------+ |
|                                                                   |
|  +--------------------------------------------------------------+ |
|  |                 ScheduleRepository                           | |
|  | - getVisits(dateRange): Flow<List<Visit>>                    | |
|  | - createVisit(visit): Result<Visit>                          | |
|  | - updateVisit(visit): Result<Visit>                          | |
|  | - cancelVisit(id): Result<Unit>                              | |
|  | - getAvailableSlots(date, duration): List<TimeSlot>          | |
|  +--------------------------------------------------------------+ |
+------------------------------------------------------------------+
```

#### Visit Booking Flow

```
+------------------------------------------------------------------+
|                   VISIT BOOKING FLOW                              |
|                                                                   |
|    +------------+                                                 |
|    | User       |                                                 |
|    | Selects    |                                                 |
|    | Date/Time  |                                                 |
|    +-----+------+                                                 |
|          |                                                        |
|          v                                                        |
|    +-----+------+     +-------------+                             |
|    | Check      |---->| Buffer Time |                             |
|    | Availability|     | Validation  |                            |
|    +-----+------+     +------+------+                             |
|          |                   |                                    |
|          |    +--------------+                                    |
|          v    v                                                   |
|    +-----+----+------+                                            |
|    | Fatigue Rules   |                                            |
|    | Check           |                                            |
|    | - Max daily     |                                            |
|    | - Rest periods  |                                            |
|    +--------+--------+                                            |
|             |                                                     |
|             v                                                     |
|    +--------+--------+                                            |
|    | Capacity Check  |                                            |
|    | - Room limit    |                                            |
|    | - Concurrent    |                                            |
|    +--------+--------+                                            |
|             |                                                     |
|             v                                                     |
|    +--------+--------+     +------------------+                   |
|    | Rule Engine     |---->| Apply Visitor    |                   |
|    | Evaluation      |     | Restrictions     |                   |
|    +--------+--------+     +--------+---------+                   |
|             |                       |                             |
|             +-----------+-----------+                             |
|                         |                                         |
|                         v                                         |
|             +-----------+----------+                              |
|             |     All Checks       |                              |
|             |       Passed?        |                              |
|             +----+------------+----+                              |
|                  |            |                                   |
|              YES |            | NO                                |
|                  v            v                                   |
|          +-------+---+  +----+--------+                           |
|          | Create    |  | Show        |                           |
|          | Visit     |  | Conflicts & |                           |
|          | & Notify  |  | Alternatives|                           |
|          +-----------+  +-------------+                           |
+------------------------------------------------------------------+
```

### 3.3 Rules Engine Module

```
+------------------------------------------------------------------+
|                      RULES ENGINE                                 |
|                                                                   |
|  +--------------------------------------------------------------+ |
|  |                    RULE TYPES                                | |
|  |                                                              | |
|  |  +------------------+  +------------------+                   | |
|  |  | TIME-BASED       |  | VISITOR-BASED    |                   | |
|  |  | - Blackout dates |  | - Approved only  |                   | |
|  |  | - Operating hrs  |  | - Blacklist      |                   | |
|  |  | - Holiday rules  |  | - Role-based     |                   | |
|  |  +------------------+  +------------------+                   | |
|  |                                                              | |
|  |  +------------------+  +------------------+                   | |
|  |  | LOCATION-BASED   |  | CAPACITY-BASED   |                   | |
|  |  | - Geo-fencing    |  | - Max concurrent |                   | |
|  |  | - Room restrict  |  | - Daily limits   |                   | |
|  |  | - Zone access    |  | - Weekly quotas  |                   | |
|  |  +------------------+  +------------------+                   | |
|  +--------------------------------------------------------------+ |
|                                                                   |
|  +--------------------------------------------------------------+ |
|  |                 RULE EVALUATION ENGINE                       | |
|  |                                                              | |
|  |    +-----------+                                             | |
|  |    | Rule      |                                             | |
|  |    | Request   |                                             | |
|  |    +-----+-----+                                             | |
|  |          |                                                   | |
|  |          v                                                   | |
|  |    +-----+-----+                                             | |
|  |    | Load      |                                             | |
|  |    | Active    |                                             | |
|  |    | Rules     |                                             | |
|  |    +-----+-----+                                             | |
|  |          |                                                   | |
|  |          v                                                   | |
|  |    +-----+-----+    +------------+    +------------+         | |
|  |    | Evaluate  |--->| Time Rules |--->| Visitor    |         | |
|  |    | Chain     |    +------------+    | Rules      |         | |
|  |    +-----+-----+                      +-----+------+         | |
|  |          |                                  |                | |
|  |          |    +------------+    +-----------+                | |
|  |          +--->| Location   |--->| Capacity  |                | |
|  |               | Rules      |    | Rules     |                | |
|  |               +------------+    +-----+-----+                | |
|  |                                       |                      | |
|  |                                       v                      | |
|  |                              +--------+--------+             | |
|  |                              | Aggregate       |             | |
|  |                              | Results         |             | |
|  |                              | (AND logic)     |             | |
|  |                              +--------+--------+             | |
|  |                                       |                      | |
|  |                                       v                      | |
|  |                              +--------+--------+             | |
|  |                              | RuleResult      |             | |
|  |                              | - allowed: Bool |             | |
|  |                              | - violations[]  |             | |
|  |                              +-----------------+             | |
|  +--------------------------------------------------------------+ |
+------------------------------------------------------------------+
```

### 3.4 Notification Module

```
+------------------------------------------------------------------+
|                   NOTIFICATION MODULE                             |
|                                                                   |
|  +--------------------------------------------------------------+ |
|  |               NOTIFICATION CHANNELS                          | |
|  |                                                              | |
|  |  +--------+  +--------+  +--------+  +--------+              | |
|  |  |  Push  |  |  SMS   |  | Email  |  | In-App |              | |
|  |  +---+----+  +---+----+  +---+----+  +---+----+              | |
|  |      |           |           |           |                   | |
|  |      +-----+-----+-----+-----+-----+-----+                   | |
|  |            |                 |                               | |
|  |            v                 v                               | |
|  |  +---------+---------+  +----+----+                          | |
|  |  | Channel Router    |  | Template|                          | |
|  |  | (User Prefs)      |  | Engine  |                          | |
|  |  +-------------------+  +---------+                          | |
|  +--------------------------------------------------------------+ |
|                                                                   |
|  +--------------------------------------------------------------+ |
|  |               NOTIFICATION TYPES                             | |
|  |                                                              | |
|  |  +------------------+  +------------------+                   | |
|  |  | VISIT            |  | APPROVAL         |                   | |
|  |  | - Confirmed      |  | - Request rcvd   |                   | |
|  |  | - Reminder       |  | - Approved       |                   | |
|  |  | - Cancelled      |  | - Rejected       |                   | |
|  |  | - Modified       |  +------------------+                   | |
|  |  +------------------+                                        | |
|  |                                                              | |
|  |  +------------------+  +------------------+                   | |
|  |  | CHECKIN          |  | SYSTEM           |                   | |
|  |  | - Visitor arrived|  | - Security alert |                   | |
|  |  | - Visit started  |  | - Maintenance    |                   | |
|  |  | - Visit ended    |  | - Updates        |                   | |
|  |  +------------------+  +------------------+                   | |
|  +--------------------------------------------------------------+ |
+------------------------------------------------------------------+
```

---

## 4. Data Flow

### 4.1 Unidirectional Data Flow (UDF)

```
+------------------------------------------------------------------+
|                UNIDIRECTIONAL DATA FLOW                           |
|                                                                   |
|         +-------------+                                           |
|         |    VIEW     |                                           |
|         | (Composable)|                                           |
|         +------+------+                                           |
|                |                                                  |
|                | User Action (Event)                              |
|                v                                                  |
|         +------+------+                                           |
|         |  ViewModel  |                                           |
|         |  (Process)  |                                           |
|         +------+------+                                           |
|                |                                                  |
|                | Execute Use Case                                 |
|                v                                                  |
|         +------+------+                                           |
|         |  Use Case   |                                           |
|         |  (Business) |                                           |
|         +------+------+                                           |
|                |                                                  |
|                | Repository Call                                  |
|                v                                                  |
|         +------+------+                                           |
|         | Repository  |                                           |
|         | (Data)      |                                           |
|         +------+------+                                           |
|                |                                                  |
|    +-----------+-----------+                                      |
|    |                       |                                      |
|    v                       v                                      |
| +--+---+              +----+----+                                  |
| |Remote|              | Local   |                                  |
| | API  |              |Database |                                  |
| +--+---+              +----+----+                                  |
|    |                       |                                      |
|    +-----------+-----------+                                      |
|                |                                                  |
|                | Result/Flow                                      |
|                v                                                  |
|         +------+------+                                           |
|         | Repository  |                                           |
|         | (Aggregate) |                                           |
|         +------+------+                                           |
|                |                                                  |
|                | Domain Model                                     |
|                v                                                  |
|         +------+------+                                           |
|         |  ViewModel  |                                           |
|         | (Map State) |                                           |
|         +------+------+                                           |
|                |                                                  |
|                | UiState (StateFlow)                              |
|                v                                                  |
|         +------+------+                                           |
|         |    VIEW     |                                           |
|         |  (Render)   |                                           |
|         +-------------+                                           |
+------------------------------------------------------------------+
```

### 4.2 Offline-First Data Strategy

```
+------------------------------------------------------------------+
|                  OFFLINE-FIRST STRATEGY                           |
|                                                                   |
|    +-------------------+                                          |
|    | User Action       |                                          |
|    +--------+----------+                                          |
|             |                                                     |
|             v                                                     |
|    +--------+----------+                                          |
|    | Check Network     |                                          |
|    +--------+----------+                                          |
|             |                                                     |
|    +--------+--------+                                            |
|    |                 |                                            |
|    v                 v                                            |
| ONLINE           OFFLINE                                          |
|    |                 |                                            |
|    v                 v                                            |
| +--+-------+    +----+------+                                     |
| | API Call |    | Queue     |                                     |
| +--+-------+    | Operation |                                     |
|    |            +----+------+                                     |
|    v                 |                                            |
| +--+-------+         |                                            |
| | Update   |         |                                            |
| | Local DB |<--------+                                            |
| +--+-------+         |                                            |
|    |                 |                                            |
|    v                 v                                            |
| +--+-------+    +----+------+                                     |
| | Sync     |    | Optimistic|                                     |
| | Complete |    | UI Update |                                     |
| +----------+    +----+------+                                     |
|                      |                                            |
|                      v                                            |
|              +-------+------+                                     |
|              | When Online: |                                     |
|              | Sync Queue   |                                     |
|              | Resolve      |                                     |
|              | Conflicts    |                                     |
|              +--------------+                                     |
+------------------------------------------------------------------+
```

---

## 5. API Layer Design

### 5.1 API Endpoints

```yaml
# Authentication
POST   /api/v1/auth/login
POST   /api/v1/auth/logout
POST   /api/v1/auth/refresh
POST   /api/v1/auth/mfa/verify
POST   /api/v1/auth/mfa/resend
POST   /api/v1/auth/password/reset
POST   /api/v1/auth/password/change

# Users
GET    /api/v1/users/me
PUT    /api/v1/users/me
GET    /api/v1/users/{id}
GET    /api/v1/users/care-circle/{circleId}/members

# Care Circles
GET    /api/v1/care-circles
POST   /api/v1/care-circles
GET    /api/v1/care-circles/{id}
PUT    /api/v1/care-circles/{id}
DELETE /api/v1/care-circles/{id}
POST   /api/v1/care-circles/{id}/members
DELETE /api/v1/care-circles/{id}/members/{userId}

# Visits
GET    /api/v1/visits
POST   /api/v1/visits
GET    /api/v1/visits/{id}
PUT    /api/v1/visits/{id}
DELETE /api/v1/visits/{id}
POST   /api/v1/visits/{id}/checkin
POST   /api/v1/visits/{id}/checkout
GET    /api/v1/visits/available-slots

# Visitors
GET    /api/v1/visitors
POST   /api/v1/visitors/request
GET    /api/v1/visitors/pending
POST   /api/v1/visitors/{id}/approve
POST   /api/v1/visitors/{id}/reject
DELETE /api/v1/visitors/{id}/revoke

# Rules
GET    /api/v1/rules
POST   /api/v1/rules
GET    /api/v1/rules/{id}
PUT    /api/v1/rules/{id}
DELETE /api/v1/rules/{id}
POST   /api/v1/rules/evaluate

# Calendar Sync
GET    /api/v1/calendar/sync/status
POST   /api/v1/calendar/sync/google
POST   /api/v1/calendar/sync/outlook
POST   /api/v1/calendar/sync/apple
DELETE /api/v1/calendar/sync/{provider}

# Notifications
GET    /api/v1/notifications
PUT    /api/v1/notifications/{id}/read
PUT    /api/v1/notifications/read-all
GET    /api/v1/notifications/preferences
PUT    /api/v1/notifications/preferences

# Analytics
GET    /api/v1/analytics/dashboard
GET    /api/v1/analytics/visits
GET    /api/v1/analytics/visitors
GET    /api/v1/analytics/export

# Video Calls
POST   /api/v1/video/initiate
POST   /api/v1/video/{id}/join
POST   /api/v1/video/{id}/end
GET    /api/v1/video/{id}/token
```

### 5.2 API Response Structure

```kotlin
// Standard API Response
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
    val meta: ResponseMeta? = null
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null
)

@Serializable
data class ResponseMeta(
    val page: Int? = null,
    val perPage: Int? = null,
    val total: Int? = null,
    val timestamp: Long
)

// Paginated Response
@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val page: Int,
    val perPage: Int,
    val totalItems: Int,
    val totalPages: Int
)
```

### 5.3 Ktor Client Configuration

```kotlin
// Network Module Architecture
object NetworkModule {

    fun provideHttpClient(
        authTokenProvider: AuthTokenProvider,
        logger: Logger
    ): HttpClient = HttpClient {

        // JSON Serialization
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }

        // Authentication
        install(Auth) {
            bearer {
                loadTokens {
                    BearerTokens(
                        accessToken = authTokenProvider.getAccessToken(),
                        refreshToken = authTokenProvider.getRefreshToken()
                    )
                }
                refreshTokens {
                    // Token refresh logic
                }
            }
        }

        // Logging
        install(Logging) {
            logger = logger
            level = LogLevel.BODY
        }

        // Timeout
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }

        // Default Request Config
        defaultRequest {
            url(BuildConfig.API_BASE_URL)
            contentType(ContentType.Application.Json)
        }
    }
}
```

---

## 6. Database Schema

### 6.1 SQLDelight Schema

```sql
-- schema.sq

-- Users Table
CREATE TABLE User (
    id TEXT NOT NULL PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    firstName TEXT NOT NULL,
    lastName TEXT NOT NULL,
    phone TEXT,
    role TEXT NOT NULL,
    avatarUrl TEXT,
    mfaEnabled INTEGER NOT NULL DEFAULT 0,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL
);

-- Care Circles Table
CREATE TABLE CareCircle (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    patientId TEXT NOT NULL,
    description TEXT,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL,
    FOREIGN KEY (patientId) REFERENCES User(id)
);

-- Care Circle Members (Junction Table)
CREATE TABLE CareCircleMember (
    id TEXT NOT NULL PRIMARY KEY,
    circleId TEXT NOT NULL,
    userId TEXT NOT NULL,
    role TEXT NOT NULL,
    joinedAt INTEGER NOT NULL,
    FOREIGN KEY (circleId) REFERENCES CareCircle(id),
    FOREIGN KEY (userId) REFERENCES User(id),
    UNIQUE(circleId, userId)
);

-- Visits Table
CREATE TABLE Visit (
    id TEXT NOT NULL PRIMARY KEY,
    careCircleId TEXT NOT NULL,
    visitorId TEXT NOT NULL,
    patientId TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    scheduledStart INTEGER NOT NULL,
    scheduledEnd INTEGER NOT NULL,
    actualStart INTEGER,
    actualEnd INTEGER,
    status TEXT NOT NULL DEFAULT 'SCHEDULED',
    visitType TEXT NOT NULL DEFAULT 'IN_PERSON',
    location TEXT,
    notes TEXT,
    createdBy TEXT NOT NULL,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL,
    FOREIGN KEY (careCircleId) REFERENCES CareCircle(id),
    FOREIGN KEY (visitorId) REFERENCES User(id),
    FOREIGN KEY (patientId) REFERENCES User(id)
);

CREATE INDEX visit_scheduled_start ON Visit(scheduledStart);
CREATE INDEX visit_status ON Visit(status);
CREATE INDEX visit_visitor ON Visit(visitorId);

-- Rules Table
CREATE TABLE Rule (
    id TEXT NOT NULL PRIMARY KEY,
    careCircleId TEXT NOT NULL,
    name TEXT NOT NULL,
    ruleType TEXT NOT NULL,
    config TEXT NOT NULL,
    isActive INTEGER NOT NULL DEFAULT 1,
    priority INTEGER NOT NULL DEFAULT 0,
    createdBy TEXT NOT NULL,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL,
    FOREIGN KEY (careCircleId) REFERENCES CareCircle(id)
);

CREATE INDEX rule_type ON Rule(ruleType);
CREATE INDEX rule_active ON Rule(isActive);

-- Notifications Table
CREATE TABLE Notification (
    id TEXT NOT NULL PRIMARY KEY,
    userId TEXT NOT NULL,
    type TEXT NOT NULL,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    data TEXT,
    isRead INTEGER NOT NULL DEFAULT 0,
    createdAt INTEGER NOT NULL,
    FOREIGN KEY (userId) REFERENCES User(id)
);

CREATE INDEX notification_user ON Notification(userId);
CREATE INDEX notification_read ON Notification(isRead);

-- Calendar Sync Table
CREATE TABLE CalendarSync (
    id TEXT NOT NULL PRIMARY KEY,
    userId TEXT NOT NULL,
    provider TEXT NOT NULL,
    externalCalendarId TEXT NOT NULL,
    accessToken TEXT NOT NULL,
    refreshToken TEXT,
    tokenExpiry INTEGER,
    lastSyncAt INTEGER,
    isActive INTEGER NOT NULL DEFAULT 1,
    FOREIGN KEY (userId) REFERENCES User(id),
    UNIQUE(userId, provider)
);

-- Pending Sync Queue (Offline Support)
CREATE TABLE SyncQueue (
    id TEXT NOT NULL PRIMARY KEY,
    entityType TEXT NOT NULL,
    entityId TEXT NOT NULL,
    operation TEXT NOT NULL,
    payload TEXT NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    lastAttempt INTEGER,
    createdAt INTEGER NOT NULL
);

CREATE INDEX sync_queue_created ON SyncQueue(createdAt);
```

### 6.2 Entity Relationship Diagram

```
+------------------------------------------------------------------+
|                    ENTITY RELATIONSHIPS                           |
|                                                                   |
|    +--------+         +-------------+         +--------+          |
|    |  User  |-------->| CareCircle  |<--------|  User  |          |
|    |(Patient)|   1:N  |   Member    |   N:1   |(Visitor)|         |
|    +--------+         +------+------+         +--------+          |
|         |                    |                     |              |
|         |                    |                     |              |
|         v                    v                     v              |
|    +----+----+         +-----+-----+         +-----+-----+        |
|    |  Care   |<--------|   Visit   |-------->|  Check    |        |
|    | Circle  |   1:N   |           |   1:1   |   In/Out  |        |
|    +----+----+         +-----+-----+         +-----------+        |
|         |                    |                                    |
|         |                    |                                    |
|         v                    v                                    |
|    +----+----+         +-----+-----+                              |
|    |  Rule   |         |Notification|                             |
|    |         |         |           |                              |
|    +---------+         +-----------+                              |
|                                                                   |
+------------------------------------------------------------------+
```

---

## 7. Security Architecture

### 7.1 Security Layers

```
+------------------------------------------------------------------+
|                    SECURITY ARCHITECTURE                          |
|                                                                   |
|  +--------------------------------------------------------------+ |
|  |                    APPLICATION LAYER                         | |
|  |                                                              | |
|  |  +------------------+  +------------------+                   | |
|  |  | Input Validation |  | Output Encoding  |                   | |
|  |  | - Sanitization   |  | - XSS Prevention |                   | |
|  |  | - Type checking  |  | - Data masking   |                   | |
|  |  +------------------+  +------------------+                   | |
|  |                                                              | |
|  |  +------------------+  +------------------+                   | |
|  |  | Auth & AuthZ     |  | Session Mgmt     |                   | |
|  |  | - JWT tokens     |  | - Secure tokens  |                   | |
|  |  | - MFA            |  | - Timeout        |                   | |
|  |  | - RBAC           |  | - Revocation     |                   | |
|  |  +------------------+  +------------------+                   | |
|  +--------------------------------------------------------------+ |
|                                                                   |
|  +--------------------------------------------------------------+ |
|  |                    DATA LAYER                                | |
|  |                                                              | |
|  |  +------------------+  +------------------+                   | |
|  |  | Encryption Rest  |  | Encryption Trans |                   | |
|  |  | - AES-256-GCM    |  | - TLS 1.3        |                   | |
|  |  | - Key rotation   |  | - Cert pinning   |                   | |
|  |  +------------------+  +------------------+                   | |
|  |                                                              | |
|  |  +------------------+  +------------------+                   | |
|  |  | Secure Storage   |  | Data Handling    |                   | |
|  |  | - Keychain (iOS) |  | - PHI isolation  |                   | |
|  |  | - Keystore (And) |  | - Audit logging  |                   | |
|  |  +------------------+  +------------------+                   | |
|  +--------------------------------------------------------------+ |
|                                                                   |
|  +--------------------------------------------------------------+ |
|  |                    NETWORK LAYER                             | |
|  |                                                              | |
|  |  +------------------+  +------------------+                   | |
|  |  | Certificate Pin  |  | Network Security |                   | |
|  |  | - SHA-256 pins   |  | - No HTTP        |                   | |
|  |  | - Backup pins    |  | - HSTS           |                   | |
|  |  +------------------+  +------------------+                   | |
|  +--------------------------------------------------------------+ |
+------------------------------------------------------------------+
```

### 7.2 HIPAA Compliance Matrix

```
+------------------------------------------------------------------+
|                  HIPAA COMPLIANCE REQUIREMENTS                    |
|                                                                   |
|  ADMINISTRATIVE SAFEGUARDS                                        |
|  +--------------------------+----------------------------------+  |
|  | Requirement              | Implementation                   |  |
|  +--------------------------+----------------------------------+  |
|  | Access Control           | RBAC with role hierarchy         |  |
|  | Audit Controls           | Comprehensive audit logging      |  |
|  | Integrity Controls       | Data validation, checksums       |  |
|  | Transmission Security    | TLS 1.3, certificate pinning     |  |
|  +--------------------------+----------------------------------+  |
|                                                                   |
|  TECHNICAL SAFEGUARDS                                             |
|  +--------------------------+----------------------------------+  |
|  | Requirement              | Implementation                   |  |
|  +--------------------------+----------------------------------+  |
|  | Encryption (Rest)        | AES-256-GCM                      |  |
|  | Encryption (Transit)     | TLS 1.3                          |  |
|  | Authentication           | MFA, Biometrics, JWT             |  |
|  | Access Controls          | Session timeout, token expiry    |  |
|  | Audit Logging            | Immutable logs, timestamps       |  |
|  +--------------------------+----------------------------------+  |
|                                                                   |
|  PHYSICAL SAFEGUARDS                                              |
|  +--------------------------+----------------------------------+  |
|  | Requirement              | Implementation                   |  |
|  +--------------------------+----------------------------------+  |
|  | Device Access Control    | Biometric, PIN lock requirement  |  |
|  | Workstation Security     | Screen lock, secure storage      |  |
|  | Data Disposal            | Secure wipe on logout/uninstall  |  |
|  +--------------------------+----------------------------------+  |
+------------------------------------------------------------------+
```

### 7.3 Authentication Flow

```
+------------------------------------------------------------------+
|                   AUTHENTICATION FLOW                             |
|                                                                   |
|    +--------+                                                     |
|    | User   |                                                     |
|    +---+----+                                                     |
|        |                                                          |
|        | 1. Login (email/password)                                |
|        v                                                          |
|    +---+----+     +------------+                                  |
|    | Client |---->| Auth API   |                                  |
|    +---+----+     +-----+------+                                  |
|        |                |                                         |
|        |                | 2. Validate credentials                 |
|        |                v                                         |
|        |          +-----+------+                                  |
|        |          | User DB    |                                  |
|        |          +-----+------+                                  |
|        |                |                                         |
|        |                | 3. Check MFA requirement                |
|        |                v                                         |
|        |          +-----+------+                                  |
|        |          | MFA Check  |                                  |
|        |          +-----+------+                                  |
|        |                |                                         |
|        |    +-----------+-----------+                             |
|        |    |                       |                             |
|        |    v                       v                             |
|        | MFA Required           No MFA                            |
|        |    |                       |                             |
|        |    v                       |                             |
|    +---+----+----+                  |                             |
|    | Enter MFA   |                  |                             |
|    | Code/TOTP   |                  |                             |
|    +---+----+----+                  |                             |
|        |    |                       |                             |
|        |    v                       |                             |
|        |  Verify                    |                             |
|        |    |                       |                             |
|        +----+-----------+-----------+                             |
|                         |                                         |
|                         v                                         |
|                   +-----+------+                                  |
|                   | Generate   |                                  |
|                   | JWT Tokens |                                  |
|                   | - Access   |                                  |
|                   | - Refresh  |                                  |
|                   +-----+------+                                  |
|                         |                                         |
|                         v                                         |
|                   +-----+------+                                  |
|                   | Store in   |                                  |
|                   | Secure     |                                  |
|                   | Storage    |                                  |
|                   +------------+                                  |
+------------------------------------------------------------------+
```

---

## 8. Technology Stack

### 8.1 Core Technologies

| Layer | Technology | Version | Purpose |
|-------|------------|---------|---------|
| Language | Kotlin | 2.0+ | Primary language |
| UI | Compose Multiplatform | 1.6+ | Cross-platform UI |
| Networking | Ktor Client | 2.3+ | HTTP client |
| Database | SQLDelight | 2.0+ | Local persistence |
| DI | Koin | 3.5+ | Dependency injection |
| Serialization | kotlinx-serialization | 1.6+ | JSON parsing |
| Datetime | kotlinx-datetime | 0.5+ | Date/time handling |
| Async | Coroutines + Flow | 1.8+ | Reactive streams |
| Navigation | Voyager / Decompose | Latest | Navigation |
| Image Loading | Coil | 3.0+ | Image loading |

### 8.2 Platform-Specific

| Platform | Technology | Purpose |
|----------|------------|---------|
| Android | AndroidX Security | Encrypted SharedPreferences |
| Android | BiometricPrompt | Biometric authentication |
| Android | Firebase Cloud Messaging | Push notifications |
| iOS | Keychain Services | Secure storage |
| iOS | LocalAuthentication | Biometric authentication |
| iOS | APNs | Push notifications |

### 8.3 Backend Integration

| Service | Technology | Purpose |
|---------|------------|---------|
| Video | Twilio / Agora | Video calling |
| Push | Firebase / APNs | Notifications |
| Email | SendGrid | Email delivery |
| SMS | Twilio | SMS delivery |
| Calendar | Google/Microsoft APIs | Calendar sync |

---

## 9. Deployment Architecture

### 9.1 CI/CD Pipeline

```
+------------------------------------------------------------------+
|                      CI/CD PIPELINE                               |
|                                                                   |
|    +------------+                                                 |
|    | Developer  |                                                 |
|    | Push       |                                                 |
|    +-----+------+                                                 |
|          |                                                        |
|          v                                                        |
|    +-----+------+                                                 |
|    | GitHub     |                                                 |
|    | Actions    |                                                 |
|    +-----+------+                                                 |
|          |                                                        |
|    +-----+------+------+------+                                   |
|    |            |      |      |                                   |
|    v            v      v      v                                   |
| +--+---+   +---+--+ +--+--+ +--+--+                               |
| | Lint |   | Test | |Build| |Scan |                               |
| +--+---+   +---+--+ +--+--+ +--+--+                               |
|    |           |       |       |                                  |
|    +-----------+---+---+-------+                                  |
|                    |                                              |
|                    v                                              |
|              +-----+------+                                       |
|              | All Passed |                                       |
|              +-----+------+                                       |
|                    |                                              |
|          +---------+---------+                                    |
|          |                   |                                    |
|          v                   v                                    |
|    +-----+------+     +------+-----+                              |
|    | Android    |     | iOS        |                              |
|    | Build      |     | Build      |                              |
|    +-----+------+     +------+-----+                              |
|          |                   |                                    |
|          v                   v                                    |
|    +-----+------+     +------+-----+                              |
|    | Play Store |     | App Store  |                              |
|    | Deploy     |     | Deploy     |                              |
|    +------------+     +------------+                              |
+------------------------------------------------------------------+
```

### 9.2 Environment Configuration

```
+------------------------------------------------------------------+
|                     ENVIRONMENTS                                  |
|                                                                   |
|  +------------------+  +------------------+  +------------------+  |
|  | DEVELOPMENT      |  | STAGING          |  | PRODUCTION       |  |
|  |                  |  |                  |  |                  |  |
|  | - Debug builds   |  | - Release builds |  | - Release builds |  |
|  | - Mock services  |  | - Test API       |  | - Prod API       |  |
|  | - Verbose logs   |  | - Limited logs   |  | - Minimal logs   |  |
|  | - No analytics   |  | - Test analytics |  | - Full analytics |  |
|  +------------------+  +------------------+  +------------------+  |
|                                                                   |
|  API Endpoints:                                                   |
|  - Dev:  https://dev-api.visischeduler.com                       |
|  - Stg:  https://staging-api.visischeduler.com                   |
|  - Prod: https://api.visischeduler.com                           |
+------------------------------------------------------------------+
```

---

## Appendix A: Coding Standards

### A.1 Naming Conventions

```kotlin
// Classes: PascalCase
class ScheduleViewModel
class CreateVisitUseCase

// Functions: camelCase
fun getAvailableSlots()
fun validateBufferTime()

// Constants: SCREAMING_SNAKE_CASE
const val MAX_DAILY_VISITS = 10
const val DEFAULT_BUFFER_MINUTES = 15

// Variables: camelCase
val currentUser: User
var isLoading: Boolean

// Packages: lowercase
package com.markduenas.visischeduler.domain.usecase
```

### A.2 Architecture Patterns

- **MVVM + Clean Architecture** for presentation layer
- **Repository Pattern** for data abstraction
- **Use Case Pattern** for business logic encapsulation
- **Dependency Injection** via Koin
- **Unidirectional Data Flow** for state management

---

## Appendix B: Error Handling Strategy

```kotlin
// Result wrapper for all operations
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: AppException) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

// Centralized error types
sealed class AppException(message: String) : Exception(message) {
    class NetworkException(message: String) : AppException(message)
    class AuthException(message: String) : AppException(message)
    class ValidationException(message: String) : AppException(message)
    class BusinessRuleException(message: String) : AppException(message)
    class UnknownException(message: String) : AppException(message)
}
```

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0.0 | 2026-01-23 | Architecture Team | Initial architecture design |
