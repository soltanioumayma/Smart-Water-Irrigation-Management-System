plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

/** Lit GEMINI_API_KEY depuis local.properties (sans java.util, compatible Gradle Kotlin DSL). */
fun readGeminiApiKeyFromLocalProperties(): String {
    val f = rootProject.file("local.properties")
    if (!f.exists()) return ""
    f.readLines().forEach { line ->
        val t = line.trim()
        if (t.startsWith("GEMINI_API_KEY=")) {
            return t.removePrefix("GEMINI_API_KEY=").trim().removeSurrounding("\"")
        }
    }
    return ""
}

/** Lit HF_TOKEN depuis local.properties */
fun readHfTokenFromLocalProperties(): String {
    val f = rootProject.file("local.properties")
    if (!f.exists()) return ""
    f.readLines().forEach { line ->
        val t = line.trim()
        if (t.startsWith("HF_TOKEN=")) {
            return t.removePrefix("HF_TOKEN=").trim().removeSurrounding("\"")
        }
    }
    return ""
}

val geminiApiKey: String = readGeminiApiKeyFromLocalProperties()
val hfToken: String = readHfTokenFromLocalProperties()

android {
    namespace = "com.example.smart_water_projet"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.smart_water_projet"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GEMINI_API_KEY", "\"${geminiApiKey.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "HF_TOKEN", "\"${hfToken.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.okhttp)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // 🔥 Firebase Dependencies - VERSION STABLE ET COMPATIBLE
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-database")


    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}