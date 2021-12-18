package me.kcra.datagenerator.mapping

data class MappingOverride(
    private var _comment: String = "",
    val type: String,
    val new: String,
    val old: String,
    val versions: List<String>
)
