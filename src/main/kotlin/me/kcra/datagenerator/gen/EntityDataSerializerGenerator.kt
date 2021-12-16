package me.kcra.datagenerator.gen

import com.fasterxml.jackson.databind.ObjectMapper
import me.kcra.datagenerator.gen.bytecode.EntityDataSerializersVisitor
import me.kcra.datagenerator.gen.model.EntityDataSerializer
import me.kcra.datagenerator.mapping.ClassRemapper
import me.kcra.datagenerator.utils.MinecraftJarReader
import net.minecraftforge.srgutils.IMappingFile

class EntityDataSerializerGenerator(
    jsonMapper: ObjectMapper,
    classRemapper: ClassRemapper,
    jarReader: MinecraftJarReader
) : AbstractGenerator<Array<EntityDataSerializer>>(jsonMapper, classRemapper, jarReader) {
    override fun generate(): Array<EntityDataSerializer> {
        val className: IMappingFile.IClass = classRemapper.getClass("net/minecraft/network/syncher/EntityDataSerializers")
            ?: throw RuntimeException("Could not remap class net/minecraft/network/syncher/EntityDataSerializers")
        val visitor = EntityDataSerializersVisitor(className.original, classRemapper)
        jarReader.readClass(className.original).accept(visitor, 0)

        return visitor.serializers.stream()
            .map { EntityDataSerializer(visitor.serializers.indexOf(it), it) }
            .toArray { size -> arrayOfNulls<EntityDataSerializer>(size) }
    }
}