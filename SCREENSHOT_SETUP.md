# Screenshot Generation with Roborazzi for KMP Projects

This document describes how to set up automated screenshot generation for Kotlin Multiplatform (KMP) Compose projects using Roborazzi.

## Overview

Roborazzi is a screenshot testing library that works with Robolectric to render Compose UI without requiring an emulator or device. It's particularly well-suited for KMP projects because it handles Compose Multiplatform's resource system correctly.

## Why Roborazzi over Paparazzi?

- **Better KMP compatibility**: Paparazzi has issues with Compose Multiplatform's font and resource loading
- **Native graphics mode**: Uses Robolectric's native graphics rendering for accurate output
- **Compose UI testing integration**: Works seamlessly with `createComposeRule()`

## Setup Instructions

### 1. Add Dependencies to Version Catalog

In `gradle/libs.versions.toml`:

```toml
[versions]
roborazzi = "1.42.0"
robolectric = "4.14.1"

[libraries]
roborazzi = { module = "io.github.takahirom.roborazzi:roborazzi", version.ref = "roborazzi" }
roborazzi-compose = { module = "io.github.takahirom.roborazzi:roborazzi-compose", version.ref = "roborazzi" }
roborazzi-rule = { module = "io.github.takahirom.roborazzi:roborazzi-junit-rule", version.ref = "roborazzi" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }

[plugins]
roborazzi = { id = "io.github.takahirom.roborazzi", version.ref = "roborazzi" }
```

### 2. Add Plugin to Root build.gradle.kts

```kotlin
plugins {
    // ... other plugins
    alias(libs.plugins.roborazzi) apply false
}
```

### 3. Create Screenshots Module

Create a new module `screenshots/` with `build.gradle.kts`:

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.example.screenshots"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("robolectric.graphicsMode", "NATIVE")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(project(":composeApp"))  // Your KMP module
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.rule)
    testImplementation("androidx.compose.ui:ui-test-junit4:1.8.3")
    testImplementation("androidx.compose.ui:ui-test-manifest:1.8.3")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")
}
```

### 4. Include Module in settings.gradle.kts

```kotlin
include(":composeApp")
include(":screenshots")
```

### 5. Create Screenshot Tests

Create test files in `screenshots/src/test/kotlin/`:

```kotlin
package com.example.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel6)
class ScreenshotTests {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun myScreen_screenshot() {
        composeRule.setContent {
            ScreenshotWrapper {
                MyScreen(/* demo data */)
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/myScreen.png")
    }
}

@Composable
private fun ScreenshotWrapper(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MyAppTheme(darkTheme = darkTheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            content()
        }
    }
}
```

## Commands

### Record Screenshots (Generate/Update)
```bash
./gradlew :screenshots:recordRoborazziDebug
```

### Verify Screenshots (CI/Testing)
```bash
./gradlew :screenshots:verifyRoborazziDebug
```

### Clean and Re-record
```bash
./gradlew :screenshots:cleanRecordRoborazziDebug
```

## Output Location

Screenshots are saved to: `screenshots/screenshots/*.png`

## Device Configurations

### Google Play Store (Android)

Roborazzi provides device qualifiers for common Android devices:

```kotlin
// Pixel 6 (1080x2400) - Recommended for Play Store
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel6)

// Pixel 7 Pro (1440x3120)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7Pro)

// Small phone (720x1280)
@Config(qualifiers = RobolectricDeviceQualifiers.SmallPhone)

// Tablet
@Config(qualifiers = RobolectricDeviceQualifiers.NexusTablet)
```

### Apple App Store (iOS)

App Store requires **exact pixel dimensions**. Use custom qualifiers with xxhdpi (480 dpi) density:

```kotlin
// iPhone 6.7" (1290 x 2796) - iPhone 15 Pro Max, 14 Pro Max
// Calculation: 1290/3 = 430dp, 2796/3 = 932dp
@Config(sdk = [34], qualifiers = "w430dp-h932dp-xxhdpi")

// iPhone 6.5" (1284 x 2778) - iPhone 14 Plus, 13 Pro Max, 12 Pro Max, 11 Pro Max
// Calculation: 1284/3 = 428dp, 2778/3 = 926dp
@Config(sdk = [34], qualifiers = "w428dp-h926dp-xxhdpi")

// iPhone 5.5" (1242 x 2208) - iPhone 8 Plus, 7 Plus, 6s Plus
// Calculation: 1242/3 = 414dp, 2208/3 = 736dp
@Config(sdk = [34], qualifiers = "w414dp-h736dp-xxhdpi")
```

**How the calculation works:**
- App Store requires exact pixel dimensions
- Using xxhdpi (480 dpi): `pixels = dp * 3`
- So: `dp = pixels / 3`
- Width and height in dp go into the qualifier string

### Output Organization

For multi-store support, organize screenshots by platform:

```
screenshots/
├── screenshots/           # Play Store (Pixel 6)
│   ├── homeScreen.png
│   └── ...
└── ios/
    ├── iphone67/         # App Store 6.7"
    │   ├── homeScreen.png
    │   └── ...
    ├── iphone65/         # App Store 6.5"
    │   └── ...
    └── iphone55/         # App Store 5.5"
        └── ...
```

## Tips for Store Screenshots

### Play Store
1. **Resolution**: Pixel 6 (1080x2400) is ideal
2. **Aspect ratio**: 16:9 or 9:16 recommended
3. **File format**: PNG or JPEG
4. **Max size**: 8MB per image

### App Store
1. **Exact dimensions required** - no flexibility
2. **iPhone 6.7"**: 1290 x 2796 (required for iPhone 15 Pro Max)
3. **iPhone 6.5"**: 1284 x 2778 (required for older Max models)
4. **iPhone 5.5"**: 1242 x 2208 (required for Plus models)
5. **File format**: PNG or JPEG

### Both Stores
1. **Both themes**: Generate light and dark mode screenshots
2. **Demo data**: Use realistic but impressive demo data
3. **Key screens**: Home, main gameplay, results/achievements, settings
4. **Naming convention**: Use descriptive names like `homeScreen_experiencedPlayer.png`

## Troubleshooting

### "NoSuchElementException" or Font errors with Paparazzi
- Use Roborazzi instead - it handles Compose Multiplatform resources correctly

### Screenshots are blank
- Ensure `isIncludeAndroidResources = true` in testOptions
- Verify `robolectric.graphicsMode = NATIVE` system property

### Tests not found
- Ensure you have both `kotlinAndroid` plugin and proper source set structure
- Test files should be in `src/test/kotlin/` not `src/androidTest/`

### Compose resources not loading
- Make sure your KMP module is properly configured with compose resources
- The screenshots module depends on your composeApp module

## Project Structure

```
project/
├── composeApp/                 # KMP Compose module
│   └── src/
│       └── commonMain/
│           └── kotlin/
│               └── ui/screens/ # Your screens
├── screenshots/                # Screenshot module
│   ├── build.gradle.kts
│   ├── src/
│   │   ├── main/kotlin/       # Empty (required)
│   │   └── test/kotlin/
│   │       └── screenshots/
│   │           └── ScreenshotTests.kt
│   └── screenshots/           # Generated PNG files
│       ├── homeScreen.png
│       └── ...
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/libs.versions.toml
```
