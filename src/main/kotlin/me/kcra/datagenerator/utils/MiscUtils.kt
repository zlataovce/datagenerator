package me.kcra.datagenerator.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.io.InputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

fun getFromURL(url: String): File? {
    val file: Path = Files.createTempFile("downloaded_file", null)
    try {
        URL(url).openStream().use { inputStream ->
            Files.copy(inputStream, file, StandardCopyOption.REPLACE_EXISTING)
        }
    } catch (ignored: Exception) {
        return null
    }
    return file.toFile()
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