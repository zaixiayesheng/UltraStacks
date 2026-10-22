plugins {
    id("java")
    id("net.neoforged.moddev.legacyforge") version "2.0.141"
}

val modId = "ultrastacks"
val modVersion = "20.1-1.0.0-alpha"
val mcVersion = "1.20.1"
val forgeVersion = "47.4.0"
val parchmentMappings = "2023.09.03"

base {
    archivesName = "UltraStacks-$mcVersion"
    version = modVersion
    group = "com.zaixiayesheng.ultrastacks"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

repositories {
    maven {
        name = "Sponge (Mixin)"
        url = uri("https://repo.spongepowered.org/repository/maven-public/")
    }
    mavenCentral()
}

dependencies {
    // Mixin annotation processor: generates the refmap (SRG names) required by Forge production.
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

legacyForge {
    version = "$mcVersion-$forgeVersion"

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

mixin {
    add(sourceSets.main.get(), "$modId.refmap.json")
    config("$modId.mixins.json")
}

val expandProps = mapOf(
    "version" to modVersion,
    "minecraft_version" to mcVersion,
    "forge_version" to forgeVersion
)

tasks.processResources {
    inputs.properties(expandProps)
    filteringCharset = "UTF-8"
    filesMatching("META-INF/mods.toml") {
        expand(expandProps)
    }
}

tasks.jar {
    manifest {
        attributes("MixinConfigs" to "$modId.mixins.json")
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
