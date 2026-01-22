import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.tasks.PackageAndroidArtifact
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.android.application)
}

val pattern: DateTimeFormatter? = DateTimeFormatter.ofPattern("yyMMdd_HHmm")
val now: String? = LocalDateTime.now().format(pattern)

configure<ApplicationExtension> {
    namespace = "top.bogey.yolo"
    compileSdk = common.versions.targetSdk.get().toInt()
    ndkVersion = common.versions.ndkVersion.get()
    buildToolsVersion = common.versions.buildToolsVersion.get()

    defaultConfig {
        applicationId = "top.bogey.yolo"
        minSdk = common.versions.minSdk.get().toInt()
        targetSdk = common.versions.targetSdk.get().toInt()
        versionCode = 1
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        viewBinding = true
        aidl = true
    }
}

tasks.withType<PackageAndroidArtifact>().configureEach {
    if (name.contains("release", true)) {
        doLast {
            val dir = outputDirectory.get().asFile
            val apk = dir.listFiles()?.firstOrNull { it.extension == "apk" } ?: return@doLast

            val target = File(dir, "点击助手_Yolo_${now}.APK")
            apk.copyTo(target, true)
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)

    implementation(libs.litert)
    implementation(libs.litert.support.api) {
        exclude(group = "com.google.ai.edge.litert", module = "litert-api")
    }

    implementation(libs.mmkv)
    implementation(libs.gson)
}