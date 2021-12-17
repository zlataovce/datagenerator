package me.kcra.datagenerator.utils

import org.objectweb.asm.ClassReader
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

class MinecraftJarReader(file: File, version: String) {
    private val zip: ZipFile
    val classLoader: URLClassLoader

    init {
        val currentFile = ZipFile(file)
        if (currentFile.getEntry("net/minecraft/bundler/Main.class") != null) {
            println("Detected bundler JAR!")
            val bundledJar: File = File.createTempFile("bundled_file", ".jar")
            Files.copy(
                currentFile.getInputStream(currentFile.getEntry("META-INF/versions/$version/server-$version.jar")),
                bundledJar.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
            zip = ZipFile(bundledJar)
            val extractedJar: File = Files.createTempDirectory("extracted_jar").toFile()
            println("Unzipping Minecraft server JAR...")
            unzip(currentFile, extractedJar)
            classLoader = URLClassLoader.newInstance(
                Files.walk(extractedJar.toPath())
                    .map { it.toFile() }
                    .filter { it.isFile }
                    .filter { it.name.endsWith(".jar") }
                    .map { it.toURI().toURL() }
                    .toArray { size -> arrayOfNulls<URL>(size) },
                ClassLoader.getSystemClassLoader()
            )
        } else {
            zip = currentFile
            classLoader = URLClassLoader.newInstance(
                Array(1) { file.toURI().toURL() },
                ClassLoader.getSystemClassLoader()
            )
        }
        println("Loaded Minecraft server classes.")
    }

    fun readClass(path: String): ClassReader {
        println("Reading class $path...")
        return ClassReader(zip.getInputStream(zip.getEntry("$path.class")))
    }
}