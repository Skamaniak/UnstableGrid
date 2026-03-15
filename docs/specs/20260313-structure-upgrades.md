# Structure Upgrades

**Date:** 2026-03-13
**Status:** Done
**Author:** feature-planner

## Requirements

### Feature: Structure Upgrades

**Overview:** Structures (generators, power storages, towers) defined in JSON with multiple level entries become upgradeable in-game. Players spend scrap to upgrade a structure to its next level, gaining improved stats. Conduits are NOT upgradeable.

### Upgrade Mechanic
- Each structure type already has multiple levels defined in its JSON. Upgrading advances the structure to the next level, replacing its current stats with the next level's stats.
- The upgrade cost is the `cost` field of the next level's JSON entry.
- The game tracks total scrap invested in a structure (placement cost + all upgrade costs paid) for accurate sell refunds.
- Selling a structure refunds 100% of all scrap invested (placement + upgrades). Current sell logic already refunds 100% of placement cost -- this must be extended to include upgrade costs.

### UI -- Right-Click Context Menu (ContextMenu)
- Add an "Upgrade (X scrap)" button to the ContextMenu, always positioned at the TOP of the menu (above Wire, Remove Wire, Sell).
- The button uses a **light blue** background color to distinguish it from other buttons.
- Disabled (red, not clickable) when the player has insufficient scrap -- same pattern as the existing Sell button logic.
- At max level: show a disabled "Max Level" button (no cost shown) in the same position/color.
- The upgrade button triggers the upgrade immediately on click (no two-click confirmation needed).
- When upgrade is applied: deduct scrap, update the structure's stats to the next level's values, close/refresh the context menu.

### UI -- Chevron Level Indicator (on structure icons)
- Gold chevrons drawn in the top-right quadrant of the structure's tile icon.
- 0 chevrons at base level (level 1), N-1 chevrons at level N. Level is 1-based: level 1 = 0 chevrons, level 2 = 1 chevron, level 3 = 2 chevrons.
- Stacked vertically within the top-right quadrant.
- Sized so all chevrons for the max level fit within the quadrant (typically 2-3 upgrade levels expected).
- Drawn using ShapeRenderer (no new texture needed) as upward-pointing chevron shapes (two diagonal lines forming a "^").

### UI -- Details Panel (DetailsMenu)
- After an upgrade, the DetailsMenu must immediately show the new level's stats.
- No next-level preview is needed -- just current stats reflecting the current level.

### Known codebase context
- JSON asset classes (Generator, Tower, PowerStorage) have private fields with no setters -- use Mockito mocks in tests.
- GameAssetManager.INSTANCE is a singleton; draw() methods are not unit tested.
- The existing sell flow in ContextMenu/PlayerActionFactory handles scrap refunds -- extend it to include upgrade costs.
- ScrapHud already polls gameState.getScrap() each frame and flashes on changes.
- ContextMenu already has disabled-button logic for the Sell button when insufficient scrap -- reuse this pattern.
- SpriteBatch color must be reset to WHITE before batch.begin() each frame (already done in draw()).
- ShapeRenderer is available on the UnstableGrid game object.
- Chevron rendering happens in the entity's draw() method or a new drawOverlay() pass in GameState.

## Motivation

Upgrading structures is a core progression mechanic in tower defense games. Players invest scrap incrementally to improve existing structures rather than always building new ones. The JSON asset definitions already include multiple levels per structure type, but there is currently no in-game mechanism to advance a structure beyond level 1. This feature closes that gap, giving players meaningful mid-game decisions about whether to expand (build new) or deepen (upgrade existing).

## Design

### Entity-level upgrade tracking

Each entity class (`GeneratorEntity`, `PowerStorageEntity`, `TowerEntity`) already has a `private int level = 1` field and a method that reads the current level from the asset's levels list (e.g., `generatorLevel()` returns `generator.getLevels().get(level - 1)`). The upgrade mechanic increments this `level` field. No new fields are needed for level tracking.

**Total scrap invested:** A new `private int totalScrapInvested` field is added to `GameEntity`. It is initialized to the placement cost when the entity is created and incremented by each upgrade cost. The existing `getScrapCost()` method (which currently returns the current level's scrap cost) is left unchanged -- it represents the current level's cost for display purposes. A new `getTotalScrapInvested()` method on `GameEntity` returns the cumulative investment for sell refunds.

**Power bank clamping on upgrade:** When a structure's `powerStorage` capacity changes on upgrade, the current `powerBank` may exceed the new capacity (if downgraded, which is not possible here) or be well below it. Since upgrades always increase capacity, `powerBank` is left as-is -- it will naturally fill to the new capacity over time. No clamping is needed.

### Max level detection

Each entity needs to know whether it can be upgraded. A new method `getMaxLevel()` on each entity returns the size of its asset's levels list. A new method `canUpgrade()` returns `level < getMaxLevel()`. A new method `getUpgradeCost()` returns the scrap cost of the next level (or -1 if at max).

These are added to `GameEntity` as abstract methods so `ContextMenu` and `GameState` can operate on any entity type uniformly.

### GameState.upgradeEntity()

A new method `GameState.upgradeEntity(GameEntity entity)` handles the upgrade transaction:
1. Checks `entity.canUpgrade()` -- returns false if at max level.
2. Checks and deducts scrap via `spendScrap(entity.getUpgradeCost())` -- returns false if insufficient.
3. Calls `entity.applyUpgrade()` which increments `level` and updates `totalScrapInvested`.
4. Returns true on success.

### Sell refund update

The current sell methods (`sellGenerator`, `sellStorage`, `sellTower`) call `addScrap(entity.getScrapCost())` which returns only the current level's cost. This must change to `addScrap(entity.getTotalScrapInvested())` to refund all invested scrap. Similarly, `computeSellValue()` must use `getTotalScrapInvested()` instead of `getScrapCost()`.

### ContextMenu upgrade button

**Action type: one-shot (Rule 7).** The upgrade button performs the upgrade immediately on click and closes the menu. No persistent mode, no multi-frame state.

**Flag lifecycle (Rule 1).** A new one-shot boolean `upgradeRequested` is set in the button's click handler. Following the established `sellConfirmed` pattern: `hide()` is called first to dismiss the menu, then `upgradeRequested = true` is set AFTER hide() to avoid the blanket reset in `hide()`. A new `resetUpgradeRequested()` method clears the flag after `selectPlayerAction()` consumes it.

**Multi-frame persistence (Rule 2).** Not applicable -- upgrade is one-shot, consumed in a single frame.

**Exit conditions (Rule 3).** The upgrade button is one-shot, so there is no persistent mode to exit. The menu itself dismisses on click-outside (existing behavior).

**Preconditions (Rule 4).** The Upgrade button is disabled (red, not clickable) when: (a) the player has insufficient scrap, or (b) the entity is already at max level (shows "Max Level" text instead).

**Mode conflicts (Rule 5).** The upgrade is processed inside `selectPlayerAction()` before other mode checks, similar to sell confirm. Since it is one-shot and closes the context menu, it does not conflict with wiring or wire removal modes. The existing `openMenu()` already cancels wiring and building modes.

**Duplicate edge cases (Rule 6).** Upgrading an already-max-level entity is prevented by the disabled button and by `canUpgrade()` returning false. No duplicate relationship is created.

**Stuck-state prevention (Rule 8).** When the upgrade button is disabled (insufficient scrap or max level), other buttons (Wire, Sell) remain functional. The menu dismisses on click-outside. No stuck state.

**New UI elements (Rule 9).** No new Stage is added. The upgrade button is added to the existing `ContextMenu`'s `menuTable`. No `isClickOnUI()` registration needed.

**SpriteBatch color (Rule 10).** The light blue button background is applied via Scene2D styling on the TextButton, not via `batch.setColor()`. No contamination risk.

### Priority in selectPlayerAction()

The upgrade check is added at the very top of `selectPlayerAction()`, before the sell confirm check:

```
upgrade requested  (NEW - one-shot)
sell confirmed
wire removal requested
wiring requested
wire removal persistence
build menu
persistent wiring
detailsSelection (default)
```

This ordering ensures upgrade is consumed immediately. Since upgrade closes the menu (no other flags will be set simultaneously), there is no interaction with other modes.

### Chevron rendering

Chevrons are drawn in the ShapeRenderer pass, not the SpriteBatch pass, to avoid needing new textures. Each entity's chevrons are drawn in `GameState.drawShapes()` by iterating all entities and calling a new `drawChevrons(ShapeRenderer)` method on `GameEntity`.

**Chevron count:** Level is 1-based. A level-N entity displays N-1 chevrons. Level 1 (base) = 0 chevrons, level 2 = 1 chevron, level 3 = 2 chevrons.

**Chevron geometry:** Each chevron is an upward-pointing "^" drawn as two lines using `ShapeRenderer.line()`. They are positioned in the top-right quadrant of the tile (x: 32-64px, y: 32-64px relative to tile origin). With a max of 3 upgrade levels (up to 2 chevrons at level 3), each chevron is approximately 12px wide and 6px tall, centered horizontally in the quadrant. Stack vertically with even spacing, bottom to top.

**Performance (Performance Rules).** No object allocation per frame. Chevron positions are computed from the entity's position and level using arithmetic only. The `drawChevrons` method uses `ShapeRenderer.line()` calls directly.

**Level 1 (base) entities:** Zero chevrons drawn. The method returns immediately if `getLevel() <= 1`.

### DetailsMenu auto-refresh

The `DetailsSelection` action already calls `selectedEntity.getDetails()` every frame via `handleMouseMove()`. Since `getDetails()` reads from the current `level` field (which is updated by `applyUpgrade()`), the details panel automatically shows updated stats after an upgrade with no additional work needed.

However, upgrading closes the context menu and the `selectPlayerAction()` resets to `detailsSelection`. If the player had previously selected the entity, the details will refresh on the next `handleMouseMove` call. If not, the entity will need to be re-selected. This is acceptable behavior.

## Files to Modify

- `core/src/main/java/com/skamaniak/ugfs/game/entity/GameEntity.java` -- Add `totalScrapInvested` field, `getTotalScrapInvested()`, and abstract methods `canUpgrade()`, `getMaxLevel()`, `getUpgradeCost()`, `applyUpgrade()`, `getLevel()`, `drawChevrons(ShapeRenderer)`.
- `core/src/main/java/com/skamaniak/ugfs/game/entity/GeneratorEntity.java` -- Implement `canUpgrade()`, `getMaxLevel()`, `getUpgradeCost()`, `applyUpgrade()`, `getLevel()`, `drawChevrons()`. Initialize `totalScrapInvested` in constructor.
- `core/src/main/java/com/skamaniak/ugfs/game/entity/TowerEntity.java` -- Same as GeneratorEntity.
- `core/src/main/java/com/skamaniak/ugfs/game/entity/PowerStorageEntity.java` -- Same as GeneratorEntity. Remove empty `levelUp()` method.
- `core/src/main/java/com/skamaniak/ugfs/game/GameState.java` -- Add `upgradeEntity(GameEntity)` method. Update `sellGenerator()`, `sellStorage()`, `sellTower()` to use `getTotalScrapInvested()`. Update `computeSellValue()` to use `getTotalScrapInvested()`.
- `core/src/main/java/com/skamaniak/ugfs/ui/ContextMenu.java` -- Add `upgradeRequested` flag, `isUpgradeRequested()`, `resetUpgradeRequested()`. Add Upgrade button at top of `openMenu()` with light blue style, disabled state for insufficient scrap or max level. Update `hide()` to reset `upgradeRequested`.
- `core/src/main/java/com/skamaniak/ugfs/GameScreen.java` -- Add upgrade handling block at top of `selectPlayerAction()`. No other changes needed.

## Files to Create

None. All changes fit within existing files.

## Implementation Steps

- [x] Step 1: Add upgrade infrastructure to `GameEntity`. Add `totalScrapInvested` field (protected, initialized to 0). Add `getTotalScrapInvested()` getter. Add abstract methods: `canUpgrade()`, `getMaxLevel()`, `getUpgradeCost()`, `applyUpgrade()`, `getLevel()`.

- [x] Step 2: Implement upgrade methods in `GeneratorEntity`. Implement `getMaxLevel()` as `generator.getLevels().size()`. Implement `canUpgrade()` as `level < getMaxLevel()`. Implement `getUpgradeCost()` as `canUpgrade() ? generator.getLevels().get(level).getScrapCost() : -1` (note: `level` is 1-based, so `getLevels().get(level)` is the next level at index `level`). Implement `applyUpgrade()`: increment `level`, add upgrade cost to `totalScrapInvested`. Implement `getLevel()` returning `level`. Initialize `totalScrapInvested` to `generatorLevel().getScrapCost()` in constructor (placement cost = level 1 cost).

- [x] Step 3: Implement upgrade methods in `TowerEntity`. Same pattern as GeneratorEntity, adapted for `tower.getLevels()`.

- [x] Step 4: Implement upgrade methods in `PowerStorageEntity`. Same pattern. Remove the empty `levelUp()` stub.

- [x] Step 5: Update sell refunds in `GameState`. Change `sellGenerator()`, `sellStorage()`, `sellTower()` to call `addScrap(entity.getTotalScrapInvested())` instead of `addScrap(entity.getScrapCost())`. Change `computeSellValue()` to use `entity.getTotalScrapInvested()` instead of `entity.getScrapCost()`.

- [x] Step 6: Add `GameState.upgradeEntity(GameEntity entity)`. Check `entity.canUpgrade()`, check and deduct scrap via `spendScrap(entity.getUpgradeCost())`, call `entity.applyUpgrade()`, return true/false.

- [x] Step 7: Add upgrade button to `ContextMenu.openMenu()`. Add `upgradeRequested` boolean field. In `openMenu()`, before the Wire/Sell buttons, add the Upgrade button. Create a light blue `TextButtonStyle` by copying the skin's default style and setting `up`/`down` to a light blue drawable (use `skin.newDrawable("white", new Color(0.6f, 0.8f, 1f, 1f))` or similar). For max level: show disabled "Max Level" button. For insufficient scrap: show red disabled button with cost text. For affordable: show clickable button. On click: call `hide()` then set `upgradeRequested = true`. Add `isUpgradeRequested()` and `resetUpgradeRequested()` accessors. Add `upgradeRequested = false` to `hide()`.

- [x] Step 8: Add upgrade handling to `GameScreen.selectPlayerAction()`. At the very top (before sell confirm), add: if `contextMenu.isUpgradeRequested()`, get target entity, reset the flag, call `gameState.upgradeEntity(entity)`, set `pendingPlayerAction = detailsSelection`, return.

- [x] Step 9: Add `drawChevrons(ShapeRenderer)` to `GameEntity` as a concrete method. It reads `getLevel()` and returns immediately if level <= 1. Otherwise draws `getLevel() - 1` gold chevrons in the top-right quadrant of the tile. Each chevron is two lines forming a "^" shape using `ShapeRenderer.line()`, approximately 12px wide and 6px tall, centered horizontally in the quadrant. Stack vertically with even spacing. Color: gold (`new Color(1f, 0.84f, 0f, 1f)`) -- store as a `private static final` constant to avoid per-frame allocation.

- [x] Step 10: Call chevron rendering from `GameState.drawShapes(ShapeRenderer)`. Iterate all generators, storages, and towers, calling `entity.drawChevrons(shapeRenderer)` for each.

## Testing Plan

### Unit testable (pure logic)

1. **`canUpgrade()` / `getMaxLevel()` / `getUpgradeCost()`** for all three entity types. Create mocked assets with multi-level lists via `TestAssetFactory` (which needs new overloads returning multi-level assets). Verify `canUpgrade()` is true at level 1 when 2+ levels exist, false at max level. Verify `getUpgradeCost()` returns next level's cost.

2. **`applyUpgrade()`** for all three entity types. Verify `level` increments, `totalScrapInvested` increases by the upgrade cost, and `getScrapCost()` returns the new level's cost.

3. **`getTotalScrapInvested()`** tracks cumulative investment. Build at level 1 (cost 50), upgrade to level 2 (cost 75), verify total = 125.

4. **`GameState.upgradeEntity()`** -- Test with sufficient scrap (success, scrap deducted, entity level incremented). Test with insufficient scrap (failure, scrap unchanged, level unchanged). Test at max level (failure).

5. **`GameState.computeSellValue()`** -- After upgrading, verify sell value includes total invested scrap plus connected conduit costs.

6. **`GameState.sellEntity()`** -- After upgrading, verify full scrap refund equals total invested.

7. **`TestAssetFactory` updates** -- Add overloads that create multi-level mocked assets (e.g., `createGenerator(int numLevels, ...)` with configurable per-level stats).

### What was actually tested

**`TestAssetFactory`** (`core/src/test/java/com/skamaniak/ugfs/TestAssetFactory.java`):
- Added `createMultiLevelGenerator(int[]... levelStats)` — each element is `{capacity, generationRate, scrapCost}`.
- Added `createMultiLevelPowerStorage(int[]... levelStats)` — each element is `{capacity, standbyLoss, scrapCost}`.
- Added `createMultiLevelTower(int[]... levelStats)` — each element is `{capacity, standbyLoss, shotCost, scrapCost}` (fireRate defaults to 1.0).

**`UpgradeInfrastructureTest`** (`core/src/test/java/com/skamaniak/ugfs/game/entity/UpgradeInfrastructureTest.java`) — 38 tests covering all three entity types:
- `getLevel()` returns 1 on construction for each type.
- `getMaxLevel()` reflects the number of levels in the mocked asset for each type.
- `canUpgrade()` returns true below max level and false at max level for each type.
- `getUpgradeCost()` returns the next level's cost and returns -1 at max level for each type.
- `applyUpgrade()` increments level, adds upgrade cost to `totalScrapInvested`, and `getScrapCost()` reflects the new level for each type.
- `getTotalScrapInvested()` initialised to level-1 cost at construction for each type.
- Cumulative `totalScrapInvested` after two upgrades for GeneratorEntity and TowerEntity.
- `canUpgrade()` returns false after upgrading to max level (GeneratorEntity).

**`GameStateUpgradeTest`** (`core/src/test/java/com/skamaniak/ugfs/game/GameStateUpgradeTest.java`) — 20 tests:
- `upgradeEntity()` success: returns true, deducts upgrade cost, increments entity level (GeneratorEntity).
- `upgradeEntity()` insufficient scrap: returns false, scrap unchanged, entity level unchanged.
- `upgradeEntity()` at max level: returns false, scrap unchanged.
- `upgradeEntity()` with exact scrap boundary: succeeds, leaves zero balance.
- `upgradeEntity()` success for PowerStorageEntity and TowerEntity.
- `sellGenerator()` / `sellStorage()` / `sellTower()` after upgrade refund `totalScrapInvested` (not just current level cost).
- `sellGenerator()` without upgrade refunds placement cost only (verifying no regression).
- `computeSellValue()` before upgrade equals placement cost only.
- `computeSellValue()` after upgrade equals total invested scrap (placement + upgrade).
- `computeSellValue()` after upgrade includes connected conduit costs alongside total invested scrap.
- `sellGenerator()` after two upgrades refunds the sum of all three level costs.

### Requires LibGDX (not unit tested)

- `drawChevrons()` -- uses ShapeRenderer.
- ContextMenu upgrade button rendering, styling, and click handling.
- Light blue button style creation.
- Integration of upgrade in `selectPlayerAction()`.

### Manual test scenarios

1. **Happy path upgrade.** Place a Solar Panel (costs 50 scrap). Right-click it. Verify "Upgrade (75 scrap)" button appears at top of context menu in light blue. Click it. Verify: scrap decreases by 75, context menu closes, structure is now level 2. Right-click again. Verify "Upgrade (100 scrap)" button shows the level 3 cost. Click it. Verify level 3. Right-click again. Verify "Max Level" disabled button appears.

2. **Insufficient scrap.** With less than 75 scrap remaining, right-click a level-1 Solar Panel. Verify the Upgrade button shows the cost but is red/disabled. Click it -- nothing happens.

3. **Sell after upgrade.** Upgrade a Capacitor from level 1 (cost 25) to level 2 (cost 50). Right-click and sell. Verify scrap refund is 75 (25 + 50) plus any connected conduit costs.

4. **Chevron display.** Place a Solar Panel. Verify no chevrons on the tile. Upgrade to level 2. Verify one gold chevron in the top-right of the tile. Upgrade to level 3. Verify two gold chevrons stacked vertically.

5. **Details panel after upgrade.** Select a level-1 generator (left-click). Verify details show "Level: 1" and level-1 stats. Right-click, upgrade. Left-click the same entity again. Verify details show "Level: 2" with updated stats (capacity 300, generation 100).

6. **Max-level entity with no upgrades available (single-level asset).** Right-click a Tesla Tower (only 1 level in JSON). Verify "Max Level" disabled button appears. No upgrade possible.

7. **Upgrade does not break wiring.** Wire a generator to a storage. Upgrade the generator. Verify the wire still exists and power flows.

8. **Context menu interaction.** While in wiring mode, right-click to open context menu on a different entity. Verify wiring mode is canceled and the upgrade button appears in the new context menu.

9. **Click-outside dismiss.** Open context menu showing upgrade button. Click elsewhere on the map. Verify menu closes and no upgrade occurs.

## Risks & Trade-offs

1. **`totalScrapInvested` initialization in existing entities.** The `populateGameStateWithDummyData()` method creates entities directly without going through a factory that sets `totalScrapInvested`. The entity constructors must initialize `totalScrapInvested` to the level-1 cost from the asset. If the asset mock does not have levels configured, this will NPE in tests -- `TestAssetFactory` must be updated first.

2. **Light blue button styling.** LibGDX Scene2D button styling depends on the skin's drawable resources. Creating a tinted drawable from the skin's "white" texture is the standard approach, but the skin must have a "white" drawable. If not, a `Pixmap`-based approach may be needed, which allocates a texture once during `openMenu()`. This is acceptable since `openMenu()` is not a hot path.

3. **Chevron visibility at zoom levels.** At far zoom, the 6px chevrons may be too small to see. This is acceptable for the initial implementation -- a future iteration could scale chevron size or switch to a different indicator.

4. **Power bank exceeding new capacity (edge case).** If a future feature allows downgrading, `powerBank` could exceed capacity. For now, upgrades only increase capacity, so this is not a concern. No clamping is added.

## Open Questions

1. **Should the context menu re-open after upgrade to allow consecutive upgrades?** Current design closes the menu. The player must right-click again to upgrade further. This is simpler and consistent with sell behavior but slightly less convenient for multi-level upgrades.

2. **Should the Upgrade button show a preview of next-level stats?** The requirements say no preview is needed. A tooltip could be added later.

3. **Should `totalScrapInvested` be persisted in save files?** Level loading/saving is not yet implemented, so this is deferred. When save/load is added, `totalScrapInvested` must be serialized alongside `level`.
