package me.kcra.datagenerator.utils

import org.objectweb.asm.ClassReader
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class MinecraftJarReader(file: File, version: String) {
    private val zip: ZipFile
    val classLoader: URLClassLoader

    init {
        val currentFile = ZipFile(file)
        if (currentFile.getEntry("net/minecraft/bundler/Main.class") != null) {
            println("Detected bundler JAR!")
            val bundledJar: File = Workspace.currentWorkspace?.createFile("bundled.jar")
                ?: throw RuntimeException("Current workspace is not set")
            val bundledEntry: ZipEntry = currentFile.getEntry("META-INF/versions/$version/server-$version.jar")
            if (!bundledJar.isFile || !verifyChecksum(bundledEntry, bundledJar.toPath())) {
                Files.copy(
                    currentFile.getInputStream(bundledEntry),
                    bundledJar.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
            zip = ZipFile(bundledJar)
            val extractedJar: File = Workspace.currentWorkspace?.createDirectory("extracted")
                ?: throw RuntimeException("Current workspace is not set")
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