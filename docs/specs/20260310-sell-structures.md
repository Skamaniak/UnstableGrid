# Sell/Destroy Structures

**Date:** 2026-03-10
**Status:** Done
**Author:** feature-planner

## Motivation

Players can currently build structures and wire them together, but cannot remove them. This means any misplaced building or obsolete wiring permanently wastes scrap and clutters the grid. Selling structures and removing wires -- with scrap refund -- closes the gameplay loop and lets players adapt their defenses over time.

## Design

The feature has four parts that build on each other:

### 1. Scrap Management in GameState

`GameState` already has a private `scrap` field and an incomplete `sellConduit()` stub. We complete the scrap API and add sell methods for all entity types.

**Refund policy:** Selling refunds the entity's full `scrapCost` (100% of build cost). No depreciation factor.

**Cascade deletion for structures:** When a structure (generator, storage, tower) is sold, all conduits where that entity is either the `from` or `to` endpoint must be removed first (each refunding its own scrap cost), then the structure itself is removed and its scrap cost refunded.

**Bug in existing `PowerGrid.removeSource()`:** The current implementation calls `source.removeTo(conduit.to)` instead of `conduit.unregister()`. Since `ConduitEntity` is the actual consumer registered on the source (not the final `to` target), the `removeTo` call targets the wrong object. The conduit's `unregister()` method already calls `from.removeTo(this)` correctly. The sell methods in `GameState` will use a new `findConnectedConduits()` helper and call `sellConduit()` for each, which handles `grid.removeConduit()` -> `conduit.unregister()` correctly. The buggy `PowerGrid.removeSource/removeSink` methods will be fixed to also use `conduit.unregister()` for consistency, though they will no longer be the primary removal path.

**Important:** `PowerGrid.removeStorage()` currently does not cascade-remove conduits at all. A `PowerStorageEntity` can be both a `from` and a `to` on conduits. The new `GameState.sellStorage()` handles this by finding all conduits where the storage is either endpoint.

### 2. Context Menu (ContextMenu UI)

A new Scene2D popup menu replaces the current direct-to-WiringMenu right-click behavior. The context menu is a single `Table` inside a popup `Window` that appears at the mouse position on right-click over any structure.

**For structures that are PowerProducers (GeneratorEntity, PowerStorageEntity):**
- "Wire" button -- selects the entity as wiring source and opens the existing WiringMenu conduit picker. Clicking a conduit type enters Wiring mode just like today.
- "Remove Wire" button -- enters WireRemoval mode centered on this structure.
- "Sell (+X scrap)" button -- two-click confirm (see below). The scrap value X includes the structure's own scrap cost PLUS the scrap cost of all conduits that would be cascade-removed (i.e., all conduits connected to this structure in either direction).

**For structures that are only PowerConsumers (TowerEntity):**
- "Sell (+X scrap)" button only (towers cannot be wiring sources, so no Wire or Remove Wire).

**Two-click sell confirmation:** First click arms the button (changes label to "Confirm Sell (+X scrap)" with a warning/red style). Second click executes the sale. The armed state resets if the player clicks elsewhere, closes the menu, or right-clicks a different entity.

**Sell scrap display calculation:** When building the sell button label, compute the total refund as: `entity.scrapCost + SUM(connectedConduit.scrapCost for all connected conduits)`. Use the same `findConnectedConduits()` helper used by the sell methods.

**Menu lifecycle:**
- Right-click on a structure opens the context menu.
- Left-click outside the menu, pressing Escape, or executing any action closes it.
- The menu stores a reference to the clicked entity and its screen position.

**Integration with existing WiringMenu:** The current `WiringMenu` opens directly on right-click via the `shouldWiringMenuOpen` callback in `GameScreen`. This behavior moves into the ContextMenu: the "Wire" button in the context menu triggers the WiringMenu conduit list to appear. The `shouldWiringMenuOpen` callback and the direct right-click-to-WiringMenu path in `GameScreen` are replaced by the ContextMenu flow.

### 3. Wire Removal Mode (WireRemoval PlayerAction)

A new `PlayerAction` implementation inside `PlayerActionFactory` (following the existing pattern of `Wiring`, `Building`, `DetailsSelection`).

**Entry:** Triggered from the ContextMenu "Remove Wire" button on a `PowerProducer` structure.

**Behavior:**
- No range circle is shown (unlike Wiring mode). Since we are removing existing wires, range is irrelevant -- any connected consumer is a valid target regardless of distance.
- All consumers that have a conduit FROM the source entity are visually highlighted (e.g., a colored overlay or tinted reticle on their tile) so the player immediately sees which entities are valid click targets and understands they should click one.
- On mouse hover over a highlighted consumer: the specific conduit wire from the source to that consumer is drawn in a distinct color (e.g., red) to show which wire would be removed on click. The conduit to highlight is the one where `conduit.from == source && conduit.to == hoveredEntity`.
- Left-clicking a highlighted consumer removes the conduit via `GameState.sellConduit()` (which refunds scrap). The conduit matched is the one where `conduit.from == source && conduit.to == clickedConsumer`.

**Exit:** Returns to `DetailsSelection` after a successful removal, or on right-click elsewhere.

**Finding the conduit to remove:** `GameState.findConduit(PowerSource from, PowerConsumer to)` iterates the `conduits` set. The set is small enough that linear scan is fine.

### 4. Scrap HUD

A `Label` in the top-left corner of the screen (above or offset from the BuildMenu window) showing the current scrap count (e.g., "Scrap: 150").

**Flash effect:** A simple color tween:
- On scrap gain: label color briefly changes to green, then fades back to white over 0.5s.
- On scrap spend: label color briefly changes to red, then fades back to white over 0.5s.

**Implementation approach -- polling:** The HUD stores `lastDisplayedScrap` and compares with `gameState.getScrap()` each frame. If different, it triggers the color flash via a Scene2D color `Action`. This keeps `GameState` free of UI callbacks and avoids adding listener infrastructure.

## Files to Modify

- **`core/src/main/java/com/skamaniak/ugfs/game/GameState.java`**
  - Add `getScrap()` public getter
  - Add `addScrap(int amount)` method
  - Add `spendScrap(int amount)` with validation (returns boolean; false if insufficient funds)
  - Complete `sellConduit()` -- add `addScrap(conduit.conduit.getScrapCost())`
  - Add `sellGenerator(GeneratorEntity)` -- collect connected conduits via `findConnectedConduits()`, sell each conduit (refunding scrap per conduit), remove from `generators` set, remove from `entityByPosition`, call `grid.removeSource()`, then `addScrap(generator level scrapCost)`
  - Add `sellStorage(PowerStorageEntity)` -- same pattern, finds conduits where storage is either `from` or `to`, sells each, removes from `storages`, `entityByPosition`, `grid.removeStorage()`, refunds scrap
  - Add `sellTower(TowerEntity)` -- same pattern, finds conduits where tower is `to`, removes from `towers`, `entityByPosition`, `grid.removeSink()`, refunds scrap
  - Add `sellEntity(GameEntity)` convenience dispatcher (instanceof check, like `registerEntity`)
  - Add `findConduit(PowerSource from, PowerConsumer to)` -- linear scan returning the matching `ConduitEntity` or null
  - Add `findConnectedConduits(Object entity)` -- returns `List<ConduitEntity>` where `conduit.from == entity || conduit.to == entity`
  - Add `computeSellValue(GameEntity entity)` -- returns total scrap refund: entity's own scrap cost + sum of all connected conduits' scrap costs. Used by ContextMenu to display the sell price.

- **`core/src/main/java/com/skamaniak/ugfs/simulation/PowerGrid.java`**
  - Fix `removeSource()` (line 34): replace `source.removeTo(conduit.to)` with `conduit.unregister()`
  - Fix `removeSink()` (line 66): replace `conduit.from.removeTo(sink)` with `conduit.unregister()`
  - Fix `removeStorage()`: add conduit cascade removal (find conduits where `conduit.from == storage || conduit.to == storage`, call `conduit.unregister()`, remove from conduits set) before removing from storages set

- **`core/src/main/java/com/skamaniak/ugfs/action/PlayerActionFactory.java`**
  - Add `WireRemoval` private static inner class implementing `PlayerAction`
  - Add `wireRemoval(GameEntity source)` factory method returning the `WireRemoval` instance
  - `WireRemoval` fields: `GameState gameState`, `PlayerInput input`, `GameEntity source`, `Runnable onWireRemoved`
  - `drawShapes()`: draws a highlight overlay (e.g., colored circle or reticle) on each consumer tile that has a conduit from the source, so the player sees all valid removal targets. When the mouse hovers over one of these highlighted consumers, draws a red line overlay between the source and that consumer to preview which wire would be removed.
  - `handleClick()`: calls `gameState.findConduit(source, clickedConsumer)`, if found calls `gameState.sellConduit(conduit)`, then invokes the `onWireRemoved` callback
  - `handleMouseMove()`: tracks hovered entity for wire highlighting

- **`core/src/main/java/com/skamaniak/ugfs/GameScreen.java`**
  - Add `ContextMenu contextMenu` field; create in constructor
  - Add `ScrapHud scrapHud` field; create in constructor, passing `gameState`
  - Register `contextMenu.getStage()` and `scrapHud.getStage()` in the `InputMultiplexer` in `show()`
  - Update `readInputs()`: add `contextMenu.handleInput()` and `scrapHud.handleInput()`
  - Update `draw()`: add `contextMenu.draw()` and `scrapHud.draw()`
  - Update `resize()`: add `contextMenu.resize()` and `scrapHud.resize()`
  - Update `dispose()`: add `contextMenu.dispose()` and `scrapHud.dispose()`
  - Remove the `shouldWiringMenuOpen()` method (context menu replaces this)
  - Update `selectPlayerAction()`: check ContextMenu state. If context menu requests WireRemoval, set `pendingPlayerAction = playerActionFactory.wireRemoval(entity)`. If context menu triggered wiring via WiringMenu, use existing wiring path. If context menu triggered sell, call `gameState.sellEntity(entity)` and reset to DetailsSelection.
  - Add callback methods for context menu sell and wire removal actions

- **`core/src/main/java/com/skamaniak/ugfs/ui/WiringMenu.java`**
  - Remove the right-click `InputListener` in `setupInputListener()` that currently opens the menu on right-click directly
  - Add `show(float x, float y)` public method that positions the menu table and makes it visible
  - Keep `getSelectedConduit()`, `resetSelection()`, and all other public API as-is

## Files to Create

- **`core/src/main/java/com/skamaniak/ugfs/ui/ContextMenu.java`**
  - Scene2D popup menu appearing on right-click over a structure
  - Own `Stage` and `ScreenViewport` (same pattern as `WiringMenu`, `BuildMenu`, `DetailsMenu`)
  - Contains a `Table` with buttons for "Wire", "Remove Wire", "Sell (+X scrap)"
  - Buttons are conditionally shown based on entity type (Wire/Remove Wire only for PowerProducer)
  - Manages two-click sell confirmation state (armed boolean, resets on hide or target change)
  - Computes sell value by calling `gameState.computeSellValue(entity)` to include cascade conduit refunds in the displayed price
  - Exposes state query methods for `GameScreen.selectPlayerAction()`:
    - `getTargetEntity()` -- the right-clicked entity
    - `isWiringRequested()` -- true after "Wire" button clicked
    - `isWireRemovalRequested()` -- true after "Remove Wire" button clicked
    - `isSellConfirmed()` -- true after second click on armed sell button
  - `hide()` method clears all state and makes the menu invisible
  - Right-click listener on the stage opens the menu if `gameState.getEntityAt()` returns non-null

- **`core/src/main/java/com/skamaniak/ugfs/ui/ScrapHud.java`**
  - Own `Stage` and `ScreenViewport`
  - Contains a `Label` positioned in the top-left corner
  - Constructor takes `GameState` reference (for polling scrap)
  - In `handleInput()` (called each frame via `stage.act()`): compares `gameState.getScrap()` with `lastDisplayedScrap`. If changed, updates label text and applies a `SequenceAction(color change, delay, color fade back to white)` using Scene2D Actions
  - Green flash for gain (new > old), red flash for spend (new < old)
  - Standard `draw()`, `resize()`, `dispose()`, `getStage()` methods

## Implementation Steps

- [x] **Step 1: GameState scrap API** -- Add `getScrap()`, `addScrap()`, `spendScrap()`. Complete `sellConduit()` with scrap refund. Add `findConduit()` and `findConnectedConduits()` helper methods. Add `computeSellValue()`.

- [x] **Step 2: Fix PowerGrid remove methods** -- Fix `removeSource()` and `removeSink()` to use `conduit.unregister()`. Fix `removeStorage()` to cascade-remove conduits.

- [x] **Step 3: GameState sell methods** -- Implement `sellGenerator()`, `sellStorage()`, `sellTower()`, and `sellEntity()` dispatcher. Each method: calls `findConnectedConduits()`, sells each conduit (refunding scrap per conduit), removes entity from its set and `entityByPosition`, removes from `PowerGrid`, refunds entity scrap cost.

- [x] **Step 4: ScrapHud** -- Create `ScrapHud` class. Integrate into `GameScreen` (constructor, multiplexer, draw, resize, dispose). Display `gameState.getScrap()`. Implement color flash on change.

- [x] **Step 5: ContextMenu** -- Create `ContextMenu` class with buttons for "Wire", "Remove Wire", "Sell (+X scrap)". Show sell price including cascade conduit refunds. Implement two-click sell confirmation. Integrate into `GameScreen` replacing the direct right-click-to-WiringMenu path.

- [x] **Step 6: Refactor WiringMenu** -- Remove the right-click `InputListener` from `WiringMenu`. Add `show(float x, float y)`. Wire the ContextMenu "Wire" button to open the `WiringMenu`.

- [x] **Step 7: WireRemoval PlayerAction** -- Add `WireRemoval` class inside `PlayerActionFactory`. Highlight all connected consumers with overlays so the player knows which tiles are clickable. On hover over a valid consumer, draw a red line overlay previewing the wire to be removed. On click, remove the conduit and refund scrap. Wire it up in `GameScreen.selectPlayerAction()` via ContextMenu state.

- [x] **Step 8: Integration and selectPlayerAction update** -- Update `GameScreen.selectPlayerAction()` to check ContextMenu state for sell actions, wiring, and wire removal. Ensure the priority chain: ContextMenu actions > BuildMenu > DetailsSelection.

## Testing Plan

### Unit-testable (pure logic, no LibGDX dependencies)

- **`PowerGrid` removal methods** (fully testable, no static dependencies):
  - `removeSource()` cascade-removes all conduits connected to the source and unregisters them
  - `removeSink()` cascade-removes all conduits connected to the sink and unregisters them
  - `removeStorage()` cascade-removes conduits in both directions (where storage is `from` or `to`)
  - After removal, `simulatePropagation()` no longer routes power through removed paths
  - Verify conduit count after removal
  - Verify the source/sink/storage sets no longer contain the removed entity

- **`GameState` scrap and sell methods** -- `GameState` constructor requires `UnstableGrid` which holds LibGDX objects (`SpriteBatch`, `ShapeRenderer`). This class **cannot be directly unit tested** without refactoring. The sell logic is tested indirectly:
  - Pure scrap arithmetic is trivial (addition/subtraction) and verified through `PowerGrid` integration tests plus manual testing
  - `findConnectedConduits()` and `findConduit()` are linear scans over the `conduits` set -- correctness is validated through the `PowerGrid` removal tests which exercise equivalent lookup patterns
  - `computeSellValue()` is simple summation over `findConnectedConduits()` results -- verified manually

- **`ConduitEntity.unregister()`** (already testable): confirm that after `unregister()`, the conduit's `from` source no longer has this conduit in its consumer set.

### Implemented tests

**`core/src/test/java/com/skamaniak/ugfs/simulation/PowerGridRemovalTest`** (16 tests):
- `removeSource_removesSourceFromGrid` -- source set is empty after removal
- `removeSource_cascadesAndRemovesAttachedConduit` -- conduit set is empty after removeSource
- `removeSource_unregistersConduitFromSource_soNoPropagation` -- re-added source doesn't forward power through old conduit
- `removeSource_cascadesMultipleConduitsFromSameSource` -- all conduits from one source are removed together
- `removeSource_doesNotRemoveConduitsFromOtherSources` -- surviving source's conduit is untouched
- `removeSink_removesSinkFromGrid` -- sinks set is empty after removal
- `removeSink_cascadesAndRemovesAttachedConduit` -- conduit set is empty after removeSink
- `removeSink_unregistersConduitFromSource_soNoPropagation` -- conduit unregistered so no forwarding
- `removeSink_onlyRemovesConduitsTerminatingAtThatSink` -- other sinks' conduits are untouched
- `removeStorage_removesStorageFromGrid` -- storages set is empty after removal
- `removeStorage_cascadesOutgoingConduit` -- both incoming and outgoing conduits removed
- `removeStorage_cascadesIncomingConduit` -- incoming conduit is removed
- `removeStorage_unregistersIncomingConduit_soNoPropagation` -- no power routes after storage + conduit removal
- `removeStorage_doesNotRemoveConduitsUnrelatedToRemovedStorage` -- unrelated conduits are untouched
- `afterRemoveSource_simulationDoesNotRouteThroughRemovedSource` -- fresh tower gets no power post-removal
- `afterRemoveStorage_powerFlowChainIsBroken` -- chain gen→storage→tower is severed by storage removal

**`core/src/test/java/com/skamaniak/ugfs/game/entity/ConduitEntityUnregisterTest`** (4 tests):
- `unregister_fromGeneratorSource_conduitNoLongerReceivesPower` -- real GeneratorEntity no longer forwards power after unregister
- `unregister_fromStorageSource_conduitNoLongerReceivesPower` -- real PowerStorageEntity no longer forwards power after unregister
- `register_thenUnregister_conduitCanBeReregistered` -- conduit can be re-registered after unregister
- `unregister_calledTwice_doesNotThrow` -- calling unregister twice does not throw

**`core/src/test/java/com/skamaniak/ugfs/game/entity/ScrapCostTest`** (7 tests):
- `generatorEntity_getScrapCost_returnsLevelScrapCost` -- GeneratorEntity.getScrapCost() delegates to level
- `generatorEntity_getScrapCost_zeroByDefault` -- returns 0 when no scrap cost configured
- `towerEntity_getScrapCost_returnsLevelScrapCost` -- TowerEntity.getScrapCost() delegates to level
- `towerEntity_getScrapCost_zeroByDefault` -- returns 0 when no scrap cost configured
- `powerStorageEntity_getScrapCost_returnsLevelScrapCost` -- PowerStorageEntity.getScrapCost() delegates to level
- `powerStorageEntity_getScrapCost_zeroByDefault` -- returns 0 when no scrap cost configured
- `conduitEntity_getScrapCostFromAsset_returnsConfiguredValue` -- Conduit asset model getScrapCost() returns configured value

**`TestAssetFactory`** updated to add optional `scrapCost` parameter to all four factory methods (`createGenerator`, `createPowerStorage`, `createTower`, `createConduit`) via overloads; existing call sites unchanged.

### Not unit-testable (requires LibGDX)

- `ContextMenu` -- Scene2D UI, requires `SpriteBatch`, `Skin`, `Stage`
- `ScrapHud` -- Scene2D UI
- `WireRemoval.drawShapes()` / `drawTextures()` -- rendering
- `GameScreen.selectPlayerAction()` integration -- requires full game context
- Two-click sell confirmation UX
- Color flash animation on scrap change
- Consumer highlight overlays and wire preview in WireRemoval mode

## Risks & Trade-offs

1. **Cascade conduit removal during sell is O(n) over all conduits.** The conduit set is expected to remain small (tens to low hundreds), so a linear scan per sell is acceptable. If conduit counts grew large, an adjacency index (entity -> set of conduits) would be needed, but that is premature now.

2. **Concurrent modification during cascade sell.** The `sellGenerator/sellStorage/sellTower` methods must collect conduits to remove into a temporary list before iterating, to avoid `ConcurrentModificationException` on the `conduits` set. The existing `PowerGrid.removeSource()` already uses this pattern (`toRemove` list).

3. **Context menu replaces existing right-click behavior.** The current direct right-click-to-WiringMenu is a simpler interaction. Adding a context menu adds one click of indirection for wiring. However, this is necessary to support sell/remove actions on the same right-click target. Players who primarily wire will need one extra click.

4. **`GameState` is not unit-testable** due to its `UnstableGrid` dependency (holds `SpriteBatch`, `ShapeRenderer`). The scrap management and sell logic are pure computation, but they live in a class that cannot be instantiated in tests. Mitigation: test the `PowerGrid` removal paths directly (which is fully possible), and rely on manual testing for the full sell flow. Do NOT refactor `GameState` to extract a testable interface -- that would be adding indirection for testability, which the project conventions forbid.

5. **Selling during active simulation.** Sell actions execute inside `handleClick()` which runs during `readInputs()` (before `simulate()` and `draw()`), so the entity is removed before the next simulate and draw. Timing is safe.

6. **Two-click sell confirmation state.** The "armed" state on the sell button must reset if the player clicks elsewhere, closes the menu, or if a different entity is right-clicked. The `ContextMenu.hide()` method clears the armed state.

7. **Wire highlight rendering in WireRemoval mode.** The `WireRemoval` action needs to draw highlights on connected consumer tiles and a red line overlay for the hovered wire. Rather than duplicating the `ConduitEntity.draw()` texture-based wire rendering, `WireRemoval.drawShapes()` uses `ShapeRenderer` to draw colored circles/rectangles on valid target tiles and a colored line between source and hovered target. This is a simpler approximation that avoids coupling to `ConduitEntity.draw()` internals.

## Open Questions

None -- all design decisions have been resolved.
