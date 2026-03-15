# Wave System UI & Game Flow

**Date:** 2026-03-15
**Status:** Done
**Author:** feature-planner

## Requirements

**Feature: Wave System UI & Game Flow**

### Wave HUD (top-center)
- Countdown timer showing seconds until next wave starts (e.g. "Next wave in: 12s")
- Enemy count: alive on field + yet to spawn in current wave
- Remaining waves count (future waves not yet started)
- When all waves are done, still show enemy counts (alive/to-spawn) but indicate no more waves coming
- The HUD should be visible at all times during gameplay

### Build Restriction During Waves
- Structure placement disabled while a wave is active (from first spawn until last enemy of that wave dies)
- Wiring and selling remain allowed during waves
- Wave start: flash text center-screen -- "Enemies approaching!" with subtitle "Building is disabled"
- Wave end (last enemy dies): flash text -- "Wave cleared!" with subtitle "Building is enabled"
- The build menu items should be visually disabled/grayed out when building is restricted

### Initial Build Phase
- Add a configurable delay before wave 1 with countdown visible in the HUD
- Player gets time to build before enemies arrive
- This delay is already defined in the Level JSON asset as `waves[0].delay` (e.g. `{"wave": 1, "delay": 60.0}`)

### Enemy Repathing on Sell
- When a structure is sold during a wave, all alive enemies recalculate their paths using TilePathfinder
- This is needed because selling removes a structure, potentially opening new paths

### Victory Screen
- Triggers when all waves exhausted AND all enemies dead
- Big green "Victory" text centered on screen
- Single "Main Menu" button below the text
- Replaces/extends the current end-game flow

### Defeat Screen
- Triggers when any enemy reaches the base (existing gameOver logic)
- Big red "Defeated" text centered on screen
- Single "Main Menu" button below the text
- Replaces current instant-redirect to MainMenuScreen behavior

## Motivation

The game currently lacks core game flow elements: there is no visible wave countdown, no build phase indicator, no build restrictions during combat, no victory condition, and defeat immediately boots the player to the main menu. These omissions make the game feel incomplete and unplayable as a real tower defense experience. This feature adds the wave-phase lifecycle, communicates game state to the player, and provides proper end-game screens.

## Design

### 1. WaveManager state exposure

`WaveManager` already tracks `waveActive`, `waveTimer`, and `currentWaveNumber` internally, but exposes none of this to the outside. Rather than adding many getters, introduce a single `WaveStatus` data class that `WaveManager` populates each frame. This avoids coupling `GameScreen` to `WaveManager` internals and keeps the hot path clean (one method call, one object, no allocation -- reuse a mutable instance).

`WaveStatus` fields:
- `boolean waveActive` -- true when a wave is in progress
- `int currentWaveNumber` -- 1-indexed wave number currently active or about to start
- `int totalWaves` -- total number of waves in the level
- `float countdown` -- seconds until next wave starts (only meaningful when `!waveActive`)
- `boolean allWavesExhausted` -- true when all waves have been started (no more future waves)
- `int pendingSpawnCount` -- enemies yet to spawn in the active wave (from active SpawnTimers)

`WaveManager` will own a single `WaveStatus` instance and update it at the end of each `update()` call. `GameState` exposes it via `getWaveStatus()`.

### 2. Initial build phase

The initial build phase is already supported by the existing data model. The `delay` field on `waves[0]` (e.g. `"delay": 60.0`) controls the countdown before wave 1 starts. `WaveManager` already initializes `waveTimer = waves.get(0).getDelay()` in its constructor. No Level JSON or model changes are needed. The `WaveHud` will simply display this countdown to the player, making the existing invisible delay visible.

### 3. Wave HUD (WaveHud)

A new Scene2D UI class `WaveHud`, positioned top-center. It polls `GameState.getWaveStatus()` and `GameState.getAliveEnemyCount()` each frame in `handleInput()`. It displays:
- During build phase: "Next wave in: Xs" + "Wave N of M" + "Enemies: 0"
- During active wave: "Wave N of M -- ACTIVE" + "Enemies: A alive, S spawning"
- After all waves exhausted + enemies remain: "Final wave -- ACTIVE" + enemy counts
- After all waves exhausted + no enemies: "All waves cleared!"

Uses a single `Label` with multi-line text to keep implementation simple. Follows the `ScrapHud` pattern (own Stage + ScreenViewport). Must be registered in `isClickOnUI()` though it has no interactive elements (convention compliance).

Flash text (wave start/end announcements) will be a separate `Label` in the same `WaveHud` Stage, centered on screen, using `Actions.sequence(fadeIn, delay, fadeOut)` for the flash effect. This avoids creating a separate overlay system.

### 4. Build restriction during waves

**Approach:** `GameState` exposes `isBuildingAllowed()` which returns `!waveStatus.waveActive` (building allowed only between waves). `BuildMenu` queries this each frame in `handleInput()` and grays out ALL build buttons when building is restricted, plus ignores click events. `GameScreen.selectPlayerAction()` also checks `isBuildingAllowed()` before entering build mode, as a safety net.

**UI state machine analysis (Rules 1-10):**

- **Rule 1 (Flag lifecycle):** No new flags introduced for build restriction. It is a per-frame condition derived from `waveStatus.waveActive`. No set/read ordering concerns.
- **Rule 2 (Multi-frame persistence):** Build restriction persists as long as `waveActive` is true in `WaveStatus`. This is a per-frame condition, not a flag.
- **Rule 3 (Exit conditions):** Build mode already exits on successful placement or `resetSelection()`. With the new restriction, the build menu buttons simply do nothing when clicked during a wave, so the player cannot enter build mode at all. If the player was somehow in build mode when a wave starts (race condition: they selected a build item in the same frame the wave starts), `selectPlayerAction()` will refuse to enter build mode and fall through to `detailsSelection`. Explicit handling: if `buildAsset != null && !isBuildingAllowed()`, call `buildMenu.resetSelection()` and fall through.
- **Rule 4 (Preconditions):** Build buttons are visually disabled (grayed out) and non-interactive when building is restricted.
- **Rule 5 (Mode conflicts):** No new mode is introduced. Building is simply blocked.
- **Rule 7 (One-shot vs persistent):** Build restriction is neither -- it is a derived condition.
- **Rule 8 (Stuck-state prevention):** If all build buttons are disabled because a wave is active, the player is not stuck -- they can still wire, sell, and interact with existing structures. The build menu is visually present but all items are disabled.
- **Rule 9 (New UI elements):** `WaveHud` gets its own Stage, must be added to `isClickOnUI()` and the `InputMultiplexer` in `show()`. `GameOverOverlay` likewise.
- **Rule 10 (SpriteBatch color):** Build button tinting uses `setColor()` on actors (existing pattern in `handleInput()`), not on the batch. The existing `batch.setColor(Color.WHITE)` reset in `draw()` already covers this.

### 5. Enemy repathing on sell

When `GameState.sellEntity()` is called, after removing the entity from `entityByPosition`, iterate over all alive `EnemyInstance` objects and recompute their paths. `EnemyInstance` needs a `repath(List<Vector2> newPath)` method that replaces the current path with a new one computed from the enemy's current tile position to the base.

Key details:
- Flying enemies skip repathing (path is always `[current, base]`).
- The enemy's current tile is derived from `worldPosition` divided by `TILE_SIZE_PX`.
- If `findPath()` returns `null` (no path exists), keep the old path. The enemy will continue on its current trajectory. This handles the edge case where selling a structure does not actually create a valid path.
- `repath()` resets `pathIndex` to 0 and sets the first waypoint to the enemy's current world position (so the enemy does not teleport).

### 6. Victory condition

`GameState` gains a `boolean victory` field. In `simulateEnemies()`, after processing deaths/removals, check: if `waveManager != null && waveStatus.allWavesExhausted && aliveCount == 0 && no pending spawns`, set `victory = true`. Expose via `isVictory()`.

The alive count check already exists in `simulateEnemies()` as a local variable. The `allWavesExhausted` flag comes from `WaveStatus`. The "no pending spawns" condition is implied by `allWavesExhausted` + `activeSpawnTimers.isEmpty()` inside `WaveManager` (which is what sets `allWavesExhausted`).

### 7. Victory and Defeat screens (GameOverOverlay)

Rather than separate Screen classes (which would require disposing the entire GameScreen), implement an overlay approach: a new `GameOverOverlay` UI class that renders on top of the game. When `gameState.isGameOver()` or `gameState.isVictory()` is true, `GameScreen` stops simulation and input processing, shows the overlay, and waits for the "Main Menu" button click.

The overlay consists of:
- A semi-transparent black background covering the full viewport (a tinted full-screen `Image` actor)
- A large colored label ("Victory" in green or "Defeated" in red)
- A "Main Menu" `TextButton`

This replaces the current `render()` logic that instantly redirects to `MainMenuScreen` on `gameOver`. Instead, `GameScreen.render()` will check for game-over/victory AFTER drawing, show the overlay, and only transition on button click.

**UI state machine impact:**
- **Rule 9:** The overlay has its own Stage, must be registered in `isClickOnUI()` and the InputMultiplexer.
- When the overlay is visible, all game input is blocked (no `readInputs()`, no `selectPlayerAction()`). Only `draw()` and the overlay's own Stage process input.
- The overlay's "Main Menu" button calls `game.setScreen(new MainMenuScreen(game))` and `dispose()` on GameScreen.

### 8. GameScreen render flow changes

Current flow:
```
readInputs -> updateCamera -> simulate -> [gameOver? -> redirect] -> selectPlayerAction -> draw
```

New flow:
```
if overlayActive:
    draw (frozen game state) + overlay.draw
    overlay.handleInput
    return
readInputs -> updateCamera -> simulate -> selectPlayerAction -> draw
if gameOver or victory:
    show overlay, set overlayActive = true
```

The check happens AFTER draw so the player sees the final frame (enemy reaching base, or last enemy dying) before the overlay appears. `overlayActive` is a boolean field on `GameScreen` that, once true, short-circuits the render loop to only draw + handle overlay input.

## Files to Modify

- `core/src/main/java/com/skamaniak/ugfs/game/WaveManager.java` -- Add `WaveStatus` inner class, populate it each frame in `update()`, expose `getWaveStatus()`. Track `pendingSpawnCount` from active SpawnTimers.
- `core/src/main/java/com/skamaniak/ugfs/game/GameState.java` -- Expose `getWaveStatus()`, `isBuildingAllowed()`, `isVictory()`, `getAliveEnemyCount()`. Add victory detection in `simulateEnemies()`. Change `sellEntity()` to trigger repathing on all alive enemies after entity removal.
- `core/src/main/java/com/skamaniak/ugfs/game/entity/EnemyInstance.java` -- Add `repath(List<Vector2> newPath)` method. Add `isFlying()` accessor delegating to `enemy.isFlying()`.
- `core/src/main/java/com/skamaniak/ugfs/GameScreen.java` -- Integrate `WaveHud` and `GameOverOverlay`. Change `render()` to show overlay instead of instant redirect on game-over/victory. Block simulation and input when overlay is active. Add build restriction check in `selectPlayerAction()`. Register new UI stages in `isClickOnUI()`, `InputMultiplexer`, `resize()`, `dispose()`.
- `core/src/main/java/com/skamaniak/ugfs/ui/BuildMenu.java` -- In `handleInput()`, when `!gameState.isBuildingAllowed()`, gray out all buttons and ignore clicks. When building becomes allowed again, restore normal tinting.

## Files to Create

- `core/src/main/java/com/skamaniak/ugfs/ui/WaveHud.java` -- Wave countdown, enemy counts, and flash text overlay. Follows the `ScrapHud` pattern (own Stage, ScreenViewport, polls GameState each frame).
- `core/src/main/java/com/skamaniak/ugfs/ui/GameOverOverlay.java` -- Victory/defeat overlay with semi-transparent background, large status label, and Main Menu button.

## Implementation Steps

- [x] 1. Add `WaveStatus` inner class to `WaveManager` with fields: `waveActive`, `currentWaveNumber`, `totalWaves`, `countdown`, `allWavesExhausted`, `pendingSpawnCount`
- [x] 2. Update `WaveManager.update()` to populate `WaveStatus` at end of each call (reuse mutable instance). Track `pendingSpawnCount` from `activeSpawnTimers`
- [x] 3. Expose `WaveManager.getWaveStatus()` returning the reusable `WaveStatus` instance
- [x] 4. Add `GameState.getWaveStatus()`, `GameState.isBuildingAllowed()`, `GameState.getAliveEnemyCount()`
- [x] 5. Add `EnemyInstance.repath(List<Vector2> newPath)` -- replaces path from current position, resets pathIndex
- [x] 6. Add `EnemyInstance.isFlying()` delegating to `enemy.isFlying()`
- [x] 7. Update `GameState.sellEntity()` to trigger repathing for all alive non-flying enemies after entity removal
- [x] 8. Add victory detection in `GameState.simulateEnemies()` -- set `victory = true` when all waves exhausted and no alive enemies
- [x] 9. Add `GameState.isVictory()` getter
- [x] 10. Create `WaveHud.java` -- Labels for wave info + flash text with fade animations. Detect wave transitions by comparing previous `waveActive` state to trigger flash text
- [x] 11. Create `GameOverOverlay.java` -- Semi-transparent overlay with victory/defeat label and Main Menu button. Accepts `UnstableGrid` game reference and `GameScreen` for disposal
- [x] 12. Update `BuildMenu.handleInput()` to gray out and block all buttons when `!gameState.isBuildingAllowed()`
- [x] 13. Update `GameScreen.selectPlayerAction()` to check `isBuildingAllowed()` before entering build mode; if not allowed and buildAsset is selected, call `buildMenu.resetSelection()`
- [x] 14. Integrate `WaveHud` into `GameScreen` -- construction, draw, handleInput, resize, dispose, InputMultiplexer, isClickOnUI
- [x] 15. Integrate `GameOverOverlay` into `GameScreen` -- construction, draw, InputMultiplexer, isClickOnUI, resize, dispose
- [x] 16. Change `GameScreen.render()`: add `overlayActive` boolean. When active, skip readInputs/simulate/selectPlayerAction, only draw + overlay. After draw, check `isGameOver()` or `isVictory()` to activate overlay
- [x] 17. Compile and run `./gradlew core:test` to verify no regressions

## Testing Plan

### Unit testable (pure logic)

- **`WaveStatus` population:** Test `WaveManager.update()` produces correct `WaveStatus` values across the full wave lifecycle: initial build phase countdown, wave start, mid-wave (pending spawns), wave end, inter-wave delay, all waves exhausted. Use existing `WaveManager` test setup with mock `EnemyLookup` and `PathComputer`.
- **`pendingSpawnCount`:** Verify it decreases as enemies spawn within a wave.
- **`allWavesExhausted`:** Verify it becomes true only after the last wave ends.
- **`EnemyInstance.repath()`:** Test that calling `repath()` with a new path replaces the old path and resets movement. Verify the enemy continues moving from its current position along the new path. Verify that a `null` path argument is handled gracefully (old path kept).
- **`EnemyInstance.isFlying()`:** Trivial delegation test.

### Implemented tests

**`WaveManagerStatusTest`** (`core/src/test/java/com/skamaniak/ugfs/game/WaveManagerStatusTest.java`) — 19 tests covering:
- `waveActive` is false during the initial build-phase countdown and true once a wave starts, false again after wave ends.
- `currentWaveNumber` is 0 before any wave starts; increments to 1 when wave 1 starts.
- `countdown` decreases by the elapsed delta during the build phase.
- `totalWaves` reflects the size of the configured wave list.
- `allWavesExhausted` is false during build phase, false while a wave is active, false when a second wave is still pending, true after the only wave ends, true after the last of two waves ends, true immediately when no waves are configured at all.
- `pendingSpawnCount` equals remaining enemies in the active `SpawnTimer`(s), decreases as each enemy spawns, is zero after all enemies in a wave have spawned, is zero between waves, and is summed across multiple active spawn timers.
- `getWaveStatus()` returns the same mutable instance on every call (no per-frame allocation).

**`EnemyInstanceRepathTest`** (`core/src/test/java/com/skamaniak/ugfs/game/entity/EnemyInstanceRepathTest.java`) — 10 tests covering:
- `isFlying()` returns false for a ground enemy and true for a flying enemy (pure delegation to the `Enemy` asset mock).
- `repath(null)` is a no-op: old path is kept, no exception thrown.
- `repath(newPath)` replaces the old path: enemy moves along the new path direction after repathing.
- `repath(newPath)` resets `pathIndex` to 0 so the enemy traverses the whole new path.
- `repath(newPath)` overwrites `newPath[0]` with the enemy's current world position, so the enemy does not teleport.
- `repath(newPath)` does not alter `worldPosition`: enemy's position is unchanged by the call itself.
- `repath(emptyList)` does not throw and leaves the enemy stationary (move does nothing on an empty path).

### Requires LibGDX (not unit tested)

- `WaveHud` (Scene2D labels, Actions for flash text)
- `GameOverOverlay` (Scene2D Stage, semi-transparent background rendering)
- `BuildMenu` tinting changes (actor color manipulation)
- `GameScreen.render()` flow changes (screen transitions, overlay display)
- Flash text fade animations
- Victory/defeat detection integration in `GameState.simulateEnemies()` (calls `GameAssetManager` indirectly via `waveManager` setup)

### Manual test scenarios

1. **Initial build phase:** Start a new game. Verify the Wave HUD shows "Next wave in: 60s" (matching the tutorial level's wave 1 delay of 60.0) counting down. Build structures during this phase. Confirm building works normally. Wait for countdown to reach 0.

2. **Wave start flash and build restriction:** When countdown reaches 0, verify flash text "Enemies approaching!" / "Building is disabled" appears center-screen and fades out. Verify all build menu buttons become grayed out. Click a build button -- confirm nothing happens (no build mode entered).

3. **Wave HUD during active wave:** During an active wave, verify the HUD shows the current wave number, alive enemy count, and pending spawn count. Confirm counts update as enemies spawn and die.

4. **Wiring and selling during wave:** During an active wave, right-click a structure. Verify Wire, Remove Wire, and Sell options work normally. Confirm selling a structure triggers enemy repathing (enemies visibly change direction if the sold structure was blocking a shorter path).

5. **Wave end flash and build re-enabled:** Kill all enemies in a wave. Verify flash text "Wave cleared!" / "Building is enabled" appears. Verify build menu buttons return to normal coloring. Confirm building works again.

6. **Inter-wave countdown:** After clearing a wave, verify the HUD shows countdown to the next wave. Confirm building is allowed during this period.

7. **Final wave exhausted:** After the last wave's enemies are all dead, verify the HUD indicates no more waves. If enemies are still alive from the last wave, confirm the HUD still shows enemy counts.

8. **Victory screen:** Kill all enemies from all waves. Verify a semi-transparent overlay appears with green "Victory" text and a "Main Menu" button. Verify the game world is still visible behind the overlay but simulation is stopped. Click "Main Menu" -- confirm transition to main menu screen.

9. **Defeat screen:** Let an enemy reach the base tile. Verify a semi-transparent overlay appears with red "Defeated" text and a "Main Menu" button (instead of instant redirect). Click "Main Menu" -- confirm transition to main menu screen.

10. **Sell repathing -- no valid path:** Sell a structure that, when removed, does not actually change any paths. Verify enemies continue on their current paths without issues.

11. **Build restriction edge case -- selecting build item on wave start frame:** Select a build item from the menu. If a wave starts on the same frame (unlikely but possible), verify the build selection is reset and the player is returned to details selection mode.

12. **All build items disabled during wave (stuck-state check):** During an active wave, verify the build menu is visible but all items grayed out. Verify the player can interact with other UI (details, context menu, wiring, selling) and is not stuck.

## Risks & Trade-offs

- **Repathing cost on sell:** Repathing all alive enemies when selling a structure involves running A* for each alive enemy. For small enemy counts (<50) this is negligible. If enemy counts grow large, this could cause a frame hitch. Mitigation: the operation only happens on sell (player-initiated, infrequent), not every frame. If it becomes a problem, batching or deferred repathing could be added later.

- **WaveStatus mutable reuse:** Using a single mutable `WaveStatus` instance avoids allocation but means the caller must not store references across frames. This is fine because `WaveHud` reads it fresh each frame and `GameScreen` only reads it in `selectPlayerAction()` within the same frame.

- **Flash text timing:** The flash text uses Scene2D Actions for fade-in/fade-out. If multiple wave transitions happen rapidly (e.g., a wave with only 1 enemy that dies instantly), flash messages could overlap. Mitigation: clear any active flash before starting a new one.

- **Build restriction granularity:** Building is disabled for the entire wave duration, not just when enemies are near. This is a deliberate design choice that creates distinct build vs. combat phases, but some players may find it restrictive. This matches the requirements as specified.

## Open Questions

1. Should the defeat overlay stop music immediately, or let it continue until the player clicks Main Menu? Currently, music is stopped in `dispose()`.

2. Should enemies that are currently mid-tile when a sell triggers repathing snap to the nearest tile center, or should the new path start from their exact world position? The spec currently proposes starting from exact position with the first waypoint set to current position, which avoids visual snapping.
