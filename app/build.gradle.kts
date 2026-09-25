import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// ---------------------------------------------------------------------------------------
// Release signing credentials
//
// The release keystore password must never live in version control. Each field is resolved
// from the first source that supplies a non-blank value:
//
//   1. Gradle project property (-PDOLITA_*), then environment variable:
//        DOLITA_STORE_FILE, DOLITA_STORE_PASSWORD, DOLITA_KEY_ALIAS, DOLITA_KEY_PASSWORD
//   2. `keystore.properties` at the repository root (gitignored, never committed):
//        storeFile, storePassword, keyAlias, keyPassword
//
// Missing credentials are NOT a configuration error: `assembleDebug` signs with the AGP
// auto-generated debug keystore and never reads any of these values. The guard below is
// attached to the release variant's tasks, so only a release build can fail.
// ---------------------------------------------------------------------------------------

val keystorePropertiesFile = rootProject.file("keystore.properties")

val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.isFile) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}

fun firstConfigured(vararg candidates: String?): String? =
    candidates.firstOrNull { !it.isNullOrBlank() }

val releaseStoreFile: File = firstConfigured(
    providers.gradleProperty("DOLITA_STORE_FILE").orNull,
    System.getenv("DOLITA_STORE_FILE"),
    keystoreProperties.getProperty("storeFile"),
)?.let { configuredPath ->
    val candidate = File(configuredPath)
    when {
        candidate.isAbsolute -> candidate
        rootProject.file(configuredPath).isFile -> rootProject.file(configuredPath)
        else -> project.file(configuredPath)
    }
} ?: rootProject.file("app/dolita-release.jks")

val releaseStorePassword = firstConfigured(
    providers.gradleProperty("DOLITA_STORE_PASSWORD").orNull,
    System.getenv("DOLITA_STORE_PASSWORD"),
    keystoreProperties.getProperty("storePassword"),
)

val releaseKeyAlias = firstConfigured(
    providers.gradleProperty("DOLITA_KEY_ALIAS").orNull,
    System.getenv("DOLITA_KEY_ALIAS"),
    keystoreProperties.getProperty("keyAlias"),
)

val releaseKeyPassword = firstConfigured(
    providers.gradleProperty("DOLITA_KEY_PASSWORD").orNull,
    System.getenv("DOLITA_KEY_PASSWORD"),
    keystoreProperties.getProperty("keyPassword"),
)

val releaseSigningProblems: List<String> = buildList {
    if (!releaseStoreFile.isFile) {
        add("keystore file not found: ${releaseStoreFile.absolutePath}")
    }
    if (releaseStorePassword.isNullOrBlank()) {
        add("missing store password (DOLITA_STORE_PASSWORD / storePassword)")
    }
    if (releaseKeyAlias.isNullOrBlank()) {
        add("missing key alias (DOLITA_KEY_ALIAS / keyAlias)")
    }
    if (releaseKeyPassword.isNullOrBlank()) {
        add("missing key password (DOLITA_KEY_PASSWORD / keyPassword)")
    }
}

val releaseSigningErrorMessage: String = """
    |Release signing credentials are not configured, so the release variant is blocked.
    |
    |Every release task stops here on purpose: without credentials the build would happily
    |produce a silently unsigned release artifact, which is far worse than a loud failure.
    |
    |Configure the credentials in one of these ways (first match wins per field):
    |
    |1. Export environment variables, or pass matching -P flags:
    |     DOLITA_STORE_FILE=<path to the release .jks>
    |     DOLITA_STORE_PASSWORD=<keystore store password>
    |     DOLITA_KEY_ALIAS=<key alias inside the keystore>
    |     DOLITA_KEY_PASSWORD=<key password>
    |
    |2. Create a gitignored `keystore.properties` file at the repository root:
    |     storeFile=<path to the release .jks>
    |     storePassword=<keystore store password>
    |     keyAlias=<key alias inside the keystore>
    |     keyPassword=<key password>
    |
    |Expected properties file: ${keystorePropertiesFile.absolutePath}
    |`keystore.properties` is listed in .gitignore, so it will never be committed.
    |Debug builds are unaffected: they use the AGP auto-generated debug keystore.
    |
    |Unresolved: ${releaseSigningProblems.joinToString("; ")}
    |""".trimMargin()

/**
 * Task action that blocks release tasks while signing credentials are missing.
 *
 * Declared as a named class holding only plain list/string data on purpose: a Kotlin
 * lambda created in a build script captures the script instance, and the configuration
 * cache refuses to serialize Gradle script object references.
 */
class RequireReleaseSigningCredentials(
    private val problems: List<String>,
    private val errorMessage: String,
) : Action<Task> {
    override fun execute(task: Task) {
        if (problems.isNotEmpty()) {
            throw GradleException(errorMessage)
        }
    }
}

// Fail loudly, but only for builds that touch the release variant. `doFirst` runs before
// the task's own action, so signing never happens with half-configured credentials.
tasks.configureEach {
    if (name.endsWith("release", ignoreCase = true)) {
        doFirst(RequireReleaseSigningCredentials(releaseSigningProblems, releaseSigningErrorMessage))
    }
}

android {
    namespace = "com.example.erp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.erp"
        minSdk = 30
        targetSdk = 37
        versionCode = 1019001
        versionName = "1.19.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = releaseStoreFile
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.okhttp)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    testImplementation(libs.kotlinx.coroutines.test)
    
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.org.json)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.robolectric)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}