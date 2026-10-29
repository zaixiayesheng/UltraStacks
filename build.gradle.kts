plugins {
    id("java")
    id("net.neoforged.moddev.legacyforge") version "2.0.141"
}

val modId = "ultrastacks"
val modVersion = "20.1-1.0.0"
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
    // 本地没有 libs/ 时，兼容所需的编译期目标 jar 从这里按固定版本拉取。
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
        content { includeGroup("maven.modrinth") }
    }
    mavenCentral()
}

dependencies {
    // Mixin annotation processor: generates the refmap (SRG names) required by Forge production.
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")

    // 兼容目标（compileOnly：只用于让 Mixin 注解处理器解析 target，不会打进产物）。
    // 本地有 libs/ 时优先用本地 jar（离线、快）；没有时（例如刚 clone）从 Modrinth Maven 拉固定版本。
    val libsDir = file("libs")
    if (libsDir.exists()) {
        compileOnly(fileTree(libsDir) { include("*.jar") })
    } else {
        compileOnly("maven.modrinth:sophisticated-core:kflpbKPn")
        compileOnly("maven.modrinth:mekanism:uxe1WQp4")
        compileOnly("maven.modrinth:mekanical-create:ouJL4uq3")
        compileOnly("maven.modrinth:ae2:jYgSnxye")
        compileOnly("maven.modrinth:refined-storage:ZITLFjjf")
        compileOnly("maven.modrinth:extended-ae:uq3lO4ER")
        compileOnly("maven.modrinth:iron-furnaces:KHAcRQwi")
        compileOnly("maven.modrinth:cucumber:xnLznSfT")
        compileOnly("maven.modrinth:immersiveengineering:WUzj4tgJ")
        compileOnly("maven.modrinth:cloth-config:nr1znv5v")
    }
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
    config("$modId.crafting.mixins.json")
    config("$modId.display.mixins.json")
    config("$modId.compat.mixins.json")
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
        attributes("MixinConfigs" to "$modId.mixins.json,$modId.crafting.mixins.json,$modId.display.mixins.json,$modId.compat.mixins.json")
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
