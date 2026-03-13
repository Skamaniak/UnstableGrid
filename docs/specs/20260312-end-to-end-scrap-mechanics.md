# End-to-End Scrap Mechanics

**Date:** 2026-03-12
**Status:** Done
**Author:** feature-planner

## Requirements

1. **Building costs scrap:** Placing structures (towers, generators, power storages) and wires (conduits) must deduct their `scrapCost` from the current scrap balance in `GameState`.

2. **Overdraft prevention:** If the player cannot afford an item, its button in `BuildMenu` (for structures) and `WiringMenu` (for conduits) must be visually disabled — red background with greyish text, and unclickable. There is NO need for placement-time rejection since scrap only decreases via explicit player build actions (never passively).

3. **Cost labels on buttons:** Each button in `BuildMenu` and `WiringMenu` should display the scrap cost of the item (e.g. "Generator - 50").

4. **Reactive UI updates:** Buttons must refresh their enabled/disabled state dynamically as scrap changes. For example, if an enemy kill grants scrap and the player now has enough to afford a previously-disabled item, the button should become enabled without the player needing to reopen the menu. This means the UI must poll or observe `gameState.getScrap()` each frame (or on relevant events).

5. **Starting scrap:** Each level defines a starting scrap amount in its Level JSON asset. The game already loads the tutorial level. The `GameState` should be initialized with the level's starting scrap.

6. **Existing sell/refund flow:** Already implemented and working — no changes needed to sell/destroy mechanics.

### Key constraints
- Scrap can only decrease when the player explicitly builds something. It increases when enemies are killed (future) or structures/wires are sold (already implemented).
- The `scrapCost` field already exists on `GameAsset` subclasses (used by the sell/refund flow).
- The Level model class already exists — it may or may not have a `startingScrap` field; check and add if needed.
- Do NOT refactor static singletons or add dependency injection. Follow existing patterns.
- Java 8 source compatibility (though `Set.of()` is used elsewhere, so modern JVM is assumed at runtime).

## Motivation

Building structures and wires currently does not cost any scrap. The `Building.handleClick()` method places entities without checking or deducting scrap, and the `Wiring` action creates conduits where `registerLink()` already calls `spendScrap()` but there is no pre-check or UI feedback. Players can build infinitely, which removes all resource management from the game. This feature closes the loop: building costs scrap, the UI shows costs, and buttons are disabled when the player cannot afford them.

## Design

### Scrap cost access from GameAsset

The core challenge is that `BuildMenu` and `WiringMenu` work with `GameAsset` references, but scrap costs live on inner `Level` classes (`Generator.Level`, `Tower.Level`, `PowerStorage.Level`) and directly on `Conduit`. To display costs on buttons and check affordability, we need a uniform way to get the build cost from a `GameAsset`.

**Decision:** Add a `getBuildCost()` method to `GameAsset` (base class) that returns `0` by default. Override it in `Generator`, `PowerStorage`, and `Tower` to return `levels.get(0).getScrapCost()` (level-1 cost, since newly placed structures start at level 1). Override it in `Conduit` to return `scrapCost` directly. This avoids `instanceof` checks in UI code and keeps the accessor on the model class where it belongs.

Why `getBuildCost()` instead of `getScrapCost()`: The entity classes already have `getScrapCost()` which returns the cost for the entity's *current* level. `getBuildCost()` on the asset model always returns the level-1 cost, which is what the build/wire UI needs. For `Conduit`, these are the same value.

### Building cost deduction

`GameScreen.buildGameObject()` is the callback invoked by `Building.handleClick()` when placement succeeds. This is the single point where structures are placed, so it is the right place to deduct scrap. The flow becomes:

1. `Building.handleClick()` checks terrain validity via `isBuildable()` (unchanged).
2. If valid, calls `build.accept(position, assetToBuild)` which is `GameScreen.buildGameObject()`.
3. `buildGameObject()` calls `gameState.spendScrap(asset.getBuildCost())` before registering the entity.

Since the requirement states scrap only decreases via explicit player actions (never passively), and the UI will disable buttons when the player cannot afford an item, there is no need for a redundant affordability check at placement time. However, as a defensive measure, `buildGameObject()` will still call `spendScrap()` which returns `false` if insufficient -- in that case, the entity is simply not placed. This guards against edge cases like rapid double-clicks.

### Wire cost deduction

`registerLink()` already calls `spendScrap(conduit.getScrapCost())` (line 93 of `GameState`). This is already correct. The only missing piece is the UI affordability check, which prevents the player from entering wiring mode with an unaffordable conduit.

### Affordability checking in BuildMenu

`BuildMenu` needs a reference to `GameState` to poll `getScrap()`. The constructor will accept a `GameState` parameter. Each frame during `handleInput()`, the menu iterates its buttons and updates their enabled/disabled styling based on `gameState.getScrap() >= asset.getBuildCost()`.

**Button storage:** Currently buttons are stored in a flat `Collection<Button> buildButtons` with no association to their `GameAsset`. Change this to a `Map<Button, GameAsset> buildButtonAssets` so that each button can be looked up to find its cost. The existing `buildButtons` collection is only used in `resetSelection()` to uncheck all buttons -- this can iterate the map's key set instead.

**Button labels:** Change from `gameAsset.getName()` to `gameAsset.getName() + " - " + gameAsset.getBuildCost()` to show costs.

**Disabled styling:** When `gameState.getScrap() < asset.getBuildCost()`:
- Set the button's color to a reddish tint: `button.setColor(0.8f, 0.3f, 0.3f, 1f)` (red background effect).
- The button remains in the UI but its click listener checks affordability and returns early if unaffordable.
- When affordable again, reset color to `Color.WHITE`.

This polling approach (check every frame in `handleInput()`) matches the existing pattern used by `ScrapHud` and satisfies the reactive update requirement without introducing an observer/event system.

**Click guard:** The `ClickListener` on each build button must also check affordability at click time (not just visual state) to prevent race conditions. If `gameState.getScrap() < asset.getBuildCost()`, the click is ignored.

### Affordability checking in WiringMenu

Same pattern as BuildMenu. `WiringMenu` receives a `GameState` reference in its constructor. Each frame in `handleInput()`, buttons are updated. The button click listener guards against clicking unaffordable conduits.

**Button storage:** Same approach -- `Map<TextButton, Conduit> buttonConduits` to associate buttons with their conduit assets.

**Button labels:** Change from `conduit.getName()` to `conduit.getName() + " - " + conduit.getScrapCost()`.

**Disabled styling:** Same red tint approach as BuildMenu.

### Starting scrap

`Level` already has a `scrap` field and `getScrap()` method. `GameState` constructor already initializes `this.scrap = level.getScrap()`. The tutorial level JSON already has `"scrap": 100`. This is fully implemented -- no changes needed.

### No changes to sell/refund

Already working as designed. `sellEntity()`, `sellConduit()`, `addScrap()` are all correct.

## Files to Modify

- `core/src/main/java/com/skamaniak/ugfs/asset/model/GameAsset.java` — Add `getBuildCost()` method returning `0` (default).

- `core/src/main/java/com/skamaniak/ugfs/asset/model/Generator.java` — Override `getBuildCost()` to return `levels.get(0).getScrapCost()`.

- `core/src/main/java/com/skamaniak/ugfs/asset/model/PowerStorage.java` — Override `getBuildCost()` to return `levels.get(0).getScrapCost()`.

- `core/src/main/java/com/skamaniak/ugfs/asset/model/Tower.java` — Override `getBuildCost()` to return `levels.get(0).getScrapCost()`.

- `core/src/main/java/com/skamaniak/ugfs/asset/model/Conduit.java` — Override `getBuildCost()` to return `scrapCost`.

- `core/src/main/java/com/skamaniak/ugfs/ui/BuildMenu.java` — Accept `GameState` in constructor. Replace `Collection<Button> buildButtons` with `Map<Button, GameAsset> buildButtonAssets`. Add cost labels to buttons. Add per-frame affordability polling in `handleInput()`. Add click-time affordability guard.

- `core/src/main/java/com/skamaniak/ugfs/ui/WiringMenu.java` — Accept `GameState` in constructor. Add `Map<TextButton, Conduit> buttonConduits`. Add cost labels to buttons. Add per-frame affordability polling in `handleInput()`. Add click-time affordability guard.

- `core/src/main/java/com/skamaniak/ugfs/GameScreen.java` — Pass `gameState` to `BuildMenu` and `WiringMenu` constructors. Add `spendScrap()` call in `buildGameObject()` with early return on failure.

## Files to Create

None.

## Implementation Steps

- [x] **Step 1: Add `getBuildCost()` to `GameAsset` and subclasses.**
  Add `public int getBuildCost() { return 0; }` to `GameAsset`. Override in `Generator`, `PowerStorage`, `Tower` to return `levels.get(0).getScrapCost()`. Override in `Conduit` to return `scrapCost`.

- [x] **Step 2: Add scrap deduction to `buildGameObject()`.**
  In `GameScreen.buildGameObject()`, before `gameState.registerEntity(newEntity)`, add:
  ```java
  if (!gameState.spendScrap(asset.getBuildCost())) {
      buildMenu.resetSelection();
      return;
  }
  ```
  Note: `registerLink()` already handles conduit cost deduction.

- [x] **Step 3: Update `BuildMenu` constructor and button creation.**
  - Add `GameState gameState` parameter to constructor. Store as field.
  - Replace `Collection<Button> buildButtons` with `Map<Button, GameAsset> buildButtonAssets`.
  - In `createBuildSubmenu()`, change button label to `gameAsset.getName() + " - " + gameAsset.getBuildCost()`.
  - Store each button-to-asset mapping in the map.
  - Update `resetSelection()` to iterate `buildButtonAssets.keySet()`.
  - Add affordability guard in the button `ClickListener`: if `gameState.getScrap() < gameAsset.getBuildCost()`, return without selecting.

- [x] **Step 4: Add per-frame affordability polling to `BuildMenu.handleInput()`.**
  After `stage.act()`, iterate `buildButtonAssets` entries. For each:
  ```java
  if (gameState.getScrap() < asset.getBuildCost()) {
      button.setColor(0.8f, 0.3f, 0.3f, 1f);
  } else {
      button.setColor(Color.WHITE);
  }
  ```

- [x] **Step 5: Update `WiringMenu` constructor and button creation.**
  - Add `GameState gameState` parameter to constructor. Store as field.
  - Add `Map<TextButton, Conduit> buttonConduits` field.
  - In `createMenu()`, change button label to `conduit.getName() + " - " + conduit.getScrapCost()`.
  - Store each button-to-conduit mapping.
  - Add affordability guard in the button `InputListener.touchDown()`: if `gameState.getScrap() < conduit.getScrapCost()`, return `false`.

- [x] **Step 6: Add per-frame affordability polling to `WiringMenu.handleInput()`.**
  Same pattern as BuildMenu: iterate `buttonConduits`, set color based on affordability.

- [x] **Step 7: Update `GameScreen` constructor.**
  Pass `gameState` to `BuildMenu` and `WiringMenu` constructors:
  ```java
  this.buildMenu = new BuildMenu(unstableGrid.batch, gameState);
  this.wiringMenu = new WiringMenu(unstableGrid.batch, gameState);
  ```

- [x] **Step 8: Compile and verify.**
  Run `./gradlew build` to confirm no compilation errors.

## Testing Plan

### Unit testable (pure logic)

- **`GameAsset.getBuildCost()` and subclass overrides:** Tested in `GameAssetBuildCostTest` (`core/src/test/java/com/skamaniak/ugfs/asset/model/`). Since the asset model classes have private fields with no setters (designed for JSON deserialization), reflection is used to set fields on real instances — no production code was modified. Tests cover: `GameAsset` base returns 0; `Generator`, `PowerStorage`, and `Tower` each return the level-0 `scrapCost`; all three use `levels.get(0)` and ignore higher-level entries; `Conduit.getBuildCost()` equals `getScrapCost()`. Zero-cost cases are also covered for each type.

- **`GameState.spendScrap()` edge cases:** Tested in `GameStateSpendScrapTest` (`core/src/test/java/com/skamaniak/ugfs/game/`). `GameState` is constructed with a Mockito-mocked `Level` (returns the starting scrap and an empty tile map) and `null` for `UnstableGrid` (which is only referenced in draw/sound paths not exercised here). Tests cover: initial scrap matches `Level.getScrap()`; spending an affordable amount returns `true` and deducts correctly; spending the exact balance returns `true` and leaves 0; spending 0 always succeeds without changing the balance; spending more than the balance returns `false` and leaves the balance unchanged; overdraft with a 0 balance returns `false` and does not go negative; `addScrap()` increases the balance; failed spend does not corrupt the balance for subsequent calls.

- **`GameState.registerLink()` scrap deduction and rejection:** Tested in `GameStateRegisterLinkTest` (`core/src/test/java/com/skamaniak/ugfs/game/`). Tests cover: conduit created when scrap is sufficient; scrap deducted by the conduit cost; conduit not created when scrap is insufficient; balance unchanged when registration fails; exact-balance boundary creates conduit and leaves zero scrap; same conduit type on same endpoints is a no-op (no double-charge); replacing a different conduit type refunds the old cost and charges the new cost; replacement leaves the new conduit type in place.

- **`GameState.registerLinkFree()` — free registration:** Tested in `GameStateRegisterLinkTest`. Tests cover: conduit is created; scrap is not deducted; succeeds even when balance is 0; same conduit type on same endpoints is a no-op without error.

### Requires LibGDX (not unit tested)

- `BuildMenu` affordability polling and button styling (Scene2D UI).
- `WiringMenu` affordability polling and button styling (Scene2D UI).
- `GameScreen.buildGameObject()` scrap deduction (calls `GameAssetManager`-dependent code transitively).
- Visual verification that disabled buttons show red tint and enabled buttons show normal color.

### Manual test scenarios

1. Start game with 100 scrap. Verify buttons for items costing > 100 are red/disabled.
2. Build a solar panel (cost 50). Verify scrap drops to 50 and items costing > 50 become disabled.
3. Sell the solar panel. Verify scrap returns to 100 and buttons re-enable.
4. Try clicking a disabled build button. Verify nothing happens.
5. Open wiring menu. Verify conduit buttons show costs and unaffordable ones are disabled.
6. Wire two structures with an affordable conduit. Verify scrap deducts.
7. Verify that the scrap HUD flashes correctly on build (red flash) and sell (green flash).

## Risks & Trade-offs

1. **Per-frame button iteration in `handleInput()`.** Both `BuildMenu` and `WiringMenu` iterate all buttons every frame to update colors. With the current small number of assets (< 20 total buttons), this is negligible. If the asset count grows significantly, this could be optimized to only update when scrap changes (track `lastCheckedScrap`), but this is premature optimization now.

2. **Button color as disabled indicator.** Using `setColor()` is simple but limited -- it tints the entire button including text. A more polished approach would use separate `TextButtonStyle` instances for enabled/disabled states. The current approach is chosen for simplicity and can be upgraded later without architectural changes.

3. **No placement-time affordability check for structures.** The spec relies on UI disabling to prevent building unaffordable items. A defensive `spendScrap()` check is added in `buildGameObject()` as a safety net, but the primary gate is the UI. If a new code path bypasses the UI (e.g., a future hotkey), it must also check affordability.

4. **`getBuildCost()` returns level-1 cost.** This is correct for initial placement but will need revisiting when upgrade mechanics are added (upgrading costs the difference between levels, or the next level's cost).

5. **`registerLink()` already deducts scrap unconditionally.** The `spendScrap()` call on line 93 of `GameState.registerLink()` does not check the return value -- if scrap is insufficient, it returns `false` but the conduit is still created. This is an existing bug but is mitigated by this feature's UI disabling. A defensive fix would be to check the return value and skip conduit creation if insufficient, but that changes existing behavior and is out of scope. The UI guard in `WiringMenu` prevents this path from being reached in practice.

## Open Questions

1. **Should `registerLink()` be made defensive?** Currently it calls `spendScrap()` but ignores the return value, creating the conduit even if scrap is insufficient. Should we add a check? This is a pre-existing issue and the UI guard makes it unreachable in normal play, but a future code path could trigger it. Recommend fixing as a follow-up.

2. **Upgrade costs.** When structure upgrading is implemented, should `getBuildCost()` be extended to support per-level costs, or should a separate `getUpgradeCost(int fromLevel)` method be added? Deferred to the upgrade feature.

3. **Button style vs color tint.** The current approach tints buttons red. Should we invest in proper disabled `TextButtonStyle` with greyed-out text? This is a polish decision that can be revisited without architectural changes.
