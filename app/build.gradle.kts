import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

data class ReleaseSigningConfig(
    val storeFile: File,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
)

fun loadProjectProperties(): Properties {
    val props = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.isFile) {
        localPropertiesFile.inputStream().use { input ->
            props.load(input)
        }
    }
    return props
}

val projectProperties = loadProjectProperties()

fun resolveProperty(name: String, envName: String): String? {
    val gradleValue = providers.gradleProperty(name).orNull?.trim()
    if (!gradleValue.isNullOrBlank()) {
        return gradleValue
    }

    val envValue = System.getenv(envName)?.trim()
    if (!envValue.isNullOrBlank()) {
        return envValue
    }

    val localValue = projectProperties.getProperty(name)?.trim()
    return if (localValue.isNullOrBlank()) null else localValue
}

val releaseSigningConfig = run {
    val storeFile = resolveProperty("retrohall.release.storeFile", "RETROHALL_RELEASE_STORE_FILE")
    val storePassword = resolveProperty("retrohall.release.storePassword", "RETROHALL_RELEASE_STORE_PASSWORD")
    val keyAlias = resolveProperty("retrohall.release.keyAlias", "RETROHALL_RELEASE_KEY_ALIAS")
    val keyPassword = resolveProperty("retrohall.release.keyPassword", "RETROHALL_RELEASE_KEY_PASSWORD")
    if (
        storeFile.isNullOrBlank() ||
        storePassword.isNullOrBlank() ||
        keyAlias.isNullOrBlank() ||
        keyPassword.isNullOrBlank()
    ) {
        null
    } else {
        ReleaseSigningConfig(
            storeFile = file(storeFile),
            storePassword = storePassword,
            keyAlias = keyAlias,
            keyPassword = keyPassword,
        )
    }
}

val privateAssetsDir = resolveProperty("retrohall.privateAssetsDir", "RETROHALL_PRIVATE_ASSETS_DIR")

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

    if (releaseSigningConfig != null) {
        signingConfigs {
            create("release") {
                storeFile = releaseSigningConfig.storeFile
                storePassword = releaseSigningConfig.storePassword
                keyAlias = releaseSigningConfig.keyAlias
                keyPassword = releaseSigningConfig.keyPassword
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
            if (releaseSigningConfig != null) {
                signingConfig = signingConfigs.getByName("release")
            }
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

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    doFirst {
        checkNotNull(releaseSigningConfig) {
            "Release signing config is missing. Set retrohall.release.storeFile, retrohall.release.storePassword, retrohall.release.keyAlias and retrohall.release.keyPassword, or provide RETROHALL_RELEASE_STORE_FILE, RETROHALL_RELEASE_STORE_PASSWORD, RETROHALL_RELEASE_KEY_ALIAS and RETROHALL_RELEASE_KEY_PASSWORD."
        }
    }
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
