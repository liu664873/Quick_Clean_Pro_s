plugins {
    `kotlin-dsl`
}

group = "com.quickcleanpro.buildlogic"

dependencies {
    implementation("com.android.tools.build:gradle:9.2.1")
    implementation("com.google.code.gson:gson:2.13.2")
}

gradlePlugin {
    plugins {
        register("productConfig") {
            id = "quickclean.product-config"
            implementationClass = "ProductConfigPlugin"
        }
    }
}
