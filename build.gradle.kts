plugins {
    id("java")
    id("net.neoforged.moddev") version "2.0.134"
}

val modId = "ultrastacks"
val modVersion = "21.1-1.0.0"
val mcVersion = "1.21.1"
val neoVersion = "21.1.251"
val parchmentMappings = "2024.11.17"

base {
    archivesName = "UltraStacks-$mcVersion"
    version = modVersion
    group = "com.zaixiayesheng.ultrastacks"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

repositories {
    // 本地没有 libs/ 时，兼容所需的编译期目标 jar 从这里按固定版本拉取。
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
        content { includeGroup("maven.modrinth") }
    }
    mavenCentral()
}

neoForge {
    enable {
        version = neoVersion
    }

    parchment {
        mappingsVersion = parchmentMappings
        minecraftVersion = mcVersion
    }

    runs {
        create("client", Action {
            client()
            gameDirectory.set(file("run"))
            jvmArgument("-Dmixin.debug.export=true")
        })

        create("server", Action {
            server()
            gameDirectory.set(file("run-server"))
            programArgument("--nogui")
            jvmArgument("-Dmixin.debug.export=true")
        })
    }

    mods {
        create(modId, Action {
            sourceSet(sourceSets.main.get())
        })
    }
}

val expandProps = mapOf(
    "version" to modVersion,
    "minecraft_version" to mcVersion,
    "neo_version" to neoVersion
)

tasks.processResources {
    inputs.properties(expandProps)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(expandProps)
    }
}

dependencies {
    // 兼容目标（compileOnly：只用于让编译器认识沉浸工程的库存接口，不会打进产物）。
    // 本地有 libs/ 时优先用本地 jar（离线、快）；没有时（例如刚 clone）从 Modrinth Maven 拉固定版本。
    val libsDir = file("libs")
    if (libsDir.exists()) {
        compileOnly(fileTree(libsDir) { include("*.jar") })
    } else {
        compileOnly("maven.modrinth:immersiveengineering:uNRARSH2")
    }
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
