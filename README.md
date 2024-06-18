# BucketLib

[![Curseforge](http://cf.way2muchnoise.eu/full_bucketlib_downloads(0D0D0D-F16436-fff-010101-fff).svg)](https://www.curseforge.com/minecraft/mc-mods/bucketlib)
[![Curseforge](http://cf.way2muchnoise.eu/versions/For%20MC_bucketlib_all(0D0D0D-F16436-fff-010101).svg)](https://www.curseforge.com/minecraft/mc-mods/bucketlib/files)
[![Modrinth](https://img.shields.io/modrinth/dt/DItE655o?label=Modrinth&logo=modrinth)](https://modrinth.com/mod/bucketlib)
[![CI/CD](https://github.com/cech12/BucketLib/actions/workflows/cicd-workflow.yml/badge.svg)](https://github.com/cech12/BucketLib/actions/workflows/cicd-workflow.yml)
[![CodeFactor](https://www.codefactor.io/repository/github/cech12/bucketlib/badge)](https://www.codefactor.io/repository/github/cech12/bucketlib)
[![License](https://img.shields.io/github/license/cech12/BucketLib)](http://opensource.org/licenses/MIT)
[![](https://img.shields.io/discord/752506676719910963.svg?style=flat&color=informational&logo=discord&label=Discord)](https://discord.gg/gRUFH5t)

BucketLib is a Minecraft library mod for developers (Forge, NeoForge, Fabric, Quilt). The purpose is to provide functionality for developers to add their own buckets 
without having trouble implementing all special cases.

## Features

- **Highly configurable buckets**:
  - Obtaining fluids, entities and blocks can be limited by using allow lists and block lists or by using the fluid temperature.
  - Buckets can be configured to crack when a fluid temperature exceeds a special value.
  - Coloring a bucket can be enabled
  - Milking entities can be disabled
- **Compatible with all fluids**: Water and Lava as well as all modded fluids are supported by buckets generated with this library mod. 
- **Entities can be obtained**: Entities like Axolotl, fish and mobs of other mods can be obtained.
- **Bucketable Blocks can be obtained**: Powder Snow and bucketable blocks of other mods can be obtained.
- **Compatible with all milk special cases**: Entities like cows and goats can be milked and the milk is drinkable. Buckets filled with milk are also compatible with mods that contains a milk fluid.
- **Dispense Behaviour** works out of the box
- **Fluid Handling**: All buckets that are generated by this library mod are compatible with mods that are using fluid container items.

## Dependencies

The Fabric / Quilt version needs the following mods:

- Fabric API ([GitHub](https://github.com/FabricMC/fabric), [Curseforge](https://www.curseforge.com/minecraft/mc-mods/fabric-api), [Modrinth](https://modrinth.com/mod/fabric-api))
- Cloth Config API ([GitHub](https://github.com/shedaniel/cloth-config), [Curseforge](https://www.curseforge.com/minecraft/mc-mods/cloth-config), [Modrinth](https://modrinth.com/mod/cloth-config))

## Adding it to your project:

[![](https://jitpack.io/v/cech12/BucketLib.svg)](https://jitpack.io/#cech12/BucketLib)

Add the following to your `build.gradle` file:

```groovy
repositories {
    maven {
      name 'Jitpack'
      url 'https://jitpack.io'
    }
}
```

Neoforge:
```groovy
dependencies {
    implementation "com.github.cech12.BucketLib:neoforge:${bucketlib_version}"
}
```

Forge:
```groovy
dependencies {
    implementation fg.deobf("com.github.cech12.BucketLib:forge:${bucketlib_version}")
}
```

Fabric:
```groovy
dependencies {
    //required dependency by bucketlib
    modImplementation("me.shedaniel.cloth:cloth-config-fabric:${cloth_config_version}") {
        exclude(group: "net.fabricmc.fabric-api")
    }
    modImplementation("com.github.cech12.BucketLib:fabric:${bucketlib_version}")
}
```

Replace `${bucketlib_version}` with the version of BucketLib that you want to use. The actual versions can be found [here](https://jitpack.io/api/builds/com.github.cech12/BucketLib) or on the Github Releases page.

Forge specific: BucketLib adds mixins and developers need to make sure to tweak their run configurations in order to launch the game in their development environment.
Add both of these lines to the `configureEach {}` run configuration block in the `build.gradle` (or to both the `client {}` and `server {}`). These can be placed anywhere within the run configuration, the order does not matter. (**not required for versions before BucketLib 1.19.3-1.2.0.0**)

```groovy
property 'mixin.env.remapRefMap', 'true'
property 'mixin.env.refMapRemappingFile', "${buildDir}/createSrgToMcp/output.srg"
```

Don't forget to re-generate your IDE runs. (`genIntellijRuns`, `genEclipseRuns`, or `genVSCodeRuns`)

Before BucketLib ***1.20.2-3.0.0.0*** the `build.gradle` file adjustments were different.

For detailed information please see the [Developer Guide](https://github.com/cech12/BucketLib/wiki/Developer-Guide).
