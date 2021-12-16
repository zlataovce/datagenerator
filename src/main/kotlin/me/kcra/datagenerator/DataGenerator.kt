package me.kcra.datagenerator

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options

fun main(args: Array<String>) {
    val opts: Options = Options()
        .addRequiredOption("v", "version", true, "Version for extraction")
        .addOption("c", "config", true, "Version config")
    val cmd: CommandLine = DefaultParser().parse(opts, args)
}