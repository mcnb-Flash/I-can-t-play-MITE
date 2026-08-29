import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	id("net.fabricmc.fabric-loom-remap")
	`maven-publish`
	id("org.jetbrains.kotlin.jvm") version "2.4.10"
}

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()

loom {
	splitEnvironmentSourceSets()
	mods {
		register("icpm") {
			sourceSet(sourceSets.main.get())
			sourceSet(sourceSets.getByName("client"))
		}
	}
	// Loom 1.17.x 不再默认启用 Mixin 注解处理器（即不再自动生成 icpm.refmap.json），
	// 必须显式开启，否则运行时报 "No refMap loaded" 崩溃。开启后 Loom 会用自带的
	// intermediary 参考环境 + sponge-mixin 处理 @Inject/@Mixin 并写出 refmap。
	mixin {
		useLegacyMixinAp = true
	}
}

fabricApi {
	configureDataGeneration {
		client = true
	}
}

dependencies {
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
    // 用官方映射（official）而非 yarn：本环境下 Fabric Maven 对 1.21.x 的 yarn 映射
    // 全部是 1.16 时代的旧包结构，会导致源码里 net.minecraft.world.level.block.* 全部无法解析。
    // officialMojangMappings() 复用 Loom 生成的分层映射（named=新结构 + 带 intermediary 列），可正确编译。
    mappings(loom.officialMojangMappings())
	modImplementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

	// Fabric API. This is technically optional, but you probably want it anyway.
	modImplementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
	modImplementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")
    // JEI 兼容：仅编译期依赖（运行时由玩家环境的 JEI 提供，避免把 JEI 打入 mod jar）。
    // 必须用 modCompileOnly 而非 compileOnly：JEI 是 intermediary 映射的 fabric mod，
    // Loom 会对 modCompileOnly 依赖做映射转换（remap 到本项目所用 official 映射），
    // 否则插件里 ItemStack / RecipeType 等类型会因映射名不一致而编译失败。
    modCompileOnly(files("libs/jei-1.21.11-fabric-27.23.0.71.jar"))
}

tasks.processResources {
	val version = version
	inputs.property("version", version)

	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}

tasks.withType<JavaCompile>().configureEach {
	// 关闭增量编译：Windows 实时杀毒会在 Gradle 删/写 build/classes 时插入临时文件，
	// 触发 "New files were found" 增量保护导致编译失败。改为每次全量编译，稳定优先。
	options.isIncremental = false
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_21
	}
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

// 直接设定产物文件名: [我不能玩MITE]ICPM-{版本号}.jar（jar 与 remapJar 都设置，避免 Loom 覆盖）
val modJarBaseName = "[我不能玩MITE]ICPM"

tasks.jar {
	archiveBaseName.set(modJarBaseName)

	val projectName = project.name
	inputs.property("projectName", projectName)

	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}

tasks.remapJar {
	archiveBaseName.set(modJarBaseName)
}

// configure the maven publication
publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}

	// See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
	repositories {
		// Add repositories to publish to here.
		// Notice: This block does NOT have the same function as the block in the top level.
		// The repositories here will be used for publishing your artifact, not for
		// retrieving dependencies.
	}
}
