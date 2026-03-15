# Shared Conventions for Unstable Grid Agents

This file is the single source of truth for implementation and review conventions. All agents should read this file before starting work. Role-specific interpretation (e.g., "the spec must state X" vs "verify X is implemented") is in each agent's own prompt.

## UI State Machine Rules

These rules exist because past features shipped with broken state machines. They apply whenever a feature involves UI interactions, player action modes, or flag-based communication between UI and game loop.

### Rule 1: Flag lifecycle — set/read/clear ordering
When a UI callback sets a flag (e.g. `sellConfirmed = true`) that a per-frame method reads (e.g. `selectPlayerAction()`), verify that no intermediate call (like `hide()`, `reset()`, or `close()`) clears the flag before the reader sees it. Cleanup methods that blanket-reset state are the #1 source of flag lifecycle bugs. **Fix:** set the flag AFTER the cleanup call, or exclude it from blanket resets.

### Rule 2: Multi-frame action persistence
`selectPlayerAction()` runs every frame and unconditionally falls through to `detailsSelection` when no condition matches. One-shot flags are consumed on the first frame, so any action that persists across multiple frames (wiring, wire removal, etc.) needs persistent state — either a field that stays true until explicitly canceled, or a per-frame condition that keeps re-evaluating as true. One-shot flags alone will cause the action to last exactly one frame.

### Rule 3: Enumerate ALL mode exit conditions
For every player action mode, list every way the player can leave it: successful completion, right-click elsewhere, opening a context menu, selecting a build item, pressing Escape, etc. Missing exit conditions cause players to get stuck in modes. **Hard convention: right-click always cancels any persistent mode** (wiring, wire removal, etc.). Any new persistent mode must include a right-click exit path.

### Rule 4: Validate action preconditions
If an action requires targets to exist (e.g. "Remove Wire" needs outgoing conduits), the action must be hidden or disabled when preconditions are not met. Do not show actions that lead to stuck states.

### Rule 5: Mode conflicts
When the player is in one mode (wiring) and triggers a different mode (building, context menu), the old mode must be canceled. Entering any mode must cancel conflicting active modes.

### Rule 6: Duplicate/conflict edge cases
For any operation that creates a relationship (wire, link, connection), define what happens if the relationship already exists — ignore, replace, or error. Specify the scrap/resource implications of replacement.

### Rule 7: One-shot vs persistent actions
One-shot actions (e.g. building places one structure then exits build mode) must reset UI state after completion. Persistent actions (e.g. wire removal stays active until canceled) must have right-click cancel (Rule 3).

### Rule 8: Stuck-state prevention
If a UI flow opens a menu where all options may be disabled/unaffordable, the player must never get trapped — either prevent opening the menu, or ensure dismissing it cleanly resets all state (including one-shot flags that would otherwise block `selectPlayerAction()` fallthrough).

### Rule 9: New UI elements and popups
- Any new Stage-based UI must be registered in `isClickOnUI()` in `GameScreen`.
- Any popup menu must dismiss on click-outside via a stage-level `InputListener` that checks `menuTable.hit()` and calls `hide()`.
- **`hide()` vs `resetSelection()`:** When canceling a mode with a visible popup, call `hide()` (hides + resets state), not just `resetSelection()` (resets state only — leaves zombie popup visible).

### Rule 10: SpriteBatch color contamination
If code tints UI actors (e.g. `setColor(red)` for disabled buttons), `draw()` in `GameScreen` must reset `batch.setColor(Color.WHITE)` before `batch.begin()`. The shared batch does NOT reset color between frames.

### Rule 11: Tinting button styles — use the skin's button drawable, not "white"
When creating a tinted `TextButtonStyle` (e.g. a light-blue upgrade button or a red disabled button), tint the **existing skin drawable** — not the generic `"white"` drawable. `skin.newDrawable("white", color)` produces a flat rectangle that ignores the skin's nine-patch borders, making the button look visually different (transparent/missing edges). The correct pattern:
```java
TextButton.TextButtonStyle defaultStyle = skin.get(TextButton.TextButtonStyle.class);
TextButton.TextButtonStyle tinted = new TextButton.TextButtonStyle(defaultStyle);
tinted.up = skin.newDrawable(defaultStyle.up, myColor);
tinted.down = skin.newDrawable(defaultStyle.up, myColor);
```
This preserves the nine-patch shape of the original button while applying the tint.

## Performance Rules

- Do NOT introduce indirection (interfaces, callbacks, dependency injection) on hot paths (simulation, rendering) just for testability. These run at 60+ FPS.
- Static singletons like `GameAssetManager.INSTANCE` are intentional. Do not refactor them.
- No object allocation inside `simulate()` or `draw()` called every frame.
- `static final` constants are free — use `GameConstants` for shared values.

## Testing Boundaries

### Testable (pure logic — no GameAssetManager, no Gdx.* calls)
- Entity logic: `consume()`, `produce()`, `attemptShot(delta, enemies)`
- `PowerGrid.simulatePropagation()` (uses `java.util.logging`)
- `NavigationUtils` coordinate conversions
- `ConduitEntity.consume()` rate limiting and loss logic
- `EnemyInstance.move()`, `takeDamage()`, `getHealthFraction()` — pure math
- `TilePathfinder.findPath()` — A* on a 2D array
- `WaveManager.update()` — wave lifecycle, uses injected interfaces (`EnemyLookup`, `PathComputer`)
- Any new pure-logic methods

### NOT testable (requires LibGDX runtime)
- All `draw()` methods
- `GameState.simulateShooting()` (plays sounds)
- `GameState.simulateEnemies()` (orchestrates wave manager + enemy list inside GameState)
- `Building.isBuildable()` (terrain lookup)
- All UI classes (`BuildMenu`, `DetailsMenu`, `WiringMenu`, `ContextMenu`, `ScrapHud`)

### Mocking strategy
- Asset model classes have private fields, no setters. Use **Mockito mocks** via `TestAssetFactory`.
- Do NOT add setters or constructors to production code for testing.
- Use `GameConstants.TILE_SIZE_PX` for tile size in tests.

## Tower Targeting Convention

`TowerEntity.shoot(enemies, level)` returns `boolean` — `true` only when a target was found and damaged. `attemptShot()` gates power consumption and fire timer reset on `shoot()` returning `true`. This ensures towers never waste power, reset their fire timer, or trigger sound effects when no enemy is in range. Any change to the shooting pipeline must preserve this gate.

When testing `attemptShot()`, always provide an enemy within range if the test is about power or timing mechanics. Using `Collections.emptyList()` makes `attemptShot()` return `false` regardless of power/timer state, which silently tests the wrong condition.

## Entity & Asset Conventions

- `ConduitEntity` does NOT extend `GameEntity` (no tile position).
- New entities must be registered in both `GameState` entity sets AND `PowerGrid`.
- New `GridComponent` types must implement `resetPropagation()`.
- Asset model classes have no setters — populated by JSON deserialization only.
- New entity types need: JSON in `assets/json/{type}/`, a `GameAsset` subclass, and a `GameEntity` subclass.
- `EnemyInstance` is NOT a `GameEntity` — it uses world coordinates directly, has no tile position, and is not part of the power grid.
- `WaveManager` and `TilePathfinder` are decoupled from LibGDX via interfaces/constructor params. Keep them testable — do not add `GameAssetManager.INSTANCE` or `Gdx.*` calls.
- Coordinate conversions go through `NavigationUtils`.
- Java 8 source compatibility (exception: `KeyboardControls` uses `Set.of()`).
