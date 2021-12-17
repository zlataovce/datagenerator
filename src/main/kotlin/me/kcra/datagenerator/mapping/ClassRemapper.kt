package me.kcra.datagenerator.mapping

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.minecraftforge.srgutils.IMappingFile

class ClassRemapper(private val refMapping: MappingSet, private val mapping: SecondaryMappingSet?) {
    private val overrides: Map<String, String> = jacksonObjectMapper().readValue<HashMap<String, String>>(this.javaClass.getResourceAsStream("/overrides.json")!!)

    private fun getMappedClass(file: IMappingFile, mapped: String): IMappingFile.IClass? {
        return file.classes.stream()
            .filter { it.mapped.equals(mapped) || it.mapped.equals(overrides[mapped]) }
            .findFirst()
            .orElse(null)
    }

    private fun getMethod(cls: IMappingFile.IClass, method: String): IMappingFile.IMethod? {
        return cls.methods.stream()
            .filter { it.original.equals(method) }
            .findFirst()
            .orElse(null)
    }

    private fun getMappedMethod(cls: IMappingFile.IClass, method: String): IMappingFile.IMethod? {
        return cls.methods.stream()
            .filter { it.mapped.equals(method) }
            .findFirst()
            .orElse(null)
    }

    private fun getMappedField(cls: IMappingFile.IClass, method: String): IMappingFile.IField? {
        return cls.fields.stream()
            .filter { it.mapped.equals(method) }
            .findFirst()
            .orElse(null)
    }

    fun getClass(mapped: String): IMappingFile.IClass? {
        if (mapping == null) {
            return getMappedClass(refMapping.mojang, mapped)
        }
        if (mapping.intermediary != null) {
            return getMappedClass(mapping.intermediary, refMapping.intermediary.getClass(getMappedClass(refMapping.mojang, mapped)?.original).mapped)
        }
        return getMappedClass(mapping.searge, refMapping.searge.getClass(getMappedClass(refMapping.mojang, mapped)?.original).mapped)
    }

    fun remapClass(obf: String): IMappingFile.IClass? {
        if (mapping == null) {
            return refMapping.mojang.getClass(obf)
        }
        if (mapping.intermediary != null) {
            return refMapping.mojang.getClass(getMappedClass(refMapping.intermediary, mapping.intermediary.getClass(obf).mapped)?.original)
        }
        return refMapping.mojang.getClass(getMappedClass(refMapping.searge, mapping.searge.getClass(obf).mapped)?.original)
    }

    fun remapMethod(cls: String, method: String): IMappingFile.IMethod? {
        if (mapping == null) {
            return refMapping.mojang.getClass(cls).methods.stream()
                .filter { it.mapped.equals(method) }
                .findFirst()
                .orElse(null)
        }
        if (mapping.intermediary != null) {
            val intermClass: IMappingFile.IClass = mapping.intermediary.getClass(cls)
            val intermMethod: IMappingFile.IMethod? = getMethod(intermClass, method)
            val intermVClass: IMappingFile.IClass? = getMappedClass(refMapping.intermediary, intermClass.mapped)
            val intermVMethod: IMappingFile.IMethod? = intermVClass?.let { intermMethod?.let { it1 -> getMappedMethod(it, it1.mapped) } }
            return intermVClass?.let { intermVMethod?.let { it1 -> getMethod(it, it1.original) } }
        }
        val seargeClass: IMappingFile.IClass = mapping.searge.getClass(cls)
        val seargeMethod: IMappingFile.IMethod? = getMethod(seargeClass, method)
        val seargeVClass: IMappingFile.IClass? = getMappedClass(refMapping.searge, seargeClass.mapped)
        val seargeVMethod: IMappingFile.IMethod? = seargeMethod?.let { seargeVClass?.let { it1 -> getMappedMethod(it1, it.mapped) } }
        return seargeVClass?.let { seargeVMethod?.let { it1 -> getMethod(it, it1.original) } }
    }

    fun remapField(cls: String, field: String): IMappingFile.IField? {
        if (mapping == null) {
            return refMapping.mojang.getClass(cls).getField(field)
        }
        if (mapping.intermediary != null) {
            val intermClass: IMappingFile.IClass = mapping.intermediary.getClass(cls)
            val intermField: IMappingFile.IField? = intermClass.getField(field)
            val intermVClass: IMappingFile.IClass? = getMappedClass(refMapping.intermediary, intermClass.mapped)
            val intermVField: IMappingFile.IField? = intermVClass?.let { intermField?.let { it1 -> getMappedField(it, it1.mapped) } }
            return refMapping.mojang.getClass(intermVClass?.original).getField(intermVField?.original)
        }
        val seargeClass: IMappingFile.IClass = mapping.searge.getClass(cls)
        val seargeField: IMappingFile.IField? = seargeClass.getField(field)
        val seargeVClass: IMappingFile.IClass? = getMappedClass(refMapping.searge, seargeClass.mapped)
        val seargeVField: IMappingFile.IField? = seargeVClass?.let { seargeField?.let { it1 -> getMappedField(it, it1.mapped) } }
        return refMapping.mojang.getClass(seargeVClass?.original).getField(seargeVField?.original)
    }
}