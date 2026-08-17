import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Release signing is read from an untracked keystore.properties (see keystore.properties.sample).
// Absent that file (e.g. CI debug builds), the release signingConfig is simply not attached.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) load(keystorePropsFile.inputStream())
}

android {
    namespace = "com.middleeastcontainer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.middleeastcontainer" // same package as legacy (Q5)
        minSdk = 29                                // Android 10 (Q1)
        targetSdk = 35
        versionCode = 2                            // legacy shipped versionCode 1
        versionName = "2.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Q8: Under_Floor stays out of the /container/test payload by default.
        // Under_Floor IS uploaded. The legacy backend never received it, so this
        // was off while that server was the target; the MECRC server accepts all
        // 11 sides, and leaving it off silently discarded a photo inspectors had
        // taken (it was purged with the container after upload).
        buildConfigField("boolean", "INCLUDE_UNDER_FLOOR_IN_TEST_PAYLOAD", "true")

        // Upload image sizing. 640px keeps a 10-side POST near ~5 MB, under PHP's
        // usual 8 MB post_max_size. Raise once the real server is confirmed.
        buildConfigField("int", "UPLOAD_IMAGE_MAX_EDGE", "1280")
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    // Environment flavors. The frozen backend contract (Q2) is host-parameterized here:
    // point a build at staging to run the wire-validation without touching code.
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "MAIN_BASE_URL", "\"http://testrun.adlibsol.com/container_web/\"")
            buildConfigField("String", "EXTRA_BASE_URL", "\"http://testrun.adlibsol.com/container_web/\"")
        }
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            // TODO: replace with the confirmed staging host before the R1 validation run.
            buildConfigField("String", "MAIN_BASE_URL", "\"http://testrun.adlibsol.com/container_web/\"")
            buildConfigField("String", "EXTRA_BASE_URL", "\"http://testrun.adlibsol.com/container_web/\"")
        }
        create("prod") {
            dimension = "environment"
            // TODO: replace with the production host (prefer HTTPS) once confirmed.
            buildConfigField("String", "MAIN_BASE_URL", "\"http://testrun.adlibsol.com/container_web/\"")
            buildConfigField("String", "EXTRA_BASE_URL", "\"http://testrun.adlibsol.com/container_web/\"")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            // Every phone in the yard is arm64. Packaging the other three
            // architectures adds roughly 30 MB of ML Kit native libraries that
            // nothing installs, and slows every build to produce them.
            // Release keeps all four, so distribution is never restricted.
            ndk { abiFilters += "arm64-v8a" }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false                   // L9: never ship debuggable
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.work.compiler)

    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.text.recognition)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
