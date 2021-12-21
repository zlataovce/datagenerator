package me.kcra.datagenerator.mapping

import me.kcra.datagenerator.utils.Version

data class MappingOverride(
    private var _comment: String = "",
    val type: String,
    val new: String,
    val old: String,
    val versions: Version.VersionRange
)
