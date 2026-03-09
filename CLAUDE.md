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
- **`GameScreen`** — Main game screen. Owns `SceneCamera`, `FitViewport` (1024x1024), `PlayerInput`, UI menus, and `PlayerAction` state machine. Currently hardcodes entities via `populateGameStateWithDummyData()` — real level loading is not yet implemented.
- **`game/GameState`** — Central game state. Holds all entity sets (`generators`, `storages`, `towers`, `conduits`) and the `PowerGrid`. Handles entity registration, terrain lookups, simulation ticking, and rendering delegation.

### Power Grid Simulation (`simulation/`)

The power grid is a directed graph: **Generators** → **PowerStorages** → **Towers**, connected by **Conduits**. Wires are unidirectional by design.

- `GridComponent` — base interface with `resetPropagation()`
- `PowerConsumer` — can consume power (`consume(power, delta)`)
- `PowerSource` — can connect to consumers (`addTo`/`removeTo`)
- `PowerProducer extends PowerSource` — actively produces power (`produce(delta)`)
- `PowerGrid` — orchestrates simulation each frame: resets propagation on all nodes directly from its flat sets (no recursive traversal), then generators produce, then storages produce

**Propagation flags in `PowerStorageEntity`:** Two boolean flags are needed to correctly handle all graph topologies:
- `inProgress` — set before recursing into downstream consumers, cleared after. A cycle re-entry sees `inProgress=true` and refuses the power (returns it to the caller) to break the cycle.
- `propagated` — set after processing completes. A second source reaching the same storage sees `propagated=true` and absorbs power into the bank without forwarding downstream again.

**`ConduitEntity`** acts as an intermediary `PowerConsumer` — it registers itself via `from.addTo(this)` and forwards limited power (rate, loss) to its actual `to` target. It does **not** extend `GameEntity` so it cannot be found by `getEntityAt()`.

### Entity Hierarchy (`game/entity/`)

`GameEntity` (abstract, has position) → concrete types: `GeneratorEntity`, `PowerStorageEntity`, `TowerEntity`. `ConduitEntity` is separate — it implements `PowerConsumer` and `Drawable` but does not extend `GameEntity` (a conduit has no tile position of its own). `GameEntityFactory` creates entities from `GameAsset` definitions.

### Asset System (`asset/`)

- **`GameAssetManager`** — Singleton (`INSTANCE`). Loads and caches all game data, textures, sounds, and skin. Tile size is 64px (`TILE_SIZE_PX`).
- **`JsonAssetLoader`** — Reads JSON definitions from `assets/json/` using LibGDX's `Json` deserializer.
- **`AssetType`** enum maps asset categories to filesystem paths under `assets/json/` (tower, generator, power-storage, conduit, terrain, level).
- Asset model classes (`Tower`, `Generator`, `PowerStorage`, `Conduit`, `Terrain`, `Level`) all extend `GameAsset`.

### Player Actions (`action/`)

`PlayerAction` interface with three implementations inside `PlayerActionFactory`:
- **`DetailsSelection`** — Default mode. Left-click selects an entity and shows its details. The selected entity reference is kept and details are refreshed every frame via `handleMouseMove`.
- **`Building`** — Active when a build menu item is selected. Shows valid/invalid placement overlay, places entity on click.
- **`Wiring`** — Active when a conduit type is selected from the WiringMenu (right-click on a PowerProducer) and that entity is still under the right-click position. Left-clicking a valid `PowerConsumer` within range creates the conduit link and resets the mode.

`GameScreen.selectPlayerAction()` re-evaluates the active action every frame based on UI state and right-click position. Wiring mode stays active as long as a conduit is selected in `WiringMenu` and the right-click position is on a `PowerProducer`.

### UI (`ui/`)

Three Scene2D-based menus: `BuildMenu`, `DetailsMenu`, `WiringMenu`. All share the game's `SpriteBatch`. Each has its own `Stage` registered in an `InputMultiplexer`. `PlayerInput` is first in the multiplexer so game clicks are not consumed by UI stages.

### Wire Rendering (`ConduitEntity`)

Wires are drawn as a repeated texture rotated to the wire angle using `SpriteBatch.draw` with origin at `(0, thickness/2)` (left-center of the sprite). To separate parallel wires (e.g. A→B and B→A in a cycle), both endpoints are offset perpendicular to the wire. The perpendicular is computed from a **canonical lesser→greater endpoint ordering** (not the wire's own direction vector) so opposite-direction wires always offset to opposite sides.

### Coordinate System

Two coordinate spaces: **world coordinates** (pixels, 64px per tile) and **mesh/grid coordinates** (tile indices). `NavigationUtils` converts between them. Entity positions are stored in mesh coordinates; rendering multiplies by `TILE_SIZE_PX`.

## Key Conventions

- Assets are defined as JSON files in `assets/json/{type}/` and loaded at startup via `JsonAssetLoader`.
- Game assets (textures, sounds, skin) live under `assets/` at the project root. The `assets/` directory is the working directory at runtime.
- Asset paths in code are relative to `assets/` (e.g., `"assets/visual/select-reticle.png"`).
- The Gradle wrapper is configured with Java source/target compatibility 8, but `KeyboardControls` uses `Set.of()` which requires Java 9+. This works at runtime on modern JVMs but would break a strict JDK 8 toolchain.

## Known Issues & Incomplete Areas

- **`TowerEntity.shoot()`** is empty — tower firing plays a sound but deals no damage and has no enemy targeting.
- **Enemy simulation** (`simulateEnemies`) is not implemented.
- **Level loading** is not implemented — `GameScreen` uses `populateGameStateWithDummyData()` instead.
- **Entity removal** is not exposed in the UI — `removeSource`, `removeStorage`, `removeSink` exist in `PowerGrid` but are never called from gameplay.
