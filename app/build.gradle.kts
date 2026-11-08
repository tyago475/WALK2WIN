plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "pt.ubi.pdm.quizpenaltis"
    compileSdk = 36

    defaultConfig {
        applicationId = "pt.ubi.pdm.quizpenaltis"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Adicionar para Room
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true"
                )
            }
        }
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
        buildConfig = true   // para BuildConfig.DEBUG
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.splashscreen)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.preference)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)



    implementation("androidx.viewpager2:viewpager2:1.0.0")

    //Progresso - ATUALIZADO para versão mais recente
    //implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // ROOM - ATUALIZADO para versões consistentes
    val roomVersion = "2.6.0"
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")
}