plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

val jdaVersion = "6.7.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
    compileOnly("net.dv8tion:JDA:$jdaVersion")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    runServer {
        minecraftVersion("26.3")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version, "jdaVersion" to jdaVersion)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}