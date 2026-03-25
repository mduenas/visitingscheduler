import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    @Suppress("DEPRECATION")
    sourceSets {
        androidMain.dependencies {
            implementation(projects.shared)

            // Compose Multiplatform
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            // AndroidX
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.runtime)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.compose)

            // Security (HIPAA compliance)
            implementation(libs.androidx.security.crypto)
            implementation(libs.androidx.biometric)
            implementation(libs.androidx.work.runtime)

            // Koin DI
            implementation(libs.koin.android)
            implementation(libs.koin.compose)

            // Navigation
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.screenmodel)
            implementation(libs.voyager.transitions)
            implementation(libs.voyager.koin)

            // Image Loading
            implementation(libs.coil.compose)

            // Logging
            implementation(libs.napier)
        }
    }
}

android {
    namespace = "com.markduenas.visischeduler"
    compileSdk = 35

    // Load local signing config (gitignored). CI/CD uses env vars instead.
    val localProps = Properties().also { props ->
        val f = rootProject.file("local.properties")
        if (f.exists()) props.load(f.inputStream())
    }
    fun signingProp(envKey: String, localKey: String, fallback: String = "") =
        System.getenv(envKey) ?: localProps.getProperty(localKey) ?: fallback

    signingConfigs {
        create("release") {
            storeFile = file(signingProp("KEYSTORE_PATH", "signing.storeFile", "../keystore/upload_keystore.jks"))
            storePassword = signingProp("KEYSTORE_PASSWORD", "signing.storePassword")
            keyAlias = signingProp("KEY_ALIAS", "signing.keyAlias")
            keyPassword = signingProp("KEY_PASSWORD", "signing.keyPassword")
        }
    }

    defaultConfig {
        applicationId = "com.markduenas.visischeduler"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // BuildConfig fields for environment configuration
        buildConfigField("String", "API_BASE_URL", "\"https://api.visischeduler.com/v1\"")
        buildConfigField("Boolean", "ENABLE_LOGGING", "true")
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("String", "API_BASE_URL", "\"https://dev-api.visischeduler.com/v1\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
        }

        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_BASE_URL", "\"https://api.visischeduler.com/v1\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "false")
        }

        create("staging") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            buildConfigField("String", "API_BASE_URL", "\"https://staging-api.visischeduler.com/v1\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/previous-compilation-data.bin"
        }
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        disable += setOf("ObsoleteLintCustomCheck")
    }
}

dependencies {
    // Compose BOM for consistent versions
    implementation(platform(libs.compose.bom))

    // Firebase BOM for consistent versions
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.storage)

    // AdMob
    implementation(libs.admob.android)

    // In-App Purchases (Billing)
    implementation(libs.billing.ktx)

    // Debug dependencies
    debugImplementation(libs.compose.ui.tooling)
}
