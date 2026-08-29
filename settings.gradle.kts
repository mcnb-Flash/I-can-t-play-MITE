pluginManagement {
	repositories {
		maven {
			name = "Fabric"
			url = uri("https://maven.fabricmc.net/")
		}
		maven {
			name = "HuaweiMavenCentral"
			url = uri("https://repo.huaweicloud.com/repository/maven/")
		}
		maven {
			name = "HuaweiGradlePluginPortal"
			url = uri("https://repo.huaweicloud.com/repository/gradle-plugin/")
		}
	}

	plugins {
		id("net.fabricmc.fabric-loom-remap") version providers.gradleProperty("loom_version")
	}
}

dependencyResolutionManagement {
	repositories {
		maven {
			name = "Fabric"
			url = uri("https://maven.fabricmc.net/")
		}
		maven {
			name = "HuaweiMavenCentral"
			url = uri("https://repo.huaweicloud.com/repository/maven/")
		}
	}
}

// Should match your modid
rootProject.name = "icpm"
