package me.kcra.datagenerator.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

@JsonDeserialize(using = Version.VersionDeserializer::class)
class Version(version: String) {
    val majorVersion: Int
    val minorVersion: Int
    val patchVersion: Int
    val suffix: String?

    init {
        val split: List<String> =
            (if (version.contains('-')) version.substring(0, version.indexOf('-')) else version).split('.')
        majorVersion = split[0].toInt()
        minorVersion = split[1].toInt()
        patchVersion = split.getOrNull(2)?.toInt() ?: 0
        suffix = version.indexOf('-').let {
            if (it == -1) {
                return@let null
            }
            return@let version.substring(it + 1)
        }
    }

    fun matches(version: String): Boolean {
        return toString() == version
    }

    fun matches(version: Version): Boolean {
        return matches(version.majorVersion, version.minorVersion, version.patchVersion, version.suffix)
    }

    fun matches(major: Int, minor: Int, patch: Int, suffix: String?): Boolean {
        return this.majorVersion == major && this.minorVersion == minor && this.patchVersion == patch && this.suffix == suffix
    }

    fun isOlderThan(version: String): Boolean {
        val other = Version(version)
        return majorVersion < other.majorVersion ||
                (majorVersion == other.majorVersion && minorVersion < other.minorVersion) ||
                (majorVersion == other.majorVersion && minorVersion == other.minorVersion && patchVersion < other.patchVersion)
    }

    fun isOlderThan(major: Int, minor: Int, patch: Int, suffix: String?): Boolean {
        return majorVersion < major || minorVersion < minor || patchVersion < patch || this.suffix != suffix
    }

    fun isOlderThan(other: Version): Boolean {
        return majorVersion < other.majorVersion ||
                majorVersion == other.majorVersion && minorVersion < other.minorVersion ||
                majorVersion == other.majorVersion && minorVersion == other.minorVersion && patchVersion < other.patchVersion
    }

    fun isNewerThan(version: String): Boolean {
        val other = Version(version)
        return majorVersion > other.majorVersion ||
                (majorVersion == other.majorVersion && minorVersion > other.minorVersion) ||
                (majorVersion == other.majorVersion && minorVersion == other.minorVersion && patchVersion > other.patchVersion)
    }

    fun isNewerThan(major: Int, minor: Int, patch: Int, suffix: String?): Boolean {
        return majorVersion > major || (minorVersion > minor && patchVersion > patch) || this.suffix != suffix
    }

    fun isNewerThan(other: Version): Boolean {
        return majorVersion > other.majorVersion ||
                majorVersion == other.majorVersion && minorVersion > other.minorVersion ||
                majorVersion == other.majorVersion && minorVersion == other.minorVersion && patchVersion > other.patchVersion
    }

    fun isBetween(min: String, max: String): Boolean {
        return isBetween(Version(min), Version(max))
    }

    fun isBetween(min: Version, max: Version): Boolean {
        return (isNewerThan(min) && isOlderThan(max)) || matches(min) || matches(max)
    }

    override fun toString(): String {
        return "$majorVersion.$minorVersion${if (patchVersion != 0) ".$patchVersion" else ""}${if (suffix != null) "-$suffix" else ""}"
    }

    @JsonDeserialize(using = VersionRangeDeserializer::class)
    data class VersionRange(
        val minVersion: Version,
        val maxVersion: Version
    )

    class VersionDeserializer : StdDeserializer<Version>(Version::class.java) {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Version {
            return Version(p.valueAsString)
        }
    }

    class VersionRangeDeserializer : StdDeserializer<VersionRange>(VersionRange::class.java) {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): VersionRange {
            return p.valueAsString.split("->", limit = 2).let { VersionRange(Version(it[0]), Version(it[1])) }
        }
    }
}