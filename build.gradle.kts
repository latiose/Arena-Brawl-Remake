plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://ci.lib.co.nz/")
    maven("https://mvn.lib.co.nz/public")
    maven ("https://repo.codemc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    compileOnly("com.github.retrooper:packetevents-spigot:2.6.0")
    implementation(group = "me.libraryaddict.disguises", name = "libsdisguises", version = "11.0.13")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    runServer {
        minecraftVersion("26.2")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
