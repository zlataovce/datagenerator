package me.kcra.datagenerator.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

fun getFromURL(url: String): File? {
    val file: File = Workspace.currentWorkspace?.createFile(url.substring(url.lastIndexOf('/') + 1))
        ?: throw RuntimeException("Current workspace is not set")
    if (!file.isFile || file.length() != getContentLength(URL(url))) {
        try {
            URL(url).openStream().use { inputStream ->
                Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (ignored: Exception) {
            return null
        }
    }
    return file
}

private fun getContentLength(url: URL): Long {
    var conn: URLConnection? = null
    try {
        conn = url.openConnection()
        if (conn is HttpURLConnection) {
            conn.requestMethod = "HEAD"
        }
        return conn.contentLengthLong
    } catch (e: IOException) {
        throw RuntimeException(e)
    } finally {
        if (conn != null && conn is HttpURLConnection) {
            conn.disconnect()
        }
    }
}

fun minecraftResource(mapper: ObjectMapper, version: String, res: String): File? {
    val manifest: JsonNode = mapper.readTree(URL("https://launchermeta.mojang.com/mc/game/version_manifest.json"))
    manifest.path("versions").forEach { jsonNode ->
        if (jsonNode.get("id").asText().equals(version)) {
            val versionManifest: JsonNode = mapper.readTree(URL(jsonNode.get("url").asText()))
            if (versionManifest.path("downloads").has(res)) {
                return getFromURL(versionManifest.path("downloads").path(res).get("url").asText())
            }
        }
    }
    return null
}

fun seargeMapping(version: String): InputStream? {
    return seargeMapping0("https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/$version/mcp_config-$version.zip")
        ?: seargeMapping0("https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp/$version/mcp-$version-srg.zip")
}

private fun seargeMapping0(url: String): InputStream? {
    val mcpZip: File = getFromURL(url) ?: return null
    val zipFile = ZipFile(mcpZip)
    return zipFile.getInputStream(
        zipFile.stream()
            .filter { it.name.equals("config/joined.tsrg") || it.name.equals("joined.srg") }
            .findFirst()
            .orElseThrow()
    )
}

fun intermediaryMapping(version: String): File? {
    return getFromURL("https://raw.githubusercontent.com/FabricMC/intermediary/master/mappings/$version.tiny")
}

fun unzip(zipFile: ZipFile, dest: File): File {
    val iter: Enumeration<out ZipEntry> = zipFile.entries()
    while (iter.hasMoreElements()) {
        val entry: ZipEntry = iter.nextElement()
        if (entry.isDirectory) {
            continue
        }
        if (entry.name.contains("..")) {
            throw IOException("Entry is outside of target directory!")
        }
        val entryFile: File = Path.of(dest.absolutePath, entry.name).toFile()
        entryFile.mkdirs()
        Files.copy(zipFile.getInputStream(entry), entryFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
    return dest
}

fun verifyChecksum(entry: ZipEntry, file: Path): Boolean {
    return CRC32().also { it.update(Files.readAllBytes(file)) }.value == entry.crc
}