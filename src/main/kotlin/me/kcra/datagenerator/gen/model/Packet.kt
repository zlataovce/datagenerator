package me.kcra.datagenerator.gen.model

data class Packet(
    val mojangName: String,
    val fields: List<PacketField>
)

data class PacketField(
    val mojangName: String?,
    val type: String
)
