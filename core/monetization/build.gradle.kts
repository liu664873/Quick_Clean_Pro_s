plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.quickcleanpro.phonecleaner.core.monetization"
    compileSdk {
        version =
            release(36) {
                minorApiLevel = 1
            }
    }

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(project(":core:model"))
    api(libs.pdffox.advertise) {
        exclude(group = "com.pangle.global", module = "pag-sdk")
    }
    implementation(libs.pangle.pag.sdk.ad)
    implementation(libs.tiktok.business.android.sdk.comp)
}
