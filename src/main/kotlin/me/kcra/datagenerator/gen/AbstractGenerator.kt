package me.kcra.datagenerator.gen

import com.fasterxml.jackson.databind.ObjectMapper
import me.kcra.datagenerator.mapping.ClassRemapper
import me.kcra.datagenerator.utils.MinecraftJarReader

abstract class AbstractGenerator<T>(
    val jsonMapper: ObjectMapper,
    val classRemapper: ClassRemapper,
    val jarReader: MinecraftJarReader
) {
    abstract fun generate(): T

    open fun generateJson(): String {
        return jsonMapper.writeValueAsString(generate())
    }
}