import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

val localProperties = Properties()
val localPropertiesFile = project.rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    namespace = "com.mynigga.chatapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mynigga.chatapp"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val sUrl = localProperties.getProperty("supabase.url") ?: ""
        val sKey = localProperties.getProperty("supabase.key") ?: ""
        
        buildConfigField("String", "SUPABASE_URL", "\"$sUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$sKey\"")
        
        buildConfigField("String", "AVATAR_1", "\"https://api.dicebear.com/7.x/avataaars/svg?seed=Felix\"")
        buildConfigField("String", "AVATAR_2", "\"https://api.dicebear.com/7.x/avataaars/svg?seed=Aner\"")
        buildConfigField("String", "AVATAR_3", "\"https://api.dicebear.com/7.x/avataaars/svg?seed=Jack\"")
        buildConfigField("String", "AVATAR_4", "\"https://api.dicebear.com/7.x/avataaars/svg?seed=Luna\"")
        buildConfigField("String", "AVATAR_5", "\"https://api.dicebear.com/7.x/avataaars/svg?seed=Milo\"")
        buildConfigField("String", "AVATAR_6", "\"https://api.dicebear.com/7.x/avataaars/svg?seed=Zoe\"")
        buildConfigField("String", "AVATAR_7", "\"https://api.dicebear.com/7.x/avataaars/svg?seed=Leo\"")
        buildConfigField("String", "AVATAR_8", "\"https://api.dicebear.com/7.x/avataaars/svg?seed=Mia\"")
        buildConfigField("String", "AVATAR_9", "\"https://api.dicebear.com/7.x/avataaars/svg?seed=Max\"")
        buildConfigField("String", "AVATAR_10", "\"https://api.dicebear.com/7.x/avataaars/svg?seed=Ivy\"")
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
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.material.icons.extended)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.analytics)
    implementation(libs.play.services.auth)

    // Supabase
    implementation(platform(libs.supabaseBom))
    implementation(libs.supabasePostgrest)
    implementation(libs.supabaseStorage)
    implementation(libs.ktorClientAndroid)

    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
