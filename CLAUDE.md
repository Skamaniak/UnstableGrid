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

**Overlay mode:** When `overlayActive` is true (victory or defeat), the render loop short-circuits: only `waveHud.handleInput()` (to finish flash animations), `draw(delta)`, and the `GameOverOverlay` are processed. No simulation, no input reading, no `selectPlayerAction()`. The overlay is activated AFTER the final `draw()` so the player sees the last frame before the overlay appears.

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

**`PowerStorageEntity`** uses `inProgress`/`propagated` boolean flags to handle cycles and multi-source topologies during propagation. Read the source for details.

**`ConduitEntity`** acts as an intermediary `PowerConsumer` — forwards limited power (rate, loss) to its target. Does **not** extend `GameEntity` (no tile position).

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

**`selectPlayerAction()` — critical design pattern.** This method runs every frame and derives `pendingPlayerAction` from UI state. It unconditionally falls through to `detailsSelection` at the bottom when no condition matches. The detailed rules for flag lifecycle, mode persistence, cancellation, and UI click-through prevention are in `.claude/agents/shared-conventions.md` Rules 1–9 — read those before modifying any player action code.

Priority chain in `selectPlayerAction()`: sell confirm → wire removal request → wiring request → wire removal persistence → build restriction check → build menu → persistent wiring → detailsSelection.

**Build restriction during waves:** `selectPlayerAction()` checks `gameState.isBuildingAllowed()` before entering build mode. If a build item is selected but building is not allowed (wave active), `buildMenu.resetSelection()` is called and the action falls through to `detailsSelection`.

### UI (`ui/`)

Seven Scene2D-based UI elements: `BuildMenu`, `DetailsMenu`, `WiringMenu`, `ContextMenu`, `ScrapHud`, `WaveHud`, `GameOverOverlay`. All share the game's `SpriteBatch`. Each has its own `Stage` registered in an `InputMultiplexer`. `PlayerInput` is first in the multiplexer so game clicks are not consumed by UI stages.

- **`ContextMenu`** — Right-click popup on structures. Shows Wire, Remove Wire (if outgoing conduits exist), and Sell buttons. Sell uses two-click confirmation. Sets one-shot flags (`wiringRequested`, `wireRemovalRequested`, `sellConfirmed`) read by `selectPlayerAction()`. Opening a new menu cancels both wiring and building via `wiringMenu.resetSelection()` and `buildMenu.resetSelection()`.
- **`WiringMenu`** — Popup showing conduit types for wiring. Shown by ContextMenu's Wire button. Dismisses on click-outside via stage listener.
- **`BuildMenu`** — Bottom-left build panel with tabbed categories. Disables all buttons (`Touchable.disabled`) during active waves and when unaffordable.
- **`ScrapHud`** — Top-left label showing current scrap count. Flashes green on gain, red on spend.
- **`WaveHud`** — Top-center labels showing wave countdown, wave number, enemy counts. Flash text below the HUD on wave transitions (red on wave start, green on wave end).
- **`GameOverOverlay`** — Semi-transparent overlay shown on victory (green "Victory") or defeat (red "Defeated") with a "Main Menu" button.

UI implementation conventions (Label sizing, button disabling, popup dismiss, tinting, Stage disposal safety) are in `.claude/agents/shared-conventions.md` Rules 9–15.

### Wire Rendering (`ConduitEntity`)

Wires are drawn as a repeated texture rotated to the wire angle using `SpriteBatch.draw` with origin at `(0, thickness/2)` (left-center of the sprite). To separate parallel wires (e.g. A→B and B→A in a cycle), both endpoints are offset perpendicular to the wire. The perpendicular is computed from a **canonical lesser→greater endpoint ordering** (not the wire's own direction vector) so opposite-direction wires always offset to opposite sides.

### Coordinate System

Two coordinate spaces: **world coordinates** (pixels, 64px per tile) and **mesh/grid coordinates** (tile indices). `NavigationUtils` converts between them. Entity positions are stored in mesh coordinates; rendering multiplies by `TILE_SIZE_PX`.

### Enemy & Wave System (`game/`)

Enemies spawn from level-defined spawn locations, pathfind to a base tile, and are targeted by towers. The simulation runs each frame in `GameState.simulateEnemies(delta)` after power propagation and shooting.

- **`Enemy`** — JSON asset with `health`, `speed`, `scrap` (kill reward), `flying`. Loaded from `assets/json/enemy/`.
- **`EnemyInstance`** — NOT a `GameEntity`. Uses world coordinates, holds path (list of waypoints), alive/reachedBase flags. `repath()` replaces path from current position without teleporting.
- **`WaveManager`** — Drives wave lifecycle. Decoupled from LibGDX via `EnemyLookup` and `PathComputer` interfaces. Exposes `WaveStatus` (mutable, reused per frame) with wave state for the HUD.
- **`TilePathfinder`** — A* on tile grid (4-directional). Obstacles = structures + water + impassable terrain. Flying enemies skip pathfinding.
- **`TowerEntity.shoot()`** — Returns `boolean`; `attemptShot()` gates power/timer/sound on it returning `true`.

**Game flow:**
- **Build phase:** Countdown before each wave. Building allowed, wiring/selling always allowed.
- **Wave active:** Building disabled (`isBuildingAllowed()` returns false). Enemies spawn and pathfind.
- **Sell during wave:** Triggers repathing for all alive non-flying enemies.
- **Victory:** All waves exhausted + no alive enemies (gated by `!gameOver`). Shows overlay.
- **Defeat:** Enemy reaches base. Shows overlay. Music continues until player exits.

**Rendering (temporary)** — Spawn points: red circles. Base: green circle. Enemies: orange circles with health bars.

## Key Conventions

- Assets are defined as JSON files in `assets/json/{type}/` and loaded at startup via `JsonAssetLoader`.
- Game assets (textures, sounds, skin) live under `assets/` at the project root. The `assets/` directory is the working directory at runtime.
- Asset paths in code are relative to `assets/` (e.g., `"assets/visual/select-reticle.png"`).
- The Gradle wrapper is configured with Java source/target compatibility 8, but `KeyboardControls` uses `Set.of()` which requires Java 9+. This works at runtime on modern JVMs but would break a strict JDK 8 toolchain.

## Refactoring & Testing Guidelines

Performance rules, testing boundaries (what is/isn't unit testable), and mocking strategy are defined in `.claude/agents/shared-conventions.md` — the single source of truth referenced by all agents. Key points:

- **Performance-first:** No indirection on hot paths for testability. `GameAssetManager.INSTANCE` is intentional.
- **Testable:** Pure-logic methods (no `GameAssetManager`, no `Gdx.*` calls) — entity logic, `PowerGrid`, `NavigationUtils`, `EnemyInstance`, `WaveManager`, `TilePathfinder`.
- **Not testable:** `draw()` methods, `simulateShooting()`, `simulateEnemies()`, `isBuildable()`, all UI classes.
- **Mocking:** Use `TestAssetFactory` for mocked assets. Never add setters/constructors to production code for testing.

## Feature Development Workflow

New features are developed using a multi-agent pipeline invoked with the `/develop-feature` skill:

1. **`feature-planner`** (Opus) — reads the codebase, writes a spec to `docs/specs/YYYYMMDD-<feature-name>.md`, returns a summary
2. **`Manual gate`** — spec is printed inline for review; proceed only after approval
3. **`implementer`** (Opus) — follows the spec, explores the state machine before coding, checks off steps as it goes, compiles and runs tests before finishing
4. **`test-generator`** (Sonnet) — writes JUnit 5 + Mockito tests for all new pure-logic code
5. **`code-reviewer`** (Sonnet) — reviews correctness, performance, and convention adherence; loops back to the implementer if blockers are found
6. **`Manual testing & bug-squasher`** (interactive) — user manually tests the feature, reports bugs; **`bug-squasher`** (Opus) fixes bugs, writes regression tests, and the code-reviewer re-reviews. Loops until user confirms no more bugs.
7. **`Spec reconciliation`** — updates the spec with all bug fixes and behavioral adjustments so it matches the final implementation

Agents are defined in `.claude/agents/`. Shared conventions (UI state machine rules, performance rules, testing boundaries) are in `.claude/agents/shared-conventions.md` — this is the single source of truth referenced by all agents. The skill is defined in `.claude/skills/develop-feature/SKILL.md`.

A PostToolUse hook (`.claude/settings.json`) automatically runs `./gradlew core:test` after any Java source file is edited, catching regressions immediately.

Feature plans and design documents live in `docs/specs/`. Each spec follows the naming convention `YYYYMMDD-<feature-name>.md` and tracks the feature from design through implementation using checkboxes.

## Known Issues & Incomplete Areas

- **Level loading** is not implemented — `GameScreen` uses `populateGameStateWithDummyData()` instead.
- **Enemy rendering** is placeholder (colored circles via `ShapeRenderer`). No sprites/icons yet.
- **Enemies do not repath when structures are built.** Existing enemies keep their spawn-time path and may walk through new structures. They DO repath when structures are sold (implemented in `GameState.sellEntity()`). Future enemies always pathfind around current structures at spawn time.
