plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val firebaseProjectId = providers.gradleProperty("FOCUS_RAID_FIREBASE_PROJECT_ID")
    .orElse(providers.environmentVariable("FOCUS_RAID_FIREBASE_PROJECT_ID"))
    .getOrElse("")
val firebaseApiKey = providers.gradleProperty("FOCUS_RAID_FIREBASE_API_KEY")
    .orElse(providers.environmentVariable("FOCUS_RAID_FIREBASE_API_KEY"))
    .getOrElse("")
val firebaseAppId = providers.gradleProperty("FOCUS_RAID_FIREBASE_APP_ID")
    .orElse(providers.environmentVariable("FOCUS_RAID_FIREBASE_APP_ID"))
    .getOrElse("")
val revenueCatGoogleApiKey = providers.gradleProperty("FOCUS_RAID_REVENUECAT_GOOGLE_API_KEY")
    .orElse(providers.environmentVariable("FOCUS_RAID_REVENUECAT_GOOGLE_API_KEY"))
    .getOrElse("")

val uploadKeystorePath = providers.gradleProperty("FOCUS_RAID_UPLOAD_KEYSTORE_PATH")
    .orElse(providers.environmentVariable("FOCUS_RAID_UPLOAD_KEYSTORE_PATH"))
    .getOrElse("")
val uploadStorePassword = providers.gradleProperty("FOCUS_RAID_UPLOAD_STORE_PASSWORD")
    .orElse(providers.environmentVariable("FOCUS_RAID_UPLOAD_STORE_PASSWORD"))
    .getOrElse("")
val uploadKeyAlias = providers.gradleProperty("FOCUS_RAID_UPLOAD_KEY_ALIAS")
    .orElse(providers.environmentVariable("FOCUS_RAID_UPLOAD_KEY_ALIAS"))
    .getOrElse("")
val uploadKeyPassword = providers.gradleProperty("FOCUS_RAID_UPLOAD_KEY_PASSWORD")
    .orElse(providers.environmentVariable("FOCUS_RAID_UPLOAD_KEY_PASSWORD"))
    .getOrElse("")
val releaseSigningConfigured = listOf(
    uploadKeystorePath,
    uploadStorePassword,
    uploadKeyAlias,
    uploadKeyPassword,
).all { it.isNotBlank() }

fun quotedBuildConfig(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.madowaku.focusraid"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.madowaku.focusraid"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        buildConfigField("String", "FIREBASE_PROJECT_ID", quotedBuildConfig(firebaseProjectId))
        buildConfigField("String", "FIREBASE_API_KEY", quotedBuildConfig(firebaseApiKey))
        buildConfigField("String", "FIREBASE_APP_ID", quotedBuildConfig(firebaseAppId))
        buildConfigField(
            "String",
            "REVENUECAT_GOOGLE_API_KEY",
            quotedBuildConfig(revenueCatGoogleApiKey),
        )
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("releaseUpload") {
                storeFile = file(uploadKeystorePath)
                storePassword = uploadStorePassword
                keyAlias = uploadKeyAlias
                keyPassword = uploadKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("releaseUpload")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.fragment:fragment:1.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.5.0-alpha27")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    implementation("com.revenuecat.purchases:purchases:10.15.1")

    val firebaseBom = platform("com.google.firebase:firebase-bom:34.4.0")
    implementation(firebaseBom)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
