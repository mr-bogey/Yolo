import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "top.bogey.yolo"
    compileSdk = 36
    ndkVersion = "29.0.14206865"
    buildToolsVersion = "36.1.0"

    val pattern = DateTimeFormatter.ofPattern("yyMMdd_HHmm")
    val now = LocalDateTime.now().format(pattern)
    val code = 1

    defaultConfig {
        applicationId = "top.bogey.yolo"
        minSdk = 24
        targetSdk = 36
        versionCode = code
        versionName = now

        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    applicationVariants.all {
        outputs.all {
            if (buildType.name == "release") {
                val impl = this as BaseVariantOutputImpl
                impl.outputFileName = "点击助手_Yolo_${now}_${code}.APK"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        viewBinding = true
        aidl = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)

    implementation(libs.litert)
    implementation(libs.litert.support) {
        exclude(group = "com.google.ai.edge.litert", module = "litert-api")
    }

    implementation(libs.mmkv)
    implementation(libs.gson)
}