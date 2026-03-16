# QoL Bundle: Persistent Building, Wiring Indicators, Wire Overlay

**Date:** 2026-03-16
**Status:** Complete
**Author:** feature-planner

## Requirements

Paste of the original brief:

**1. Persistent Building Mode**
- Building mode no longer cancels after placing one structure. Player can place multiple structures of the same type in a row.
- Exits only on: right-click, pressing Escape, selecting a different build item, or other existing exit means (sell confirm, wire removal request, etc.).
- Still respects all existing restrictions: wave-active disables building, insufficient scrap prevents placement (but stays in build mode — doesn't exit).
- When the player doesn't have enough scrap for another placement, the build mode stays active but shows the invalid placement overlay (same as placing on an invalid tile).

**2. Wiring Target Indicators**
- When in wiring mode, show filled circles (same style as wire-removal highlights, which are orange circles at tile center with radius TILE_SIZE_PX/2) on ALL valid PowerConsumer targets within range — including already-connected ones (wiring to an already-connected target replaces the existing wire).
- Use a distinct color from wire-removal highlights (wire removal uses orange RGBA(1, 0.5, 0, 0.5)). Suggest green or blue for wiring targets.
- When in wiring or wire-removal mode, also show the wire overlay (feature 3) automatically for the duration of that action.

**3. Wire Overlay Toggle**
- A UI button that toggles wire overlay on/off. Persists across frames.
- **Overlay ON:** Thicker lines, full opacity, color-coded by conduit type, percentage transfer labels shown. Wires and their connections should be clearly traceable.
- **Overlay OFF (default):** Thin semi-transparent lines, no percentage labels. Wires are visible but subtle/unobtrusive.
- Automatically forced ON when entering wiring or wire-removal mode, reverts to user's toggle state when exiting those modes.
- The button should be placed in a sensible location in the existing UI layout.

**4. Scrap Cost Preview During Building**
- While in building mode, show the scrap cost of the structure near the cursor or as part of the placement overlay, so the player doesn't need to look away at the build menu.

**5. Wire Count Badge on Structures**
- When wire overlay is ON, show a small count of connected wires on each structure that has wires (both incoming and outgoing).

## Motivation

These five features collectively improve the player experience during the most common interactions — building structures and managing the power grid. Persistent building eliminates tedious re-clicking for bulk placement. Wiring indicators and the wire overlay make the power grid topology visible and understandable at a glance. The scrap cost preview reduces cognitive load during placement. Together they reduce UI friction without changing any game mechanics.

## Design

### Feature 1: Persistent Building Mode

**Current behavior:** `Building.handleClick()` calls `build.accept(position, assetToBuild)`, which invokes `GameScreen.buildGameObject()`. On success, `buildGameObject()` calls `buildMenu.resetSelection()`, which sets `selectedAsset = null`. On the next frame, `selectPlayerAction()` sees no selected asset and falls through to `detailsSelection`. On failure (insufficient scrap), `buildGameObject()` also calls `resetSelection()`.

**New behavior:** `buildGameObject()` no longer calls `buildMenu.resetSelection()` after placement. Instead, the build mode persists as long as `buildMenu.getSelectedAsset()` returns non-null. The player stays in build mode after each placement.

**Insufficient scrap handling:** `Building.handleClick()` checks `gameState.getScrap() < assetToBuild.getBuildCost()` before calling `build.accept()`. If insufficient, the click is silently ignored (no placement, no exit). The `handleMouseMove()` overlay already shows invalid placement when `isBuildable()` returns false; we extend `isBuildable()` (or add a parallel check in `handleMouseMove()`) to also return false / show invalid overlay when scrap is insufficient.

**Exit conditions (Rule 3):**
- Right-click: `selectPlayerAction()` checks `playerInput.justClicked(Input.Buttons.RIGHT)` before entering build mode; if true, calls `buildMenu.resetSelection()` and falls through.
- Escape: same mechanism via `playerInput.isPressed(KeyboardControls.ESCAPE)` in `readInputs()` or `selectPlayerAction()`.
- Selecting a different build item: `BuildMenu` already handles this — clicking a new item calls `resetSelection()` then sets the new asset.
- Context menu open: `ContextMenu.openMenu()` already calls `buildMenu.resetSelection()`.
- Sell confirm / wire removal request / wiring request: these one-shot paths in `selectPlayerAction()` already take priority and reset to `detailsSelection`.
- Wave starts: `selectPlayerAction()` already checks `gameState.isBuildingAllowed()` and calls `buildMenu.resetSelection()` if false.

**Mode type (Rule 7):** Persistent. Right-click cancel is included.

**Flag lifecycle (Rule 1):** No new flags. Build mode persists via `buildMenu.getSelectedAsset() != null`, which is already the existing per-frame condition. The only change is removing the `resetSelection()` call in `buildGameObject()`.

**Multi-frame persistence (Rule 2):** `buildMenu.selectedAsset` stays non-null until explicitly cleared by one of the exit conditions. This is the existing persistence mechanism — we are just no longer clearing it on successful build.

**Mode conflicts (Rule 5):** Entering build mode already cancels wiring (`wiringMenu.resetSelection()` at line 298 of `selectPlayerAction()`). Context menu open cancels building. No new conflicts.

### Feature 2: Wiring Target Indicators

**Current behavior:** `Wiring.drawShapes()` draws a green semi-transparent range circle and a line from source to cursor (green if in range, red if out of range). No target indicators.

**New behavior:** In `Wiring.drawShapes()`, after drawing the range circle, iterate over all `GameEntity` instances in `GameState` that implement `PowerConsumer`, check if they are within conduit range of the source (and are not the source itself), and draw a filled circle at their tile center. Color: cyan `RGBA(0, 0.8, 1, 0.5)` — distinct from wire-removal orange `RGBA(1, 0.5, 0, 0.5)`.

The `Wiring` class already has access to `gameState` and the `isWithinRange(target)` method. We need a way to get all entities. Add `GameState.getAllEntities()` that returns an `Iterable<GameEntity>` (backed by a pre-allocated list or chained iteration over generators, storages, towers). This method is called per-frame during draw but only when in wiring mode (not a hot path concern — wiring mode is player-initiated and brief).

**Already-connected targets (Rule 6):** Wiring to an already-connected target replaces the wire — this is already implemented in `GameState.registerLink()`. The indicator shows all valid targets regardless of existing connections.

### Feature 3: Wire Overlay Toggle

**Wire rendering modes:** `ConduitEntity.draw()` currently renders all wires the same way — full opacity with percentage labels. We introduce a `WireDisplayMode` concept: `SUBTLE` (default) and `DETAILED`.

- **SUBTLE:** Render the wire at reduced opacity (alpha 0.3), reduced thickness (half of `conduit.getLineThickness()`), no percentage labels.
- **DETAILED:** Render as currently done — full opacity, normal thickness, with percentage labels. Additionally, add color tinting based on conduit type (using the conduit's texture color or a color field).

**Implementation approach:** Add a `static boolean wireOverlayOn` field to a shared location. Rather than adding parameters to `ConduitEntity.draw()` (which would require changing the `Drawable` interface and all implementations), we use a static field in `GameConstants` — `GameConstants.wireOverlayDetailed`. This is a simple boolean read on the render path, zero overhead.

**Auto-force during wiring/wire-removal:** `GameScreen` tracks `boolean wireOverlayForcedOn`. When entering wiring or wire-removal mode (detected in `selectPlayerAction()`), set `wireOverlayForcedOn = true` and `GameConstants.wireOverlayDetailed = true`. When exiting those modes (falling through to build or detailsSelection), clear `wireOverlayForcedOn` and restore `GameConstants.wireOverlayDetailed` to the user's toggle value (`wireOverlayUserToggle`).

**UI button:** A single `TextButton` ("Wires: ON" / "Wires: OFF") placed in the top-right area of the screen, near the scrap HUD. This is a standalone button, not a popup — no dismiss-on-click-outside needed. It toggles `wireOverlayUserToggle` and (if not forced) updates `GameConstants.wireOverlayDetailed`.

**Rule 9 (isClickOnUI):** The new button's Stage must be registered in `isClickOnUI()`.

**Rule 14 (Stage disposal):** The new `WireOverlayButton` must be disposed in `GameScreen.dispose()`.

### Feature 4: Scrap Cost Preview During Building

**Current behavior:** `Building.drawTextures()` draws the valid/invalid selection overlay at the cursor tile. No cost info.

**New behavior:** After drawing the selection overlay, also draw a small text label showing the scrap cost of `assetToBuild`. Use `GameAssetManager.INSTANCE.getFont().draw()` to render `"-" + assetToBuild.getBuildCost()` slightly below the cursor tile (at `y - 4`). If placement is invalid for any reason (unbuildable tile, occupied tile, or insufficient scrap), render in red; otherwise in white. The color is derived from the cached `texture` field (which contains "invalid" for all invalid scenarios).

This is purely a rendering addition to `Building.drawTextures()`. No state machine changes.

### Feature 5: Wire Count Badge on Structures

**Current behavior:** `GameState.drawGameEntities()` draws entities but no wire count.

**New behavior:** When `GameConstants.wireOverlayDetailed` is true, after drawing each entity, count the number of conduits connected to it (using `findConnectedConduits()` or a cached count) and draw a small badge.

**Performance consideration:** `findConnectedConduits()` iterates all conduits for each entity, giving O(entities * conduits) per frame. For typical game sizes (tens of entities, tens of conduits), this is negligible. If it becomes a concern, we can cache wire counts and invalidate on conduit add/remove — but premature optimization is unnecessary here.

**Rendering:** Use `GameAssetManager.INSTANCE.getFont().draw()` to render the count at the top-left corner of the entity tile. Only draw when count > 0. Format: small number like "3" in a contrasting color (white with dark outline, or yellow).

## Files to Modify

- **`GameScreen.java`** — Remove `buildMenu.resetSelection()` from `buildGameObject()`. Add right-click/Escape exit for build mode in `selectPlayerAction()`. Add `wireOverlayUserToggle` and `wireOverlayForcedOn` fields. Track overlay force-on in `selectPlayerAction()`. Create and wire `WireOverlayButton`. Register new Stage in `isClickOnUI()`, `InputMultiplexer`, `resize()`, `dispose()`.
- **`PlayerActionFactory.java` (Building)** — Add scrap-insufficient check in `handleClick()`. Add scrap check to `handleMouseMove()` / `isBuildable()` for overlay. Add cost label rendering in `drawTextures()`. Add `GameAsset.getBuildCost()` access (already available via `assetToBuild`).
- **`PlayerActionFactory.java` (Wiring)** — Add target indicator rendering in `drawShapes()`. Need access to all entities (via `gameState`).
- **`ConduitEntity.java`** — Read `GameConstants.wireOverlayDetailed` in `draw()` / `drawLine()`. Adjust opacity, thickness, and label visibility based on mode.
- **`GameConstants.java`** — Add `public static boolean wireOverlayDetailed = false;` field.
- **`GameState.java`** — Add `getAllEntities()` method for wiring target iteration. Add wire count badge rendering in `drawGameEntities()` when overlay is detailed. Add `getConduits()` accessor if needed for wire count.

## Files to Create

- **`core/src/main/java/com/skamaniak/ugfs/ui/WireOverlayButton.java`** — Standalone Scene2D button that toggles `wireOverlayUserToggle` and updates `GameConstants.wireOverlayDetailed`. Has its own `Stage`, `ScreenViewport`. Follows the same pattern as `ScrapHud`.

## Implementation Steps

### Feature 1: Persistent Building Mode
- [x] In `GameScreen.buildGameObject()`, remove both `buildMenu.resetSelection()` calls (success and failure paths). On scrap failure, just return without placing — do not exit build mode.
- [x] In `Building.handleClick()`, add a scrap check before calling `build.accept()`: if `gameState.getScrap() < assetToBuild.getBuildCost()`, return early without placing.
- [x] In `Building.handleMouseMove()`, treat insufficient scrap as invalid placement (show `invalid-selection.png`). Modify the condition: `if (isBuildable(mousePosition) && gameState.getScrap() >= assetToBuild.getBuildCost())`.
- [x] In `selectPlayerAction()`, before entering build mode, check for right-click: `if (playerInput.justClicked(Input.Buttons.RIGHT)) { buildMenu.resetSelection(); }` — add this check before the `buildAsset != null` block, or inside it.
- [x] Add Escape key handling: in `readInputs()` or `selectPlayerAction()`, if Escape is pressed and build mode is active, call `buildMenu.resetSelection()`.
- [x] Add `ESCAPE` to `KeyboardControls` if not already present.

### Feature 3: Wire Overlay Toggle (implement before Feature 2 since Feature 2 depends on it)
- [x] Add `public static boolean wireOverlayDetailed = false;` to `GameConstants`.
- [x] Create `WireOverlayButton.java` with a toggle button. On click, flip `wireOverlayDetailed` and update button text. Expose `Stage getStage()`, `void draw()`, `void handleInput()`, `void resize()`, `void dispose()`, and `boolean isUserToggleOn()`.
- [x] In `GameScreen`, instantiate `WireOverlayButton`. Add to `InputMultiplexer`, `isClickOnUI()`, `draw()`, `resize()`, `dispose()`, `handleInput()` (in `readInputs()`).
- [x] Add `boolean wireOverlayForcedOn` field to `GameScreen`. User toggle state is tracked inside `WireOverlayButton.isUserToggleOn()` instead of a separate `wireOverlayUserToggle` field — single source of truth.
- [x] In `selectPlayerAction()`, set `wireOverlayForcedOn = true` and `GameConstants.wireOverlayDetailed = true` when entering wiring or wire-removal mode. When falling through to build mode or detailsSelection, set `wireOverlayForcedOn = false` and `GameConstants.wireOverlayDetailed = wireOverlayUserToggle`.
- [x] Modify `ConduitEntity.draw()` / `drawLine()`: when `!GameConstants.wireOverlayDetailed`, use half thickness, `batch.setColor(1, 1, 1, 0.3f)` before drawing, and skip the percentage label. Restore `batch.setColor(Color.WHITE)` after drawing.

### Feature 2: Wiring Target Indicators
- [x] Expose `GameState.getStorages()`, `getTowers()`, `getGenerators()` accessors. Instead of a combined `getAllEntities()`, the wiring target loop iterates storages and towers directly (the only `PowerConsumer` types), avoiding unnecessary allocation.
- [x] In `Wiring.drawShapes()`, after the range circle, iterate `gameState.getAllEntities()`. For each entity that is a `PowerConsumer`, is not the source, and is within range, draw a filled circle at tile center with color `RGBA(0, 0.8, 1, 0.5)` and radius `TILE_SIZE_PX / 2`.
- [x] Wire overlay auto-force is handled by Feature 3 implementation above.

### Feature 4: Scrap Cost Preview During Building
- [x] In `Building.drawTextures()`, after drawing the selection overlay, draw the cost text. Use `GameAssetManager.INSTANCE.getFont()`. Position: tile-aligned X + small offset, tile-aligned Y - 4. Text: `"-" + assetToBuild.getBuildCost()`. Color is red when placement is invalid (uses the cached `texture` field containing "invalid"), white otherwise. Reset color after drawing.
- [x] `Building` needs access to `gameState.getScrap()` — it already has a `GameState` reference.

### Feature 5: Wire Count Badge on Structures
- [x] In `GameState.drawGameEntities()`, after drawing each entity type (generators, storages, towers), if `GameConstants.wireOverlayDetailed`, count connected conduits and draw the count at the entity's tile top-left corner using `getFont().draw()`. Only draw if count > 0.
- [x] Alternatively, compute wire counts for all entities in a single pass over conduits before drawing, storing in a temporary `Map<GameEntity, Integer>` (allocated once as a field, cleared each frame). This avoids O(entities * conduits) nested iteration.

## Bug Fixes

### Bug 1: Scrap cost text stays white on unbuildable tiles
**Symptom:** When in building mode with sufficient scrap, hovering over an unbuildable tile (e.g. water) showed the red invalid placement overlay correctly, but the scrap cost text stayed white instead of turning red.
**Root cause:** `Building.drawTextures()` only checked `gameState.getScrap() < cost` for the cost label color, ignoring tile buildability.
**Fix:** Changed the color condition to check `texture.contains("invalid")` instead. The `texture` field is already set by `handleMouseMove()` to `"invalid-selection.png"` for all invalid placement scenarios (unbuildable terrain, occupied tile, or insufficient scrap), so the cost label color now correctly matches the overlay in all cases.
**File:** `PlayerActionFactory.java` (Building.drawTextures, line ~201)


## Testing Plan

### Unit testable (pure logic)

Almost nothing in this bundle is pure logic. The changes are to rendering (`draw()` methods), UI elements (`WireOverlayButton`), and state machine transitions (`selectPlayerAction()`). The only potentially testable addition is `GameState.getAllEntities()` — but it is trivial (returns a collection) and testing it would require mocking the full `GameState` constructor which depends on LibGDX.

### Requires LibGDX (not unit tested)

- All rendering changes (wire overlay modes, target indicators, cost preview, wire count badges)
- `WireOverlayButton` (Scene2D Stage-based UI)
- State machine changes in `selectPlayerAction()`
- `Building.handleClick()` scrap check (depends on `GameState` which depends on LibGDX)

### Manual test scenarios

**Persistent Building:**
1. Select a generator from the build menu. Place it on a valid tile. Verify build mode stays active (cursor still shows placement overlay). Place a second generator. Verify both are placed and build mode persists.
2. While in build mode, right-click on empty space. Verify build mode exits (cursor returns to selection reticle on click).
3. While in build mode, press Escape. Verify build mode exits.
4. While in build mode, select a different build item. Verify the new item is now the active build type.
5. While in build mode with insufficient scrap for another placement, verify the overlay shows invalid (red tint) on all tiles. Click a valid tile. Verify nothing is placed and build mode persists. Earn scrap (e.g., from enemy kill). Verify overlay returns to valid on buildable tiles.
6. While in build mode, right-click on an existing structure to open context menu. Verify build mode exits and context menu appears.
7. Start a wave while in build mode. Verify build mode exits (existing behavior).
8. Place structures until scrap hits exactly the build cost. Place one more. Verify it places and build mode persists but now shows invalid overlay (0 scrap remaining).

**Wiring Target Indicators:**
9. Right-click a generator, click Wire, select a conduit. Verify cyan circles appear on all PowerConsumer entities within range. Verify no circle on the source itself. Verify no circle on out-of-range entities.
10. While in wiring mode, verify the wire overlay is automatically in detailed mode (thick wires, percentage labels visible).
11. Click a valid target (cyan circle). Verify wire is created and mode exits. Verify wire overlay reverts to user's toggle state.
12. While in wiring mode, verify that targets which already have a wire from this source still show cyan circles (replacement is valid).

**Wire Overlay Toggle:**
13. At game start, verify wires are rendered in subtle mode (thin, semi-transparent, no percentage labels).
14. Click the wire overlay toggle button. Verify wires switch to detailed mode (thick, full opacity, percentage labels).
15. Click the toggle button again. Verify wires return to subtle mode.
16. Toggle overlay ON. Enter wiring mode. Exit wiring mode (right-click). Verify overlay stays ON (user's toggle was ON).
17. Toggle overlay OFF. Enter wiring mode. Verify overlay is forced ON. Exit wiring mode. Verify overlay returns to OFF.
18. Toggle overlay OFF. Enter wire-removal mode. Verify overlay is forced ON. Complete or cancel wire removal. Verify overlay returns to OFF.

**Scrap Cost Preview:**
19. Enter build mode with a structure costing 50 scrap. Move cursor over the map. Verify "-50" text appears near the cursor tile.
20. With insufficient scrap, verify the cost text appears in red.
21. With sufficient scrap, verify the cost text appears in white.

**Wire Count Badge:**
22. Toggle wire overlay ON. Verify structures with wires show a count badge (number at top-left of tile). Verify structures with no wires show no badge.
23. Add a wire to a structure. Verify the badge count increases.
24. Remove a wire. Verify the badge count decreases.
25. Toggle wire overlay OFF. Verify badges disappear.

## Risks & Trade-offs

1. **`GameConstants.wireOverlayDetailed` is a mutable static field.** This is intentional to avoid passing parameters through the draw pipeline. It follows the project's convention of using static singletons on hot paths. The risk is that it creates implicit global state, but the alternatives (passing a flag through every `draw()` call, or wrapping in an interface) violate the project's performance-first conventions.

2. **Wire count per frame.** Computing wire counts by iterating conduits each frame is O(conduits) per frame. For typical game sizes this is negligible, but if conduit counts grow large, a cached count (invalidated on conduit add/remove) would be needed.

3. **SpriteBatch color contamination in subtle wire mode.** Setting `batch.setColor()` with alpha 0.3 for subtle wires and not resetting it would poison subsequent draws. The implementation must restore `batch.setColor(Color.WHITE)` after each conduit draw. The existing `draw()` in `GameScreen` already resets batch color before `batch.begin()`, but conduit draws happen mid-batch — so each `ConduitEntity.draw()` must clean up after itself.

4. **`Building` scrap check race.** Between `handleMouseMove()` showing "valid" and `handleClick()` checking scrap, scrap could theoretically change (e.g., an enemy kill adds scrap between frames). This is harmless — the click just fails silently and the next frame's `handleMouseMove()` corrects the overlay.

5. **Persistent building changes the established UX pattern.** Players accustomed to one-shot building will need to learn to right-click or press Escape to exit. This is a common pattern in other tower defense games and should feel natural, but it is a behavioral change.

## Open Questions

1. **Wire overlay button placement.** The spec suggests top-right near the scrap HUD. Should it be a standalone floating button, or integrated into an existing HUD panel? The current UI has no top-right elements, so a standalone button is simplest.

2. **Color coding for detailed wire mode.** The spec says "color-coded by conduit type." Currently conduits have a `texture` field but no `color` field. Options: (a) derive color from the conduit's texture name (e.g., copper = orange, ACSR = silver), (b) add a `color` field to the Conduit JSON, (c) use the texture's average color, or (d) skip color-coding and just use full opacity with the existing texture. Option (d) is simplest and still achieves the "clearly traceable" goal. Recommend (d) for now.

3. **Wire count badge rendering.** Using `getFont().draw()` for the badge means it shares the game's default font, which may be too large for a small badge. Consider using a scaled font or drawing a smaller number. For now, the default font at the tile corner is acceptable as a first pass.
