plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    kotlin("native.cocoapods")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
    id("org.jetbrains.kotlinx.kover") version "0.7.5"
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // iOS targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    // CocoaPods configuration for iOS dependencies
    cocoapods {
        summary = "VisiScheduler shared module"
        homepage = "https://github.com/markduenas/visischeduler"
        version = "1.0"
        ios.deploymentTarget = "15.0"
        podfile = project.file("../iosApp/Podfile")

        framework {
            baseName = "shared"
            isStatic = true // Static linking to avoid framework lookup issues
        }

        // Firebase pods required by GitLive SDK
        pod("FirebaseCore") {
            version = "~> 12.1"
        }
        pod("FirebaseFirestore") {
            version = "~> 12.1"
        }
        pod("FirebaseAuth") {
            version = "~> 12.1"
        }
    }

    // Add opt-in for experimental APIs
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
        freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalDuration")
        freeCompilerArgs.add("-opt-in=kotlin.ExperimentalStdlibApi")
        freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
        freeCompilerArgs.add("-opt-in=kotlinx.coroutines.FlowPreview")
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    @Suppress("DEPRECATION")
    sourceSets {
        commonMain.dependencies {
            // Compose Multiplatform
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)

            // Navigation - Voyager
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.tab.navigator)
            implementation(libs.voyager.screenmodel)
            implementation(libs.voyager.transitions)
            implementation(libs.voyager.koin)
            implementation(libs.voyager.bottom.sheet.navigator)

            // Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // Serialization
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.auth)

            // SQLDelight
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)

            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            // GitLive Firebase KMP SDK (cross-platform Firestore & Auth)
            implementation(libs.gitlive.firebase.app)
            implementation(libs.gitlive.firebase.auth)
            implementation(libs.gitlive.firebase.firestore)
        }

        commonTest.dependencies {
            // Core testing
            implementation(kotlin("test"))
            implementation(kotlin("test-common"))
            implementation(kotlin("test-annotations-common"))

            // Coroutines testing
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

            // Flow testing - Turbine
            implementation("app.cash.turbine:turbine:1.1.0")

            // Koin testing
            implementation("io.insert-koin:koin-test:4.0.4")

            // SQLDelight test driver
            implementation("app.cash.sqldelight:sqlite-driver:2.0.2")
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.koin.android)

            // Billing
            implementation("com.android.billingclient:billing-ktx:6.1.0")

            // Firebase (explicit versions without BOM)
            implementation("com.google.firebase:firebase-firestore-ktx:25.1.4")
            implementation("com.google.firebase:firebase-auth-ktx:23.2.0")
            implementation("com.google.firebase:firebase-analytics-ktx:22.4.0")
            implementation("com.google.firebase:firebase-crashlytics-ktx:19.4.2")

            // Security - EncryptedSharedPreferences
            implementation("androidx.security:security-crypto:1.1.0-alpha06")

            // Google Ads (AdMob)
            implementation("com.google.android.gms:play-services-ads:23.6.0")

            // Camera (QR scanning)
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.mlkit.barcode.scanning)
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("junit:junit:4.13.2")
                implementation("io.mockk:mockk:1.13.16")
                implementation("io.mockk:mockk-agent-jvm:1.13.16")
                implementation("org.robolectric:robolectric:4.14.1")
                implementation("androidx.test:core:1.6.1")
                implementation("androidx.test.ext:junit:1.2.1")
            }
        }

        val iosMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.ktor.client.darwin)
                implementation(libs.sqldelight.native.driver)
            }
        }

        val iosX64Main by getting { dependsOn(iosMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }

        val iosTest by creating {
            dependsOn(commonTest.get())
        }

        val iosX64Test by getting { dependsOn(iosTest) }
        val iosArm64Test by getting { dependsOn(iosTest) }
        val iosSimulatorArm64Test by getting { dependsOn(iosTest) }
    }
}

android {
    namespace = "com.markduenas.visischeduler.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("VisiSchedulerDatabase") {
            packageName.set("com.markduenas.visischeduler.data.local")
        }
    }
}

// Kover coverage configuration
koverReport {
    filters {
        excludes {
            // Exclude generated code
            classes("*_Factory", "*_HiltModules*", "*Hilt_*", "*_Impl")
            // Exclude DI modules
            packages("*.di")
            // Exclude BuildConfig
            classes("*.BuildConfig")
            // Exclude SQLDelight generated code
            packages("com.markduenas.visischeduler.data.local")
        }
    }

    defaults {
        html {
            onCheck = true
        }
        xml {
            onCheck = true
        }
    }

    verify {
        rule {
            isEnabled = true
            entity = kotlinx.kover.gradle.plugin.dsl.GroupingEntityType.APPLICATION

            bound {
                minValue = 80
                metric = kotlinx.kover.gradle.plugin.dsl.MetricType.LINE
                aggregation = kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE
            }

            bound {
                minValue = 75
                metric = kotlinx.kover.gradle.plugin.dsl.MetricType.BRANCH
                aggregation = kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE
            }
        }
    }
}

tasks.withType<Test> {
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }

    // Fail fast on CI
    if (System.getenv("CI") == "true") {
        failFast = true
    }
}
