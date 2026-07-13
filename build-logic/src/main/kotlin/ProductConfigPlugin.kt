import com.android.build.api.dsl.ApplicationExtension
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.Properties

class ProductConfigPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.withPlugin("com.android.application") {
            configureAndroid(project)
        }
    }

    private fun configureAndroid(project: Project) {
        val rootDir = project.rootProject.projectDir
        val configFile = rootDir.resolve("config/product.json")
        if (!configFile.isFile) throw GradleException("Missing product config: $configFile")

        val config = Gson().fromJson(configFile.readText(), ProductConfig::class.java)
        config.validate()
        val policyFile = rootDir.resolve(config.advertising.policyFile).canonicalFile
        if (!policyFile.toPath().startsWith(rootDir.canonicalFile.toPath())) {
            throw GradleException("advertising.policyFile must stay inside the repository")
        }
        validateAdPolicy(policyFile)
        validateNoSourceAdResourceDuplicates(project)

        val local = loadProperties(rootDir.resolve("config/product.local.properties"))
        val legacyLocal = loadProperties(rootDir.resolve("config/app.local.properties"))
        val value = ProductValueResolver(project, local, legacyLocal)
        val nativeIdsJson = Gson().toJson(config.advertising.admob.nativeIds)

        val android = project.extensions.getByType(ApplicationExtension::class.java)
        configureGeneratedAdResources(project, android, configFile, policyFile)
        android.defaultConfig.apply {
                val defaults = this
                defaults.applicationId = config.identity.applicationId
                defaults.versionCode = config.version.code
                defaults.versionName = config.version.name

                defaults.resValue("string", "app_name", config.identity.appName)
                defaults.resValue("string", "app_profile_key", config.identity.profileKey)
                defaults.resValue("string", "app_theme_key", config.identity.themeKey)
                defaults.resValue("string", "privacy_policy_url", config.legal.privacyUrl)
                defaults.resValue("string", "terms_of_service_url", config.legal.termsUrl)

                val trustlookKey = value.resolve("TRUSTLOOK_QUICKCLEANPRO_API_KEY")
                defaults.manifestPlaceholders.putAll(
                    mapOf(
                        "trustlookApiKey" to trustlookKey,
                        "advAdmobAppId" to value.resolve("ADV_ADMOB_APP_ID", config.advertising.admob.appId),
                        "advFacebookAppId" to value.resolve("ADV_FACEBOOK_APP_ID"),
                        "advFacebookClientToken" to value.resolve("ADV_FACEBOOK_CLIENT_TOKEN"),
                    ),
                )

                defaults.buildConfigString("TRUSTLOOK_API_KEY", trustlookKey)
                defaults.buildConfigString("PRODUCT_PROFILE_KEY", config.identity.profileKey)
                defaults.buildConfigString("PRODUCT_THEME_KEY", config.identity.themeKey)
                defaults.buildConfigString("ADV_PRIVACY_URL", config.legal.privacyUrl)
                defaults.buildConfigString("ADV_TERMS_URL", config.legal.termsUrl)
                defaults.buildConfigString("ADV_ADMOB_APP_ID", value.resolve("ADV_ADMOB_APP_ID", config.advertising.admob.appId))
                defaults.buildConfigString("ADV_ADMOB_BANNER_ID", value.resolve("ADV_ADMOB_BANNER_ID", config.advertising.admob.bannerId))
                defaults.buildConfigString("ADV_ADMOB_INTERSTITIAL_ID", value.resolve("ADV_ADMOB_INTERSTITIAL_ID", config.advertising.admob.interstitialId))
                defaults.buildConfigString("ADV_ADMOB_NATIVE_ID", value.resolve("ADV_ADMOB_NATIVE_ID", config.advertising.admob.nativeId))
                defaults.buildConfigString("ADV_ADMOB_NATIVE_IDS_JSON", value.resolve("ADV_ADMOB_NATIVE_IDS_JSON", nativeIdsJson))
                defaults.buildConfigString("ADV_ADMOB_OPEN_ID", value.resolve("ADV_ADMOB_OPEN_ID", config.advertising.admob.openId))
                defaults.buildConfigString("ADV_ADMOB_REWARDED_ID", value.resolve("ADV_ADMOB_REWARDED_ID", config.advertising.admob.rewardedId))
                defaults.buildConfigString("ADV_DEFAULT_TOPIC", value.resolve("ADV_DEFAULT_TOPIC", config.advertising.defaultTopic))
                defaults.buildConfigString("ADV_AD_DEBUG_OVERRIDE_MODE", value.resolve("ADV_AD_DEBUG_OVERRIDE_MODE", config.advertising.debugOverrideMode))
                defaults.buildConfigString("ADV_SERVER_RELEASE_HOST", value.resolve("ADV_SERVER_RELEASE_HOST", config.services.releaseHost))
                defaults.buildConfigString("ADV_SERVER_TEST_HOST", value.resolve("ADV_SERVER_TEST_HOST", config.services.testHost))
                defaults.buildConfigString("ADV_PLAY_INTEGRITY_PARSE_TOKEN_KEY", value.resolve("ADV_PLAY_INTEGRITY_PARSE_TOKEN_KEY"))
                defaults.buildConfigString("ADV_REMOTE_CONFIG_ENCRYPTION_KEY", value.resolve("ADV_REMOTE_CONFIG_ENCRYPTION_KEY"))
                defaults.buildConfigString("ADV_REMOTE_CONFIG_ENCRYPTION_KEY_ID", value.resolve("ADV_REMOTE_CONFIG_ENCRYPTION_KEY_ID"))
                defaults.buildConfigString("ADV_THINKING_APP_KEY", value.resolve("ADV_THINKING_APP_KEY", config.analytics.thinkingAppKey))
                defaults.buildConfigString("ADV_THINKING_SERVER_URL", value.resolve("ADV_THINKING_SERVER_URL", config.analytics.thinkingServerUrl))
                defaults.buildConfigString("ADV_FACEBOOK_APP_ID", value.resolve("ADV_FACEBOOK_APP_ID"))
                defaults.buildConfigString("ADV_FACEBOOK_CLIENT_TOKEN", value.resolve("ADV_FACEBOOK_CLIENT_TOKEN"))
                defaults.buildConfigString("ADV_SINGULAR_API_KEY", value.resolve("ADV_SINGULAR_API_KEY"))
                defaults.buildConfigString("ADV_SINGULAR_SECRET", value.resolve("ADV_SINGULAR_SECRET"))
                defaults.buildConfigString("ADV_TIKTOK_ACCESS_TOKEN", value.resolve("ADV_TIKTOK_ACCESS_TOKEN"))
                defaults.buildConfigString("ADV_TIKTOK_TT_APP_ID", value.resolve("ADV_TIKTOK_TT_APP_ID"))
                defaults.buildConfigString("ADV_TIKTOK_APP_ID", value.resolve("ADV_TIKTOK_APP_ID"))
                defaults.buildConfigString("ADV_SAFE_EXPECTED_SIGNATURES", value.resolve("ADV_SAFE_EXPECTED_SIGNATURES"))
                defaults.buildConfigField(
                    "long",
                    "ADV_PLAY_INTEGRITY_CLOUD_PROJECT_NUMBER",
                    "${value.resolve("ADV_PLAY_INTEGRITY_CLOUD_PROJECT_NUMBER", config.services.playIntegrityCloudProjectNumber.toString()).toLongOrNull() ?: throw GradleException("ADV_PLAY_INTEGRITY_CLOUD_PROJECT_NUMBER must be a long")}L",
                )
        }

        validateFirebasePackage(project, config.identity.applicationId)
    }
}

private fun configureGeneratedAdResources(
    project: Project,
    android: ApplicationExtension,
    configFile: java.io.File,
    policyFile: java.io.File,
) {
    val generatedResDirectory = project.layout.buildDirectory.dir("generated/product-config/res")
    android.sourceSets.getByName("main").res.directories.add(generatedResDirectory.get().asFile.absolutePath)

    val generateTask =
        project.tasks.register("generateProductAdResources") {
            group = "build setup"
            description = "Generates ad_policy.json and native_ad_ids.json from canonical product configuration."
            inputs.files(configFile, policyFile)
            outputs.dir(generatedResDirectory)
            doLast {
                val config = Gson().fromJson(configFile.readText(), ProductConfig::class.java)
                val policy = Gson().fromJson(policyFile.readText(), JsonObject::class.java)
                val packagedPolicy =
                    JsonObject().apply {
                        addProperty("package_name", config.identity.applicationId)
                        policy.entrySet().forEach { (key, value) -> add(key, value.deepCopy()) }
                    }
                val rawDirectory = generatedResDirectory.get().asFile.resolve("raw").apply { mkdirs() }
                val gson = GsonBuilder().setPrettyPrinting().create()
                rawDirectory.resolve("ad_policy.json").writeText(gson.toJson(packagedPolicy) + "\n")
                rawDirectory.resolve("native_ad_ids.json").writeText(gson.toJson(config.advertising.admob.nativeIds) + "\n")
            }
        }

    project.tasks.matching {
        it.name == "preBuild" || (it.name.startsWith("merge") && it.name.endsWith("Resources"))
    }.configureEach {
        dependsOn(generateTask)
    }
}

private fun com.android.build.api.dsl.ApplicationDefaultConfig.buildConfigString(name: String, value: String) {
    val escaped = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\r", "\\r")
        .replace("\n", "\\n")
    buildConfigField("String", name, "\"$escaped\"")
}

private class ProductValueResolver(
    private val project: Project,
    private val local: Properties,
    private val legacyLocal: Properties,
) {
    fun resolve(name: String, defaultValue: String = ""): String =
        project.providers.gradleProperty(name).orNull
            ?: project.providers.environmentVariable(name).orNull
            ?: local.getProperty(name)
            ?: legacyLocal.getProperty(name)
            ?: defaultValue
}

private fun loadProperties(file: java.io.File): Properties = Properties().apply {
    if (file.isFile) file.inputStream().use(::load)
}

private fun validateFirebasePackage(project: Project, applicationId: String) {
    val file = project.file("google-services.json")
    if (!file.isFile) throw GradleException("Missing Firebase config: $file")
    val root = Gson().fromJson(file.readText(), JsonObject::class.java)
    val packages = root.getAsJsonArray("client")
        ?.mapNotNull { client ->
            client.asJsonObject
                .getAsJsonObject("client_info")
                ?.getAsJsonObject("android_client_info")
                ?.get("package_name")
                ?.asString
        }
        .orEmpty()
    if (applicationId !in packages) {
        throw GradleException("google-services.json does not contain applicationId '$applicationId'")
    }
}

private fun validateAdPolicy(file: java.io.File) {
    if (!file.isFile) throw GradleException("Missing canonical ad policy: $file")
    val policy = Gson().fromJson(file.readText(), JsonObject::class.java)
    if (policy.has("package_name")) {
        throw GradleException("Canonical ad policy must not define package_name; it is generated from identity.applicationId")
    }
    val areaKeys =
        policy.getAsJsonArray("ad_units")
            ?.mapNotNull { it.asJsonObject.get("areakey")?.asString }
            .orEmpty()
    if (areaKeys.isEmpty()) throw GradleException("Canonical ad policy must define ad_units")
    val duplicateAreaKeys = areaKeys.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    if (duplicateAreaKeys.isNotEmpty()) {
        throw GradleException("Canonical ad policy contains duplicate area keys: ${duplicateAreaKeys.sorted()}")
    }
}

private fun validateNoSourceAdResourceDuplicates(project: Project) {
    listOf("ad_policy.json", "native_ad_ids.json").forEach { fileName ->
        val sourceFile = project.file("src/main/res/raw/$fileName")
        if (sourceFile.exists()) {
            throw GradleException("$sourceFile duplicates generated product configuration and must be removed")
        }
    }
}

private fun ProductConfig.validate() {
    if (schemaVersion != 1) throw GradleException("Unsupported product schemaVersion: $schemaVersion")
    if (!identity.applicationId.matches(Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$"))) {
        throw GradleException("Invalid applicationId: ${identity.applicationId}")
    }
    if (identity.appName.isBlank() || identity.profileKey.isBlank() || identity.themeKey.isBlank()) {
        throw GradleException("Product identity fields must not be blank")
    }
    if (version.code < 1 || version.name.isBlank()) throw GradleException("Invalid product version")
    if (advertising.policyFile != "config/ad_policy.json") {
        throw GradleException("advertising.policyFile must be 'config/ad_policy.json'")
    }
    val nativeIds = advertising.admob.nativeIds
    if (nativeIds.isEmpty() || nativeIds.any { ids ->
            listOf("highPriceID", "midPriceID", "lowPriceID").any { ids[it].isNullOrBlank() }
        }
    ) {
        throw GradleException("Every advertising.admob.nativeIds entry must define highPriceID, midPriceID, and lowPriceID")
    }
    listOf(legal.privacyUrl, legal.termsUrl, services.releaseHost, analytics.thinkingServerUrl).forEach { url ->
        if (!url.startsWith("https://")) throw GradleException("Production URL must use HTTPS: $url")
    }
}

private data class ProductConfig(
    val schemaVersion: Int,
    val identity: ProductIdentity,
    val version: ProductVersion,
    val legal: ProductLegal,
    val advertising: ProductAdvertising,
    val services: ProductServices,
    val analytics: ProductAnalytics,
)

private data class ProductIdentity(val applicationId: String, val appName: String, val profileKey: String, val themeKey: String)
private data class ProductVersion(val code: Int, val name: String)
private data class ProductLegal(val privacyUrl: String, val termsUrl: String)
private data class ProductAdvertising(
    val defaultTopic: String,
    val debugOverrideMode: String,
    val policyFile: String,
    val admob: AdMobConfig,
)
private data class AdMobConfig(
    val appId: String,
    val bannerId: String,
    val interstitialId: String,
    val nativeId: String,
    val openId: String,
    val rewardedId: String,
    val nativeIds: List<Map<String, String>>,
)
private data class ProductServices(val releaseHost: String, val testHost: String, val playIntegrityCloudProjectNumber: Long)
private data class ProductAnalytics(val thinkingAppKey: String, val thinkingServerUrl: String)
