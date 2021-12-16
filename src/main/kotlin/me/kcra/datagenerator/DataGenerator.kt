package me.kcra.datagenerator

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import me.kcra.datagenerator.gen.EntityDataSerializerGenerator
import me.kcra.datagenerator.mapping.ClassRemapper
import me.kcra.datagenerator.mapping.MappingSet
import me.kcra.datagenerator.mapping.SecondaryMappingSet
import me.kcra.datagenerator.utils.MinecraftJarReader
import me.kcra.datagenerator.utils.intermediaryMapping
import me.kcra.datagenerator.utils.minecraftResource
import me.kcra.datagenerator.utils.seargeMapping
import net.minecraftforge.srgutils.IMappingFile
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import java.io.File

fun main(args: Array<String>) {
    val opts: Options = Options()
        .addRequiredOption("v", "version", true, "Version for extraction")
    val cmd: CommandLine
    try {
        cmd = DefaultParser().parse(opts, args)
    } catch (ignored: Exception) {
        HelpFormatter().printHelp("DataGenerator", opts)
        return
    }
    val version: String = cmd.getOptionValue("v")

    val mapper: ObjectMapper = jacksonObjectMapper()
    println("Retrieving Minecraft server JAR...")
    val minecraftJar: File? = minecraftResource(mapper, version, "server")
    if (minecraftJar == null) {
        println("Could not retrieve Minecraft JAR for version $version!")
        return
    }
    val mojMap: File? = minecraftResource(mapper, version, "server_mappings")
    val refMapping: MappingSet
    val mapping: SecondaryMappingSet?
    if (mojMap != null) {
        println("Mojang mappings available, using them for remapping.")
        refMapping = MappingSet(
            IMappingFile.load(mojMap).reverse(),
            IMappingFile.load(seargeMapping(version)),
            IMappingFile.load(intermediaryMapping(version))
        )
        mapping = null
    } else {
        println("Mojang mappings are not available for the selected version, tracing history using Searge and Intermediary.")
        refMapping = MappingSet(
            IMappingFile.load(minecraftResource(mapper, "1.16.5", "server_mappings")).reverse(),
            IMappingFile.load(seargeMapping("1.16.5")),
            IMappingFile.load(intermediaryMapping("1.16.5"))
        )
        val intermediary: File? = intermediaryMapping(version)
        mapping = SecondaryMappingSet(
            IMappingFile.load(seargeMapping(version)),
            if (intermediary != null) IMappingFile.load(intermediary) else null
        )
    }
    val classRemapper = ClassRemapper(refMapping, mapping)
    val minecraftJarReader = MinecraftJarReader(minecraftJar, version)
    println(EntityDataSerializerGenerator(mapper, classRemapper, minecraftJarReader).generateJson())
}