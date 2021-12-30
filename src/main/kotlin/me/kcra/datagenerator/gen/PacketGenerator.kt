package me.kcra.datagenerator.gen

import com.fasterxml.jackson.databind.ObjectMapper
import me.kcra.datagenerator.gen.bytecode.PacketVisitor
import me.kcra.datagenerator.gen.bytecode.ClassSignatureVisitor
import me.kcra.datagenerator.gen.model.Packet
import me.kcra.datagenerator.mapping.ClassRemapper
import me.kcra.datagenerator.utils.MinecraftJarReader
import org.objectweb.asm.ClassReader
import java.io.InputStream

class PacketGenerator(
    jsonMapper: ObjectMapper,
    classRemapper: ClassRemapper,
    jarReader: MinecraftJarReader
) : AbstractGenerator<Array<Packet>>(jsonMapper, classRemapper, jarReader) {
    override fun generate(): Array<Packet> {
        val packets: MutableList<Packet> = mutableListOf()
        val packetClass: String = classRemapper.getClass("net/minecraft/network/protocol/Packet")?.original
            ?: throw RuntimeException("Could not remap class net/minecraft/network/protocol/Packet")
        for (entry: Map.Entry<String, InputStream> in jarReader.minecraftClasses().entries) {
            val classReader = ClassReader(entry.value)
            if (!ClassSignatureVisitor().also { classReader.accept(it, 0) }.interfaces.contains(packetClass)) {
                continue
            }
            val visitor: PacketVisitor = PacketVisitor(classRemapper).also { classReader.accept(it, 0) }
            packets.add(
                Packet(
                    classRemapper.remapClass(visitor.className!!)?.mapped
                        ?: "obfuscated:${visitor.className}",
                    visitor.fields
                )
            )
        }
        return packets.toTypedArray()
    }
}