package me.kcra.datagenerator.mapping

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import me.kcra.datagenerator.utils.Version

data class MappingOverride(
    private var _comment: String = "",
    val type: String,
    val new: String,
    val old: String,
    @JsonIgnore
    var minVersion: Version?,
    @JsonIgnore
    var maxVersion: Version?
) {
    @JsonProperty("versions")
    private fun getJsonVersions(): String {
        return "$minVersion->$maxVersion"
    }

    @JsonProperty("versions")
    private fun setJsonVersions(versions: String) {
        versions.split("->", limit = 2).also {
            minVersion = Version(it[0])
            maxVersion = Version(it[1])
        }
    }
}
