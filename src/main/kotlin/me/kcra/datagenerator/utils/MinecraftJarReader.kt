package me.kcra.datagenerator.utils

import org.objectweb.asm.ClassReader
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

class MinecraftJarReader(file: File, version: String) {
    private val zip: ZipFile

    init {
        val currentFile = ZipFile(file)
        zip = if (currentFile.getEntry("net/minecraft/bundler/Main.class") != null) {
            println("Detected bundler JAR!")
            val bundledJar: File = File.createTempFile("bundled_file", ".jar")
            Files.copy(
                currentFile.getInputStream(currentFile.getEntry("META-INF/versions/$version/server-$version.jar")),
                bundledJar.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
            ZipFile(bundledJar)
        } else {
            currentFile
        }
    }

    fun readClass(path: String): ClassReader {
        println("Reading class $path...")
        return ClassReader(zip.getInputStream(zip.getEntry("$path.class")))
    }
}