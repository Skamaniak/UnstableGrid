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

`PlayerAction` interface with four implementations inside `PlayerActionFactory`:
- **`DetailsSelection`** — Default mode. Left-click selects an entity and shows its details. The selected entity reference is kept and details are refreshed every frame via `handleMouseMove`.
- **`Building`** — Active when a build menu item is selected. Shows valid/invalid placement overlay, places entity on click.
- **`Wiring`** — Active when a conduit type is selected from the WiringMenu via ContextMenu. Left-clicking a valid `PowerConsumer` within range creates the conduit link and resets the mode. Persists as long as `wiringMenu.getSelectedConduit()` is non-null.
- **`WireRemoval`** — Active when "Remove Wire" is clicked in the ContextMenu. Highlights connected consumers; clicking one removes the conduit and refunds scrap. Exits on right-click or successful removal.

**`selectPlayerAction()` — critical design pattern.** This method runs every frame and derives `pendingPlayerAction` from UI state. It unconditionally falls through to `detailsSelection` at the bottom when no condition matches. This means:
- **One-shot flags** (set by a UI callback, consumed by `selectPlayerAction`) only survive one frame. Actions that must persist across frames need either a per-frame condition that stays true (e.g. `wiringMenu.getSelectedConduit() != null`) or a guard boolean (e.g. `inWireRemovalMode`) that blocks the fallthrough.
- **Flag lifecycle matters.** If a UI callback sets a flag then calls a cleanup method (like `hide()`) that blanket-resets all flags, the flag is cleared before `selectPlayerAction` reads it. Always set outbound flags AFTER cleanup calls.
- **Mode cancellation.** Entering a new mode must cancel conflicting active modes — e.g. opening the ContextMenu calls `wiringMenu.resetSelection()` to cancel wiring; selecting a build item also cancels wiring.

Priority chain in `selectPlayerAction()`: sell confirm → wire removal request → wiring request → wire removal persistence → persistent wiring → build menu → detailsSelection.

### UI (`ui/`)

Five Scene2D-based UI elements: `BuildMenu`, `DetailsMenu`, `WiringMenu`, `ContextMenu`, `ScrapHud`. All share the game's `SpriteBatch`. Each has its own `Stage` registered in an `InputMultiplexer`. `PlayerInput` is first in the multiplexer so game clicks are not consumed by UI stages.

- **`ContextMenu`** — Right-click popup on structures. Shows Wire, Remove Wire (if outgoing conduits exist), and Sell buttons. Sell uses two-click confirmation. Sets one-shot flags (`wiringRequested`, `wireRemovalRequested`, `sellConfirmed`) read by `selectPlayerAction()`. Opening a new menu cancels wiring mode via `wiringMenu.resetSelection()`.
- **`ScrapHud`** — Top-left label showing current scrap count. Polls `gameState.getScrap()` each frame; flashes green on gain, red on spend.

### Wire Rendering (`ConduitEntity`)

Wires are drawn as a repeated texture rotated to the wire angle using `SpriteBatch.draw` with origin at `(0, thickness/2)` (left-center of the sprite). To separate parallel wires (e.g. A→B and B→A in a cycle), both endpoints are offset perpendicular to the wire. The perpendicular is computed from a **canonical lesser→greater endpoint ordering** (not the wire's own direction vector) so opposite-direction wires always offset to opposite sides.

### Coordinate System

Two coordinate spaces: **world coordinates** (pixels, 64px per tile) and **mesh/grid coordinates** (tile indices). `NavigationUtils` converts between them. Entity positions are stored in mesh coordinates; rendering multiplies by `TILE_SIZE_PX`.

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
- Entity logic methods (`consume()`, `produce()`, `attemptShot()`) are pure computation — no static calls, fully testable with mocked asset objects.
- `PowerGrid.simulatePropagation()` is testable (uses `java.util.logging` instead of `Gdx.app`).
- `NavigationUtils` coordinate conversions are pure math.
- `ConduitEntity.consume()` rate limiting and loss logic is pure math.

### What requires LibGDX and is NOT unit tested
- All `draw()` methods (texture loading via `GameAssetManager.INSTANCE`).
- `GameState.simulateShooting()` (plays sounds via `GameAssetManager.INSTANCE`).
- `Building.isBuildable()` (terrain lookup via `GameAssetManager.INSTANCE`).
- UI classes (`BuildMenu`, `DetailsMenu`, `WiringMenu`).

### Mocking strategy
- Asset model classes (`Generator`, `Tower`, `PowerStorage`, `Conduit`) have private fields with no setters (designed for JSON deserialization). Use **Mockito mocks** to create test instances — do NOT add setters or constructors to production code for testing purposes.
- Use `TestAssetFactory` helper class in `core/src/test/` for creating mocked assets with configurable stats.
- `GameConstants.TILE_SIZE_PX` provides the tile size constant without importing `GameAssetManager`.

## Feature Development Workflow

New features are developed using a multi-agent pipeline invoked with the `/develop-feature` skill:

1. **`feature-planner`** (Opus) — reads the codebase, writes a spec to `docs/specs/YYYYMMDD-<feature-name>.md`, returns a summary
2. **Manual gate** — spec is printed inline for review; proceed only after approval
3. **`implementer`** (Sonnet) — follows the spec, checks off steps as it goes, compiles before finishing
4. **`test-generator`** (Sonnet) — writes JUnit 5 + Mockito tests for all new pure-logic code
5. **`code-reviewer`** (Sonnet) — reviews correctness, performance, and convention adherence; loops back to the implementer if blockers are found

Agents are defined in `.claude/agents/`. The skill is defined in `.claude/skills/develop-feature/SKILL.md`.

Feature plans and design documents live in `docs/specs/`. Each spec follows the naming convention `YYYYMMDD-<feature-name>.md` and tracks the feature from design through implementation using checkboxes.

## Known Issues & Incomplete Areas

- **`TowerEntity.shoot()`** is empty — tower firing plays a sound but deals no damage and has no enemy targeting.
- **Enemy simulation** (`simulateEnemies`) is not implemented.
- **Level loading** is not implemented — `GameScreen` uses `populateGameStateWithDummyData()` instead.
- **Scrap cost for building** — `registerLink()` now charges scrap, but structure placement in `Building.handleClick()` does not yet check or spend scrap.
