pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("http://192.168.10.128:8081/repository/maven-public/")
            isAllowInsecureProtocol = true
        }
        maven { url = uri("https://www.jitpack.io") }
        maven { url = uri("https://repository.liferay.com/nexus/content/repositories/public/") }
        maven { url = uri("https://maven.singular.net/") }
        maven { url = uri("https://cboost.jfrog.io/artifactory/chartboost-ads/") }
        maven { url = uri("https://artifact.bytedance.com/repository/pangle") }
        maven { url = uri("https://android-sdk.is.com/") }
        maven { url = uri("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea") }
        maven { url = uri("https://jfrog.anythinktech.com/artifactory/overseas_sdk") }

        flatDir {
            dirs("app/libs")
        }
    }
}

rootProject.name = "QuickCleanPRO"
include(":app")
include(":core:model")
include(":core:platform")
include(":core:monetization")
include(":core:designsystem")
