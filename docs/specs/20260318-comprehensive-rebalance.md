# Comprehensive Rebalance: Towers, Power Economy, Enemies & Tutorial

**Date:** 2026-03-18
**Status:** Done
**Author:** feature-planner

## Requirements

### Tower Rebalance & Upgrade Levels
Rebalance all 5 offensive towers with distinct roles and add 3 upgrade levels per tower with meaningful stat progression. Tower roles:
- **Laser** — cheap, fast-firing, low damage. Starter/workhorse tower.
- **Plasma** — mid-cost projectile, good single-target DPS. Main damage dealer. Has `deferDamage: true`.
- **Tesla** — expensive, slow, massive single-hit damage. Boss/heavy killer.
- **Microwave** — AOE fast-tick, low damage per hit. Swarm clearer.
- **EMP** — expensive AOE nuke, very slow. Wave-clearing ultimate.
- **Barrier** — cheap wall for pathing (stays as-is, no rebalance needed).

Each tower currently only has level 1. Add levels 2 and 3 with meaningful stat increases (damage, range, fire rate, power efficiency improvements). The upgrade system already exists in TowerEntity — it reads from the `levels` array in the JSON.

### Power Economy Rebalance
Rebalance generators (solar-panel.json, water.json) and power storages (battery.json, capacitor.json) to support the "build the power grid" narrative. The core challenge of the game is building power infrastructure. Towers should require real investment in generators, storages, and conduits. A single generator should NOT be able to sustain a high-tier tower — players should need to think about power chains.

Current generator stats:
- Solar Panel: L1 50 gen/s, 100 storage, 50 scrap → L3 150 gen/s, 600 storage, 100 scrap
- Water Generator: L1 100 gen/s, 200 storage, 75 scrap → L3 200 gen/s, 600 storage, 175 scrap

Current power storage stats:
- Battery: L1 10000 capacity, 2 loss, 75 scrap → L3 20000 capacity, 4 loss, 150 scrap
- Capacitor: L1 1000 capacity, 4 loss, 25 scrap → L3 2000 capacity, 8 loss, 50 scrap

### New Enemy Types
Create 3-4 new enemy types to add variety beyond zombie (10 HP, speed 1.0, ground) and zombird (5 HP, speed 2.0, flying). Enemy asset fields are: health, speed, scrap (kill reward), flying (boolean). Suggested archetypes:
- Tanky slow ground enemy
- Fast runner ground enemy
- Shielded/armored heavy ground enemy
- Flying tank (high HP flying enemy)

### Tutorial Level Update
Update the spawn plans in assets/json/level/tutorial.json to showcase all enemy types across 3 waves. The tutorial is currently just a testing playground, so difficulty doesn't matter — the goal is to see all enemy types in action. Keep the same level structure (16x16 map, 2 spawn points at (7,15) and (8,15), base at (5,3), 3 waves).

## Motivation

The game currently has placeholder balance values and only two enemy types. Towers lack upgrade levels (only L1 exists), making the upgrade system non-functional. The power economy is trivially solved — a single generator can sustain most towers. This rebalance establishes distinct tower roles, creates meaningful power infrastructure decisions, adds enemy variety, and makes the tutorial showcase all content.

## Design

Balance values are **JSON-only changes** — all stats live in asset JSON files loaded at startup by `JsonAssetLoader`. The tower upgrade system already reads from the `levels` array in each tower JSON, so adding L2/L3 entries is all that is needed. The one Java change is in `GameState.drawShapes()` to render enemy types with distinct colors, sizes, and shapes (circles for ground, triangles for flying).

### Design Principles

1. **Power scarcity drives gameplay.** Towers should consume enough power that players must build dedicated power chains (generator -> storage -> conduit -> tower). A single L1 Solar Panel (50 gen/s) should barely sustain a L1 Laser but not a Plasma or Tesla.

2. **Tower roles are defined by fire rate x damage x power cost.** Each tower occupies a unique niche in the DPS-per-power-invested spectrum.

3. **Upgrade scaling is sub-linear for stats, linear for cost.** L2 gives ~50% more effectiveness for ~60-75% more scrap. L3 gives ~100% more effectiveness (vs L1) for ~100-150% more scrap. This makes upgrades efficient but not game-breaking.

4. **Enemy variety creates demand for different tower types.** Swarms need Microwave/EMP, heavies need Tesla/Plasma, flyers need Laser/Plasma (the only towers that can target flying enemies).

5. **Anti-air is a scarce capability.** Only Laser and Plasma can target flying enemies. Tesla (needs grounding), Microwave (ground radiation), and EMP (ground pulse) cannot. This is controlled by a `canTargetFlying` boolean on the Tower JSON — data-driven, no hardcoded ID checks, no default value. Every tower JSON must explicitly declare `canTargetFlying`.

### Tower Balance Analysis

Key derived metrics for each tower at each level, computed as DPS (damage x fireRate) and power/second consumption (powerCostShot x fireRate + powerLossStandby):

#### Power Budget Reference
- L1 Solar Panel: 50 power/s generation
- L1 Water Gen: 100 power/s generation
- L3 Solar Panel: 150 power/s generation
- L3 Water Gen: 200 power/s generation

The balance is designed so that:
- L1 Laser (105 power/s firing) needs ~2 L1 Solar Panels or 1 L1 Water Gen
- L1 Plasma (285 power/s firing) needs ~3 L1 Solar Panels or a L1 Water Gen + battery buffer
- L1 Tesla (101 power/s firing, bursty) needs large storage buffer + steady generation
- L1 Microwave (510 power/s firing) needs serious power infrastructure
- L1 EMP (260 power/s firing, very bursty) needs massive storage buffer

This ensures even L1 towers require power planning, and L3 towers demand significant grid investment.

### Tower Stats

#### Laser Tower (Starter/Workhorse — cheap, fast, low damage) — `canTargetFlying: true`
| Stat | L1 | L2 | L3 |
|------|----|----|-----|
| damage | 1 | 2 | 3 |
| fireRate | 5.0 | 5.5 | 6.0 |
| towerRange | 4 | 4.5 | 5 |
| powerStorage | 500 | 600 | 750 |
| powerCostShot | 20 | 22 | 25 |
| powerLossStandby | 1 | 1 | 2 |
| scrapCost | 50 | 75 | 100 |

DPS: L1=5, L2=11, L3=18. Power/s firing: L1=101, L2=122, L3=152. Cheap and efficient — the backbone tower.

#### Plasma Tower (Main DPS — mid-cost projectile, strong single-target) — `canTargetFlying: true`
| Stat | L1 | L2 | L3 |
|------|----|----|-----|
| damage | 12 | 18 | 25 |
| fireRate | 1.0 | 1.2 | 1.5 |
| towerRange | 3 | 3.5 | 4 |
| powerStorage | 1000 | 1200 | 1500 |
| powerCostShot | 250 | 275 | 300 |
| powerLossStandby | 3 | 4 | 5 |
| scrapCost | 75 | 125 | 175 |

DPS: L1=12, L2=21.6, L3=37.5. Power/s firing: L1=253, L2=334, L3=455. High damage per shot, needs solid power chain.

#### Tesla Tower (Boss Killer — expensive, slow, massive single-hit) — `canTargetFlying: false`
| Stat | L1 | L2 | L3 |
|------|----|----|-----|
| damage | 100 | 160 | 250 |
| fireRate | 0.2 | 0.25 | 0.3 |
| towerRange | 3 | 3.5 | 4 |
| powerStorage | 3000 | 4000 | 5000 |
| powerCostShot | 500 | 600 | 750 |
| powerLossStandby | 1 | 2 | 3 |
| scrapCost | 200 | 325 | 450 |

DPS: L1=20, L2=40, L3=75. Power/s firing: L1=101, L2=152, L3=228. Low sustained power draw but huge per-shot cost requires large storage buffers. Excels vs high-HP targets.

**Note:** Tesla L1 `powerStorage` reduced from 1000 to 3000 — the old value of 1000 was too small to hold even two shots (500 each). With 3000 storage, it can buffer 6 shots worth, making battery pairing meaningful.

#### Microwave Tower (Swarm Clearer — AOE fast-tick, low damage) — `canTargetFlying: false`
| Stat | L1 | L2 | L3 |
|------|----|----|-----|
| damage | 1 | 2 | 3 |
| fireRate | 10.0 | 10.0 | 10.0 |
| towerRange | 2 | 2.5 | 3 |
| powerStorage | 1000 | 1200 | 1500 |
| powerCostShot | 50 | 55 | 60 |
| powerLossStandby | 5 | 6 | 8 |
| scrapCost | 100 | 175 | 250 |

DPS per enemy in range: L1=10, L2=20, L3=30 (AOE — hits ALL enemies in range each tick). Power/s firing: L1=505, L2=556, L3=608. Extreme power hunger balanced by devastating swarm clear. Range increase is the key upgrade — more range means more enemies caught.

**Note:** Microwave L1 `powerCostShot` increased from 100 to 50. At 100 cost x 10 fire rate = 1000 power/s, which was absurdly expensive. At 50 x 10 = 500/s it is still the most power-hungry tower but achievable with 3+ generators.

#### EMP Tower (Wave-Clear Ultimate — AOE nuke, very slow) — `canTargetFlying: false`
| Stat | L1 | L2 | L3 |
|------|----|----|-----|
| damage | 250 | 400 | 600 |
| fireRate | 0.1 | 0.12 | 0.15 |
| towerRange | 4 | 4.5 | 5 |
| powerStorage | 5000 | 6500 | 8000 |
| powerCostShot | 2500 | 3000 | 3500 |
| powerLossStandby | 10 | 12 | 15 |
| scrapCost | 250 | 400 | 550 |

DPS per enemy in range: L1=25, L2=48, L3=90 (AOE). Power/s firing: L1=260, L2=372, L3=540. Massive burst damage on 7-10s cooldown. Needs huge storage buffer to fire at all. The ultimate tower — extremely expensive but game-changing.

**Note:** EMP L1 `towerRange` reduced from 6 to 4. Range 6 on a 16x16 map covers nearly half the field, which combined with 250 AOE damage was overpowered. Starting at 4 and scaling to 5 keeps it powerful but requires placement thought.

#### Barrier (No changes)
Stays as-is: L1 only, 15 scrap, no combat stats.

### Power Economy Stats

#### Solar Panel (Cheap, low output, land-based)
| Stat | L1 | L2 | L3 |
|------|----|----|-----|
| powerGenerationRate | 50 | 80 | 120 |
| powerStorage | 100 | 200 | 350 |
| scrapCost | 50 | 75 | 100 |

Reduced L3 from 150 to 120 gen/s. Solar panels are the cheap option — players need multiples.

#### Water Generator (Premium, high output, water-only)
| Stat | L1 | L2 | L3 |
|------|----|----|-----|
| powerGenerationRate | 100 | 150 | 200 |
| powerStorage | 200 | 350 | 500 |
| scrapCost | 75 | 125 | 175 |

Unchanged generation rates. Reduced L2/L3 storage slightly (was 400/600). Water generators are strong but terrain-limited — there are only a few water tiles on the tutorial map, making them a premium resource.

#### Battery (High capacity, low loss — pairs with bursty towers)
| Stat | L1 | L2 | L3 |
|------|----|----|-----|
| powerStorage | 5000 | 8000 | 12000 |
| powerLossStandby | 2 | 3 | 4 |
| scrapCost | 75 | 125 | 175 |

Reduced from 10000/15000/20000 to 5000/8000/12000. The old values were so large that a single battery could buffer enough power for dozens of Tesla/EMP shots, trivializing power management. At 5000, a L1 battery holds exactly 10 Tesla shots or 2 EMP shots — players need multiple batteries for burst-heavy setups.

#### Capacitor (Low capacity, higher loss — cheap buffer)
| Stat | L1 | L2 | L3 |
|------|----|----|-----|
| powerStorage | 750 | 1200 | 1800 |
| powerLossStandby | 3 | 5 | 7 |
| scrapCost | 25 | 40 | 60 |

Reduced from 1000/1500/2000 to 750/1200/1800. Capacitors are the cheap option for light buffering (Laser, Plasma). The reduced loss at L1 (from 4 to 3) makes them slightly more efficient early on.

### Conduit Stats (No changes)
Conduits remain as-is:
- Copper Wire: 200 transfer rate, 1 loss, range 4, 10 scrap
- ACSR: 400 transfer rate, 1 loss, range 8, 50 scrap
- Superconductor: 2000 transfer rate, 20 loss, range 4, 200 scrap

These already create meaningful choices. Copper wire's 200 rate is sufficient for Laser/Plasma but not Microwave/EMP, forcing players to invest in ACSR or Superconductor for power-hungry towers.

### New Enemy Types

#### Crawler (Tanky slow ground)
```json
{
  "id": "enemy.crawler",
  "name": "Crawler",
  "description": "A massive armored creature that slowly crawls toward the base. Extremely tough.",
  "assetType": "ENEMY",
  "health": 50,
  "speed": 0.5,
  "scrap": 8,
  "flying": false,
  "shape": "circle",
  "color": [0.6, 0.4, 0.2],
  "radius": 18
}
```
Role: Tank. Absorbs lots of damage. Demands Tesla or sustained Plasma fire. Slow speed gives towers more time to shoot.

#### Runner (Fast ground)
```json
{
  "id": "enemy.runner",
  "name": "Runner",
  "description": "A nimble creature that sprints past defenses. Low health but hard to catch.",
  "assetType": "ENEMY",
  "health": 6,
  "speed": 3.0,
  "scrap": 5,
  "flying": false,
  "shape": "circle",
  "color": [1.0, 1.0, 0.2],
  "radius": 10
}
```
Role: Rush. Tests tower coverage and range. Fast enough to slip through gaps. Demands Laser/Microwave placement or Barrier mazing.

#### Golem (Armored heavy ground)
```json
{
  "id": "enemy.golem",
  "name": "Golem",
  "description": "An enormous stone construct with devastating resilience. The heaviest ground threat.",
  "assetType": "ENEMY",
  "health": 200,
  "speed": 0.3,
  "scrap": 25,
  "flying": false,
  "shape": "circle",
  "color": [0.7, 0.1, 0.1],
  "radius": 24
}
```
Role: Boss-tier. Requires Tesla or concentrated fire from multiple towers. Very slow — gives time to kill but punishes weak setups. High scrap reward.

#### Harpy (Flying tank)
```json
{
  "id": "enemy.harpy",
  "name": "Harpy",
  "description": "A large winged predator that flies directly to the base. Tougher than a zombird.",
  "assetType": "ENEMY",
  "health": 25,
  "speed": 1.5,
  "scrap": 15,
  "flying": true,
  "shape": "triangle",
  "color": [0.0, 0.9, 0.9],
  "radius": 18
}
```
Role: Flying threat. Cannot be blocked by Barriers. Moderate speed, significant health. Forces anti-air tower investment. Harder to kill than zombirds but slower.

### Enemy Visual Differentiation (Data-Driven)

Currently all enemies are drawn as identical orange circles. To make enemy types visually distinct without textures, each enemy JSON defines its own visual properties: **shape**, **color**, and **radius**. The renderer reads these from the asset — no switch on enemy ID.

#### Enemy.java changes

Add three new fields to `Enemy.java`:

- `String shape` — `"circle"` or `"triangle"`. Determines which ShapeRenderer primitive to use.
- `float[] color` — RGB array, e.g. `[1.0, 0.6, 0.0]`. Used as `shapeRenderer.setColor(color[0], color[1], color[2], 1)`.
- `int radius` — pixel radius for the shape. Also used to offset the health bar below.

These fields are read by `GameState.drawShapes()` via `enemy.getEnemy().getShape()`, `.getColor()`, `.getRadius()`. Adding a new enemy type requires only a JSON file — zero Java changes.

#### Visual properties per enemy JSON

| Enemy | shape | color | radius | Rationale |
|-------|-------|-------|--------|-----------|
| Zombie | `"circle"` | `[1.0, 0.6, 0.0]` (orange) | 14 | Familiar default, medium size |
| Crawler | `"circle"` | `[0.6, 0.4, 0.2]` (brown) | 18 | Larger circle = tanky feel |
| Runner | `"circle"` | `[1.0, 1.0, 0.2]` (yellow) | 10 | Small + bright = fast/nimble |
| Golem | `"circle"` | `[0.7, 0.1, 0.1]` (dark red) | 24 | Biggest circle = boss presence |
| Zombird | `"triangle"` | `[0.5, 0.8, 1.0]` (light blue) | 12 | Small triangle = light flyer |
| Harpy | `"triangle"` | `[0.0, 0.9, 0.9]` (cyan) | 18 | Large triangle = heavy flyer |

**Triangle drawing:** For `"triangle"` shape, draw an equilateral triangle (point-up) centered on the enemy position using `shapeRenderer.triangle()`. The `radius` defines the circumradius.

**Health bar:** Stays the same for all types — gray background + red-to-green bar below the shape. Position offset scales with `radius` so it sits below the shape.

### Tutorial Level Spawn Plans

Wave 1 (30s build time) — Introduction. Light enemies, both ground and air:
- Spawn 1 (7,15): 5 zombies, 1s spacing
- Spawn 2 (8,15): 3 zombirds, 2s spacing

Wave 2 (60s build time) — Variety. Introduces crawlers, runners, and harpies:
- Spawn 1 (7,15): 3 runners (1s spacing), then 2 crawlers (2s spacing)
- Spawn 2 (8,15): 3 zombirds (1.5s spacing), then 2 harpies (2s spacing)

Wave 3 (60s build time) — All types including golem boss:
- Spawn 1 (7,15): 4 zombies (0.8s spacing), 2 runners (1s spacing), 2 crawlers (2s spacing), 1 golem (3s delay)
- Spawn 2 (8,15): 3 zombirds (1.5s spacing), 2 harpies (2s spacing), 3 runners (1s spacing)

## Files to Modify

- `assets/json/tower/laser.json` — Add L2 and L3 levels
- `assets/json/tower/plasma.json` — Add L2 and L3 levels
- `assets/json/tower/tesla.json` — Rebalance L1 (powerStorage 1000->3000), add L2 and L3
- `assets/json/tower/microwave.json` — Rebalance L1 (powerCostShot 100->50), add L2 and L3
- `assets/json/tower/emp.json` — Rebalance L1 (towerRange 6->4), add L2 and L3, add `canTargetFlying: false`
- `assets/json/tower/barrier.json` — Add `canTargetFlying: false`
- `assets/json/generator/solar-panel.json` — Reduce L2/L3 generation and storage
- `assets/json/generator/water.json` — Reduce L2/L3 storage
- `assets/json/power-storage/battery.json` — Reduce all level capacities significantly
- `assets/json/power-storage/capacitor.json` — Reduce all level capacities and loss
- `assets/json/level/tutorial.json` — Replace spawn plans with new enemy variety waves
- `core/src/main/java/com/skamaniak/ugfs/asset/model/Tower.java` — Add `canTargetFlying` (boolean) field with getter
- `core/src/main/java/com/skamaniak/ugfs/game/entity/TowerEntity.java` — In `shootSingle()` and `shootAoe()`, skip flying enemies when `tower.isCanTargetFlying()` is false
- `core/src/main/java/com/skamaniak/ugfs/asset/model/Enemy.java` — Add `shape` (String), `color` (float[]), `radius` (int) fields with getters
- `core/src/main/java/com/skamaniak/ugfs/game/GameState.java` — Update enemy rendering loop in `drawShapes()` to read visual properties from enemy asset (data-driven, no switch on ID)
- `assets/json/enemy/zombie.json` — Add visual fields: circle, orange, radius 14
- `assets/json/enemy/zombird.json` — Add visual fields: triangle, light blue, radius 12

## Files to Create

- `assets/json/enemy/crawler.json` — Tanky slow ground enemy
- `assets/json/enemy/runner.json` — Fast ground enemy
- `assets/json/enemy/golem.json` — Armored heavy ground boss
- `assets/json/enemy/harpy.json` — Flying tank enemy

## Implementation Steps

- [x]Create `assets/json/enemy/crawler.json` with stats: 50 HP, 0.5 speed, 8 scrap, ground
- [x]Create `assets/json/enemy/runner.json` with stats: 6 HP, 3.0 speed, 5 scrap, ground
- [x]Create `assets/json/enemy/golem.json` with stats: 200 HP, 0.3 speed, 25 scrap, ground
- [x]Create `assets/json/enemy/harpy.json` with stats: 25 HP, 1.5 speed, 15 scrap, flying
- [x]Update `assets/json/tower/laser.json` — add L2 and L3 levels per stat table above
- [x]Update `assets/json/tower/plasma.json` — add L2 and L3 levels per stat table above
- [x]Update `assets/json/tower/tesla.json` — change L1 powerStorage to 3000, add L2 and L3
- [x]Update `assets/json/tower/microwave.json` — change L1 powerCostShot to 50, add L2 and L3
- [x]Update `assets/json/tower/emp.json` — change L1 towerRange to 4, add L2 and L3
- [x]Update `assets/json/generator/solar-panel.json` — adjust L2 (80 gen/s, 200 storage) and L3 (120 gen/s, 350 storage)
- [x]Update `assets/json/generator/water.json` — adjust L2 (350 storage) and L3 (500 storage)
- [x]Update `assets/json/power-storage/battery.json` — reduce to 5000/8000/12000 capacity, adjust upgrade costs
- [x]Update `assets/json/power-storage/capacitor.json` — reduce to 750/1200/1800 capacity, 3/5/7 loss, adjust costs
- [x]Update `assets/json/level/tutorial.json` — replace spawn plans with 3-wave progression using all 6 enemy types
- [x]Add `canTargetFlying` boolean field to `Tower.java` with getter (no default — must be explicit in every tower JSON)
- [x]Add `"canTargetFlying": true` to laser.json, plasma.json. Add `"canTargetFlying": false` to tesla.json, microwave.json, emp.json, barrier.json.
- [x]Update `TowerEntity.shootSingle()` and `shootAoe()` — skip enemies where `enemy.isFlying()` is true when `tower.isCanTargetFlying()` is false
- [x]Add `shape`, `color`, `radius` fields to `Enemy.java` with getters
- [x]Update `assets/json/enemy/zombie.json` — add `"shape": "circle"`, `"color": [1.0, 0.6, 0.0]`, `"radius": 14`
- [x]Update `assets/json/enemy/zombird.json` — add `"shape": "triangle"`, `"color": [0.5, 0.8, 1.0]`, `"radius": 12`
- [x]Update `GameState.drawShapes()` — replace hardcoded orange circle with data-driven rendering: read `shape`, `color`, `radius` from `enemy.getEnemy()`. Draw circle or triangle based on shape. Adjust health bar offset to scale with radius.

## Testing Plan

### Unit testable (pure logic)
- `TowerEntity.shootSingle()` / `shootAoe()` — the `canTargetFlying` filtering logic. Test that a tower with `canTargetFlying=false` ignores flying enemies and only hits ground enemies, while a tower with `canTargetFlying=true` hits both.
- Existing tests for `TowerEntity`, `PowerGrid`, `EnemyInstance`, and `WaveManager` should continue to pass.

### Implemented in `TowerEntityFlyingTargetingTest`

All 16 tests pass. Tests are split into two `@Nested` classes:

**`SingleTargetingTower` (8 tests — covers `shootSingle` path):**
- `shootSingle_groundOnly_flyingEnemyInRange_notDamaged` — flying enemy is untouched when `canTargetFlying=false`
- `shootSingle_groundOnly_flyingEnemyInRange_doesNotFire` — `attemptShot` returns `false` with only a flying enemy present and `canTargetFlying=false`
- `shootSingle_groundOnly_groundEnemyInRange_isDamaged` — ground enemies are still targeted normally
- `shootSingle_groundOnly_mixedEnemies_onlyGroundIsDamaged` — flying enemy placed closer than ground enemy is skipped; tower hits the farther ground enemy
- `shootSingle_groundOnly_onlyFlyingEnemiesPresent_shotResultNotFired` — `ShotResult.fired` is false when all enemies are flying and `canTargetFlying=false`
- `shootSingle_antiAir_flyingEnemyInRange_isDamaged` — flying enemy is hit when `canTargetFlying=true`
- `shootSingle_antiAir_groundEnemyInRange_isDamaged` — ground enemy is still hit when `canTargetFlying=true`
- `shootSingle_antiAir_targetsClosestRegardlessOfFlyingStatus` — closest-enemy logic applies to both types when `canTargetFlying=true`

**`AoeTargetingTower` (8 tests — covers `shootAoe` path):**
- `shootAoe_groundOnly_flyingEnemyInRange_notDamaged` — flying enemy untouched in AOE with `canTargetFlying=false`
- `shootAoe_groundOnly_flyingEnemyInRange_doesNotFire` — `attemptShot` returns `false` with only a flying enemy in range and `canTargetFlying=false`
- `shootAoe_groundOnly_groundEnemyInRange_isDamaged` — ground enemies still hit by AOE
- `shootAoe_groundOnly_mixedEnemies_onlyGroundEnemiesDamaged` — two flyers and two ground enemies in range; only ground enemies take damage
- `shootAoe_groundOnly_aoeTargetCount_excludesFlyingEnemies` — `ShotResult.aoeTargetCount` counts only ground enemies when `canTargetFlying=false`
- `shootAoe_antiAir_flyingEnemyInRange_isDamaged` — flying enemy hit by AOE when `canTargetFlying=true`
- `shootAoe_antiAir_mixedEnemies_allInRangeDamaged` — both flying and ground enemies damaged when `canTargetFlying=true`
- `shootAoe_antiAir_aoeTargetCount_includesBothTypes` — `ShotResult.aoeTargetCount` includes both types when `canTargetFlying=true`

**`TestAssetFactory` update:** Added a new `createTowerWithRange` overload accepting `canTargetFlying` as an explicit parameter, and updated the existing 8-parameter overload to delegate to it with `canTargetFlying=true` (a neutral default that does not affect any pre-existing test). This allows new tests to set the flag without modifying production code.

### Requires LibGDX (not unit tested)
- JSON deserialization of new enemy types (tested implicitly by game startup)
- Tower level array parsing for L2/L3 entries
- Visual effect spawning for all tower types at all levels
- Enemy visual differentiation rendering (color, size, shape per enemy type)

### Manual test scenarios

1. **Game starts without errors.** Launch the game and load the tutorial level. Verify no JSON parsing errors in the console. All 6 enemy types, 6 tower types, 2 generators, 2 storages, and 3 conduits should load.

2. **Tower upgrade levels are available.** Build each offensive tower (Laser, Plasma, Tesla, Microwave, EMP). Select it and verify the upgrade option appears. Upgrade to L2, verify stats change in the details menu. Upgrade to L3, verify stats change again. Verify no further upgrade is offered at L3.

3. **Laser tower functions as workhorse.** Build a L1 Laser with a Solar Panel and Copper Wire. Verify it fires rapidly (5 shots/s) during wave 1. It should handle zombies efficiently. Upgrade to L3 and verify increased damage and fire rate.

4. **Tesla tower requires storage buffer.** Build a L1 Tesla with only a Solar Panel (50 gen/s). Verify it fires but slowly runs out of power since it costs 500 per shot. Add a Battery — verify it can burst-fire multiple shots from the buffer.

5. **Microwave tower is power-hungry.** Build a L1 Microwave with a single Solar Panel. Verify it runs out of power quickly (500 power/s consumption vs 50 gen/s). Connect 3+ generators or a Water Generator + Battery to sustain it.

6. **EMP tower burst damage.** Build a L1 EMP with a Battery. Verify it fires ~once every 10 seconds, dealing 250 AOE damage. Verify the range circle is noticeably smaller than the old 6-tile range.

7. **Power economy forces infrastructure.** Try to build a L3 Plasma (455 power/s firing) with only one L1 Solar Panel. Verify it cannot sustain fire and depletes power. Add more generators and storages until it sustains. This should require 3+ L3 Solar Panels or 2+ Water Generators.

8. **New enemy types spawn correctly.** Start wave 1. Verify zombies and zombirds appear from the two spawn points. Start wave 2. Verify runners (fast ground), crawlers (slow tanky ground), zombirds, and harpies (flying) appear. Start wave 3. Verify all types appear including the golem (very slow, very tanky).

9. **Enemy visual differentiation.** During waves, verify each enemy type is visually distinct:
   - Ground enemies are circles, flying enemies are triangles (point-up).
   - Zombie = medium orange circle, Crawler = large brown circle, Runner = small yellow circle, Golem = very large dark red circle.
   - Zombird = small light-blue triangle, Harpy = large cyan triangle.
   - Health bars sit below each shape, offset scaling with size.

10. **Anti-air targeting restriction.** During a wave with both ground and flying enemies, verify: Tesla, Microwave, and EMP ignore flying enemies entirely (zombirds/harpies pass through their range unharmed). Laser and Plasma target both ground and flying enemies.

11. **Enemy health scaling is visible.** Observe health bars during wave 3. Zombies should die in 1-2 Laser hits. Runners in 1-2 hits. Crawlers should take many hits. The golem should require sustained fire from multiple towers to kill.

12. **Barrier unchanged.** Build a Barrier. Verify it costs 15 scrap, has no combat stats, and blocks pathing as before.

13. **Battery/Capacitor capacity reduction.** Build a L1 Battery. Verify in details that capacity is 5000 (not 10000). Build a L1 Capacitor. Verify capacity is 750 (not 1000).

## Risks & Trade-offs

1. **Balance will need iteration.** These are first-pass numbers designed around the power-scarcity principle. Actual gameplay testing will reveal if towers are too weak/strong or if the power economy is too punishing/generous. The tutorial level does not need to be balanced — it is a testing playground.

2. **Microwave powerCostShot reduction (100->50) is a significant change.** The old value of 100 made Microwave consume 1000 power/s, which was essentially unusable. The new value of 500 power/s is still the highest of any tower but achievable.

3. **Battery capacity reduction (10000->5000) changes the "feel" of power management.** Players who were used to batteries being infinite buffers will notice they drain faster. This is intentional — it makes battery count and placement matter.

4. **EMP range reduction (6->4) makes it less of a screen-clear.** On a 16x16 map, range 6 was nearly half the map. Range 4 still covers a large area but requires more thought about placement.

5. **No damage types or resistances.** The `damageType` field exists in tower JSON but there is no resistance system on enemies. All damage is flat. A future feature could add resistances that create hard counters (e.g., golems resist heat but are weak to electricity).

## Open Questions

1. **Should the tutorial starting scrap be adjusted?** Currently 1000 scrap. With the power economy rebalance, players need more infrastructure per tower. 1000 may be too little or too much — needs playtesting.

2. **Should conduit transfer rates be rebalanced too?** Copper Wire at 200 rate can sustain a L3 Laser (152 power/s) but not a Microwave (608 power/s). This is arguably correct — it forces conduit upgrades for power-hungry towers. Left unchanged for now.

3. **Wave delays (30s/60s/60s) — are they enough?** With the new enemy variety and power demands, players may need more build time. Left unchanged since the tutorial is a testing playground.
