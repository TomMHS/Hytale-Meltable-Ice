# Tommhs Meltable Ice

A Hytale server plugin that ships with an asset pack and adds a **craftable meltable ice block**.

Place the ice like normal blocks, then place a **heat source** nearby (torch / campfire / fire) to melt it into **water**.

## Features

- Adds a placeable block item: `Tommhs_Meltable_Rock_Ice`
- Crafting recipe (Farming Workbench):
  - **Input:** `Rock_Ice` ×1 + `Essence_of_Life` ×10
  - **Output:** `Tommhs_Meltable_Rock_Ice` ×10
- Heat source detection:
  - Triggers when a placed block key contains: `torch`, `campfire`, or `fire`
- Melting behavior:
  - Scans a radius around the heat source and converts matching meltable ice blocks into water (fluid layer)

## Installation

1. Build the plugin JAR (see **Build** below).
2. Copy the resulting JAR into your mods folder, for example:
  - User mods folder:
    - `C:\Users\<YOU>\AppData\Roaming\Hytale\UserData\Mods\`
  - Or your dedicated server mods folder (depends on your setup)

After starting the server, you should see logs indicating that the plugin loaded and the melt system is registered.

## Build

### Prerequisites

- **Java 25**
- **Gradle 9.1+** (required for Java 25)
  - The project includes the Gradle wrapper (`gradlew` / `gradlew.bat`) — use that.

### Configure local paths (not committed)

This repo ignores `gradle.properties` on purpose because it contains local machine paths.

You have two options:

#### Option A: Pass paths via Gradle `-P` parameters (recommended)

PowerShell example:

```powershell
./gradlew build `
  -PhytaleServerJarDir="C:/Users/<YOU>/AppData/Roaming/Hytale/install/release/package/game/latest/Server" `
  -PhytaleModsDir="C:/Users/<YOU>/AppData/Roaming/Hytale/UserData/Mods"