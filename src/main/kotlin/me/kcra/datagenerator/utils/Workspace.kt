package me.kcra.datagenerator.utils

import java.io.File
import java.nio.file.Path

class Workspace(private val dataFolder: File) {
    companion object {
        @JvmStatic
        var currentWorkspace: Workspace? = null

        @JvmStatic
        fun createWorkspace(version: Version) {
            currentWorkspace = Workspace(
                Path.of(System.getProperty("user.dir"), "work", version.toString().replace('.', '_')).toFile()
                    .also { it.mkdirs() }
            )
        }
    }

    fun createFile(name: String): File {
        return Path.of(dataFolder.absolutePath, name).toFile()
    }

    fun createDirectory(name: String): File {
        return Path.of(dataFolder.absolutePath, name).toFile().also { it.mkdirs() }
    }
}