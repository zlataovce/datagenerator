package me.kcra.datagenerator.gen.bytecode

import me.kcra.datagenerator.gen.model.PacketField
import me.kcra.datagenerator.mapping.ClassRemapper
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Opcodes
import java.lang.reflect.Modifier

class PacketVisitor(private val classRemapper: ClassRemapper) : ClassVisitor(Opcodes.ASM9) {
    var className: String? = null
    val fields: MutableList<PacketField> = mutableListOf()

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        className = name
    }

    override fun visitField(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        value: Any?
    ): FieldVisitor? {
        if (!Modifier.isStatic(access)) {
            fields.add(
                PacketField(
                    classRemapper.remapField(className!!, name)?.mapped
                        ?: "obfuscated:$className:$name",
                    convertFieldDescriptor(descriptor)
                )
            )
        }
        return null
    }

    private fun convertFieldDescriptor(descriptor: String): String {
        if (descriptor.startsWith('L') && descriptor.endsWith(';')) {
            val clazz: String = descriptor.substring(1, descriptor.length - 1)
            if (!clazz.contains('/')) {
                return classRemapper.remapClass(clazz)?.mapped
                    ?: "obfuscated:$clazz"
            }
            return clazz
        }
        return convertBytecodeType(descriptor)
    }

    private fun convertBytecodeType(type: String): String {
        return when (type) {
            "B" -> "byte"
            "C" -> "char"
            "D" -> "double"
            "F" -> "float"
            "I" -> "int"
            "J" -> "long"
            "S" -> "short"
            "Z" -> "boolean"
            "V" -> "void"
            else -> {
                return if (type.startsWith("[")) {
                    convertBytecodeType(type.substring(1)) + "[]"
                } else if (type.endsWith(";")) {
                    type.substring(1, type.length - 1).replace("/", ".")
                } else {
                    type.substring(1).replace("/", ".")
                }
            }
        }
    }
}