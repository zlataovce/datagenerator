package me.kcra.datagenerator.gen.bytecode

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes

class ClassSignatureVisitor : ClassVisitor(Opcodes.ASM9) {
    var superClass: String? = null
    val interfaces: MutableList<String> = mutableListOf()

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        superClass = superName
        if (interfaces != null) {
            this.interfaces.addAll(interfaces)
        }
    }
}