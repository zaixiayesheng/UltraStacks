plugins {
    id("java")
    id("net.neoforged.moddev") version "2.0.148"
}

val modId = "ultrastacks"
val modVersion = "26.1-1.0.0"
val mcVersion = "26.1.2"
val neoVersion = "26.1.2.122"

base {
    archivesName = "UltraStacks-$mcVersion"
    version = modVersion
    group = "com.zaixiayesheng.ultrastacks"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

repositories {
    mavenCentral()
}

neoForge {
    enable {
        version = neoVersion
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
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
