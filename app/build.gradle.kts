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
val debugGeneratedPrivateAssetsDir = layout.buildDirectory.get().asFile.resolve("generated/retrohallPrivateAssets")
val releaseGeneratedCoreAssetsDir = layout.buildDirectory.get().asFile.resolve("generated/retrohallReleaseCoreAssets")

fun sanitizeReleaseManifest(text: String): String {
    val gamesSection = Regex("(?s)\"games\"\\s*:\\s*\\[.*?\\]\\s*,")
    return gamesSection.replace(text, "\"games\": [],")
}

fun manifestCorePaths(text: String): Set<String> =
    Regex("\"(cores/[^\"\\\\]+(?:/[^\"\\\\]+)*\\.so)\"")
        .findAll(text)
        .map { it.groupValues[1] }
        .toSet()

android {
    namespace = "com.richard.retrohall"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.richard.retrohall"
        minSdk = 23
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"

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
            assets.srcDir(debugGeneratedPrivateAssetsDir)
        }
        getByName("release") {
            assets.srcDir(releaseGeneratedCoreAssetsDir)
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
    into(debugGeneratedPrivateAssetsDir.resolve("retrohall_private"))
}

val prepareRetroHallReleaseCoreAssets by tasks.registering {
    onlyIf {
        privateAssetsDir != null &&
            File(file(privateAssetsDir), "manifest.json").isFile &&
            File(file(privateAssetsDir), "cores").isDirectory
    }
    if (privateAssetsDir != null) {
        inputs.file(File(file(privateAssetsDir), "manifest.json"))
        inputs.dir(File(file(privateAssetsDir), "cores"))
    }
    outputs.dir(releaseGeneratedCoreAssetsDir)
    doLast {
        val sourceDir = file(privateAssetsDir ?: return@doLast)
        val manifestSource = File(sourceDir, "manifest.json")
        val sourceCoresDir = File(sourceDir, "cores")
        check(manifestSource.isFile) {
            "Release manifest is missing: ${manifestSource.absolutePath}"
        }
        check(sourceCoresDir.isDirectory) {
            "Release core assets dir does not exist: ${sourceCoresDir.absolutePath}"
        }

        val releaseRoot = releaseGeneratedCoreAssetsDir.resolve("retrohall_private")
        releaseGeneratedCoreAssetsDir.deleteRecursively()
        releaseRoot.mkdirs()
        val manifestText = manifestSource.readText(Charsets.UTF_8)
        val sanitizedManifest = sanitizeReleaseManifest(manifestText)
        val corePaths = manifestCorePaths(sanitizedManifest)
        check(corePaths.isNotEmpty()) {
            "Release manifest does not reference any libretro core."
        }
        releaseRoot.resolve("manifest.json").writeText(sanitizedManifest, Charsets.UTF_8)

        corePaths.forEach { relativeCorePath ->
            val sourceFile = File(sourceDir, relativeCorePath)
            check(sourceFile.isFile) {
                "Release core file is missing: ${sourceFile.absolutePath}"
            }
            val targetFile = releaseRoot.resolve(relativeCorePath)
            targetFile.parentFile?.mkdirs()
            sourceFile.copyTo(targetFile, overwrite = true)
        }
    }
}

tasks.matching { it.name == "mergeDebugAssets" }.configureEach {
    dependsOn(prepareRetroHallPrivateAssets)
}

tasks.matching { it.name == "mergeReleaseAssets" }.configureEach {
    dependsOn(prepareRetroHallReleaseCoreAssets)
}

tasks.matching {
    it.name.contains("Release", ignoreCase = true) &&
        it.name.contains("Lint", ignoreCase = true)
}.configureEach {
    dependsOn(prepareRetroHallReleaseCoreAssets)
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    doFirst {
        checkNotNull(releaseSigningConfig) {
            "Release signing config is missing. Set retrohall.release.storeFile, retrohall.release.storePassword, retrohall.release.keyAlias and retrohall.release.keyPassword, or provide RETROHALL_RELEASE_STORE_FILE, RETROHALL_RELEASE_STORE_PASSWORD, RETROHALL_RELEASE_KEY_ALIAS and RETROHALL_RELEASE_KEY_PASSWORD."
        }
        checkNotNull(privateAssetsDir) {
            "Release private assets dir is missing. Set retrohall.privateAssetsDir or RETROHALL_PRIVATE_ASSETS_DIR so release APK can package the emulator core."
        }
        check(File(file(privateAssetsDir), "manifest.json").isFile) {
            "Release manifest is missing: ${File(file(privateAssetsDir), "manifest.json").absolutePath}"
        }
        check(File(file(privateAssetsDir), "cores").isDirectory) {
            "Release core assets dir does not exist: ${File(file(privateAssetsDir), "cores").absolutePath}"
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
