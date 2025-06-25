plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
}

android {
    namespace = "com.example.royalfreshapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.royalfreshapp"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
//
//    implementation(libs.androidx.material.icons.extended)
//    implementation(libs.androidx.runtime.livedata)
//    implementation(libs.androidx.navigation.compose)
//    implementation(libs.androidx.lifecycle.viewmodel.compose)

//
//    implementation(libs.androidx.material.icons.core)
//    implementation(libs.material.icons.extended)
//    implementation(libs.androidx.animation.core)

//
//    implementation(libs.androidx.lifecycle.viewmodel.compose.v270)
//   implementation(libs.androidx.navigation.compose.jvmstubs)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.compose.v277)
    //implementation(libs.androidx.runtime.livedata)
    //implementation(libs.androidx.room.common.jvm)

    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.room:room-paging:2.6.1") // Optional, for Paging support
    testImplementation("androidx.room:room-testing:2.6.1")

    // LiveData with Compose
    implementation("androidx.compose.runtime:runtime-livedata")

    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}






// For animations like splash screen fade
// Navigation Compose
// implementation("androidx.navigation:navigation-compose:$navigation_compose_version")
// implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle_version")
// implementation("androidx.compose.runtime:runtime-livedata")



// Testing Dependencies (Optional but recommended) testImplementation("junit:junit:4.13.2")
// androidTestImplementation("androidx.test.ext:junit:1.1.5")
// androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
// androidTestImplementation(platform("androidx.compose:compose-bom:2024.05.00"))
// androidTestImplementation("androidx.compose.ui:ui-test-junit4")
// debugImplementation("androidx.compose.ui:ui-tooling")
// debugImplementation("androidx.compose.ui:ui-test-manifest")
//
// }















