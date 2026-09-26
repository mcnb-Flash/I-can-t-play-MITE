import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("net.neoforged.moddev") version "2.0.146"
    id("org.jetbrains.kotlin.jvm") version "2.4.10"
}

version = "1.1.6"
group = "name.icpm"

neoForge {
    version = "21.11.45"

    mods {
        register("icpm") {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        create("client") {
            client()
            programArguments.addAll("--username", "Dev")
        }
        create("server") {
            server()
        }
    }
}

repositories {
    mavenCentral()
    // Kotlin for Forge（Kotlin 运行时前置，NeoForge 端等价 fabric-language-kotlin）
    maven {
        name = "KotlinForForge"
        url = uri("https://thedarkcolour.github.io/KotlinForForge/")
        content { includeGroup("thedarkcolour") }
    }
}

dependencies {
    // NeoForge 端无原版 malilib，配置 GUI 改用其非官方移植 MaFgLib（见下方 compileOnly）。
    // Kotlin 运行时：NeoForge 不自带 kotlin-stdlib（缺失会 NoClassDefFoundError: kotlin/enums/EnumEntriesKt）
    // → 以 Kotlin for Forge 作前置库（用户 mods 目录需安装），此处引入使其进入 dev 运行期类路径，
    //   并在 META-INF/neoforge.mods.toml 声明 required 依赖（缺前置时给出明确报错而非崩溃）。
    implementation("thedarkcolour:kotlinforforge-neoforge:6.3.0")
    // MaFgLib（malilib 的 (Neo)Forge 移植，包名同为 fi.dy.masa.malilib）：仅编译期依赖。
    // 运行时若玩家装了 MaFgLib，ICPM 才注册图形配置 GUI（经反射安全层 ICPMConfigAccess 调用），
    // 未装则静默降级为 /icpmconfig 命令；不会把 MaFgLib 打进本 mod jar。
    compileOnly(files("libs/mafglib-neoforge-1.21.11-0.4.6.jar"))
}

// Mixin：NeoForge 1.21 通过 jar 根 *.mixins.json 自动发现（FML MixinService）。
// ModDevGradle 处理 AP/refmap。
tasks.withType<JavaCompile>().configureEach {
    options.isIncremental = false
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

val modJarBaseName = "ICPM-Neoforge"
tasks.jar {
    archiveBaseName.set(modJarBaseName)
    exclude("fabric.mod.json") // NeoForge 用 neoforge.mods.toml
}
