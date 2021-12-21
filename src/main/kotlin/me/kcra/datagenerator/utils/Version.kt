package me.kcra.datagenerator.utils

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
        return majorVersion > major || minorVersion > minor || patchVersion > patch || this.suffix != suffix
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
        return (isNewerThan(min) && isOlderThan(max)) || (equals(min) || equals(max))
    }

    override fun toString(): String {
        return "$majorVersion.$minorVersion${if (patchVersion != 0) ".$patchVersion" else ""}${if (suffix != null) "-$suffix" else ""}"
    }

    override fun hashCode(): Int {
        var result = majorVersion
        result = 31 * result + minorVersion
        result = 31 * result + patchVersion
        result = 31 * result + (suffix?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Version

        if (majorVersion != other.majorVersion) return false
        if (minorVersion != other.minorVersion) return false
        if (patchVersion != other.patchVersion) return false
        if (suffix != other.suffix) return false

        return true
    }
}