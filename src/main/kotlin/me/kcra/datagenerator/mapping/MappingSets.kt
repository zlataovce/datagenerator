package me.kcra.datagenerator.mapping

import net.minecraftforge.srgutils.IMappingFile

data class MappingSet(
    val mojang: IMappingFile, // make sure that it is reversed
    val searge: IMappingFile,
    val intermediary: IMappingFile
)

data class SecondaryMappingSet(
    val searge: IMappingFile,
    val intermediary: IMappingFile?
)
