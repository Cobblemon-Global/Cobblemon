plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven("https://maven.architectury.dev/")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.minecraftforge.net/")
}

dependencies {
    implementation(libs.kotlin)

    implementation(libs.licenser)
    implementation(libs.shadow)
    implementation(libs.loom)
    implementation(libs.architectury)

    implementation(libs.blossom)
    implementation(libs.ideaExt)
}

configurations {
    all {
        resolutionStrategy {
            force("com.google.code.gson:gson:2.10.1")
        }
    }
}