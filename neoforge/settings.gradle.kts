pluginManagement {
    repositories {
        maven {
            name = "NeoForged"
            url = uri("https://maven.neoforged.net/releases/")
        }
        maven {
            name = "HuaweiMavenCentral"
            url = uri("https://repo.huaweicloud.com/repository/maven/")
        }
        maven {
            name = "HuaweiGradlePluginPortal"
            url = uri("https://repo.huaweicloud.com/repository/gradle-plugin/")
        }
        gradlePluginPortal()
    }
}

rootProject.name = "icpm-neoforge"
