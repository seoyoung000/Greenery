plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
}


android {
    namespace = "com.example.greenery"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.greenery"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding=true
        dataBinding=true
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
}

dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.9.0") // Retrofit
    implementation("com.google.code.gson:gson:2.8.9") // Gson
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // Retrofit Gson Converter
    implementation("com.github.bumptech.glide:glide:4.13.2") // Glide
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3") // OkHttp Logging Interceptor
    implementation("androidx.recyclerview:recyclerview:1.2.1") // RecyclerView
    implementation("com.squareup.picasso:picasso:2.71828") // Picasso 라이브러리

    implementation(platform("com.google.firebase:firebase-bom:33.1.2")) // Firebase BOM
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.android.gms:play-services-auth:20.3.0")
    implementation("com.google.android.gms:play-services-location:21.3.0") // Google Play Services Location
    implementation ("com.google.firebase:firebase-auth:21.0.0")
    implementation("com.google.firebase:firebase-auth-ktx") // 버전 명시할 필요 없음
    implementation("com.google.firebase:firebase-firestore-ktx") // 버전 명시할 필요 없음
    implementation("com.google.firebase:firebase-storage-ktx") // 버전 명시할 필요 없음

    // Splash
    implementation ("androidx.core:core-splashscreen:1.0.1")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.activity)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
