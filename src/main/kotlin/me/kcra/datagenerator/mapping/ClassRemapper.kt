package me.kcra.datagenerator.mapping

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import me.kcra.datagenerator.utils.Version
import net.minecraftforge.srgutils.IMappingFile
import java.util.*
import kotlin.collections.ArrayList

class ClassRemapper(
    private val refMapping: MappingSet,
    private val refMapping2: MappingSet?,
    private val mapping: SecondaryMappingSet?,
    private val refVersion: Version,
    private val currentVersion: Version
) {
    private val overrides: List<MappingOverride> =
        jacksonObjectMapper().readValue<ArrayList<MappingOverride>>(this.javaClass.getResourceAsStream("/overrides.json")!!)

    companion object {
        @JvmStatic
        val typeParamPattern: Regex = Regex("<[^>]>")
    }

    private fun checkOverrides(mapped: String, type: String): String {
        val override: Optional<MappingOverride> = overrides.stream()
            .filter {
                currentVersion.isBetween(it.versions.minVersion, it.versions.maxVersion)
                        // && !refVersion.isBetween(it.versions.minVersion, it.versions.maxVersion)
                        && it.new == mapped && it.type == type
            }
            .findFirst()
        if (override.isPresent) {
            return override.orElseThrow().old
        }
        return mapped
    }

    private fun getMappedClassPkgInsensitive(file: IMappingFile, mapped: String): IMappingFile.IClass? {
        return file.classes.stream()
            .filter {
                val overriden: String = checkOverrides(mapped, "class")
                it.mapped.substring(it.mapped.lastIndexOf('/') + 1) == overriden.substring(overriden.lastIndexOf('/') + 1)
            }
            .findFirst()
            .orElse(null)
    }

    private fun getMappedClass(file: IMappingFile, mapped: String): IMappingFile.IClass? {
        return file.classes.stream()
            .filter { it.mapped.equals(checkOverrides(mapped, "class")) }
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
            .filter {
                it.mappedDescriptor.replace(typeParamPattern, "") == descriptor
                        && it.mapped.equals(checkOverrides(method, "method"))
            }
            .findFirst()
            .orElse(null)
    }

    private fun getMappedMethod(cls: IMappingFile.IClass, method: String): IMappingFile.IMethod? {
        return cls.methods.stream()
            .filter { it.mapped.equals(checkOverrides(method, "method")) }
            .findFirst()
            .orElse(null)
    }

    private fun getMappedField(cls: IMappingFile.IClass, field: String): IMappingFile.IField? {
        return cls.fields.stream()
            .filter { it.mapped.equals(checkOverrides(field, "field")) }
            .findFirst()
            .orElse(null)
    }

    fun getClass(mapped: String): IMappingFile.IClass? {
        if (mapping == null) {
            return getMappedClass(refMapping.mojang, mapped)
        }
        if (mapping.intermediary != null) {
            return getMappedClass(
                mapping.intermediary,
                refMapping.intermediary.getClass(getMappedClass(refMapping.mojang, mapped)?.original).mapped
            )
        }
        val refClass: IMappingFile.IClass = refMapping.searge.getClass(getMappedClass(refMapping.mojang, mapped)?.original)
        val result: IMappingFile.IClass? = getMappedClass(
            mapping.searge,
            refClass.mapped
        )
        // 1.14> searge entity class name inconsistency fix
        if (result == null && isSeargeEntityClassNew(refClass.mapped)) {
            val originalClass: String = refClass.mapped.substring(refClass.mapped.lastIndexOf('/') + 1)
            val fixedClass: String = "Entity" + originalClass.replaceFirst("Entity", "")
            return getMappedClassPkgInsensitive(mapping.searge, refClass.mapped.replaceFirst(originalClass, fixedClass))
        }
        return result
    }

    fun remapClass(obf: String): IMappingFile.IClass? {
        if (mapping == null) {
            return refMapping.mojang.getClass(obf)
        }
        if (mapping.intermediary != null) {
            return refMapping.mojang.getClass(
                getMappedClass(
                    refMapping.intermediary,
                    mapping.intermediary.getClass(obf).mapped
                )?.original
            )
        }
        return refMapping.mojang.getClass(
            getMappedClass(
                refMapping.searge,
                mapping.searge.getClass(obf).mapped
            )?.original
        )
    }

    fun getMethod(cls: String, method: String, descriptor: String): IMappingFile.IMethod? {
        if (mapping == null) {
            return getMappedClass(refMapping.mojang, cls)?.let { getMappedMethod(it, method, descriptor) }
        }
        val mojangRefClass: IMappingFile.IClass? = getMappedClass(refMapping.mojang, cls)
        if (mapping.intermediary != null) {
            val intermClass: IMappingFile.IClass? = refMapping.intermediary.getClass(mojangRefClass?.original)
            val intermMethod: IMappingFile.IMethod? =
                intermClass?.let {
                    mojangRefClass?.let { getMappedMethod(it, method, descriptor) }
                        ?.let { it1 -> it.getMethod(it1.original, it1.descriptor) }
                }
            val intermVClass: IMappingFile.IClass? =
                intermClass?.let { getMappedClass(mapping.intermediary, it.mapped) }
            val result: IMappingFile.IMethod? =
                intermMethod?.let { intermVClass?.let { it1 -> getMappedMethod(it1, it.mapped, intermMethod.mappedDescriptor) } }
            // intermediary for Registry#getId is missing in 1.14.x for some reason
            if (result != null) {
                return result
            }
        }
        val seargeClass: IMappingFile.IClass? = refMapping.searge.getClass(mojangRefClass?.original)
        val seargeMethod: IMappingFile.IMethod? =
            seargeClass?.let {
                mojangRefClass?.let { getMappedMethod(it, method, descriptor) }
                    ?.let { it1 -> it.getMethod(it1.original, it1.descriptor) }
            }
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
                intermClass?.let {
                    mojangRefClass?.let { getMappedMethod(it, method)?.original }
                        ?.let { it1 -> getMethod0(it, it1) }
                }
            val intermVClass: IMappingFile.IClass? =
                intermClass?.let { getMappedClass(mapping.intermediary, it.mapped) }
            return intermVClass?.let { intermMethod?.let { it1 -> getMappedMethod(it, it1.mapped) } }
        }
        val seargeClass: IMappingFile.IClass? = refMapping.searge.getClass(mojangRefClass?.original)
        val seargeMethod: IMappingFile.IMethod? =
            seargeClass?.let {
                mojangRefClass?.let { getMappedMethod(it, method)?.original }
                    ?.let { it1 -> getMethod0(it, it1) }
            }
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
            val intermVMethod: IMappingFile.IMethod? =
                intermVClass?.let { intermMethod?.let { it1 -> getMappedMethod(it, it1.mapped) } }
            return intermVClass?.let { intermVMethod?.let { it1 -> getMethod0(it, it1.original) } }
        }
        val seargeClass: IMappingFile.IClass = mapping.searge.getClass(cls)
        val seargeMethod: IMappingFile.IMethod? = getMethod0(seargeClass, method)
        val seargeVClass: IMappingFile.IClass? = getMappedClass(refMapping.searge, seargeClass.mapped)
        val seargeVMethod: IMappingFile.IMethod? =
            seargeMethod?.let { seargeVClass?.let { it1 -> getMappedMethod(it1, it.mapped) } }
        return seargeVClass?.let { seargeVMethod?.let { it1 -> getMethod0(it, it1.original) } }
    }

    fun getField(cls: String, field: String): IMappingFile.IField? {
        if (mapping == null) {
            return getMappedClass(refMapping.mojang, cls)?.let { getMappedField(it, field) }
        }
        val mojangRefClass: IMappingFile.IClass? = getMappedClass(refMapping.mojang, cls)
        if (mapping.intermediary != null) {
            val intermClass: IMappingFile.IClass? = refMapping.intermediary.getClass(mojangRefClass?.original)
            val intermField: IMappingFile.IField? =
                intermClass?.getField(mojangRefClass?.let { getMappedField(it, field)?.original })
            val intermVClass: IMappingFile.IClass? =
                intermClass?.let { getMappedClass(mapping.intermediary, it.mapped) }
            return intermVClass?.let { intermField?.let { it1 -> getMappedField(it, it1.mapped) } }
        }
        val seargeClass: IMappingFile.IClass? = refMapping.searge.getClass(mojangRefClass?.original)
        val seargeField: IMappingFile.IField? =
            seargeClass?.getField(mojangRefClass?.let { getMappedField(it, field)?.original })
        val seargeVClass: IMappingFile.IClass? = seargeClass?.let { getMappedClass(mapping.searge, it.mapped) }
        return seargeVClass?.let { seargeField?.let { it1 -> getMappedField(it, it1.mapped) } }
    }
    
    fun remapField(cls: String, field: String): IMappingFile.IField? {
        return remapField(cls, field, refMapping, true)
    }

    private fun remapField(cls: String, field: String, refMappingLocal: MappingSet, firstRemapping: Boolean): IMappingFile.IField? {
        if (mapping == null) {
            return refMappingLocal.mojang.getClass(cls).getField(field)
        }
        if (mapping.intermediary != null) {
            val intermClass: IMappingFile.IClass = mapping.intermediary.getClass(cls)
            val intermField: IMappingFile.IField? = intermClass.getField(field)
            val intermVClass: IMappingFile.IClass? = getMappedClass(refMappingLocal.intermediary, intermClass.mapped)
            val intermVField: IMappingFile.IField? =
                intermVClass?.let { intermField?.let { it1 -> getMappedField(it, it1.mapped) } }
            return refMappingLocal.mojang.getClass(intermVClass?.original)?.getField(intermVField?.original) ?: if (refMapping2 != null && firstRemapping) remapField(cls, field, refMapping2, false) else null
        }
        val seargeClass: IMappingFile.IClass = mapping.searge.getClass(cls)
        val seargeField: IMappingFile.IField? = seargeClass.getField(field)
        if (cls == "bcs") {
            println("mapped class")
        }
        var seargeVClass: IMappingFile.IClass? = getMappedClass(refMappingLocal.searge, seargeClass.mapped)
        // 1.14> searge entity class name inconsistency fix
        if (seargeVClass == null && isSeargeEntityClassOld(seargeClass.mapped)) {
            seargeVClass = getMappedClassPkgInsensitive(refMappingLocal.searge, seargeClass.mapped.replaceFirst("Entity", "") + "Entity")
        }
        val seargeVField: IMappingFile.IField? =
            seargeVClass?.let { seargeField?.let { it1 -> getMappedField(it, it1.mapped) } }
        if (cls == "bcs") {
            println(seargeField)
            println(seargeVField)
            println(seargeClass)
            println(seargeVClass)
        }
        return refMappingLocal.mojang.getClass(seargeVClass?.original)?.getField(seargeVField?.original) ?: if (refMapping2 != null && firstRemapping) remapField(cls, field, refMapping2, false) else null
    }

    private fun isSeargeEntityClassOld(mapped: String): Boolean {
        return mapped.startsWith("net/minecraft/entity") && mapped.substring(mapped.lastIndexOf('/') + 1).startsWith("Entity")
    }

    private fun isSeargeEntityClassNew(mapped: String): Boolean {
        return mapped.startsWith("net/minecraft/entity") && mapped.substring(mapped.lastIndexOf('/') + 1).endsWith("Entity")
    }
}