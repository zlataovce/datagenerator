package me.kcra.datagenerator.gen

import com.fasterxml.jackson.databind.ObjectMapper
import me.kcra.datagenerator.gen.model.EntityType
import me.kcra.datagenerator.mapping.ClassRemapper
import me.kcra.datagenerator.utils.MinecraftJarReader
import java.util.*

class EntityTypeGenerator(
    jsonMapper: ObjectMapper,
    classRemapper: ClassRemapper,
    jarReader: MinecraftJarReader
) : AbstractGenerator<Array<EntityType>>(jsonMapper, classRemapper, jarReader) {
    override fun generate(): Array<EntityType> {
        Thread.currentThread().contextClassLoader = jarReader.classLoader
        // Registry.class
        val registryClass: Class<*> = Class.forName(
            classRemapper.getClass("net/minecraft/core/Registry")?.original
                ?: throw RuntimeException("Could not remap class net/minecraft/core/Registry"),
            true,
            jarReader.classLoader
        )
        // Registry.ENTITY_TYPE (DefaultedRegistry)
        val entityTypeRegistry: Any = registryClass.getDeclaredField(
            classRemapper.getField("net/minecraft/core/Registry", "ENTITY_TYPE")?.original
                ?: throw RuntimeException("Could not remap field ENTITY_TYPE of class net/minecraft/core/Registry")
        ).get(null)

        // Registry.ENTITY_TYPE.keySet() (Set<ResourceLocation>)
        @Suppress("UNCHECKED_CAST")
        val resourceSet: Set<Any> = entityTypeRegistry.javaClass.getMethod(
            classRemapper.getMethod("net/minecraft/core/MappedRegistry", "keySet")?.original
                ?: throw RuntimeException("Could not remap method keySet of class net/minecraft/core/MappedRegistry")
        ).invoke(entityTypeRegistry) as Set<Any>

        // EntityType.class
        val entityTypeClass: Class<*> = Class.forName(
            classRemapper.getClass("net/minecraft/world/entity/EntityType")?.original
                ?: throw RuntimeException("Could not remap class net/minecraft/world/entity/EntityType"),
            true,
            jarReader.classLoader
        )
        // EntityType fields from the EntityType class
        val entityTypeInstances: Map<Any, String> = Arrays.stream(entityTypeClass.declaredFields)
            .filter { entityTypeClass.isAssignableFrom(it.type) }
            .toList()
            .associateBy({ it.get(null) }, {
                classRemapper.remapField(entityTypeClass.name, it.name)?.mapped
                    ?: throw RuntimeException("Could not remap field " + it.name + " of class net/minecraft/world/entity/EntityType")
            })
        val entityTypes: MutableList<EntityType> = mutableListOf()
        for (entityResourceLocation: Any in resourceSet) {
            // Registry.ENTITY_TYPE.get(entityResourceLocation) (EntityType<?>)
            val entityType: Any = entityTypeRegistry.javaClass.getMethod(
                classRemapper.getMethod("net/minecraft/core/DefaultedRegistry", "get", "(Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/Object;")?.original
                    ?: throw RuntimeException("Could not remap method get of class net/minecraft/core/DefaultedRegistry"),
                Class.forName(classRemapper.getClass("net/minecraft/resources/ResourceLocation")?.original, true, jarReader.classLoader)
            ).invoke(entityTypeRegistry, entityResourceLocation)
            entityTypes.add(
                EntityType(
                    // Registry.ENTITY_TYPE.getId(entityType) (int)
                    entityTypeRegistry.javaClass.getMethod(
                        classRemapper.getMethod("net/minecraft/core/DefaultedRegistry", "getId", "(Ljava/lang/Object;)I")?.original
                            ?: throw RuntimeException("Could not remap method getId of class net/minecraft/core/DefaultedRegistry"),
                        Object::class.java
                    ).invoke(entityTypeRegistry, entityType) as Int,
                    entityTypeInstances[entityType] ?: throw RuntimeException("Missing entityType $entityType"),
                    entityResourceLocation.toString(),
                    entityType.javaClass.getMethod(
                        classRemapper.getMethod("net/minecraft/world/entity/EntityType", "fireImmune")?.original
                            ?: throw RuntimeException("Could not remap method fireImmune of class net/minecraft/world/entity/EntityType")
                    ).invoke(entityType) as Boolean,
                    entityType.javaClass.getMethod(
                        classRemapper.getMethod("net/minecraft/world/entity/EntityType", "getHeight")?.original
                            ?: throw RuntimeException("Could not remap method getHeight of class net/minecraft/world/entity/EntityType")
                    ).invoke(entityType) as Float,
                    entityType.javaClass.getMethod(
                        classRemapper.getMethod("net/minecraft/world/entity/EntityType", "getWidth")?.original
                            ?: throw RuntimeException("Could not remap method getWidth of class net/minecraft/world/entity/EntityType")
                    ).invoke(entityType) as Float,
                    entityType.javaClass.getMethod(
                        classRemapper.getMethod("net/minecraft/world/entity/EntityType", "clientTrackingRange")?.original
                            ?: throw RuntimeException("Could not remap method clientTrackingRange of class net/minecraft/world/entity/EntityType")
                    ).invoke(entityType) as Int,
                    entityType.javaClass.getMethod(
                        classRemapper.getMethod("net/minecraft/world/entity/EntityType", "getDefaultLootTable")?.original
                            ?: throw RuntimeException("Could not remap method getDefaultLootTable of class net/minecraft/world/entity/EntityType")
                    ).invoke(entityType).toString()
                )
            )
        }
        return entityTypes.toTypedArray()
    }
}