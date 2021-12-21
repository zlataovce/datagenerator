package me.kcra.datagenerator.gen

import com.fasterxml.jackson.databind.ObjectMapper
import me.kcra.datagenerator.gen.model.BlockType
import me.kcra.datagenerator.mapping.ClassRemapper
import me.kcra.datagenerator.utils.MinecraftJarReader

class FlatteningBlockTypeGenerator(
    jsonMapper: ObjectMapper,
    classRemapper: ClassRemapper,
    jarReader: MinecraftJarReader
) : AbstractGenerator<Array<BlockType>>(jsonMapper, classRemapper, jarReader) {
    override fun generate(): Array<BlockType> {
        // Registry.class
        val registryClass: Class<*> = Class.forName(
            classRemapper.getClass("net/minecraft/core/Registry")?.original
                ?: throw RuntimeException("Could not remap class net/minecraft/core/Registry"),
            true,
            jarReader.classLoader
        )
        // Registry.BLOCK (Registry)
        val blockRegistry: Any = registryClass.getDeclaredField(
            classRemapper.getField("net/minecraft/core/Registry", "BLOCK")?.original
                ?: throw RuntimeException("Could not remap field BLOCK of class net/minecraft/core/Registry")
        ).get(null)

        // Registry.BLOCK.keySet() (Set<ResourceLocation>)
        @Suppress("UNCHECKED_CAST")
        val resourceSet: Set<Any> = blockRegistry.javaClass.getMethod(
            classRemapper.getMethod("net/minecraft/core/Registry", "keySet", "()Ljava/util/Set;")?.original
                ?: throw RuntimeException("Could not remap method keySet of class net/minecraft/core/Registry")
        ).invoke(blockRegistry) as Set<Any>

        // Block.class
        val blockClass: Class<*> = Class.forName(
            classRemapper.getClass("net/minecraft/world/level/block/Block")?.original
                ?: throw RuntimeException("Could not remap class net/minecraft/world/level/block/Block"),
            true,
            jarReader.classLoader
        )

        val blockTypes: MutableList<BlockType> = mutableListOf()
        for (blockResourceLocation: Any in resourceSet) {
            // Registry.BLOCK.get(blockResourceLocation) (Block)
            val block: Any = blockRegistry.javaClass.getMethod(
                classRemapper.getMethod("net/minecraft/core/Registry", "get", "(Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/Object;")?.original
                    ?: throw RuntimeException("Could not remap method get of class net/minecraft/core/Registry"),
                Class.forName(classRemapper.getClass("net/minecraft/resources/ResourceLocation")?.original, true, jarReader.classLoader)
            ).invoke(blockRegistry, blockResourceLocation)
            TODO("implement this")
        }
        return blockTypes.toTypedArray()
    }
}