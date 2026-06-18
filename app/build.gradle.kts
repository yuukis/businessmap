import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

plugins {
    id("com.android.application")
}

android {
    namespace = "com.github.yuukis.businessmap"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.github.yuukis.businessmap"
        minSdk = 24
        targetSdk = 35
        versionCode = 224
        versionName = "1.4.0224"

        manifestPlaceholders["mapsApiKey"] =
            localProperties.getProperty("MAPS_API_KEY", "")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
}

configurations.all {
    // kotlin-stdlib 1.8+ already includes the jdk7/jdk8 extensions that these
    // legacy artifacts duplicate, which otherwise collide at dex merge time.
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.fragment:fragment:1.8.5")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
}
