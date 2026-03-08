# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Unstable Grid: Final Surge** — a tower defense game built with LibGDX (Java 8). Players build generators, power storages, and towers, connecting them with conduits to form a power grid that powers defensive towers.

## Build & Run Commands

```bash
# Run the game (desktop/LWJGL3)
./gradlew lwjgl3:run

# Build the project
./gradlew build

# Create distributable JAR
./gradlew lwjgl3:jar
# or
./gradlew lwjgl3:dist
```

There are no tests in this project currently.

## Architecture

### Module Structure

- **`core/`** — All game logic. Depends on LibGDX, Ashley ECS, Box2D, gdx-ai.
- **`lwjgl3/`** — Desktop launcher only (`Lwjgl3Launcher`). Entry point: `com.skamaniak.ugfs.lwjgl3.Lwjgl3Launcher`.

### Game Loop (GameScreen.render)

Each frame follows this sequence: `readInputs()` → `updateCamera()` → `gameState.simulate(delta)` → `selectPlayerAction()` → `draw(delta)`. Drawing uses two passes: `SpriteBatch` for textures, then `ShapeRenderer` for shapes (with alpha blending).

### Core Package Layout (`com.skamaniak.ugfs`)

- **`UnstableGrid`** — `Game` subclass, holds shared `SpriteBatch`, `ShapeRenderer`, `BitmapFont`. Starts on `MainMenuScreen`, transitions to `GameScreen`.
- **`GameScreen`** — Main game screen. Owns `SceneCamera`, `FitViewport` (1024x1024), `PlayerInput`, UI menus, and `PlayerAction` state machine.
- **`game/GameState`** — Central game state. Holds all entity sets (`generators`, `storages`, `towers`, `conduits`) and the `PowerGrid`. Handles entity registration, terrain lookups, simulation ticking, and rendering delegation.

### Power Grid Simulation (`simulation/`)

The power grid is a directed graph: **Generators** → **PowerStorages** → **Towers**, connected by **Conduits**.

- `GridComponent` — base interface
- `PowerConsumer` — can consume power (`consume(power, delta)`)
- `PowerSource` — can connect to consumers (`addTo`/`removeTo`)
- `PowerProducer extends PowerSource` — actively produces power (`produce(delta)`)
- `PowerGrid` — orchestrates simulation each frame: resets propagation, then generators produce, then storages produce (forwarding to towers)

### Entity Hierarchy (`game/entity/`)

`GameEntity` (abstract, has position) → concrete types: `GeneratorEntity`, `PowerStorageEntity`, `TowerEntity`, `ConduitEntity`. Each implements `Drawable` and simulation interfaces (`PowerProducer`, `PowerConsumer`, etc.). `GameEntityFactory` creates entities from `GameAsset` definitions.

### Asset System (`asset/`)

- **`GameAssetManager`** — Singleton (`INSTANCE`). Loads and caches all game data, textures, sounds, and skin. Tile size is 64px (`TILE_SIZE_PX`).
- **`JsonAssetLoader`** — Reads JSON definitions from `assets/json/` using LibGDX's `Json` deserializer.
- **`AssetType`** enum maps asset categories to filesystem paths under `assets/json/` (tower, generator, power-storage, conduit, terrain, level).
- Asset model classes (`Tower`, `Generator`, `PowerStorage`, `Conduit`, `Terrain`, `Level`) all extend `GameAsset`.

### Player Actions (`action/`)

`PlayerAction` interface with three implementations inside `PlayerActionFactory`:
- **`DetailsSelection`** — Default mode. Left-click shows entity details.
- **`Building`** — Active when a build menu item is selected. Shows valid/invalid placement overlay, places entity on click.
- **`Wiring`** — Active when a conduit is selected and right-clicking a PowerProducer. Shows connection range circle and wire preview.

`GameScreen.selectPlayerAction()` determines the current action each frame based on UI menu state and right-click context.

### UI (`ui/`)

Three Scene2D-based menus: `BuildMenu`, `DetailsMenu`, `WiringMenu`. Each has its own `Stage` registered in an `InputMultiplexer` alongside `PlayerInput`.

### Coordinate System

Two coordinate spaces: **world coordinates** (pixels, 64px per tile) and **mesh/grid coordinates** (tile indices). `NavigationUtils` converts between them. Entity positions are stored in mesh coordinates; rendering multiplies by `TILE_SIZE_PX`.

## Key Conventions

- Assets are defined as JSON files in `assets/json/{type}/` and loaded at startup via `JsonAssetLoader`.
- Game assets (textures, sounds, skin) live under `assets/` at the project root. The `assets/` directory is the working directory at runtime.
- Asset paths in code are relative to `assets/` (e.g., `"assets/visual/select-reticle.png"`).
- The Gradle wrapper is configured with Java source/target compatibility 8.
