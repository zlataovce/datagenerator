package me.kcra.datagenerator.gen

import com.fasterxml.jackson.databind.ObjectMapper
import me.kcra.datagenerator.gen.model.EntityMetadata
import me.kcra.datagenerator.gen.model.EntityType
import me.kcra.datagenerator.mapping.ClassRemapper
import me.kcra.datagenerator.utils.MinecraftJarReader
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
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
                classRemapper.getMethod("net/minecraft/core/Registry", "keySet", "()Ljava/util/Set;")?.original
                ?: throw RuntimeException("Could not remap method keySet of class net/minecraft/core/Registry")
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
        // EntityDataAccessor.class
        val entityDataAccessorClass: Class<*> = Class.forName(
            classRemapper.getClass("net/minecraft/network/syncher/EntityDataAccessor")?.original
                ?: throw RuntimeException("Could not remap class net/minecraft/network/syncher/EntityDataAccessor"),
            true,
            jarReader.classLoader
        )
        val entityTypes: MutableList<EntityType> = mutableListOf()
        val entityDataSerializers: Map<Any, String> = entityDataSerializers()
        val entityClasses: Map<Any, Class<*>> = entityClasses()
        for (entityResourceLocation: Any in resourceSet) {
            // Registry.ENTITY_TYPE.get(entityResourceLocation) (EntityType<?>)
            val entityType: Any = entityTypeRegistry.javaClass.getMethod(
                classRemapper.getMethod("net/minecraft/core/Registry", "get", "(Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/Object;")?.original
                    ?: throw RuntimeException("Could not remap method get of class net/minecraft/core/Registry"),
                Class.forName(classRemapper.getClass("net/minecraft/resources/ResourceLocation")?.original, true, jarReader.classLoader)
            ).invoke(entityTypeRegistry, entityResourceLocation)
            val entityClass: Class<*> = entityClasses[entityType] ?: throw RuntimeException("Missing entityType $entityType")
            entityTypes.add(
                EntityType(
                    // Registry.ENTITY_TYPE.getId(entityType) (int)
                    entityTypeRegistry.javaClass.getMethod(
                        classRemapper.getMethod("net/minecraft/core/Registry", "getId", "(Ljava/lang/Object;)I")?.original
                            ?: throw RuntimeException("Could not remap method getId of class net/minecraft/core/Registry"),
                        Object::class.java
                    ).invoke(entityTypeRegistry, entityType) as Int,
                    entityTypeInstances[entityType] ?: throw RuntimeException("Missing entityType $entityType"),
                    entityResourceLocation.toString(),
                    // entityType.fireImmune() (boolean)
                    classRemapper.getMethod("net/minecraft/world/entity/EntityType", "fireImmune")?.original.let {
                        if (it != null) entityType.javaClass.getMethod(it).invoke(entityType) as Boolean else null
                    },
                    // entityType.getHeight() (float)
                    classRemapper.getMethod("net/minecraft/world/entity/EntityType", "getHeight")?.original.let {
                        if (it != null) entityType.javaClass.getMethod(it).invoke(entityType) as Float else null
                    },
                    // entityType.getWidth() (float)
                    classRemapper.getMethod("net/minecraft/world/entity/EntityType", "getWidth")?.original.let {
                        if (it != null) entityType.javaClass.getMethod(it).invoke(entityType) as Float else null
                    },
                    // entityType.clientTrackingRange() (int)
                    classRemapper.getMethod("net/minecraft/world/entity/EntityType", "clientTrackingRange")?.original.let {
                        if (it != null) entityType.javaClass.getMethod(it).invoke(entityType) as Int else null
                    },
                    // entityType.getDefaultLootTable() (ResourceLocation)
                    classRemapper.getMethod("net/minecraft/world/entity/EntityType", "getDefaultLootTable")?.original.let {
                        if (it != null) entityType.javaClass.getMethod(it).invoke(entityType).toString() else null
                    },
                    Arrays.stream(entityClass.declaredFields)
                        .map { field ->
                            if (!entityDataAccessorClass.isAssignableFrom(field.type)) {
                                return@map null
                            }
                            field.trySetAccessible()
                            val eda: Any
                            try {
                                eda = field.get(null)
                            } catch (e: Throwable) {
                                println("Skipping metadata field " + field.name + " due to EntityDataAccessor initialization error.")
                                e.printStackTrace()
                                return@map null
                            }
                            val serializer: Any = eda.javaClass.getMethod(
                                classRemapper.getMethod("net/minecraft/network/syncher/EntityDataAccessor", "getSerializer")?.original
                                    ?: throw RuntimeException("Could not remap method getSerializer of class net/minecraft/network/syncher/EntityDataAccessor")
                            ).invoke(eda)

                            try {
                                return@map EntityMetadata(
                                    classRemapper.remapField(entityClass.name, field.name)?.mapped
                                        ?: throw RuntimeException("Could not remap field " + field.name + " of class " + entityClass.name),
                                    eda.javaClass.getMethod(
                                        classRemapper.getMethod("net/minecraft/network/syncher/EntityDataAccessor", "getId")?.original
                                            ?: throw RuntimeException("Could not remap method getId of class net/minecraft/network/syncher/EntityDataAccessor")
                                    ).invoke(eda) as Int,
                                    entityDataSerializers[serializer] ?: throw RuntimeException("Could not find EntityDataSerializer")
                                )
                            } catch (ignored: Exception) {
                                ignored.printStackTrace()
                                // ignored
                            }
                            return@map null
                        }
                        .toList()
                        .filterNotNull()
                )
            )
        }
        return entityTypes.toTypedArray()
    }

    private fun entityDataSerializers(): Map<Any, String> {
        val names: MutableMap<Any, String> = mutableMapOf()
        // EntityDataSerializers.class
        val eds1Class: Class<*> = Class.forName(
            classRemapper.getClass("net/minecraft/network/syncher/EntityDataSerializers")?.original
                ?: throw RuntimeException("Could not remap class net/minecraft/network/syncher/EntityDataSerializers"),
            true,
            jarReader.classLoader
        )
        // EntityDataSerializer.class
        val eds2Class: Class<*> = Class.forName(
            classRemapper.getClass("net/minecraft/network/syncher/EntityDataSerializer")?.original
                ?: throw RuntimeException("Could not remap class net/minecraft/network/syncher/EntityDataSerializer"),
            true,
            jarReader.classLoader
        )
        for (field: Field in eds1Class.declaredFields) {
            if (!eds2Class.isAssignableFrom(field.type)) {
                continue
            }
            names[field.get(null)] = classRemapper.remapField(eds1Class.name, field.name)?.mapped
                ?: throw RuntimeException("Could not remap field " + field.name + " of class net/minecraft/network/syncher/EntityDataSerializers")
        }
        return names
    }

    private fun entityClasses(): Map<Any, Class<*>> {
        val classes: MutableMap<Any, Class<*>> = mutableMapOf()
        // EntityType.class
        val entityTypeClass: Class<*> = Class.forName(
            classRemapper.getClass("net/minecraft/world/entity/EntityType")?.original
                ?: throw RuntimeException("Could not remap class net/minecraft/world/entity/EntityType"),
            true,
            jarReader.classLoader
        )
        for (field: Field in entityTypeClass.declaredFields) {
            if (!entityTypeClass.isAssignableFrom(field.type)) {
                continue
            }
            classes[field.get(null)] = (field.genericType as ParameterizedType).actualTypeArguments[0] as Class<*>
        }
        return classes
    }
}