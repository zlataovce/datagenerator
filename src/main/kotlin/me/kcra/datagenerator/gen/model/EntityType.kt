package me.kcra.datagenerator.gen.model

data class EntityType(
    val id: Int,
    val mojangName: String,
    val namespacedKey: String,
    val packetType: String,
    val fireImmune: Boolean?,
    val height: Float?,
    val width: Float?,
    val clientTrackingRange: Int?,
    val lootTableLocation: String?,
    val metadata: List<EntityMetadata>
)
