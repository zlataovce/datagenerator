package me.kcra.datagenerator.gen

import com.fasterxml.jackson.databind.ObjectMapper
import me.kcra.datagenerator.gen.model.EntityMetadata
import me.kcra.datagenerator.gen.model.EntityType
import me.kcra.datagenerator.mapping.ClassRemapper
import me.kcra.datagenerator.utils.MinecraftJarReader
import java.lang.reflect.ParameterizedType
import java.util.*

class FlatteningEntityTypeGenerator(
    jsonMapper: ObjectMapper,
    classRemapper: ClassRemapper,
    jarReader: MinecraftJarReader
) : AbstractGenerator<Array<EntityType>>(jsonMapper, classRemapper, jarReader) {
    private val packetTypes: Map<Class<*>, String>
    private val entityClasses: Map<Any, Class<*>>
    private val entityDataSerializers: Map<Any, String>

    init {
        packetTypes = mapOf(
            Pair(
                Class.forName(
                    classRemapper.getClass("net/minecraft/world/entity/player/Player")?.original
                        ?: throw RuntimeException("Could not remap class net/minecraft/world/entity/player/Player"),
                    true,
                    jarReader.classLoader
                ),
                "PLAYER"
            ),
            Pair(
                Class.forName(
                    classRemapper.getClass("net/minecraft/world/entity/LivingEntity")?.original
                        ?: throw RuntimeException("Could not remap class net/minecraft/world/entity/LivingEntity"),
                    true,
                    jarReader.classLoader
                ),
                "LIVING"
            ),
            Pair(
                Class.forName(
                    classRemapper.getClass("net/minecraft/world/entity/decoration/Painting")?.original
                        ?: throw RuntimeException("Could not remap class net/minecraft/world/entity/decoration/Painting"),
                    true,
                    jarReader.classLoader
                ),
                "PAINTING"
            ),
            Pair(
                Class.forName(
                    classRemapper.getClass("net/minecraft/world/entity/ExperienceOrb")?.original
                        ?: throw RuntimeException("Could not remap class net/minecraft/world/entity/ExperienceOrb"),
                    true,
                    jarReader.classLoader
                ),
                "EXPERIENCE_ORB"
            )
        )
        // EntityType.class
        val entityTypeClass: Class<*> = Class.forName(
            classRemapper.getClass("net/minecraft/world/entity/EntityType")?.original
                ?: throw RuntimeException("Could not remap class net/minecraft/world/entity/EntityType"),
            true,
            jarReader.classLoader
        )
        entityClasses = Arrays.stream(entityTypeClass.declaredFields)
            .map { field ->
                if (!entityTypeClass.isAssignableFrom(field.type)) {
                    return@map null
                }
                return@map Pair(field.get(null), (field.genericType as ParameterizedType).actualTypeArguments[0] as Class<*>)
            }
            .toList()
            .filterNotNull()
            .associateBy({ it.first }, { it.second })
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
        entityDataSerializers = Arrays.stream(eds1Class.declaredFields)
            .map { field ->
                if (!eds2Class.isAssignableFrom(field.type)) {
                    return@map null
                }
                return@map Pair(
                    field.get(null),
                    classRemapper.remapField(eds1Class.name, field.name)?.mapped
                        ?: throw RuntimeException("Could not remap field " + field.name + " of class net/minecraft/network/syncher/EntityDataSerializers")
                )
            }
            .toList()
            .filterNotNull()
            .associateBy({ it.first }, { it.second })
    }

    override fun generate(): Array<EntityType> {
        // Registry.class
        val registryClass: Class<*> = Class.forName(
            classRemapper.getClass("net/minecraft/core/Registry")?.original
                ?: throw RuntimeException("Could not remap class net/minecraft/core/Registry"),
            true,
            jarReader.classLoader
        )
        // Registry.ENTITY_TYPE (Registry)
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
                    findPacketType(entityClass),
                    // entityType.fireImmune() (boolean)
                    invokeEntityTypeMethodNullable(entityType, "fireImmune") as Boolean?,
                    // entityType.getHeight() (float)
                    invokeEntityTypeMethodNullable(entityType, "getHeight") as Float?,
                    // entityType.getWidth() (float)
                    invokeEntityTypeMethodNullable(entityType, "getWidth") as Float?,
                    // entityType.clientTrackingRange() (int)
                    invokeEntityTypeMethodNullable(entityType, "clientTrackingRange") as Int?,
                    // entityType.getDefaultLootTable() (ResourceLocation)
                    invokeEntityTypeMethodNullable(entityType, "getDefaultLootTable").toString(),
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

                            return@map EntityMetadata(
                                classRemapper.remapField(entityClass.name, field.name)?.mapped.also {
                                    if (it == null) {
                                        println("Could not remap serializer field " + field.name + " of class " + entityClass.name)
                                    }
                                },
                                eda.javaClass.getMethod(
                                    classRemapper.getMethod("net/minecraft/network/syncher/EntityDataAccessor", "getId")?.original
                                        ?: throw RuntimeException("Could not remap method getId of class net/minecraft/network/syncher/EntityDataAccessor")
                                ).invoke(eda) as Int,
                                entityDataSerializers[serializer] ?: throw RuntimeException("Could not find EntityDataSerializer")
                            )
                        }
                        .toList()
                        .filterNotNull()
                )
            )
        }
        return entityTypes.toTypedArray()
    }

    private fun findPacketType(entityClass: Class<*>): String {
        return packetTypes.entries.stream()
            .filter { it.key.isAssignableFrom(entityClass) }
            .findFirst()
            .map { it.value }
            .orElse("BASE")
    }

    private fun invokeEntityTypeMethodNullable(entityType: Any, methodName: String): Any? {
        return classRemapper.getMethod("net/minecraft/world/entity/EntityType", methodName)?.original.let {
            if (it != null) entityType.javaClass.getMethod(it).invoke(entityType) else null
        }
    }
}