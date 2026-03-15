# Enemy Simulation

**Date:** 2026-03-15
**Status:** Done
**Author:** feature-planner

## Requirements

**Enemy asset (new JSON entity type):**
- `id`, `name`, `health`, `speed`, `scrap` (reward on kill), `flying` (boolean)

**Level JSON additions:**
- `base`: `{x, y}` -- tile enemies target
- `waves`: top-level array, each entry: `wave` (number, 1-N), `delay` (seconds after previous wave ended -- or game start for wave 1)
- `spawnLocations`: array, each with `{x, y}` and `spawnPlan`
- `spawnPlan`: array of spawn entries, each with `wave` (references a wave number from `waves`), and `enemies` array
- Each enemy entry: `enemyId` + `delay` (ms after previous enemy in this group spawns)

**Wave lifecycle:**
- Wave 1 starts after its `delay` seconds from game start
- Wave N+1 starts after its `delay` seconds from when all wave-N enemies are dead
- All spawn entries across all spawn locations whose `wave` matches the current wave fire simultaneously
- When no more waves exist: nothing (victory out of scope)

**Pathfinding (ground enemies):** A* on tile grid; obstacles = structures + water + impassable terrain

**Flying enemies:** straight line to base, ignore all obstacles

**Tower targeting:** closest enemy within range; stats from existing tower asset JSON

**Lose condition:** any enemy reaches base tile -> game over -> return to main menu

**Economy:** enemy death awards `enemy.scrap` to scrap pool

**Rendering (temporary placeholder -- no sprites):**
- Spawn points: large red dot at their tile
- Base: large green dot at its tile
- Enemies: large orange dot at their current world position
- Health bar: rendered below each enemy dot showing remaining health proportion

**Additional details:**
- Each enemy displays a small health bar at the bottom area of the enemy dot
- A sample "Zombie" enemy: slow speed, 100 health, 3 scrap reward, not flying
- Tutorial level JSON updated with example base, waves, spawn locations, and spawn plans

## Motivation

The game currently has towers that fire but no enemies to shoot at. Enemy simulation is the core gameplay mechanic that makes the tower defense game playable. Without enemies, the power grid and tower systems have no purpose. This feature connects the existing tower/power infrastructure to actual gameplay by introducing enemies that navigate the map, take damage, and provide scrap rewards.

## Design

### Overview

The feature introduces three major subsystems: (1) enemy spawning driven by wave definitions in the level JSON, (2) enemy movement via A* pathfinding for ground enemies and straight-line movement for flying enemies, (3) tower targeting that connects the existing `TowerEntity.shoot()` stub to actual damage dealing.

### Asset Model: `Enemy` extends `GameAsset`

A new asset type for enemy definitions. Unlike structures, enemies are not buildable -- they have no `buildableOn`, no `buildCost`, no menu icon. The `GameAsset` base class provides `id`, `name`, `description`, `assetType`. The `Enemy` subclass adds `health` (int), `speed` (float, tiles per second), `scrap` (int, reward), and `flying` (boolean).

Enemy assets are loaded from `assets/json/enemy/` at startup, following the existing pattern. A new `ENEMY` value is added to `AssetType` enum.

### Level Model Extensions

The `Level` class gains three new fields, all populated by JSON deserialization:
- `base` (`Level.Position`) -- the tile coordinate enemies move toward.
- `waves` (`List<Level.Wave>`) -- each wave has a `wave` number and `delay` in seconds.
- `spawnLocations` (`List<Level.SpawnLocation>`) -- each has `x`, `y` (tile coords) and a `spawnPlan` list.

`Level.SpawnLocation.SpawnEntry` has `wave` (int referencing a wave number) and `enemies` (list of `Level.SpawnLocation.EnemySpawn` with `enemyId` and `delay` in milliseconds).

These are inner static classes of `Level`, following the existing `Level.Tile` pattern. All fields are private with getters only (JSON deserialization sets them).

### Runtime Enemy: `EnemyInstance`

Not a `GameEntity` (enemies do not occupy a single tile, are not selectable, not part of the power grid). Instead, `EnemyInstance` is a standalone class in `com.skamaniak.ugfs.game.entity` that holds:
- `Enemy enemy` -- the asset definition (immutable stats)
- `float currentHealth`
- `float maxHealth` -- stored at construction for health bar rendering
- `Vector2 worldPosition` -- current world-pixel position (not mesh coords, since enemies move smoothly between tiles)
- `List<Vector2> path` -- sequence of tile-center world positions to follow (ground) or just the base position (flying)
- `int pathIndex` -- current target waypoint
- `boolean alive`
- `boolean reachedBase`

`EnemyInstance` has a `move(float delta)` method that advances `worldPosition` toward the next waypoint in `path` at `enemy.speed * TILE_SIZE_PX` pixels per second. When a waypoint is reached, `pathIndex` increments. When the final waypoint (the base) is reached, the `reachedBase` flag is set. This method is pure math -- no LibGDX statics -- and is unit testable.

`EnemyInstance` has a `takeDamage(int amount)` method that reduces `currentHealth` and sets `alive = false` when health drops to zero or below. Also pure math, unit testable.

`EnemyInstance` has a `getWorldCenter()` method returning the center of the enemy's world position (for distance calculations to towers). The center is `worldPosition` itself since the enemy position represents its center point.

### Health Bar Rendering

Each enemy displays a small health bar below its orange dot. The health bar is rendered in the `ShapeRenderer` pass:
- Background: a dark gray rectangle (e.g., 24px wide, 4px tall) positioned below the enemy circle.
- Foreground: a colored rectangle whose width is `(currentHealth / maxHealth) * 24px`. Color interpolates from green (full health) to red (low health).
- Position: centered horizontally on the enemy, offset ~20px below the enemy center (just below the orange dot radius of 16px).

This requires `EnemyInstance` to expose `getHealthFraction()` returning `currentHealth / maxHealth`.

### Wave Manager: `WaveManager`

A new class in `com.skamaniak.ugfs.game` that owns the wave lifecycle. It is created by `GameState` and holds:
- The parsed wave and spawn data from `Level`
- `int currentWaveNumber` (starts at 0 meaning "no wave started yet")
- `float waveTimer` -- counts down the delay before the next wave starts
- `boolean waveActive` -- true when enemies from the current wave are still alive
- `List<SpawnTimer>` -- active spawn timers for the current wave (tracks per-spawn-entry delay between individual enemies)

Each frame, `GameState.simulateEnemies(delta)` calls `waveManager.update(delta, aliveEnemyCount)`:
1. If no wave is active and there are more waves: decrement `waveTimer`. When it hits zero, start the next wave -- create `SpawnTimer` objects for every `SpawnEntry` whose `wave` matches.
2. For each active `SpawnTimer`: decrement its internal delay counter; when ready, instantiate an `EnemyInstance` with a computed path and add it to the returned list. Remove the `SpawnTimer` when all its enemies are spawned.
3. A wave is "ended" when all `SpawnTimer`s are exhausted AND the `aliveEnemyCount` (passed in) is zero. At that point, `waveActive = false`, and the next wave's delay timer begins.

`WaveManager` does NOT call any LibGDX statics. It receives enemy asset lookups via a `Function<String, Enemy>` passed at construction (resolved from `GameAssetManager` by `GameState`). The `update()` method returns a list of newly spawned `EnemyInstance` objects. Path computation for each enemy is done by a `BiFunction<Vector2, Boolean, List<Vector2>>` (spawn position + flying flag -> path) also passed at construction. This makes `WaveManager` unit testable.

**`SpawnTimer`** is a private inner class of `WaveManager`. It tracks:
- The list of `EnemySpawn` entries remaining to spawn
- `float delayAccumulator` -- time accumulated since last spawn (ms converted to seconds for delta comparison)
- The spawn location's world position
- Index into the enemy list

### Pathfinding: `TilePathfinder`

A new class in `com.skamaniak.ugfs.game` implementing A* on the tile grid.

**Input:** the `Level` tile map, the set of occupied tile positions (from `GameState.entityByPosition`), the `Terrain` data (to check `TerrainType`), start tile `(sx, sy)`, end tile `(bx, by)`.

**Walkable tile:** A tile is walkable if (a) its `TerrainType` is `LAND` and (b) no structure occupies it, OR it is the start or end tile.

**Algorithm:** Standard A* with Manhattan distance heuristic. 4-directional movement (no diagonals). Returns a `List<Vector2>` of tile-center world positions from start to end (inclusive), or `null` if no path exists.

**Performance considerations:**
- The grid is 16x16 (256 tiles max). A* on this is negligible.
- Paths are computed once per enemy at spawn time, not every frame. If a structure is built/sold that changes the grid, existing enemies keep their current paths (they do not repath). This avoids per-frame allocation and keeps simulation simple.
- The pathfinder uses a pre-allocated `boolean[][]` visited array sized to `levelWidth x levelHeight`. Node objects for the open set are unavoidable but only allocated at spawn time, not on the hot path.

**Terrain lookup:** `TilePathfinder` receives the terrain data via constructor parameters: a 2D array of `TerrainType` values (pre-resolved from the `Level` tiles and `Terrain` assets at construction time) and the `entityByPosition` map reference for structure occupancy checks. The terrain array is built once; the entity map is a live reference so new structures are automatically accounted for in future pathfinding calls.

**Flying enemies:** Skip pathfinding entirely. Their path is just `[spawnTileCenter, baseTileCenter]`.

### Tower Targeting

`TowerEntity.shoot()` currently does nothing. It will be changed to accept a `List<EnemyInstance>` parameter (passed down from `GameState.simulateShooting`). The method:
1. Finds the closest alive `EnemyInstance` within `towerLevel().getTowerRange()` tiles (range is in tiles, so multiply by `TILE_SIZE_PX` to compare against world-pixel distance).
2. Calls `enemy.takeDamage(towerLevel().getDamage())`.

The tower's world center is `(position.x + 0.5f) * TILE_SIZE_PX, (position.y + 0.5f) * TILE_SIZE_PX`.

`attemptShot(delta)` currently returns a boolean. Its signature changes to `attemptShot(float delta, List<EnemyInstance> enemies)` so it can pass the list to `shoot()`. This changes the call site in `simulateShooting`.

`simulateShooting` already uses `GameAssetManager.INSTANCE` for sounds, so it remains non-unit-testable. The targeting logic inside `shoot()` will also depend on the enemy list, but the distance calculation itself is pure math.

### GameState Changes

`GameState` gains:
- `List<EnemyInstance> enemies` -- all active enemy instances (persistent `ArrayList`, no per-frame allocation)
- `WaveManager waveManager` -- initialized from `Level` data in the constructor
- `TilePathfinder pathfinder` -- initialized once, reused for all enemy spawns
- `boolean gameOver` -- set when any enemy reaches the base

`simulateEnemies(float delta)` implementation:
1. `waveManager.update(delta, aliveCount)` -- get newly spawned enemies, add to `enemies` list.
2. For each enemy: call `enemy.move(delta)`.
3. Check for enemies that reached the base (`enemy.hasReachedBase()`). If any: set `gameOver = true`.
4. Iterate enemies: remove dead enemies (health <= 0), awarding `enemy.getEnemy().getScrap()` via `addScrap()` for each.
5. Remove enemies that reached the base.

Use `Iterator` for safe removal during iteration, or collect dead/reached into a temporary list first.

`simulateShooting(float delta)` changes to pass `enemies` to `attemptShot`.

`simulate(float delta)` order becomes: `grid.simulatePropagation(delta)` -> `simulateShooting(delta)` -> `simulateEnemies(delta)`. Shooting happens before enemy movement so towers react to enemies' positions at frame start.

### Game Over Flow

`GameScreen.render()` checks `gameState.isGameOver()` after `simulate()`. If true, it transitions to `MainMenuScreen`:
```
game.setScreen(new MainMenuScreen(game));
dispose();
return;
```
This follows the same pattern used in `MainMenuScreen`'s play button. No new UI state machine modes are involved -- this is a screen transition, not a player action.

### Rendering

All placeholder rendering happens in `GameState.drawShapes(ShapeRenderer)`:

- **Spawn points:** `shapeRenderer.setColor(Color.RED); shapeRenderer.circle(worldCenterX, worldCenterY, 20)` for each spawn location from the level data.
- **Base:** `shapeRenderer.setColor(Color.GREEN); shapeRenderer.circle(worldCenterX, worldCenterY, 20)` for the base tile.
- **Enemies:** For each alive enemy:
  - Body: `shapeRenderer.setColor(Color.ORANGE); shapeRenderer.circle(enemy.worldPosition.x, enemy.worldPosition.y, 16)`
  - Health bar background: `shapeRenderer.setColor(Color.DARK_GRAY); shapeRenderer.rect(x - 12, y - 22, 24, 4)`
  - Health bar foreground: color lerped from RED to GREEN based on `getHealthFraction()`; `shapeRenderer.rect(x - 12, y - 22, 24 * healthFraction, 4)`

These are drawn in the existing `ShapeRenderer` pass (already has blending enabled). No new SpriteBatch color contamination concerns since this uses `ShapeRenderer` only.

### Building Interaction with Pathfinding

When a player builds a structure, the tile becomes impassable. This could block future enemy paths. The current design does NOT revalidate paths for already-spawned enemies (they keep their existing path and may walk through the new structure). Future enemies spawned after the build will compute paths around it. If no path exists from a spawn point to the base, ground enemies spawned there will be given a `null` path and should be treated as stuck (they remain stationary and can be killed by towers). This is an intentional simplification.

### Tutorial Level Data

The tutorial map is 16x16. Based on the terrain layout:
- **Base** at tile (8, 8) -- central land area, the point players must defend.
- **Spawn location 1** at tile (1, 14) -- top-left land edge.
- **Spawn location 2** at tile (14, 14) -- top-right land edge.
- **Waves:** 3 waves with increasing enemy counts.
  - Wave 1: 5-second delay, 3 zombies from each spawn.
  - Wave 2: 8-second delay after wave 1 clears, 5 zombies from each spawn.
  - Wave 3: 10-second delay, 8 zombies from each spawn.
- Enemy delay between spawns: 1000ms (1 second apart).

### Sample Enemy: Zombie

```json
{
  "id": "enemy.zombie",
  "name": "Zombie",
  "description": "A slow-moving undead creature shambling toward the base.",
  "assetType": "ENEMY",
  "health": 100,
  "speed": 1.0,
  "scrap": 3,
  "flying": false
}
```
Speed of 1.0 means 1 tile per second (64px/s), which is slow enough for towers to get multiple shots.

## Files to Modify

- `core/src/main/java/com/skamaniak/ugfs/asset/model/AssetType.java` -- add `ENEMY("assets/json/enemy")` enum value
- `core/src/main/java/com/skamaniak/ugfs/asset/model/Level.java` -- add `base`, `waves`, `spawnLocations` fields and inner classes (`Position`, `Wave`, `SpawnLocation`, `SpawnEntry`, `EnemySpawn`)
- `core/src/main/java/com/skamaniak/ugfs/asset/JsonAssetLoader.java` -- add `loadEnemies()` method
- `core/src/main/java/com/skamaniak/ugfs/asset/GameAssetManager.java` -- add `enemies` map, `getEnemy(String)`, `getEnemies()` methods; call `jsonAssetLoader.loadEnemies()` in constructor
- `core/src/main/java/com/skamaniak/ugfs/game/GameState.java` -- add `enemies` list, `waveManager`, `pathfinder`, `gameOver` flag; implement `simulateEnemies(delta)`; modify `simulateShooting(delta)` to pass enemies to towers; add `isGameOver()` getter; extend `drawShapes()` for placeholder rendering of spawn points, base, enemies with health bars
- `core/src/main/java/com/skamaniak/ugfs/game/entity/TowerEntity.java` -- change `attemptShot` signature to accept `List<EnemyInstance>`; implement `shoot()` with closest-enemy targeting and damage dealing
- `core/src/main/java/com/skamaniak/ugfs/GameScreen.java` -- add game-over check in `render()` after `simulate()` to transition to `MainMenuScreen`
- `assets/json/level/tutorial.json` -- add `base`, `waves`, and `spawnLocations` with full spawn plan data for 3 waves of zombies

## Files to Create

- `core/src/main/java/com/skamaniak/ugfs/asset/model/Enemy.java` -- enemy asset model class extending `GameAsset`
- `core/src/main/java/com/skamaniak/ugfs/game/entity/EnemyInstance.java` -- runtime enemy instance with position, health, movement, path following, health fraction getter
- `core/src/main/java/com/skamaniak/ugfs/game/WaveManager.java` -- wave lifecycle and spawn timer logic
- `core/src/main/java/com/skamaniak/ugfs/game/TilePathfinder.java` -- A* pathfinding on tile grid
- `assets/json/enemy/zombie.json` -- a slow ground enemy: 100 health, speed 1.0, 3 scrap, not flying

## Implementation Steps

- [x] 1. Create `Enemy.java` asset model class with `health` (int), `speed` (float), `scrap` (int), `flying` (boolean) fields and getters
- [x] 2. Add `ENEMY` to `AssetType` enum with path `"assets/json/enemy"`
- [x] 3. Add `loadEnemies()` to `JsonAssetLoader`; wire into `GameAssetManager` constructor with `enemies` map, `getEnemy(String)`, `getEnemies()`
- [x] 4. Extend `Level.java` with inner classes: `Position` (x, y), `Wave` (wave, delay), `SpawnLocation` (x, y, spawnPlan list), `SpawnEntry` (wave, enemies list), `EnemySpawn` (enemyId, delay). Add `base`, `waves`, `spawnLocations` fields with getters.
- [x] 5. Create `assets/json/enemy/zombie.json` with the zombie enemy definition
- [x] 6. Update `assets/json/level/tutorial.json`: add `base` at (8,8), `waves` array for 3 waves, `spawnLocations` at (1,14) and (14,14) with spawn plans referencing zombie enemies
- [x] 7. Create `EnemyInstance.java` with constructor, `move(delta)`, `takeDamage(amount)`, `hasReachedBase()`, `isAlive()`, `getWorldCenter()`, `getHealthFraction()`
- [x] 8. Create `TilePathfinder.java` with A* implementation: constructor takes terrain type 2D array and entity position map; `findPath(startX, startY, endX, endY)` returns `List<Vector2>` of tile-center world positions or null
- [x] 9. Create `WaveManager.java` with wave lifecycle, `SpawnTimer` inner class, `update(float delta, int aliveEnemyCount)` returning `List<EnemyInstance>`; receives `Function<String, Enemy>` and `BiFunction<Vector2, Boolean, List<Vector2>>` for path computation
- [x] 10. Modify `TowerEntity`: change `attemptShot(float delta)` to `attemptShot(float delta, List<EnemyInstance> enemies)`; implement `shoot(List<EnemyInstance> enemies)` finding closest alive enemy within `towerRange * TILE_SIZE_PX` pixel distance from tower center, calling `takeDamage`
- [x] 11. Modify `GameState`: add fields (`enemies` as `ArrayList`, `waveManager`, `pathfinder`, `gameOver`); build terrain type array and pathfinder in constructor; initialize `WaveManager` with lambdas; implement `simulateEnemies(delta)` with spawn/move/death/game-over logic; update `simulateShooting` to pass enemies; extend `drawShapes()` to render spawn points (red), base (green), enemies (orange with health bars)
- [x] 12. Modify `GameScreen.render()`: after `gameState.simulate(delta)`, check `gameState.isGameOver()` and transition to `MainMenuScreen` if true
- [x] 13. Compile and verify all tests pass; update existing `TowerEntity` tests for new `attemptShot` signature

## Testing Plan

### Unit testable (pure logic)

- **`EnemyInstance.move(delta)`** -- test that an enemy moves along its path at the correct speed, reaches waypoints, increments `pathIndex`, and sets `reachedBase` when the final waypoint is reached. Test with single-segment and multi-segment paths. Test that `move()` with zero delta does not change position. Test that a stuck enemy (null/empty path) does not move or crash.
- **`EnemyInstance.takeDamage(amount)`** -- test health reduction, death at zero health, overkill below zero.
- **`EnemyInstance.getHealthFraction()`** -- test returns 1.0 at full health, 0.5 at half, 0.0 when dead.
- **`TilePathfinder.findPath()`** -- test A* on small grids: open path, blocked path (returns null), path around obstacles, path on a single-tile corridor. Test that water tiles and occupied tiles are treated as impassable. Test start and end tiles are always walkable.
- **`WaveManager.update(delta, aliveCount)`** -- test wave progression: wave 1 starts after delay, enemies are spawned with correct timing (delay between individual enemies), wave transitions when all enemies die (aliveCount == 0), multiple spawn locations fire simultaneously. Use mocked `Enemy` assets. Test final wave completion (no more waves to start).

### Implemented test classes

#### `EnemyInstanceTest` (`core/src/test/.../game/entity/EnemyInstanceTest.java`)
- `takeDamage_reducesCurrentHealth` — 30 damage on 100 hp enemy leaves 70% health fraction
- `takeDamage_exactlyZeroKillsEnemy` — damage equal to max health sets `isAlive()` false
- `takeDamage_overkillDoesNotGoNegative` — health fraction clamps to 0.0 on overkill
- `takeDamage_overkillSetsAliveToFalse` — isAlive false even on overkill
- `isAlive_trueAtSpawn` — freshly spawned enemy reports alive
- `getHealthFraction_returnsOneAtFullHealth` — fraction = 1.0 at construction
- `getHealthFraction_returnsHalfAtHalfHealth` — fraction = 0.5 after 50 damage
- `getHealthFraction_returnsZeroWhenDead` — fraction = 0.0 after lethal damage
- `move_withNullPath_doesNotMoveOrCrash` — null path: position unchanged
- `move_withEmptyPath_doesNotMoveOrCrash` — empty path: position unchanged
- `move_withNullPath_doesNotSetReachedBase` — null path does not set reachedBase
- `move_withZeroDelta_doesNotChangePosition` — zero delta: no movement
- `move_advancesPositionTowardWaypoint` — speed 1.0, 0.5s → 32px movement
- `move_reachesWaypointExactly` — position snaps to waypoint when exactly reached
- `move_singleWaypoint_setsReachedBaseWhenReached` — reachedBase true after full path traversal
- `move_doesNotSetReachedBaseBeforeReachingEnd` — reachedBase false mid-path
- `move_multiSegment_incrementsPathIndex` — enemy progresses past first waypoint onto second segment
- `move_multiSegment_setsReachedBaseAtFinalWaypoint` — reachedBase after traversing full multi-segment path
- `move_deadEnemy_doesNotMove` — dead enemy stays at spawn position
- `move_speedIsScaledByTileSize` — speed 2.0 tiles/s travels 2 * TILE_SIZE_PX pixels per second

#### `TilePathfinderTest` (`core/src/test/.../game/TilePathfinderTest.java`)
- `findPath_startOutOfBounds_returnsNull` — negative coordinate returns null
- `findPath_endOutOfBounds_returnsNull` — coordinate >= width returns null
- `findPath_startEqualsEnd_returnsSingleNodePath` — trivial same-tile path has 1 node
- `findPath_startEqualsEnd_containsTileCenter` — single-node path is at tile center world coords
- `findPath_straightLine_returnsCorrectLength` — 4-step path has 5 nodes
- `findPath_straightLine_startsAtSpawn` — first node is spawn tile center
- `findPath_straightLine_endsAtBase` — last node is base tile center
- `findPath_completelyBlocked_returnsNull` — water wall between start and end returns null
- `findPath_waterTilesAreImpassable` — column of WATER blocks path
- `findPath_rockTilesAreImpassable` — ROCKS terrain is not walkable
- `findPath_aroundSingleObstacle_findsDeroute` — single occupied tile forces detour
- `findPath_occupiedTilesAreImpassable` — column of entities (LAND terrain) blocks path
- `findPath_startTileIsAlwaysWalkable_evenIfOccupied` — occupied start tile still valid
- `findPath_endTileIsAlwaysWalkable_evenIfOccupied` — occupied end tile still valid destination
- `findPath_endTileIsAlwaysWalkable_evenIfWater` — WATER end tile is valid destination
- `findPath_pathNodesAreAtTileCenters` — each node x is at (n+0.5)*TILE_SIZE_PX
- `findPath_openGrid_pathLengthIsManhattanDistancePlusOne` — shortest path in open grid

#### `WaveManagerTest` (`core/src/test/.../game/WaveManagerTest.java`)
- `update_withNullWaves_returnsEmptyList` — null waves list → always empty
- `update_withEmptyWaves_returnsEmptyList` — empty waves list → always empty
- `update_beforeWaveOneDelay_spawnsNothing` — no spawn before delay expires
- `update_exactlyAtWaveOneDelay_startsWave` — enemy spawns at exact delay moment
- `update_firstEnemySpawnsImmediatelyWhenDelayIsZero` — 0ms delay spawns on first tick
- `update_secondEnemyNotYetReadyBeforeDelay` — second enemy not spawned before 1000ms
- `update_secondEnemySpawnsAfterDelay` — second enemy spawns after 1000ms delay
- `update_multipleSpawnLocations_bothSpawnOnWaveStart` — two locations each produce one enemy simultaneously
- `update_waveEnds_whenAllSpawnersExhaustedAndAliveCountZero` — wave transitions when spawners done and alive=0
- `update_wave2StartsAfterItsDelay_followingWave1End` — wave 2 does not start before its delay
- `update_afterFinalWave_returnsEmptyListForever` — no more spawning after all waves complete
- `update_waveDoesNotEndWhileEnemiesAreAlive` — wave transition blocked while aliveCount > 0
- `update_spawnEntriesForOtherWavesAreIgnored` — non-matching wave entries do not fire
- `update_callsEnemyLookupWithCorrectId` — EnemyLookup is called with the correct enemy id
- `update_callsPathComputerWithFlyingFlag` — PathComputer receives the flying flag from the enemy asset
- `update_spawnedInstanceIsAlive` — freshly spawned EnemyInstance.isAlive() is true

#### `TowerEntityShootingTest` (`core/src/test/.../game/entity/TowerEntityShootingTest.java`)
- `attemptShot_emptyEnemyList_returnsTrue_butNoCrash` — fires true but does not crash with empty list
- `shoot_enemyWithinRange_receivesDamage` — enemy at tower center takes damage
- `shoot_enemyWithinRange_receivesExactDamageAmount` — exactly 20 damage deducted from 100 hp
- `shoot_enemyOutsideRange_receivesNoDamage` — enemy beyond range is untouched
- `shoot_targetsClosestAliveEnemy` — closer enemy hit, farther is not (list order: far first)
- `shoot_targetsClosestAliveEnemy_orderIndependent` — same result regardless of list order
- `shoot_skipsDeadEnemies` — dead enemy is not re-damaged; live enemy within range is targeted
- `shoot_allEnemiesDead_noCrashAndNoDamage` — only dead enemies in list: no exception
- `shoot_towerAtNonZeroTile_correctlyCalculatesRange` — tower at (2,3) correctly computes center
- `shoot_towerAtNonZeroTile_enemyOutsideRange_notDamaged` — range boundary respected for non-origin tower
- `attemptShot_insufficientPower_doesNotDamageEnemy` — tower with empty power bank does not shoot
- `shoot_enemyAtExactRangeBoundary_isDamaged` — enemy at distSq == rangePx^2 is included (<=)

### Requires LibGDX (not unit tested)

- `GameState.simulateEnemies()` -- orchestrates wave manager, movement, death/removal, scrap awards, and game-over detection. Lives in `GameState` which holds LibGDX references.
- `GameState.drawShapes()` -- placeholder shape rendering for spawn points, base, enemies, and health bars.
- `GameScreen.render()` game-over transition to `MainMenuScreen`.
- `TowerEntity.attemptShot()` call chain from `simulateShooting` is not tested in isolation because `simulateShooting` uses `GameAssetManager`. The targeting logic is covered by `TowerEntityShootingTest` via direct `attemptShot()` calls without the `GameState` wrapper.

### Manual test scenarios

1. **Basic wave spawning:** Start the game. After approximately 5 seconds (wave 1 delay), orange dots should appear at the two spawn point locations (near tiles 1,14 and 14,14) and begin moving toward the green base dot at tile 8,8. Red dots should be visible at spawn locations and a green dot at the base throughout.

2. **Enemy health bars:** Observe that each orange enemy dot has a small colored bar below it. At full health the bar should be green and full width. As towers damage enemies, the bar should shrink and shift toward red.

3. **Ground enemy pathfinding:** Build a structure blocking the direct path between a spawn point and the base. Start a new wave (let current wave die first). Verify newly spawned ground enemies navigate around the structure (their orange dots take a detour path, not walking through it).

4. **Tower kills enemy:** Place a tower with enough power near an enemy path. When enemies enter range, verify the tower fires (sound plays) and enemies eventually disappear (health depleted, orange dot removed). Verify scrap count in the ScrapHud increases by 3 (the zombie's scrap value) per kill.

5. **Wave progression:** Let all enemies in wave 1 die (killed by towers). Verify that after approximately 8 seconds (wave 2 delay), wave 2 enemies spawn. There should be 5 enemies from each spawn point instead of 3.

6. **Game over -- enemy reaches base:** Remove all towers or ensure they have no power. Let an enemy walk all the way to the green base dot at tile 8,8. Verify the game immediately transitions to the main menu screen.

7. **Multiple spawn locations:** When a wave starts, verify enemies spawn from both spawn locations simultaneously (orange dots appearing at both red dots at roughly the same time, with individual enemy delays staggered by 1 second).

8. **No path available:** Build structures to completely wall off one spawn point from the base. When the next wave spawns, verify the enemies from that blocked spawn location appear but stay stationary (do not crash the game). Enemies from the other spawn should move normally.

9. **Scrap accumulation across waves:** Kill several enemies across multiple waves. Verify the scrap total correctly accumulates (each zombie kill adds 3 scrap, visible in ScrapHud which should flash green).

10. **Game-over timing:** Verify game over triggers the instant any single enemy reaches the base tile, even if other enemies are still alive and moving on the map.

11. **Spawn timing within a wave:** Watch enemies spawn from a single location. They should appear approximately 1 second apart (1000ms delay), not all at once.

## Risks & Trade-offs

1. **Path invalidation on build/sell.** Existing enemies do not repath when structures are built or sold. This means enemies may walk through newly placed structures, or continue on longer paths after a structure is sold. This is an intentional simplification to avoid per-frame pathfinding. A future improvement could trigger repath for affected enemies.

2. **No projectile visuals.** Towers deal damage instantly with no projectile travel time. This matches the current design (sound plays, damage is instant). Visual projectiles are a future feature.

3. **Single-threaded pathfinding.** A* runs on the game thread at enemy spawn time. For a 16x16 grid this is negligible, but larger maps would need consideration.

4. **`attemptShot` signature change.** Changing `attemptShot(float delta)` to `attemptShot(float delta, List<EnemyInstance> enemies)` breaks any existing test for `attemptShot`. Tests must be updated to pass an enemy list (empty for "no target" cases, or containing a mock enemy within range for "shoot and damage" cases).

5. **`enemies` list management.** The `enemies` list in `GameState` is a persistent `ArrayList`. Dead enemy removal uses `Iterator.remove()` during iteration, which is O(n) but acceptable for expected enemy counts (tens to low hundreds). No per-frame list allocation.

6. **No wave HUD.** The player has no UI indication of current wave number or time until next wave. This is acceptable for the first iteration but should be added in a follow-up.

7. **Health bar rendering in ShapeRenderer pass.** Drawing health bars for many enemies adds draw calls. For the expected enemy counts (under 100) this is negligible. If enemy counts grow significantly, batching would be needed.

## Open Questions

1. **Should enemies repath when structures are built/sold?** Current design says no. This could lead to enemies walking through new structures. A follow-up feature could add selective repathing.

2. **Should there be a build-phase before wave 1?** The current design starts the wave 1 timer immediately at game start. A future feature could add an explicit "start wave" button.

3. **What happens when an enemy is stuck (no path, ground)?** Current design: enemy stays stationary and can be killed by towers. Should it deal damage to adjacent structures instead? Left as stationary for now.

4. **Should tower range checks use tile distance or pixel distance?** The spec uses pixel (Euclidean) distance with `towerRange` interpreted as tiles (multiplied by `TILE_SIZE_PX`). This gives circular range which is standard for tower defense games.

5. **Should game over have a brief delay or overlay before returning to main menu?** Current design transitions immediately. A future improvement could show a "Game Over" overlay with a short delay.
