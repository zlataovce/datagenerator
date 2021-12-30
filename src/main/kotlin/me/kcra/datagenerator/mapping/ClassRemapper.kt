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
                        && !refVersion.isBetween(it.versions.minVersion, it.versions.maxVersion)
                        && if (it.reversable) (it.new == mapped || it.old == mapped) else (it.new == mapped)
                        && it.type == type
            }
            .findFirst()
        if (override.isPresent) {
            val override0: MappingOverride = override.orElseThrow()
            if (override0.reversable && override0.old == mapped) {
                return override0.new
            }
            return override0.old
        }
        return mapped
    }

    private fun IMappingFile.getMappedClassPkgInsensitive(mapped: String): IMappingFile.IClass? {
        return classes.stream()
            .filter {
                val overriden: String = checkOverrides(mapped, "class")
                it.mapped.substring(it.mapped.lastIndexOf('/') + 1) == overriden.substring(overriden.lastIndexOf('/') + 1)
            }
            .findFirst()
            .orElse(null)
    }

    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    private fun IMappingFile.getMappedClass(mapped: String): IMappingFile.IClass? {
        return classes.stream()
            .filter { it.mapped.equals(checkOverrides(mapped, "class")) }
            .findFirst()
            .orElse(null)
    }

    private fun IMappingFile.IClass.getMethod(method: String): IMappingFile.IMethod? {
        return methods.stream()
            .filter { it.original.equals(method) }
            .findFirst()
            .orElse(null)
    }

    private fun IMappingFile.IClass.getMappedMethod(method: String, descriptor: String): IMappingFile.IMethod? {
        return methods.stream()
            .filter {
                it.mappedDescriptor.replace(typeParamPattern, "") == descriptor
                        && it.mapped.equals(checkOverrides(method, "method"))
            }
            .findFirst()
            .orElse(null)
    }

    private fun IMappingFile.IClass.getMappedMethod(method: String): IMappingFile.IMethod? {
        return methods.stream()
            .filter { it.mapped.equals(checkOverrides(method, "method")) }
            .findFirst()
            .orElse(null)
    }

    private fun IMappingFile.IClass.getMappedField(field: String): IMappingFile.IField? {
        return fields.stream()
            .filter { it.mapped.equals(checkOverrides(field, "field")) }
            .findFirst()
            .orElse(null)
    }

    fun getClass(mapped: String): IMappingFile.IClass? {
        if (mapping == null) {
            return refMapping.mojang.getMappedClass(mapped)
        }
        if (mapping.intermediary != null) {
            return mapping.intermediary.getMappedClass(
                refMapping.intermediary.getClass(refMapping.mojang.getMappedClass(mapped)?.original).mapped
            )
        }
        val refClass: IMappingFile.IClass =
            refMapping.searge.getClass(refMapping.mojang.getMappedClass(mapped)?.original)
        val result: IMappingFile.IClass? = mapping.searge.getMappedClass(refClass.mapped)
        // 1.14> searge entity class name inconsistency fix
        if (result == null && isSeargeEntityClassNew(refClass.mapped)) {
            val originalClass: String = refClass.mapped.substring(refClass.mapped.lastIndexOf('/') + 1)
            val fixedClass: String = "Entity" + originalClass.replaceFirst("Entity", "")
            return mapping.searge.getMappedClassPkgInsensitive(refClass.mapped.replaceFirst(originalClass, fixedClass))
        }
        return result
    }

    fun remapClass(obf: String): IMappingFile.IClass? {
        return remapClass(refMapping, obf, true)
    }

    private fun remapClass(refMappingLocal: MappingSet, obf: String, firstRemapping: Boolean): IMappingFile.IClass? {
        if (mapping == null) {
            return refMappingLocal.mojang.getClass(obf)
        }
        if (mapping.intermediary != null) {
            return refMappingLocal.mojang.getClass(
                refMappingLocal.intermediary.getMappedClass(
                    mapping.intermediary.getClass(obf).mapped
                )?.original
            ) ?: if (refMapping2 != null && firstRemapping) remapClass(refMapping2, obf, false) else null
        }
        val seargeClass: IMappingFile.IClass = mapping.searge.getClass(obf)
        var seargeVClass: IMappingFile.IClass? = refMappingLocal.searge.getMappedClass(seargeClass.mapped)
        // 1.14> searge packet class name inconsistency fix
        if (seargeVClass == null && (isSeargeServerPacketClassOld(seargeClass.mapped) || isSeargeClientPacketClassOld(seargeClass.mapped))) {
            val packetTypeLetter: Char = if (isSeargeServerPacketClassOld(seargeClass.mapped)) 'S' else 'C'
            val originalClass: String = seargeClass.mapped.substring(seargeClass.mapped.lastIndexOf('/') + 1)
                .let { if (it.contains('$')) it.substring(0, it.lastIndexOf('$')) else it }
            val fixedClass: String = packetTypeLetter + originalClass.replaceFirst("${packetTypeLetter}Packet", "") + "Packet"
            seargeVClass = refMappingLocal.searge.getMappedClassPkgInsensitive(
                fixedClass + (if (seargeClass.mapped.contains('$')) "$" + seargeClass.mapped.substring(seargeClass.mapped.lastIndexOf('$') + 1) else "")
            )
        }
        return refMappingLocal.mojang.getClass(
            seargeVClass?.original
        ) ?: if (refMapping2 != null && firstRemapping) remapClass(refMapping2, obf, false) else null
    }

    fun getMethod(cls: String, method: String, descriptor: String): IMappingFile.IMethod? {
        if (mapping == null) {
            return refMapping.mojang.getMappedClass(cls)?.getMappedMethod(method, descriptor)
        }
        val mojangRefClass: IMappingFile.IClass? = refMapping.mojang.getMappedClass(cls)
        if (mapping.intermediary != null) {
            val intermClass: IMappingFile.IClass? = refMapping.intermediary.getClass(mojangRefClass?.original)
            val intermMethod: IMappingFile.IMethod? =
                intermClass?.let {
                    mojangRefClass?.getMappedMethod(method, descriptor)
                        ?.let { it1 -> it.getMethod(it1.original, it1.descriptor) }
                }
            val intermVClass: IMappingFile.IClass? =
                intermClass?.let { mapping.intermediary.getMappedClass(it.mapped) }
            val result: IMappingFile.IMethod? =
                intermMethod?.let {
                    intermVClass?.getMappedMethod(
                        it.mapped,
                        intermMethod.mappedDescriptor
                    )
                }
            // intermediary for Registry#getId is missing in 1.14.x for some reason
            if (result != null) {
                return result
            }
        }
        val seargeClass: IMappingFile.IClass? = refMapping.searge.getClass(mojangRefClass?.original)
        val seargeMethod: IMappingFile.IMethod? =
            seargeClass?.let {
                mojangRefClass?.getMappedMethod(method, descriptor)
                    ?.let { it1 -> it.getMethod(it1.original, it1.descriptor) }
            }
        val seargeVClass: IMappingFile.IClass? = seargeClass?.let { mapping.searge.getMappedClass(it.mapped) }
        return seargeVClass?.let { seargeMethod?.let { it1 -> it.getMappedMethod(it1.mapped, it1.mappedDescriptor) } }
    }

    fun getMethod(cls: String, method: String): IMappingFile.IMethod? {
        if (mapping == null) {
            return refMapping.mojang.getMappedClass(cls)?.getMappedMethod(method)
        }
        val mojangRefClass: IMappingFile.IClass? = refMapping.mojang.getMappedClass(cls)
        if (mapping.intermediary != null) {
            val intermClass: IMappingFile.IClass? = refMapping.intermediary.getClass(mojangRefClass?.original)
            val intermMethod: IMappingFile.IMethod? =
                intermClass?.let {
                    mojangRefClass?.let { it.getMappedMethod(method)?.original }
                        ?.let { it1 -> it.getMethod(it1) }
                }
            val intermVClass: IMappingFile.IClass? =
                intermClass?.let { mapping.intermediary.getMappedClass(it.mapped) }
            return intermVClass?.let { intermMethod?.let { it1 -> it.getMappedMethod(it1.mapped) } }
        }
        val seargeClass: IMappingFile.IClass? = refMapping.searge.getClass(mojangRefClass?.original)
        val seargeMethod: IMappingFile.IMethod? =
            seargeClass?.let {
                mojangRefClass?.let { it.getMappedMethod(method)?.original }
                    ?.let { it1 -> it.getMethod(it1) }
            }
        val seargeVClass: IMappingFile.IClass? = seargeClass?.let { mapping.searge.getMappedClass(it.mapped) }
        return seargeVClass?.let { seargeMethod?.let { it1 -> it.getMappedMethod(it1.mapped) } }
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
            val intermMethod: IMappingFile.IMethod? = intermClass.getMethod(method)
            val intermVClass: IMappingFile.IClass? = refMapping.intermediary.getMappedClass(intermClass.mapped)
            val intermVMethod: IMappingFile.IMethod? =
                intermVClass?.let { intermMethod?.let { it1 -> it.getMappedMethod(it1.mapped) } }
            return intermVClass?.let { intermVMethod?.let { it1 -> it.getMethod(it1.original) } }
        }
        val seargeClass: IMappingFile.IClass = mapping.searge.getClass(cls)
        val seargeMethod: IMappingFile.IMethod? = seargeClass.getMethod(method)
        val seargeVClass: IMappingFile.IClass? = refMapping.searge.getMappedClass(seargeClass.mapped)
        val seargeVMethod: IMappingFile.IMethod? =
            seargeMethod?.let { seargeVClass?.getMappedMethod(it.mapped) }
        return seargeVClass?.let { seargeVMethod?.let { it1 -> it.getMethod(it1.original) } }
    }

    fun getField(cls: String, field: String): IMappingFile.IField? {
        if (mapping == null) {
            return refMapping.mojang.getMappedClass(cls)?.getMappedField(field)
        }
        val mojangRefClass: IMappingFile.IClass? = refMapping.mojang.getMappedClass(cls)
        if (mapping.intermediary != null) {
            val intermClass: IMappingFile.IClass? = refMapping.intermediary.getClass(mojangRefClass?.original)
            val intermField: IMappingFile.IField? =
                intermClass?.getField(mojangRefClass?.let { it.getMappedField(field)?.original })
            val intermVClass: IMappingFile.IClass? =
                intermClass?.let { mapping.intermediary.getMappedClass(it.mapped) }
            return intermVClass?.let { intermField?.let { it1 -> it.getMappedField(it1.mapped) } }
        }
        val seargeClass: IMappingFile.IClass? = refMapping.searge.getClass(mojangRefClass?.original)
        val seargeField: IMappingFile.IField? =
            seargeClass?.getField(mojangRefClass?.let { it.getMappedField(field)?.original })
        val seargeVClass: IMappingFile.IClass? = seargeClass?.let { mapping.searge.getMappedClass(it.mapped) }
        return seargeVClass?.let { seargeField?.let { it1 -> it.getMappedField(it1.mapped) } }
    }

    fun remapField(cls: String, field: String): IMappingFile.IField? {
        return remapField(cls, field, refMapping, true)
    }

    private fun remapField(
        cls: String,
        field: String,
        refMappingLocal: MappingSet,
        firstRemapping: Boolean
    ): IMappingFile.IField? {
        if (mapping == null) {
            return refMappingLocal.mojang.getClass(cls).getField(field)
        }
        if (mapping.intermediary != null) {
            val intermClass: IMappingFile.IClass = mapping.intermediary.getClass(cls)
            val intermField: IMappingFile.IField? = intermClass.getField(field)
            val intermVClass: IMappingFile.IClass? = refMappingLocal.intermediary.getMappedClass(intermClass.mapped)
            val intermVField: IMappingFile.IField? =
                intermVClass?.let { intermField?.let { it1 -> it.getMappedField(it1.mapped) } }
            return refMappingLocal.mojang.getClass(intermVClass?.original)?.getField(intermVField?.original)
                ?: if (refMapping2 != null && firstRemapping) remapField(cls, field, refMapping2, false) else null
        }
        val seargeClass: IMappingFile.IClass = mapping.searge.getClass(cls)
        val seargeField: IMappingFile.IField? = seargeClass.getField(field)
        // hardcoded values
        when (seargeField?.mapped) {
            "field_196649_cc" -> return newField(seargeField.mapped, "SIGN")
            "field_150444_as" -> return newField(seargeField.mapped, "WALL_SIGN")
        }
        var seargeVClass: IMappingFile.IClass? = refMappingLocal.searge.getMappedClass(seargeClass.mapped)
        // 1.14> searge entity class name inconsistency fix
        if (seargeVClass == null && isSeargeEntityClassOld(seargeClass.mapped)) {
            seargeVClass = refMappingLocal.searge.getMappedClassPkgInsensitive(
                seargeClass.mapped.replaceFirst("Entity", "") + "Entity"
            )
        }
        // 1.14> searge packet class name inconsistency fix
        if (seargeVClass == null && (isSeargeServerPacketClassOld(seargeClass.mapped) || isSeargeClientPacketClassOld(seargeClass.mapped))) {
            val packetTypeLetter: Char = if (isSeargeServerPacketClassOld(seargeClass.mapped)) 'S' else 'C'
            val originalClass: String = seargeClass.mapped.substring(seargeClass.mapped.lastIndexOf('/') + 1)
                .let { if (it.contains('$')) it.substring(0, it.lastIndexOf('$')) else it }
            val fixedClass: String = packetTypeLetter + originalClass.replaceFirst("${packetTypeLetter}Packet", "") + "Packet"
            seargeVClass = refMappingLocal.searge.getMappedClassPkgInsensitive(
                fixedClass + (if (seargeClass.mapped.contains('$')) "$" + seargeClass.mapped.substring(seargeClass.mapped.lastIndexOf('$') + 1) else "")
            )
        }
        val seargeVField: IMappingFile.IField? =
            seargeVClass?.let { seargeField?.let { it1 -> it.getMappedField(it1.mapped) } }
        return refMappingLocal.mojang.getClass(seargeVClass?.original)?.getField(seargeVField?.original)
            ?: if (refMapping2 != null && firstRemapping) remapField(cls, field, refMapping2, false) else null
    }

    private fun isSeargeEntityClassOld(mapped: String): Boolean {
        return mapped.startsWith("net/minecraft/entity") && mapped.substring(mapped.lastIndexOf('/') + 1)
            .startsWith("Entity")
    }

    private fun isSeargeEntityClassNew(mapped: String): Boolean {
        return mapped.startsWith("net/minecraft/entity") && mapped.substring(mapped.lastIndexOf('/') + 1)
            .endsWith("Entity")
    }

    private fun isSeargeServerPacketClassOld(mapped: String): Boolean {
        return mapped.startsWith("net/minecraft/network") && mapped.substring(mapped.lastIndexOf('/') + 1)
            .startsWith("SPacket")
    }

    private fun isSeargeClientPacketClassOld(mapped: String): Boolean {
        return mapped.startsWith("net/minecraft/network") && mapped.substring(mapped.lastIndexOf('/') + 1)
            .startsWith("CPacket")
    }

    private fun newField(original: String, mapped: String): IMappingFile.IField {
        return Class.forName("net.minecraftforge.srgutils.MappingFile\$Cls\$Field")
            .getDeclaredConstructor(
                Class.forName("net.minecraftforge.srgutils.MappingFile\$Cls"),
                String::class.java, String::class.java, String::class.java, Map::class.java
            )
            .also { it.trySetAccessible() }
            .newInstance(null, original, mapped, null, mapOf<String, String>()) as IMappingFile.IField
    }
}