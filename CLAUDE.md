# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew build          # Build the mod (outputs to build/libs/)
./gradlew runClient      # Run Minecraft client with mod loaded
./gradlew runServer      # Run Minecraft server with mod loaded
./gradlew runDatagen     # Run data generation
```

CI runs `./gradlew build` with Java 25 on push/PR (see `.github/workflows/build.yml`).

## Architecture

This is a **Minecraft 1.21.8 Fabric mod** (modid: `clairvoyance`) using Fabric Loom + Yarn mappings + Fabric API. Java 21. Standard Fabric split source set layout (`src/main/` for server-common, `src/client/` for client-only).

Entrypoints declared in `src/main/resources/fabric.mod.json`:
- `main` → `Clairvoyance` (server-side init, packet registration, event listeners)
- `client` → `ClairvoyanceClient` (client-side init, keybinds, HUD rendering, packet receivers)
- `fabric-datagen` → `ClairvoyanceDataGenerator`

### Feature Modules

The mod has four feature areas living under `src/main/java/.../features/` and mirrored client logic:

| Feature | Server Logic | Client Logic | Purpose |
|---|---|---|---|
| **Evil Eyes (千里眼)** | `features/evil_eyes/Evil_Eyes.java` | `client/evil_eyes/Evil_EyesClient.java` | Entity marking, multi-stage progression, watching through marked entities |
| **Gaze Guidance (视线诱导)** | `features/guidance/Gazeguidance.java` | `client/gazeguidance/GazeguidanceClient.java` | Energy-based focus/mark system with magic stick item |
| **Open Back** | In `Clairvoyance.java` directly | `client/gui/openback/` | Open other player's inventory (requires `kaibao` tag or creative) |
| **Carry Entity** | In `Clairvoyance.java` directly | Client-side callbacks in `ClairvoyanceClient` | Pick up and carry living entities (requires `kebao` tag or creative) |

Camera watching has two systems: an older one in `Evil_Eyes` and a newer one in `features/evil_eyes/server/CameraWatchManager.java` / `client/evil_eyes/watch/`.

### Networking

All packets are in `src/main/java/.../network/`, organized by feature sub-package:
- `clairvoyance/` — Evil Eyes core packets (mark, view, parrot, anchor, config, stage)
- `gazeguidance/` — Gaze guidance packets (energy, magic, focus, config)
- `camerawatch/` — Camera watch bind/unbind/update
- `openback/` — Inventory open and entity carry packets

`ModNetworking.java` centralizes S2C and C2S registration. Both `Clairvoyance.onInitialize()` and `ClairvoyanceClient.onInitializeClient()` call `ModNetworking.registerS2CPackets()` so S2C payloads are ready on both sides. C2S receivers are registered server-side in `Clairvoyance.onInitialize()`.

### Configuration

`GlobalConfigManager` (`config/GlobalConfigManager.java`) manages 7-stage configuration stored in `config/clairvoyance_global.json`. Synced to clients via `GlobalConfigS2CPacket` on join and on OP update.

`GazeConfig` (`item/config/GazeConfig.java`) stores per-player gaze guidance settings, synced via `SyncConfigS2CPacket`.

### Items

- `clairvoyance_item` — main Evil Eyes item, handled in `ClairvoyanceItem.java`
- `magic_stick` — gaze guidance tool, handled in `MagicStickItem.java`

Both registered in `ModItems.java`. The items define their model predicates and behaviors.

### Mixins

- `src/main/.../mixin/ExampleMixin.java` — server-side mixin (currently placeholder)
- `src/client/.../mixin/ExampleClientMixin.java` — client-side mixin (currently placeholder)

### Key Conventions

- Global static state (VIEWING_MAP, CARRIED_ENTITIES, etc.) lives directly in `Clairvoyance.java`
- ConcurrentHashMap is used for all shared mutable state accessed across tick events
- Server tick events (`ServerTickEvents.END_SERVER_TICK`) drive per-tick updates for camera watching, entity carrying, and parrot tracking
- Fabric API event callbacks (`AttackEntityCallback`, `UseBlockCallback`, etc.) are registered in both server and client initializers to intercept player actions
- Keybindings: `U` opens config screen (creative only), `V` is the multi-purpose action key (context-dependent on held item)
