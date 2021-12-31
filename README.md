# datagenerator
A command line utility for extracting data from Minecraft server JARs. Written in Kotlin.  
Supports 1.9.4 to the latest (some data might be unavailable in <1.14).

## Usage
```bash
java -jar datagenerator.jar -v|--version <version> [-r|--ref <ref>] [-r2|--ref2 <ref2>]
```
Changing the reference mappings (ref arguments) is not recommended and will probably break the generator (you'll need to make your own overrides).  

Generated files will be in the `generated` folder.

## Data
* `EntityDataSerializers` - 1.9.4 to the latest (mojang names, protocol ID's)
* `EntityTypes` - 1.13.1 to the latest (mojang names, translation keys, physics properties, ...)
* `Blocks` - 1.13.1 to the latest (mojang names, translation keys, physics properties, ...)
* `Packets` - 1.9.4 to the latest (mojang names, fields)
