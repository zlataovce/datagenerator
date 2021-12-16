package me.kcra.datagenerator.mapping

import net.minecraftforge.srgutils.IMappingFile

class ClassRemapper(private val refMapping: MappingSet, private val mapping: SecondaryMappingSet?) {
    private fun hasTargetMapping(): Boolean {
        return mapping != null
    }

    fun remapClass(obf: String): IMappingFile.IClass {
        if (!hasTargetMapping()) {
            return refMapping.mojang.getMappedClass
        }
        return mapping.
    }
}