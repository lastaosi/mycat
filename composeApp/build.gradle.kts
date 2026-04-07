import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosSimulatorArm64(),
        iosArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.sqldelight.android)
            implementation(libs.coil.compose)
            implementation(libs.coil.network)
            implementation(libs.ktor.client.android)
            implementation(libs.androidx.compose.material.icons)
            implementation(libs.androidx.exifinterface)
            implementation(libs.vico.compose)
            implementation(libs.vico.compose.m3)
            implementation(libs.vico.core)
            implementation(libs.google.maps)
            implementation(libs.google.maps.compose)
            implementation(libs.google.places)
            implementation(libs.google.location)
            implementation(libs.androidx.work.runtime)
            implementation("com.google.android.gms:play-services-ads:23.6.0")

        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(projects.shared)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native)
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

    }
}

// kotlin { } 블록 밖으로
dependencies {
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.lastaosi.mycat"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    signingConfigs {
        create("release") {
            storeFile = file("../mycat-release.keystore")  // 키스토어 위치
            storePassword = project.findProperty("MYCAT_STORE_PASSWORD") as String
            keyAlias = project.findProperty("MYCAT_KEY_ALIAS") as String
            keyPassword = project.findProperty("MYCAT_KEY_PASSWORD") as String
        }
    }

    defaultConfig {
        applicationId = "com.lastaosi.mycat"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${project.findProperty("GEMINI_API_KEY") ?: ""}\""
        )
        buildConfigField(
            "String",
            "MAPS_API_KEY",
            "\"${project.findProperty("MAPS_API_KEY") ?: ""}\""
        )
        buildConfigField(
            "String",
            "ADMOB_BANNER_ID",
            "\"${project.findProperty("ADMOB_BANNER_ID")}\""
        )
        manifestPlaceholders["MAPS_API_KEY"] =
            project.findProperty("MAPS_API_KEY") ?: ""

        manifestPlaceholders["ADMOB_APP_ID"] =
            project.findProperty("ADMOB_APP_ID") as String
    }
    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
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


}

sqldelight {
    databases {
        create("MyCatDatabase") {
            packageName.set("com.lastaosi.mycat.db")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight"))
        }
    }
}
//// android { } 블록 밖으로
//room {
//    schemaDirectory("$projectDir/schemas")
//}