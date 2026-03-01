# Tommhs Ice Melt

This project builds a single Hytale plugin JAR that also includes an asset pack.

## What it does

- Adds a placeable block item: `Tommhs_Rock_Ice`
- When a heat source block is placed (block key contains `torch`, `campfire`, or `fire`), nearby `Tommhs_Rock_Ice` blocks are converted into water (fluid).

## Build

### Prerequisites

- Java 25

### Build & deploy to your server mods folder

1. Edit `gradle.properties` and set:

```
hytaleModsDir=H:/workspace/Hytale/sortMod/mods
```

2. Run:

```
./gradlew build
```

If you don't have a Gradle wrapper yet, run via IntelliJ Gradle tool window or install Gradle.

### HytaleServer.jar path

By default the build uses the launcher "latest" server jar:

`%APPDATA%\\Hytale\\install\\release\\package\\game\\latest\\Server\\HytaleServer.jar`

Override if needed:

```
./gradlew build -PhytaleServerJar="C:/path/to/HytaleServer.jar"
```

## Notes

- Fluid placement uses the indexed fluid id `7` for `Water_Source`.
  If this changes in a future update, adjust `WATER_SOURCE_FLUID_ID` in `FluidPlacement`.
