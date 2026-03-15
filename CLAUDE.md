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

## Testing

```bash
# Run unit tests
./gradlew core:test
```

Tests use JUnit 5 + Mockito. Test sources are in `core/src/test/`.

## Architecture

### Module Structure

- **`core/`** — All game logic. Depends on LibGDX, Ashley ECS, Box2D, gdx-ai.
- **`lwjgl3/`** — Desktop launcher only (`Lwjgl3Launcher`). Entry point: `com.skamaniak.ugfs.lwjgl3.Lwjgl3Launcher`.

### Game Loop (GameScreen.render)

Each frame follows this sequence: `readInputs()` → `updateCamera()` → `gameState.simulate(delta)` → `selectPlayerAction()` → `draw(delta)`. Drawing uses two passes: `SpriteBatch` for textures, then `ShapeRenderer` for shapes (with alpha blending).

**SpriteBatch color reset:** `draw()` calls `game.batch.setColor(Color.WHITE)` before `batch.begin()` every frame. This is mandatory because the shared `SpriteBatch` does NOT reset its color between `begin()`/`end()` calls or between frames. Without this, any UI code that tints actors (e.g. red disabled buttons) will poison the game world rendering on the next frame.

### Core Package Layout (`com.skamaniak.ugfs`)

- **`UnstableGrid`** — `Game` subclass, holds shared `SpriteBatch`, `ShapeRenderer`, `BitmapFont`. Starts on `MainMenuScreen`, transitions to `GameScreen`.
- **`GameScreen`** — Main game screen. Owns `SceneCamera`, `FitViewport` (1024x1024), `PlayerInput`, UI menus, and `PlayerAction` state machine. Currently hardcodes entities via `populateGameStateWithDummyData()` — real level loading is not yet implemented.
- **`game/GameState`** — Central game state. Holds all entity sets (`generators`, `storages`, `towers`, `conduits`), the `PowerGrid`, enemy instances, wave manager, and pathfinder. Handles entity registration, terrain lookups, simulation ticking (power → shooting → enemies), and rendering delegation. Checks `gameOver` flag after simulation.

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

`EnemyInstance` is also NOT a `GameEntity` — enemies move smoothly between tiles in world coordinates, are not selectable, and are not part of the power grid. It holds an `Enemy` asset reference, current health, world position, a path (list of waypoints), and alive/reachedBase flags. Its `move(delta)`, `takeDamage(amount)`, and `getHealthFraction()` methods are pure math and fully testable.

### Asset System (`asset/`)

- **`GameAssetManager`** — Singleton (`INSTANCE`). Loads and caches all game data, textures, sounds, and skin. Tile size is 64px (`TILE_SIZE_PX`).
- **`JsonAssetLoader`** — Reads JSON definitions from `assets/json/` using LibGDX's `Json` deserializer.
- **`AssetType`** enum maps asset categories to filesystem paths under `assets/json/` (tower, generator, power-storage, conduit, terrain, level, enemy).
- Asset model classes (`Tower`, `Generator`, `PowerStorage`, `Conduit`, `Terrain`, `Level`, `Enemy`) all extend `GameAsset`.

### Player Actions (`action/`)

`PlayerAction` interface with four implementations inside `PlayerActionFactory`:
- **`DetailsSelection`** — Default mode. Left-click selects an entity and shows its details. The selected entity reference is kept and details are refreshed every frame via `handleMouseMove`.
- **`Building`** — Active when a build menu item is selected. Shows valid/invalid placement overlay, places entity on click. **One-shot:** after successful placement, `buildMenu.resetSelection()` is called, returning to `detailsSelection`.
- **`Wiring`** — Active when a conduit type is selected from the WiringMenu via ContextMenu. Left-clicking a valid `PowerConsumer` within range creates the conduit link and resets the mode. Persists as long as `wiringMenu.getSelectedConduit()` is non-null.
- **`WireRemoval`** — Active when "Remove Wire" is clicked in the ContextMenu. Highlights connected consumers; clicking one removes the conduit and refunds scrap. Exits on right-click or successful removal.

**`selectPlayerAction()` — critical design pattern.** This method runs every frame and derives `pendingPlayerAction` from UI state. It unconditionally falls through to `detailsSelection` at the bottom when no condition matches. This means:
- **One-shot flags** (set by a UI callback, consumed by `selectPlayerAction`) only survive one frame. Actions that must persist across frames need either a per-frame condition that stays true (e.g. `wiringMenu.getSelectedConduit() != null`) or a guard boolean (e.g. `inWireRemovalMode`) that blocks the fallthrough.
- **Flag lifecycle matters.** If a UI callback sets a flag then calls a cleanup method (like `hide()`) that blanket-resets all flags, the flag is cleared before `selectPlayerAction` reads it. Always set outbound flags AFTER cleanup calls.
- **Mode cancellation.** Entering a new mode must cancel conflicting active modes — e.g. opening the ContextMenu calls `wiringMenu.resetSelection()` and `buildMenu.resetSelection()` to cancel both wiring and building.
- **Right-click universally cancels.** All persistent player action modes (wiring, wire removal) exit on right-click. This is a hard convention — any new persistent mode must include a right-click exit path.
- **UI click-through prevention.** `readInputs()` guards `pendingPlayerAction.handleClick()` with `isClickOnUI()`, which hit-tests all UI stages. Game actions never fire when the player clicks a UI element. Any new UI with its own Stage must be added to `isClickOnUI()`.

Priority chain in `selectPlayerAction()`: sell confirm → wire removal request → wiring request → wire removal persistence → build menu → persistent wiring → detailsSelection.

### UI (`ui/`)

Five Scene2D-based UI elements: `BuildMenu`, `DetailsMenu`, `WiringMenu`, `ContextMenu`, `ScrapHud`. All share the game's `SpriteBatch`. Each has its own `Stage` registered in an `InputMultiplexer`. `PlayerInput` is first in the multiplexer so game clicks are not consumed by UI stages.

- **`ContextMenu`** — Right-click popup on structures. Shows Wire, Remove Wire (if outgoing conduits exist), and Sell buttons. Sell uses two-click confirmation. Sets one-shot flags (`wiringRequested`, `wireRemovalRequested`, `sellConfirmed`) read by `selectPlayerAction()`. Opening a new menu cancels both wiring and building via `wiringMenu.resetSelection()` and `buildMenu.resetSelection()`.
- **`WiringMenu`** — Popup showing conduit types for wiring. Shown by ContextMenu's Wire button. Dismisses on click-outside via stage listener. If dismissed without selecting a conduit, `selectPlayerAction()` detects `!wiringMenu.isVisible()` and resets `wiringRequested`.
- **Popup dismiss convention:** All popup menus (`ContextMenu`, `WiringMenu`) must dismiss on click-outside. They use a stage-level `InputListener` that checks `menuTable.hit()` and calls `hide()` when the click misses the menu. New popups must follow this pattern.
- **`hide()` vs `resetSelection()` — visual + logical vs logical only.** `hide()` hides the menu table AND resets logical state (e.g. `selectedConduit = null`). `resetSelection()` only resets logical state without affecting visibility. When canceling a mode that involves a popup, always call `hide()` — calling only `resetSelection()` leaves a zombie menu visible on screen.
- **`ScrapHud`** — Top-left label showing current scrap count. Polls `gameState.getScrap()` each frame; flashes green on gain, red on spend.
- **Tinting button styles:** When creating a colored variant of a button (e.g. light-blue upgrade, red disabled), always tint the skin's existing button drawable — not `"white"`. `skin.newDrawable("white", color)` produces a flat rectangle that loses the nine-patch borders. Correct pattern: `skin.newDrawable(defaultStyle.up, color)` where `defaultStyle = skin.get(TextButton.TextButtonStyle.class)`.

### Wire Rendering (`ConduitEntity`)

Wires are drawn as a repeated texture rotated to the wire angle using `SpriteBatch.draw` with origin at `(0, thickness/2)` (left-center of the sprite). To separate parallel wires (e.g. A→B and B→A in a cycle), both endpoints are offset perpendicular to the wire. The perpendicular is computed from a **canonical lesser→greater endpoint ordering** (not the wire's own direction vector) so opposite-direction wires always offset to opposite sides.

### Coordinate System

Two coordinate spaces: **world coordinates** (pixels, 64px per tile) and **mesh/grid coordinates** (tile indices). `NavigationUtils` converts between them. Entity positions are stored in mesh coordinates; rendering multiplies by `TILE_SIZE_PX`.

### Enemy Simulation (`game/`)

Enemies spawn from level-defined spawn locations, pathfind to a base tile, and are targeted by towers. The simulation runs each frame in `GameState.simulateEnemies(delta)` after power propagation and shooting.

**`Enemy`** (`asset/model/`) — JSON asset with `health`, `speed` (tiles/sec), `scrap` (kill reward), `flying` (boolean). Loaded from `assets/json/enemy/`.

**`Level` extensions** — Three new fields: `base` (Position), `waves` (list of Wave with `wave` number and `delay` in seconds), `spawnLocations` (list of SpawnLocation with coordinates and `spawnPlan`). Each spawn plan entry references a wave number and lists enemies with staggered delays in ms.

**`WaveManager`** — Drives the wave lifecycle. Takes `EnemyLookup` and `PathComputer` interfaces (not `java.util.function`) to stay decoupled from `GameAssetManager`. `update(delta, aliveEnemyCount)` returns newly spawned `EnemyInstance` objects. Wave N+1 starts after its configured delay once all wave-N enemies are dead. Private inner class `SpawnTimer` handles per-enemy staggered spawning within a wave.

**`TilePathfinder`** — A* on the tile grid (4-directional, Manhattan heuristic). Obstacles = structures + water + impassable terrain. Paths are computed once at spawn time, not per frame. Flying enemies skip pathfinding — their path is just `[spawn, base]`. Constructor takes a `TerrainType[][]` (pre-built, defaults to WATER for missing tiles) and a live reference to the entity position map.

**`TowerEntity.shoot()`** — Finds the closest alive `EnemyInstance` within `towerRange * TILE_SIZE_PX` pixel distance. Returns `boolean` — `true` only if a target was found and damaged. `attemptShot()` gates power consumption and fire timer reset on `shoot()` returning `true`, so towers never waste power or play sounds when no enemy is in range.

**Game over** — When any enemy reaches the base tile, `gameOver` is set. `GameScreen.render()` checks this after `simulate()` and transitions to `MainMenuScreen`. Music is stopped via stored `Sound` reference and loop ID in `dispose()`.

**Rendering (temporary placeholders)** — Spawn points: red circles. Base: green circle. Enemies: orange circles with health bars (ShapeRenderer pass). No sprites yet.

## Key Conventions

- Assets are defined as JSON files in `assets/json/{type}/` and loaded at startup via `JsonAssetLoader`.
- Game assets (textures, sounds, skin) live under `assets/` at the project root. The `assets/` directory is the working directory at runtime.
- Asset paths in code are relative to `assets/` (e.g., `"assets/visual/select-reticle.png"`).
- The Gradle wrapper is configured with Java source/target compatibility 8, but `KeyboardControls` uses `Set.of()` which requires Java 9+. This works at runtime on modern JVMs but would break a strict JDK 8 toolchain.

## Refactoring & Testing Guidelines

### Performance-first approach
- This is a game running at 60+ FPS. Do NOT introduce indirection (interfaces, callbacks, dependency injection) on hot paths (simulation, rendering) just for testability.
- Static singletons like `GameAssetManager.INSTANCE` are intentional and idiomatic for game development. Do not refactor them into injected dependencies.
- `static final` constants are free — the JVM inlines them at compile time.

### What is testable without refactoring
- Entity logic methods (`consume()`, `produce()`, `attemptShot(delta, enemies)`) are pure computation — no static calls, fully testable with mocked asset objects.
- `PowerGrid.simulatePropagation()` is testable (uses `java.util.logging` instead of `Gdx.app`).
- `NavigationUtils` coordinate conversions are pure math.
- `ConduitEntity.consume()` rate limiting and loss logic is pure math.
- `EnemyInstance.move()`, `takeDamage()`, `getHealthFraction()` — pure math, no LibGDX calls.
- `TilePathfinder.findPath()` — A* on a 2D array, pure logic.
- `WaveManager.update()` — wave lifecycle and spawn timing, uses injected interfaces instead of statics.

### What requires LibGDX and is NOT unit tested
- All `draw()` methods (texture loading via `GameAssetManager.INSTANCE`).
- `GameState.simulateShooting()` (plays sounds via `GameAssetManager.INSTANCE`).
- `GameState.simulateEnemies()` (orchestrates wave manager + enemy list inside `GameState`).
- `Building.isBuildable()` (terrain lookup via `GameAssetManager.INSTANCE`).
- UI classes (`BuildMenu`, `DetailsMenu`, `WiringMenu`).

### Mocking strategy
- Asset model classes (`Generator`, `Tower`, `PowerStorage`, `Conduit`, `Enemy`) have private fields with no setters (designed for JSON deserialization). Use **Mockito mocks** to create test instances — do NOT add setters or constructors to production code for testing purposes.
- Use `TestAssetFactory` helper class in `core/src/test/` for creating mocked assets with configurable stats.
- `GameConstants.TILE_SIZE_PX` provides the tile size constant without importing `GameAssetManager`.

## Feature Development Workflow

New features are developed using a multi-agent pipeline invoked with the `/develop-feature` skill:

1. **`feature-planner`** (Opus) — reads the codebase, writes a spec to `docs/specs/YYYYMMDD-<feature-name>.md`, returns a summary
2. **`Manual gate`** — spec is printed inline for review; proceed only after approval
3. **`implementer`** (Opus) — follows the spec, explores the state machine before coding, checks off steps as it goes, compiles and runs tests before finishing
4. **`test-generator`** (Sonnet) — writes JUnit 5 + Mockito tests for all new pure-logic code
5. **`code-reviewer`** (Sonnet) — reviews correctness, performance, and convention adherence; loops back to the implementer if blockers are found

Agents are defined in `.claude/agents/`. Shared conventions (UI state machine rules, performance rules, testing boundaries) are in `.claude/agents/shared-conventions.md` — this is the single source of truth referenced by all agents. The skill is defined in `.claude/skills/develop-feature/SKILL.md`.

A PostToolUse hook (`.claude/settings.json`) automatically runs `./gradlew core:test` after any Java source file is edited, catching regressions immediately.

Feature plans and design documents live in `docs/specs/`. Each spec follows the naming convention `YYYYMMDD-<feature-name>.md` and tracks the feature from design through implementation using checkboxes.

## Known Issues & Incomplete Areas

- **Level loading** is not implemented — `GameScreen` uses `populateGameStateWithDummyData()` instead.
- **Enemy rendering** is placeholder (colored circles via `ShapeRenderer`). No sprites/icons yet.
- **Enemies do not repath** when structures are built/sold. Existing enemies keep their spawn-time path and may walk through new structures. Future enemies pathfind around them.
- **No wave HUD** — the player has no UI indication of current wave number or countdown.
- **No victory condition** — when all waves are exhausted, nothing happens.
- **No build-phase gate** — wave 1 timer starts immediately at game start.
