plugins {
    id("net.neoforged.moddev") version "2.0.80"
    id("com.diffplug.spotless") version "7.0.0.BETA2"
    id("maven-publish")
}

val ae2Version: String by project
val architecturyVersion: String by project
val runtimeItemlistMod: String by project
val jeiMinecraftVersion: String by project
val jeiVersion: String by project
val reiVersion: String by project
val emiVersion: String by project
val emiMinecraftVersion: String by project
val neoforgeVersion: String by project
val curiosVersion: String by project
val mavenGroup: String by project
val modID: String by project

version = "0.0.0-SNAPSHOT"

val pr = System.getenv("PR_NUMBER") ?: ""
if (pr != "") {
    version = "0.0.0-pr$pr"
}

val tag = System.getenv("TAG") ?: ""
if (tag != "") {
    version = tag
}

val artifactVersion = version

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

dependencies {
    //implementation("top.theillusivec4.curios:curios-neoforge:${curiosVersion}")
    implementation("org.appliedenergistics:appliedenergistics2:${ae2Version}")
    jarJar(project(path = ":ae2wtlib_api"))
    api(project(path = ":ae2wtlib_api"))

    compileOnly("me.shedaniel:RoughlyEnoughItems-neoforge:${reiVersion}")
    compileOnly("mezz.jei:jei-${jeiMinecraftVersion}-neoforge:${jeiVersion}")
    compileOnly("dev.emi:emi-neoforge:${emiVersion}+${emiMinecraftVersion}:api")

    when (runtimeItemlistMod) {
        "rei" -> {
            runtimeOnly("me.shedaniel:RoughlyEnoughItems-neoforge:${reiVersion}")
            runtimeOnly("dev.architectury:architectury-neoforge:${architecturyVersion}")
        }

        "jei" -> runtimeOnly("mezz.jei:jei-${jeiMinecraftVersion}-neoforge:${jeiVersion}")

        "emi" -> {
            runtimeOnly("dev.emi:emi-neoforge:${emiVersion}+${emiMinecraftVersion}")
        }

        "jemi" -> {
            runtimeOnly("dev.emi:emi-neoforge:${emiVersion}+${emiMinecraftVersion}")
            runtimeOnly("mezz.jei:jei-${jeiMinecraftVersion}-neoforge:${jeiVersion}")
        }
    }

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    //testing
    //runtimeOnly(fg.deobf("maven.modrinth:aeinfinitybooster:1.20.1-1.0.0+20"))
    //implementation("maven.modrinth:spark:1.10.109-neoforge")
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            url = uri("https://modmaven.dev/")
            content {
                includeGroup("mezz.jei")
            }
        }
        maven {
            url = uri("https://maven.shedaniel.me/")
            content {
                includeGroup("me.shedaniel")
                includeGroup("me.shedaniel.cloth")
                includeGroup("dev.architectury")
            }
        }
        maven {
            url = uri("https://maven.terraformersmc.com/")
            content {
                includeGroup("dev.emi")
            }
        }
        maven {
            url = uri("https://maven.theillusivec4.top/")
            content {
                includeGroup("top.theillusivec4.curios")
            }
        }
        maven {
            url = uri("https://api.modrinth.com/maven")
            content {
                includeGroup("maven.modrinth")
            }
        }
    }
}

tasks {
    processResources {
        // Ensure the resources get re-evaluate when the version changes
        inputs.property("version", version)
        inputs.property("ae2_version", ae2Version)

        val replaceProperties = mapOf(
            "version" to version as String, "ae2_version" to ae2Version
        )

        inputs.properties(replaceProperties)
        filesMatching("META-INF/neoforge.mods.toml") {
            expand(replaceProperties)
        }
    }
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}

neoForge {
    version = neoforgeVersion
    mods {
        create(modID) {
            sourceSet(sourceSets.main.get())
        }
    }
    runs {
        configureEach {
            gameDirectory.file("run")
            systemProperty("forge.logging.console.level", "debug")
        }

        create("client") {
            client()
        }
        create("server") {
            server()
        }

        create("guide") {
            client()
            systemProperty("guideme.showOnStartup", "ae2:guide")
            systemProperty("guideme.ae2.guide.sources", file("src/main/resources/assets/ae2wtlib/ae2guide").absolutePath)
        }
    }
}

publishing {
    publications {
        create<MavenPublication>(modID) {
            groupId = mavenGroup
            artifactId = modID
            version = artifactVersion.toString()

            from(components["java"])
        }
    }
    repositories {
        maven {
            credentials {
                username = System.getenv("MODMAVEN_USER")
                password = System.getenv("MODMAVEN_PASSWORD")
            }
            name = "modmaven"
            url = uri("https://modmaven.dev/artifactory/local-releases/")
        }
    }
}

spotless {
    java {
        target("/src/**/java/**/*.java")

        endWithNewline()
        indentWithSpaces()
        removeUnusedImports()
        toggleOffOn()
        eclipse().configFile("codeformat/codeformat.xml")
        importOrderFile("codeformat/ae2wtlib.importorder")
    }
}
