package me.kcra.datagenerator

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import me.kcra.datagenerator.gen.AbstractGenerator
import me.kcra.datagenerator.gen.EntityDataSerializerGenerator
import me.kcra.datagenerator.gen.FlatteningEntityTypeGenerator
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
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream
import java.lang.reflect.Field
import java.nio.file.Path

fun main(args: Array<String>) {
    val tmpPath: Path = Path.of(System.getProperty("user.dir"), "work").toAbsolutePath()
    tmpPath.toFile().mkdirs()
    System.setProperty("java.io.tmpdir", tmpPath.toString())
    val opts: Options = Options()
        .addRequiredOption("v", "version", true, "Version for extraction")
        .addOption("r", "ref", true, "Reference mapping version (needs Mojang mappings)")
        .addOption("r2", "ref2", true, "Secondary reference mapping version (needs Mojang mappings)")
    val cmd: CommandLine
    try {
        cmd = DefaultParser().parse(opts, args)
    } catch (ignored: Exception) {
        HelpFormatter().printHelp("DataGenerator", opts)
        return
    }
    val version: String = cmd.getOptionValue("v")
    val refVersion: String = cmd.getOptionValue("r", "1.16.5")
    val refVersion2: String = cmd.getOptionValue("r2", "1.14.4")

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
            IMappingFile.load(minecraftResource(mapper, refVersion, "server_mappings")).reverse(),
            IMappingFile.load(seargeMapping(refVersion)),
            IMappingFile.load(intermediaryMapping(refVersion))
        )
        val intermediary: File? = intermediaryMapping(version)
        mapping = SecondaryMappingSet(
            IMappingFile.load(seargeMapping(version)),
            if (intermediary != null) IMappingFile.load(intermediary) else null
        )
    }
    val refMapping2 = MappingSet(
        IMappingFile.load(minecraftResource(mapper, refVersion2, "server_mappings")).reverse(),
        IMappingFile.load(seargeMapping(refVersion2)),
        IMappingFile.load(intermediaryMapping(refVersion2))
    )
    val classRemapper = ClassRemapper(refMapping, if (mapping == null) null else refMapping2, mapping, refVersion, version)
    val minecraftJarReader = MinecraftJarReader(minecraftJar, version)

    println("Preparing Minecraft internals...")
    try {
        // SharedConstants.CURRENT_VERSION
        val currentVersionField: Field = Class.forName(
            classRemapper.getClass("net/minecraft/SharedConstants")?.original
                ?: throw RuntimeException("Could not remap class net/minecraft/SharedConstants"),
            true,
            minecraftJarReader.classLoader
        ).getDeclaredField(
            classRemapper.getField("net/minecraft/SharedConstants", "CURRENT_VERSION")?.original
                ?: throw RuntimeException("Could not remap field CURRENT_VERSION of class net/minecraft/SharedConstants")
        )
        currentVersionField.trySetAccessible()
        // SharedConstants.CURRENT_VERSION = DetectedVersion.BUILT_IN
        currentVersionField.set(
            null,
            Class.forName(
                classRemapper.getClass("net/minecraft/DetectedVersion")?.original
                    ?: throw RuntimeException("Could not remap class net/minecraft/DetectedVersion"),
                true,
                minecraftJarReader.classLoader
            ).getDeclaredField(
                classRemapper.getField("net/minecraft/DetectedVersion", "BUILT_IN")?.original
                    ?: throw RuntimeException("Could not remap field BUILT_IN of class net/minecraft/DetectedVersion")
            ).get(null)
        )
    } catch (ignored: Exception) {
        // ignored
    }
    // Bootstrap.class
    val bootstrapClass: Class<*> = Class.forName(
        classRemapper.getClass("net/minecraft/server/Bootstrap")?.original
            ?: throw RuntimeException("Could not remap class net/minecraft/server/Bootstrap"),
        true,
        minecraftJarReader.classLoader
    )
    // Bootstrap.bootStrap()
    bootstrapClass.getDeclaredMethod(
        classRemapper.getMethod("net/minecraft/server/Bootstrap", "bootStrap")?.original
            ?: throw RuntimeException("Could not remap method bootStrap of class net/minecraft/server/Bootstrap")
    ).invoke(null)
    // making minecraft's slf4j stfu
    System.setOut(PrintStream(FileOutputStream(FileDescriptor.out)))
    System.setErr(PrintStream(FileOutputStream(FileDescriptor.err)))
    // Bootstrap.isBootstrapped
    val isBootstrappedField: Field = bootstrapClass.getDeclaredField(
        classRemapper.getField("net/minecraft/server/Bootstrap", "isBootstrapped")?.original
            ?: throw RuntimeException("Could not remap field isBootstrapped of class net/minecraft/server/Bootstrap")
    )
    isBootstrappedField.trySetAccessible()
    println("Is bootstrapped: " + isBootstrappedField.get(null) as Boolean)
    isBootstrappedField.set(null, true)

    val generators: List<AbstractGenerator<*>> = listOf(
        EntityDataSerializerGenerator(mapper, classRemapper, minecraftJarReader),
        FlatteningEntityTypeGenerator(mapper, classRemapper, minecraftJarReader)
    )
    Path.of(System.getProperty("user.dir"), "generated").toAbsolutePath().toFile().mkdirs()
    for (gen: AbstractGenerator<*> in generators) {
        val fileName: String = version.replace(".", "_") + "_" + gen.javaClass.simpleName.replace("generator", "", true) + "s.json"
        println("Generating $fileName...")
        gen.generateJson(Path.of(System.getProperty("user.dir"), "generated", fileName).toAbsolutePath().toFile())
    }
    minecraftJarReader.classLoader.close()
    Runtime.getRuntime().addShutdownHook(Thread {
        Path.of(System.getProperty("user.dir"), "logs").toAbsolutePath().toFile().deleteRecursively()
    })
}