plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.0" // ShadowJar ist das gleiche wie shade in Maven.
    id("xyz.jpenilla.run-paper") version "2.2.3" // Um code zu ändern, ohne den server neu zu starten.
}

group = "at.lowdfx"
version = "1.9.0"

java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.xenondevs.xyz/releases/")
    maven("https://jitpack.io")
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
    maven {
        name = "CodeMC"
        url = uri("https://repo.codemc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("xyz.xenondevs.invui:invui:1.49")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") // Vault Economy API
    implementation("de.tr7zw:item-nbt-api:2.14.1")
}

tasks {
    build {
        dependsOn(shadowJar) // ShadowJar ist das gleiche wie shade in Maven.
    }
    runServer {
        dependsOn(shadowJar)
        minecraftVersion("1.21.4")
    }
    shadowJar {
        archiveClassifier.set("") // Kein -all im .jar Namen.
        minimize()
    }
}
