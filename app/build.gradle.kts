import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.richard.retrohall"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.richard.retrohall"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    sourceSets {
        getByName("debug") {
            assets.srcDir(layout.buildDirectory.dir("generated/retrohallPrivateAssets"))
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

val privateAssetsDir = run {
    val gradleValue = providers.gradleProperty("retrohall.privateAssetsDir").orNull?.trim()
    if (!gradleValue.isNullOrBlank()) {
        gradleValue
    } else {
        val localPropertiesFile = rootProject.file("local.properties")
        if (!localPropertiesFile.isFile) {
            null
        } else {
            val props = Properties()
            localPropertiesFile.inputStream().use { input ->
                props.load(input)
            }
            val localValue = props.getProperty("retrohall.privateAssetsDir")?.trim()
            if (localValue.isNullOrBlank()) null else localValue
        }
    }
}

val prepareRetroHallPrivateAssets by tasks.registering(Copy::class) {
    onlyIf { privateAssetsDir != null && file(privateAssetsDir).exists() }
    if (privateAssetsDir != null) {
        from(file(privateAssetsDir))
    }
    into(layout.buildDirectory.dir("generated/retrohallPrivateAssets/retrohall_private"))
}

tasks.matching { it.name == "mergeDebugAssets" }.configureEach {
    dependsOn(prepareRetroHallPrivateAssets)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    ksp(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
