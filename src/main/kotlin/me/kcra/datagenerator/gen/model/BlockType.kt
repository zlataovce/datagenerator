package me.kcra.datagenerator.gen.model

data class BlockType(
    val id: Int,
    val mojangName: String,
    val namespacedKey: String,
    val explosionResistance: Float,
    val frictionFactor: Float,
    val speedFactor: Float?,
    val jumpFactor: Float?,
    val dynamicShape: Boolean,
    val lootTableLocation: String?
)
