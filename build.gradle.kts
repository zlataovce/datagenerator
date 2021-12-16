plugins {
    kotlin("jvm") version "1.6.10"
    id("com.github.johnrengelman.shadow") version "7.1.1"
}

group = "me.kcra"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.screamingsandals.org/snapshots")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("commons-cli:commons-cli:1.5.0")
    implementation("org.ow2.asm:asm:9.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0")
    implementation("net.minecraftforge:srgutils:0.4.11-20211216.125050-5")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "me.kcra.datagenerator.DataGeneratorKt"
    }
}

configurations.all {
    resolutionStrategy.cacheDynamicVersionsFor(0, "seconds")
}