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

    private fun getMethod0(cls: IMappingFile.IClass, method: String): IMappingFile.IMethod? {
        return cls.methods.stream()
            .filter { it.original.equals(method) }
            .findFirst()
            .orElse(null)
    }

    private fun getMappedMethod(cls: IMappingFile.IClass, method: String, descriptor: String): IMappingFile.IMethod? {
        return cls.methods.stream()
            .filter { it.mappedDescriptor.equals(descriptor) && it.mapped.equals(method) }
            .findFirst()
            .orElse(null)
    }

    private fun getMappedMethod(cls: IMappingFile.IClass, method: String): IMappingFile.IMethod? {
        return cls.methods.stream()
            .filter { it.mapped.equals(method) }
            .findFirst()
            .orElse(null)
    }

    private fun getMappedField(cls: IMappingFile.IClass, field: String): IMappingFile.IField? {
        return cls.fields.stream()
            .filter { it.mapped.equals(field) }
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

    fun getMethod(cls: String, method: String, descriptor: String): IMappingFile.IMethod? {
        if (mapping == null) {
            return getMappedClass(refMapping.mojang, cls)?.let { getMappedMethod(it, method, descriptor) }
        }
        val mojangRefClass: IMappingFile.IClass? = getMappedClass(refMapping.mojang, cls)
        if (mapping.intermediary != null) {
            val intermClass: IMappingFile.IClass? = refMapping.intermediary.getClass(mojangRefClass?.original)
            val intermMethod: IMappingFile.IMethod? =
                intermClass?.let { mojangRefClass?.let { getMappedMethod(it, method, descriptor) }
                    ?.let { it1 -> it.getMethod(it1.original, it1.descriptor) } }
            val intermVClass: IMappingFile.IClass? = intermClass?.let { getMappedClass(mapping.intermediary, it.mapped) }
            return intermVClass?.let { intermMethod?.let { it1 -> getMappedMethod(it, it1.mapped, it1.mappedDescriptor) } }
        }
        val seargeClass: IMappingFile.IClass? = refMapping.searge.getClass(mojangRefClass?.original)
        val seargeMethod: IMappingFile.IMethod? =
            seargeClass?.let { mojangRefClass?.let { getMappedMethod(it, method, descriptor) }
                ?.let { it1 -> it.getMethod(it1.original, it1.descriptor) } }
        val seargeVClass: IMappingFile.IClass? = seargeClass?.let { getMappedClass(mapping.searge, it.mapped) }
        return seargeVClass?.let { seargeMethod?.let { it1 -> getMappedMethod(it, it1.mapped, it1.mappedDescriptor) } }
    }

    fun getMethod(cls: String, method: String): IMappingFile.IMethod? {
        if (mapping == null) {
            return getMappedClass(refMapping.mojang, cls)?.let { getMappedMethod(it, method) }
        }
        val mojangRefClass: IMappingFile.IClass? = getMappedClass(refMapping.mojang, cls)
        if (mapping.intermediary != null) {
            val intermClass: IMappingFile.IClass? = refMapping.intermediary.getClass(mojangRefClass?.original)
            val intermMethod: IMappingFile.IMethod? =
                intermClass?.let { mojangRefClass?.let { getMappedMethod(it, method)?.original }
                    ?.let { it1 -> getMethod0(it, it1) } }
            val intermVClass: IMappingFile.IClass? = intermClass?.let { getMappedClass(mapping.intermediary, it.mapped) }
            return intermVClass?.let { intermMethod?.let { it1 -> getMappedMethod(it, it1.mapped) } }
        }
        val seargeClass: IMappingFile.IClass? = refMapping.searge.getClass(mojangRefClass?.original)
        val seargeMethod: IMappingFile.IMethod? =
            seargeClass?.let { mojangRefClass?.let { getMappedField(it, method)?.original }
                ?.let { it1 -> getMethod0(it, it1) } }
        val seargeVClass: IMappingFile.IClass? = seargeClass?.let { getMappedClass(mapping.searge, it.mapped) }
        return seargeVClass?.let { seargeMethod?.let { it1 -> getMappedMethod(it, it1.mapped) } }
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
            val intermMethod: IMappingFile.IMethod? = getMethod0(intermClass, method)
            val intermVClass: IMappingFile.IClass? = getMappedClass(refMapping.intermediary, intermClass.mapped)
            val intermVMethod: IMappingFile.IMethod? = intermVClass?.let { intermMethod?.let { it1 -> getMappedMethod(it, it1.mapped) } }
            return intermVClass?.let { intermVMethod?.let { it1 -> getMethod0(it, it1.original) } }
        }
        val seargeClass: IMappingFile.IClass = mapping.searge.getClass(cls)
        val seargeMethod: IMappingFile.IMethod? = getMethod0(seargeClass, method)
        val seargeVClass: IMappingFile.IClass? = getMappedClass(refMapping.searge, seargeClass.mapped)
        val seargeVMethod: IMappingFile.IMethod? = seargeMethod?.let { seargeVClass?.let { it1 -> getMappedMethod(it1, it.mapped) } }
        return seargeVClass?.let { seargeVMethod?.let { it1 -> getMethod0(it, it1.original) } }
    }

    fun getField(cls: String, field: String): IMappingFile.IField? {
        if (mapping == null) {
            return getMappedClass(refMapping.mojang, cls)?.let { getMappedField(it, field) }
        }
        val mojangRefClass: IMappingFile.IClass? = getMappedClass(refMapping.mojang, cls)
        if (mapping.intermediary != null) {
            val intermClass: IMappingFile.IClass? = refMapping.intermediary.getClass(mojangRefClass?.original)
            val intermField: IMappingFile.IField? = intermClass?.getField(mojangRefClass?.let { getMappedField(it, field)?.original })
            val intermVClass: IMappingFile.IClass? = intermClass?.let { getMappedClass(mapping.intermediary, it.mapped) }
            return intermVClass?.let { intermField?.let { it1 -> getMappedField(it, it1.mapped) } }
        }
        val seargeClass: IMappingFile.IClass? = refMapping.searge.getClass(mojangRefClass?.original)
        val seargeField: IMappingFile.IField? = seargeClass?.getField(mojangRefClass?.let { getMappedField(it, field)?.original })
        val seargeVClass: IMappingFile.IClass? = seargeClass?.let { getMappedClass(mapping.searge, it.mapped) }
        return seargeVClass?.let { seargeField?.let { it1 -> getMappedField(it, it1.mapped) } }
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