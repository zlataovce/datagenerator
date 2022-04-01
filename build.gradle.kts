import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.6.20"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "me.kcra"
version = project.properties["datagenerator.version"] as String

repositories {
    mavenCentral()
    maven("https://repo.screamingsandals.org/public")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("commons-cli:commons-cli:1.5.0")
    implementation("org.ow2.asm:asm:9.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")
    implementation("net.minecraftforge:srgutils:0.4.11-SNAPSHOT")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "me.kcra.datagenerator.DataGeneratorKt"
    }
}

configurations.all {
    resolutionStrategy.cacheDynamicVersionsFor(0, "seconds")
}

val dataVersions: List<String> = listOf(
    "1.18.1",
    "1.18",
    "1.17.1",
    "1.17",
    "1.16.5",
    "1.16.4",
    "1.16.3",
    "1.16.2",
    "1.16.1",
    "1.16",
    "1.15.2",
    "1.15.1",
    "1.15",
    "1.14.4",
    "1.14.3",
    "1.14.2",
    "1.14.1",
    "1.14",
    "1.13.2",
    "1.13.1",
    "1.12.2",
    "1.12.1",
    "1.12",
    "1.11.2",
    "1.11.1",
    "1.11",
    "1.10.2",
    // "1.10.1", - doesn't have searge mappings
    "1.10",
    "1.9.4"
)
val jarFile: String = tasks.getByName<ShadowJar>("shadowJar").archiveFile.get().asFile.path

tasks.register("generateData") {
    dependsOn(
        dataVersions.stream()
            .map { "generateVersion$it" }
            .toArray()
    )
    description = "Generates data for all datable versions."
    group = "generation"
}

for (ver: String in dataVersions) {
    tasks.register("generateVersion$ver", JavaExec::class) {
        dependsOn("shadowJar")
        description = "Generates data for the $ver version."
        group = "generation"
        mainClass.set("-jar")
        args(jarFile, "-v", ver)
    }
}