package me.kcra.datagenerator.gen.bytecode

import me.kcra.datagenerator.mapping.ClassRemapper
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Opcodes
import kotlin.RuntimeException

class EntityDataSerializersVisitor(private val className: String, private val classRemapper: ClassRemapper) : ClassVisitor(Opcodes.ASM9) {
    val serializers: MutableList<String> = mutableListOf()

    override fun visitField(
        access: Int,
        name: String,
        descriptor: String,
        signature: String,
        value: Any?
    ): FieldVisitor? {
        if (classRemapper.remapClass(descriptor.substring(1, descriptor.length - 1))?.mapped.equals("net/minecraft/network/syncher/EntityDataSerializer")) {
            serializers.add(classRemapper.remapField(className, name)?.mapped
                ?: throw RuntimeException("Could not remap field $name"))
        }
        return null
    }
}