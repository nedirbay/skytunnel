import java.net.URI
import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Xray core (AndroidLibXrayLite, LGPL-3.0). The 60 MB AAR stays out of git: it is
// downloaded once at this pinned version and checked against this digest.
val xrayCoreVersion = "v26.10.31"
val xrayCoreSha256 = "2efdf731b1ed624eb1cb0d5a4d0aa95c0672e89f9702226d1ba2dc90308630b6"
val xrayAar = layout.projectDirectory.file("libs/libv2ray.aar")

fun sha256Of(file: File): String = MessageDigest.getInstance("SHA-256")
    .digest(file.readBytes())
    .joinToString("") { "%02x".format(it) }

val downloadXrayAar = tasks.register("downloadXrayAar") {
    val target = xrayAar.asFile
    val version = xrayCoreVersion
    val expected = xrayCoreSha256
    outputs.file(target)
    onlyIf { !target.exists() || sha256Of(target) != expected }
    doLast {
        target.parentFile.mkdirs()
        val url = "https://github.com/2dust/AndroidLibXrayLite/releases/download/$version/libv2ray.aar"
        URI(url).toURL().openStream().use { input ->
            target.outputStream().use { input.copyTo(it) }
        }
        val actual = sha256Of(target)
        if (actual != expected) {
            target.delete()
            throw GradleException("libv2ray.aar checksum mismatch: $actual")
        }
    }
}

android {
    namespace = "tm.skytunnel.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "tm.skytunnel.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // The bundled geo databases add 27 MB and the generated config has no geo rules.
    androidResources {
        ignoreAssetsPattern = "!geoip*:!geosite*"
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(files(xrayAar).builtBy(downloadXrayAar))

    testImplementation(libs.junit)
    testImplementation(libs.org.json)

    debugImplementation(libs.androidx.ui.tooling)
}
