# Tower Projectiles, Visual Effects & Sound Randomization

**Date:** 2026-03-18
**Status:** Complete
**Author:** feature-planner

## Requirements

Towers currently deal instant (hitscan) damage with only a sound effect — no visual feedback. This feature adds per-tower-type visual effects (beams, arcs, projectiles, pulses), impact effects on enemies, AOE damage for Microwave, deferred damage for Plasma projectiles, and sound pitch/volume randomization to reduce audio fatigue.

| Tower | Visual Effect | Damage Model |
|-------|--------------|--------------|
| Laser | Sustained beam line (fading) | Instant single-target (unchanged) |
| Tesla | Jagged lightning arc | Instant single-target (unchanged) |
| Plasma | Traveling energy blob | **Deferred** — damage on projectile impact |
| Microwave | Expanding pulse from tower | **AOE** — all enemies in range |
| EMP | Expanding ring from tower | **AOE** — all enemies in range |
| Barrier | None | None |

Plus: impact flash on each damaged enemy, and sound randomization (pitch ±15%, volume ±10%).

## Motivation

Without visual effects, players have no visual confirmation that towers are firing or which enemies are being hit. All towers feel identical despite having very different themes. Sound randomization prevents the same tower firing repeatedly from sounding robotic. AOE and deferred damage models make Microwave/EMP and Plasma feel mechanically distinct.

## Design

### Sound Randomization

Replace `sound.play()` with `sound.play(volume, pitch, 0f)` using randomized values:
- Pitch: `0.85f + random.nextFloat() * 0.30f` → range [0.85, 1.15]
- Volume: `0.90f + random.nextFloat() * 0.20f` → range [0.90, 1.10]
- Extract `playTowerSound(TowerEntity)` helper method in `GameState`
- Add a `private static final Random soundRandom = new Random()` field

### ShotResult Data Object

Mutable, reusable struct (one per TowerEntity, no allocation per shot):
- `boolean fired`
- `float towerX, towerY` — tower center in world px
- `float targetX, targetY` — primary target world px
- `float rangePx` — for AOE radius
- `String targeting` — "single", "aoe", "none"
- `int damage` — for deferred projectile
- `EnemyInstance targetEnemy` — for Plasma tracking
- `EnemyInstance[] aoeTargets` (fixed array, size 64) + `int aoeTargetCount`
- `void reset()` method

### Tower.java — `deferDamage` Field

- Add `private boolean deferDamage;` field + `public boolean isDeferDamage()` getter
- JSON deserialization auto-populates; absent = `false`
- `plasma.json` — add `"deferDamage": true`
- `microwave.json` — change `"targeting": "single"` to `"targeting": "aoe"`

### TowerEntity Targeting Refactor

- Add `private final ShotResult shotResult = new ShotResult()` field + getter
- Refactor `shoot()` to populate `shotResult` fields, then dispatch:
  - `"aoe"` → `shootAoe()`: iterate all alive enemies in range, damage each, populate `aoeTargets[]`
  - `"single"` (default) → `shootSingle()`: existing closest-enemy logic, but skip `takeDamage()` when `tower.isDeferDamage()` is true
  - Safety: `fireRate <= 0` returns false in `attemptShot()` (Barrier)

### Visual Effect System

Abstract `VisualEffect` base class with `update(delta)`, `getProgress()`, `drawTextures(batch)`, `drawShapes(shapeRenderer)`.

| Class | Render Method | Duration | Visual |
|-------|--------------|----------|--------|
| `LaserBeamEffect` | `drawShapes` — `rectLine` | 0.15s | Red beam, fading alpha |
| `LightningArcEffect` | `drawShapes` — series of `rectLine` segments | 0.2s | Cyan jagged line, random offsets computed once at creation |
| `PlasmaProjectileEffect` | `drawShapes` — filled circle | distance/speed | Green/yellow blob tracking target enemy, applies deferred `takeDamage()` on arrival |
| `AoePulseEffect` | `drawShapes` — `circle` | 0.3s | Expanding disc, tower-type color, fading alpha |
| `ImpactEffect` | `drawShapes` — small `circle` | 0.1s | White flash shrinking at enemy position |

### PlasmaProjectileEffect — Deferred Damage

- Holds `EnemyInstance targetEnemy`, `int damage`, reference to effects list (for spawning impact effect)
- Each frame: move toward `targetEnemy.getWorldCenter()` (tracks moving enemy)
- Speed: 400 px/s
- On arrival: if `targetEnemy.isAlive()`, call `takeDamage(damage)`. Spawn `ImpactEffect`. Mark self dead.
- If enemy dies mid-flight: projectile still flies to last known position, impact VFX plays, no damage applied.

### GameState Integration

- `List<VisualEffect> effects` field for active effects
- `List<VisualEffect> pendingEffects` staging list — effects spawned during iteration (e.g. `PlasmaProjectileEffect` spawning `ImpactEffect` on arrival) are added here, then flushed into `effects` after the iterator loop completes, avoiding `ConcurrentModificationException`
- `updateEffects(delta)` called in `simulate()` after `simulateEnemies(delta)`
- `spawnShotEffect(TowerEntity)` switches on tower ID to create the right visual
- `private static final Color EMP_COLOR` — pre-allocated to avoid per-shot allocation on the hot path
- Effect drawing in `drawTextures()` and `drawShapes()`

## Implementation Steps

- [x] Step 1: Sound randomization — `playTowerSound()` in `GameState.java`
- [x] Step 2: `ShotResult` data object — new file
- [x] Step 3: `Tower.java` — add `deferDamage` field, update `plasma.json` and `microwave.json`
- [x] Step 4: Refactor `TowerEntity.shoot()` — `shootSingle()`, `shootAoe()`, `ShotResult` population
- [x] Step 5: Visual effect base class and concrete effects — `VisualEffect`, `LaserBeamEffect`, `LightningArcEffect`, `PlasmaProjectileEffect`, `AoePulseEffect`, `ImpactEffect`
- [x] Step 6: Integrate effects into `GameState` — `updateEffects()`, `spawnShotEffect()`, drawing

## Files

### New Files
- `core/src/main/java/com/skamaniak/ugfs/game/entity/ShotResult.java`
- `core/src/main/java/com/skamaniak/ugfs/game/effect/VisualEffect.java`
- `core/src/main/java/com/skamaniak/ugfs/game/effect/LaserBeamEffect.java`
- `core/src/main/java/com/skamaniak/ugfs/game/effect/LightningArcEffect.java`
- `core/src/main/java/com/skamaniak/ugfs/game/effect/PlasmaProjectileEffect.java`
- `core/src/main/java/com/skamaniak/ugfs/game/effect/AoePulseEffect.java`
- `core/src/main/java/com/skamaniak/ugfs/game/effect/ImpactEffect.java`

### Modified Files
- `core/src/main/java/com/skamaniak/ugfs/game/entity/TowerEntity.java` — ShotResult, shoot refactor
- `core/src/main/java/com/skamaniak/ugfs/asset/model/Tower.java` — `deferDamage` field
- `core/src/main/java/com/skamaniak/ugfs/game/GameState.java` — effects list, draw, sound, spawn
- `assets/json/tower/plasma.json` — add `deferDamage: true`
- `assets/json/tower/microwave.json` — change targeting to `"aoe"`
- `core/src/test/java/com/skamaniak/ugfs/TestAssetFactory.java` — new `createTowerWithRange()` overload
- `core/src/test/java/com/skamaniak/ugfs/game/entity/TowerEntityShootingTest.java` — AOE + deferred tests
- `core/src/test/java/com/skamaniak/ugfs/game/entity/ShotResultTest.java` — new test file
- `core/src/test/java/com/skamaniak/ugfs/game/effect/VisualEffectLifecycleTest.java` — new test file
- `core/src/test/java/com/skamaniak/ugfs/game/effect/PlasmaProjectileEffectTest.java` — new test file

## Testing Plan

### Automated (pure logic, no Gdx.* calls)

#### `TowerEntityShootingTest` (pre-existing + extended by implementer)
- AOE targeting: damages all enemies in range, skips dead enemies, skips out-of-range enemies, returns false for empty enemy list
- Deferred damage (`deferDamage=true`): `shoot()` fires and returns true but enemy health is unchanged; `shotResult.targetEnemy` and `shotResult.damage` are populated
- ShotResult population for single-target: `fired`, `towerX/Y`, `targetX/Y`, `targeting="single"`, `targetEnemy` set correctly
- ShotResult population for AOE: `fired`, `targeting="aoe"`, `aoeTargetCount` reflects hit count

#### `ShotResultTest` (new — `core/src/test/.../game/entity/ShotResultTest.java`)
- `reset()` clears every field: `fired`, `towerX/Y`, `targetX/Y`, `rangePx`, `damage`, `targeting`, `targetEnemy`, `aoeTargetCount`
- `reset()` restores `targeting` to `"single"` (not null)
- `aoeTargets` array has capacity 64 (`MAX_AOE_TARGETS`)
- `reset()` is idempotent (safe to call twice)

#### `VisualEffectLifecycleTest` (new — `core/src/test/.../game/effect/VisualEffectLifecycleTest.java`)
- `VisualEffect` base class: fresh effect is alive with progress 0; progress at 0.5 duration is 0.5; effect dies at and past its duration; `getProgress()` clamps to 1.0 past duration; elapsed accumulates across multiple `update()` calls
- `LaserBeamEffect` (duration 0.15s): alive before duration; dies after duration; progress at half duration
- `LightningArcEffect` (duration 0.2s): alive before duration; dies after duration; progress at half duration; construction with zero-length arc does not throw
- `AoePulseEffect` (duration 0.3s): alive before duration; dies after duration; progress at half duration
- `ImpactEffect` (duration 0.1s): alive before duration; dies after duration; progress at half duration

#### `PlasmaProjectileEffectTest` (new — `core/src/test/.../game/effect/PlasmaProjectileEffectTest.java`)
- Newly created projectile is alive
- Enemy takes no damage before projectile arrives
- Enemy takes exact deferred damage on arrival; projectile dies; one `ImpactEffect` is added to the effects list
- Dead enemy on arrival: projectile still dies; no additional damage applied; `ImpactEffect` still spawned
- Mid-flight enemy death: projectile continues to last-known position; no damage applied on arrival
- Incremental movement: after N frames the projectile is still alive if total step < distance; arrives and deals damage after enough frames
- `update()` after death is a no-op (no repeated damage, no extra effects)

### Manual Test Scenarios
- Run game, observe each tower type fires with correct visual effect
- Laser: red beam line from tower to enemy, fades quickly
- Tesla: jagged cyan lightning arc, fades
- Plasma: green blob travels to enemy, enemy takes damage on impact
- Microwave: orange expanding pulse, all enemies in range take damage
- EMP: blue expanding ring
- Impact: white flash on each hit enemy
- Sound: same tower firing repeatedly should sound slightly different each time
- Multiple towers firing simultaneously: no visual glitches
