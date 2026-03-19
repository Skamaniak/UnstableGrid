# Energy Level Bars on Structures

**Date:** 2026-03-19
**Status:** Complete
**Author:** feature-planner

## Requirements

**Feature:** Energy Level Bars on Structures

**Description:** Add a horizontal progress bar at the bottom of every placed structure (generators, power storages, and towers) that visually represents how "full of energy" the structure is. The bar shows `currentPower / capacity` as a fill fraction.

**Visual spec:**
- Horizontal bar at the BOTTOM of the structure's tile
- Color gradient: red (0%) -> yellow (50%) -> green (100%). The color should interpolate smoothly across the range.
- Always visible (no hover/selection required)
- Bar dimensions proportional to tile size (64px tiles). 52px wide, 5px tall, with 6px horizontal padding and 2px bottom inset.
- Drawn every frame for every placed structure.

**Applies to:** All three structure types -- `GeneratorEntity`, `PowerStorageEntity`, `TowerEntity`. All of them have `currentPower` and `capacity` fields (or equivalent) that can produce a fill fraction.

**Constraints:**
- Must be drawn via `ShapeRenderer` (consistent with existing shape rendering passes)
- Hot path rendering -- zero allocations per frame
- Color interpolation must not allocate (pre-compute or use math, no `new Color()` per frame)

**Edge cases:**
- Structures with 0 capacity (if possible) -- bar should not render or show empty
- Structures at exactly 0%, 50%, 100% -- colors should be exactly red, yellow, green respectively
- Multiple structures adjacent -- bars should not overlap neighboring tiles

## Motivation

The current energy display is a debug-quality text percentage (`drawEnergyLevel` in `GameEntity`, marked `//FIXME remove`) rendered via `SpriteBatch`/`BitmapFont`. It is hard to read at a glance, especially when zoomed out or when many structures are on screen. A colored horizontal bar provides an instant visual indicator of each structure's energy state, enabling players to quickly identify power-starved towers or full generators without clicking on each one.

## Design

### Rendering approach

The energy bars will be drawn in the `ShapeRenderer` pass (`GameState.drawShapes()`), alongside the existing chevron rendering. This keeps all shape-based overlays in one pass and avoids mixing `SpriteBatch` and `ShapeRenderer` state. The bars will be drawn **before** chevrons so that chevrons (which are in the top-right quadrant) render on top if there is any overlap.

### Color interpolation

The color gradient uses a two-segment linear interpolation with zero allocations:

- **0% to 50%:** Interpolate from red `(1,0,0)` to yellow `(1,1,0)`. This means the green channel goes from 0 to 1 linearly, red stays at 1, blue stays at 0.
- **50% to 100%:** Interpolate from yellow `(1,1,0)` to green `(0,1,0)`. This means the red channel goes from 1 to 0, green stays at 1, blue stays at 0.

This can be computed with simple arithmetic on the fill fraction -- no `Color.lerp()` needed, no temporary `Color` objects:

```java
// fraction in [0, 1]
float r, g;
if (fraction <= 0.5f) {
    r = 1f;
    g = fraction * 2f;
} else {
    r = 1f - (fraction - 0.5f) * 2f;
    g = 1f;
}
shapeRenderer.setColor(r, g, 0f, 1f);
```

At exactly 0%: `r=1, g=0` (red). At 50%: `r=1, g=1` (yellow). At 100%: `r=0, g=1` (green).

### Bar dimensions and positioning

**UPDATED:** Changed from vertical (left side) to horizontal (bottom of tile) to avoid overlapping with chevron wire-count indicators on the left.

Constants (in `GameEntity` as `static final`):

| Constant | Value | Rationale |
|---|---|---|
| `ENERGY_BAR_WIDTH` | 52px | 64 - 6px left padding - 6px right padding |
| `ENERGY_BAR_HEIGHT` | 5px | Thin horizontal bar at the bottom |
| `ENERGY_BAR_X_OFFSET` | 6px | Left padding from tile left edge |
| `ENERGY_BAR_Y_OFFSET` | 2px | Inset from bottom tile edge to avoid bleeding into adjacent tile |

The bar is drawn as two `ShapeRenderer.rect()` calls:
1. **Background:** Dark gray rectangle at full width (shows the "empty" portion).
2. **Foreground:** Colored rectangle from the left, width = `ENERGY_BAR_WIDTH * fraction`.

All coordinates are computed from `position.x * TILE_SIZE_PX + offset` -- no allocations.

### Entity API

Each entity type already has `powerBank` (current) and a way to get capacity from its level data. Rather than adding a new interface or method to `GameEntity` (which would require making `powerBank` accessible at the abstract level), the drawing will be done via a concrete method on `GameEntity` that takes `current` and `max` as parameters -- exactly like the existing `drawEnergyLevel(SpriteBatch, float, float)` but for `ShapeRenderer`. Each subclass already calls `drawEnergyLevel` with the right arguments, so we follow the same pattern.

The new method: `GameEntity.drawEnergyBar(ShapeRenderer shapeRenderer, float currentPower, float maxPower)`.

### Removing the old debug display

The existing `drawEnergyLevel(SpriteBatch, float, float)` method (marked `//FIXME remove`) will be removed along with all three call sites in `GeneratorEntity.draw()`, `PowerStorageEntity.draw()`, and `TowerEntity.draw()`. The new energy bars fully replace this debug text.

### Integration into drawShapes

`GameState.drawShapes()` already iterates all three entity sets for chevron drawing. The energy bar calls will be added inside those same loops, before the chevron call:

```java
for (GeneratorEntity generator : generators) {
    generator.drawEnergyBar(shapeRenderer);
    generator.drawChevrons(shapeRenderer);
}
```

Wait -- `drawEnergyBar` needs `currentPower` and `maxPower` arguments because `GameEntity` does not expose those. Two options:

**Option A:** Add an abstract method `drawEnergyBar(ShapeRenderer)` to `GameEntity` that each subclass implements by calling a shared protected helper with its own power values. This adds boilerplate.

**Option B:** Add a new `drawEnergyBar(ShapeRenderer)` method to each entity subclass that calls a shared `GameEntity` helper with `(shapeRenderer, powerBank, capacity)`. The helper does the actual rendering. This is identical to the existing `drawEnergyLevel` pattern.

**Chosen: Option B** -- matches existing codebase pattern exactly (see how `drawEnergyLevel` works today). The new method `drawEnergyBar(ShapeRenderer, float, float)` on `GameEntity` is `protected` and called by each subclass's public `drawEnergyBar(ShapeRenderer)`.

However, since `GameState.drawShapes()` iterates the entity sets directly and already calls `entity.drawChevrons()` (which is on `GameEntity`), we can simplify further: make `drawEnergyBar(ShapeRenderer)` abstract on `GameEntity`, where each subclass provides its own `currentPower` and `maxPower` to the protected helper. But adding abstract methods is heavier.

**Simplest approach:** Add a non-abstract `drawEnergyBar(ShapeRenderer, float, float)` to `GameEntity` (the rendering helper), and have `GameState.drawShapes()` NOT call it directly. Instead, each entity's `drawChevrons` is already called from `drawShapes`. We just add a parallel call pattern. But this requires either:
- Each subclass exposing a no-arg `drawEnergyBar(ShapeRenderer)`, or
- `GameState.drawShapes()` knowing how to extract power from each entity type.

**Final decision:** Keep it simple. Add a protected `drawEnergyBar(ShapeRenderer, float, float)` helper to `GameEntity`. Add a public `drawEnergyBar(ShapeRenderer)` to each of the three concrete entity classes that calls the helper with their specific power values. `GameState.drawShapes()` calls `entity.drawEnergyBar(shapeRenderer)` in each entity loop. This avoids any abstract method or interface change, keeps the hot path clean, and mirrors `drawChevrons`.

## Files to Modify

- **`core/src/main/java/com/skamaniak/ugfs/game/entity/GameEntity.java`**
  - Remove `drawEnergyLevel(SpriteBatch, float, float)` method.
  - Add `static final` constants for bar dimensions (`ENERGY_BAR_WIDTH`, `ENERGY_BAR_HEIGHT`, `ENERGY_BAR_X_OFFSET`, `ENERGY_BAR_Y_OFFSET`).
  - Add `protected void drawEnergyBar(ShapeRenderer shapeRenderer, float currentPower, float maxPower)` -- renders the background rect and foreground colored rect using zero-allocation color math.

- **`core/src/main/java/com/skamaniak/ugfs/game/entity/GeneratorEntity.java`**
  - Remove `drawEnergyLevel` call from `draw()`.
  - Add `public void drawEnergyBar(ShapeRenderer shapeRenderer)` that calls `drawEnergyBar(shapeRenderer, powerBank, generatorLevel().getPowerStorage())`.

- **`core/src/main/java/com/skamaniak/ugfs/game/entity/PowerStorageEntity.java`**
  - Remove `drawEnergyLevel` call from `draw()`.
  - Add `public void drawEnergyBar(ShapeRenderer shapeRenderer)` that calls `drawEnergyBar(shapeRenderer, powerBank, powerStorageLevel().getPowerStorage())`.

- **`core/src/main/java/com/skamaniak/ugfs/game/entity/TowerEntity.java`**
  - Remove `drawEnergyLevel` call from `draw()`.
  - Add `public void drawEnergyBar(ShapeRenderer shapeRenderer)` that calls `drawEnergyBar(shapeRenderer, powerBank, towerLevel().getPowerStorage())`.

- **`core/src/main/java/com/skamaniak/ugfs/game/GameState.java`**
  - In `drawShapes()`, add `entity.drawEnergyBar(shapeRenderer)` calls in each of the three entity loops, before `drawChevrons`.

## Files to Create

None.

## Implementation Steps

- [x] **Step 1:** In `GameEntity.java`, remove the `drawEnergyLevel(SpriteBatch, float, float)` method.
- [x] **Step 2:** In `GameEntity.java`, add the bar dimension constants: `ENERGY_BAR_WIDTH = 52f`, `ENERGY_BAR_HEIGHT = 5f`, `ENERGY_BAR_X_OFFSET = 6f`, `ENERGY_BAR_Y_OFFSET = 2f`.
- [x] **Step 3:** In `GameEntity.java`, add `protected void drawEnergyBar(ShapeRenderer shapeRenderer, float currentPower, float maxPower)`. Implementation: if `maxPower <= 0`, return early. Compute `fraction = currentPower / maxPower`, clamp to [0, 1]. Compute bar position from `position` and constants. Draw background rect (dark gray, full width). Compute color via two-segment interpolation (no allocation). Draw foreground rect (colored, `width * fraction`).
- [x] **Step 4:** In `GeneratorEntity.java`, remove the `drawEnergyLevel` call from `draw()`. Add `public void drawEnergyBar(ShapeRenderer shapeRenderer)` that calls `drawEnergyBar(shapeRenderer, powerBank, generatorLevel().getPowerStorage())`.
- [x] **Step 5:** In `PowerStorageEntity.java`, remove the `drawEnergyLevel` call from `draw()`. Add `public void drawEnergyBar(ShapeRenderer shapeRenderer)` that calls `drawEnergyBar(shapeRenderer, powerBank, powerStorageLevel().getPowerStorage())`.
- [x] **Step 6:** In `TowerEntity.java`, remove the `drawEnergyLevel` call from `draw()`. Add `public void drawEnergyBar(ShapeRenderer shapeRenderer)` that calls `drawEnergyBar(shapeRenderer, powerBank, towerLevel().getPowerStorage())`.
- [x] **Step 7:** In `GameState.drawShapes()`, add `entity.drawEnergyBar(shapeRenderer)` calls inside each of the three entity iteration loops (generators, storages, towers), placed before the existing `drawChevrons` call.
- [x] **Step 8:** Verify compilation with `./gradlew build` and run tests with `./gradlew core:test`.
- [x] **Step 9:** Manual visual verification (see test scenarios below).

## Bug Fixes

1. **Vertical bar overlapped chevron wire-count indicators.** The initial implementation used a vertical bar on the left side of the tile, which overlapped with the chevron indicators also rendered on the left. Fixed by changing to a horizontal bar at the bottom of the tile. Constants were swapped: `WIDTH` 5→52, `HEIGHT` 52→5, `X_OFFSET` 2→6, `Y_OFFSET` 6→2. The foreground fill direction changed from vertical (`height * fraction`) to horizontal (`width * fraction`).

## Testing Plan

### Unit testable (pure logic)

The color interpolation logic is pure math that could be unit tested if extracted to a static utility method. However, since it is only 4 lines of arithmetic inside a `protected` rendering method, extracting it solely for testing would add indirection on the hot path. The math is simple enough to verify by inspection and manual testing.

The fill fraction clamping (`Math.max(0, Math.min(fraction, 1))`) is also trivially correct.

No new unit tests are warranted for this feature.

### Requires LibGDX (not unit tested)

- `GameEntity.drawEnergyBar(ShapeRenderer, float, float)` -- uses `ShapeRenderer`.
- `GeneratorEntity.drawEnergyBar(ShapeRenderer)` -- delegates to above.
- `PowerStorageEntity.drawEnergyBar(ShapeRenderer)` -- delegates to above.
- `TowerEntity.drawEnergyBar(ShapeRenderer)` -- delegates to above.
- `GameState.drawShapes()` -- orchestrates the calls.

### Manual test scenarios

1. **Basic visibility.** Place one of each structure type (generator, power storage, tower). Verify that each shows a horizontal bar at the bottom of its tile. The bar should be ~52px wide, ~5px tall, and inset slightly from the tile edges.

2. **Generator fill cycle.** Place a generator with no outgoing wires. Watch the bar fill from red (empty after placement) through yellow to green as the generator produces power and fills its internal bank. Verify color transitions are smooth with no visible banding or flickering.

3. **Tower drain cycle.** Place a tower connected (via storage) to a generator. During a wave, observe the tower's bar fluctuating as it consumes power for shots and receives power from the grid. The bar should decrease (toward red) when firing rapidly and recover (toward green) between shots if power supply is sufficient.

4. **Power storage passthrough.** Place a power storage between a generator and tower. Verify the storage's bar reflects its current stored energy, filling when receiving power and draining when forwarding to the tower.

5. **Empty structure (0% energy).** Place a tower with no power connections. Verify the bar shows as fully red/empty (only the dark gray background should be visible, with no colored foreground or a minimal red sliver at the bottom).

6. **Full structure (100% energy).** Place a generator with no outgoing wires and wait for it to fill completely. Verify the bar is fully green, filling the entire bar height.

7. **Adjacent structures.** Place two structures side by side (horizontally and vertically). Verify that bars do not overlap into neighboring tiles. The 6px horizontal padding and 2px bottom inset should prevent this.

8. **Debug text removed.** Verify that the old percentage text (e.g., "75%") no longer appears on any structure. The text-based `drawEnergyLevel` should be fully replaced by the bar.

9. **Zoom levels.** Zoom in and zoom out. Verify the bars remain visible and correctly positioned relative to their structure tiles at all zoom levels. The bars are drawn in world coordinates so they should scale with the camera.

10. **Zero capacity edge case.** This is a defensive check -- if a structure somehow has 0 max capacity, the bar should not render (no division by zero, no visual artifact). This is unlikely in practice but the code should handle it gracefully via the early return.

## Risks & Trade-offs

- **Visual clutter.** Energy bars on every structure may feel visually noisy on dense grids. If this becomes an issue, a future enhancement could add a toggle or only show bars below a threshold. For now, always-visible matches the requirements.
- **Drawing order.** Bars are drawn in the `ShapeRenderer` pass (after `SpriteBatch` textures), so they render on top of structure textures. This is intentional -- bars should be visible over the structure sprite. However, if a structure has dark edges at its bottom, the bar may blend in. The dark gray background mitigates this.
- **Bar at bottom vs chevrons at top-right.** Chevrons are positioned in the top-right quadrant of the tile. Energy bars are at the bottom. These do not overlap vertically.

## Open Questions

- Should the bar have a thin border/outline for better visibility against dark structure textures? Starting without one for simplicity; can add a 1px outline later if needed.
- Should the bar alpha be slightly transparent (e.g., 0.8) to let the underlying texture show through, or fully opaque? Starting fully opaque per the requirements; transparency can be tuned visually.
