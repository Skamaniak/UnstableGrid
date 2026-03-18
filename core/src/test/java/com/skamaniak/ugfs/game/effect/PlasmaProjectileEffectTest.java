package com.skamaniak.ugfs.game.effect;

import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.TestAssetFactory;
import com.skamaniak.ugfs.asset.model.Enemy;
import com.skamaniak.ugfs.game.entity.EnemyInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PlasmaProjectileEffect — the only visual effect with non-trivial
 * pure logic: projectile movement, deferred damage on arrival, no-damage when
 * the target is already dead, and ImpactEffect spawning.
 *
 * draw*() methods are NOT tested (ShapeRenderer dependency).
 */
class PlasmaProjectileEffectTest {

    private static final float SPEED = 400f; // must match PlasmaProjectileEffect.SPEED

    private List<VisualEffect> effectsList;

    @BeforeEach
    void setUp() {
        effectsList = new ArrayList<>();
    }

    /** Creates a live EnemyInstance at the given world position with 100 hp. */
    private EnemyInstance enemyAt(float worldX, float worldY) {
        Enemy asset = TestAssetFactory.createEnemy(100, 1.0f, 3, false);
        return new EnemyInstance(asset, new Vector2(worldX, worldY), null);
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    void plasma_initiallyAlive() {
        EnemyInstance target = enemyAt(200f, 0f);
        PlasmaProjectileEffect plasma = new PlasmaProjectileEffect(0f, 0f, target, 30, effectsList);

        assertTrue(plasma.isAlive(), "PlasmaProjectileEffect should start alive");
    }

    @Test
    void plasma_doesNotDamageEnemyBeforeArrival() {
        // Target 200px away; with SPEED=400, one frame of 0.1s moves only 40px
        EnemyInstance target = enemyAt(200f, 0f);
        PlasmaProjectileEffect plasma = new PlasmaProjectileEffect(0f, 0f, target, 30, effectsList);

        plasma.update(0.1f);

        assertEquals(1.0f, target.getHealthFraction(), 0.001f,
            "Enemy should not take damage before the projectile arrives");
        assertTrue(plasma.isAlive(), "Projectile should still be alive mid-flight");
    }

    // -------------------------------------------------------------------------
    // Arrival and deferred damage
    // -------------------------------------------------------------------------

    @Test
    void plasma_dealsDamageOnArrival() {
        // Target at (100, 0) from (0, 0). With SPEED=400 one update of 0.5s covers 200px > 100px,
        // so projectile arrives and applies damage.
        EnemyInstance target = enemyAt(100f, 0f);
        PlasmaProjectileEffect plasma = new PlasmaProjectileEffect(0f, 0f, target, 25, effectsList);

        plasma.update(0.5f); // step > distance → arrival

        assertEquals(0.75f, target.getHealthFraction(), 0.001f,
            "Enemy (100hp) should lose 25hp on projectile arrival (75% remaining)");
    }

    @Test
    void plasma_diesAfterArrival() {
        EnemyInstance target = enemyAt(100f, 0f);
        PlasmaProjectileEffect plasma = new PlasmaProjectileEffect(0f, 0f, target, 25, effectsList);

        plasma.update(0.5f);

        assertFalse(plasma.isAlive(), "Projectile should be dead after hitting its target");
    }

    @Test
    void plasma_spawnsImpactEffectOnArrival() {
        EnemyInstance target = enemyAt(100f, 0f);
        PlasmaProjectileEffect plasma = new PlasmaProjectileEffect(0f, 0f, target, 25, effectsList);

        plasma.update(0.5f);

        assertEquals(1, effectsList.size(),
            "An ImpactEffect should be added to the effects list when the projectile arrives");
        assertInstanceOf(ImpactEffect.class, effectsList.get(0),
            "The spawned effect should be an ImpactEffect");
    }

    // -------------------------------------------------------------------------
    // Dead-enemy behaviour
    // -------------------------------------------------------------------------

    @Test
    void plasma_noDecimalDamageIfEnemyDeadOnArrival() {
        EnemyInstance target = enemyAt(100f, 0f);
        target.takeDamage(100); // kill before projectile arrives
        PlasmaProjectileEffect plasma = new PlasmaProjectileEffect(0f, 0f, target, 25, effectsList);

        plasma.update(0.5f);

        assertEquals(0.0f, target.getHealthFraction(), 0.001f,
            "Dead enemy's health should stay at 0 — no additional damage applied");
    }

    @Test
    void plasma_stillDiesAfterReachingDeadEnemy() {
        EnemyInstance target = enemyAt(100f, 0f);
        target.takeDamage(100);
        PlasmaProjectileEffect plasma = new PlasmaProjectileEffect(0f, 0f, target, 25, effectsList);

        plasma.update(0.5f);

        assertFalse(plasma.isAlive(), "Projectile should die even when target is already dead");
    }

    @Test
    void plasma_spawnsImpactEffectEvenWhenTargetDead() {
        EnemyInstance target = enemyAt(100f, 0f);
        target.takeDamage(100);
        PlasmaProjectileEffect plasma = new PlasmaProjectileEffect(0f, 0f, target, 25, effectsList);

        plasma.update(0.5f);

        assertEquals(1, effectsList.size(),
            "ImpactEffect should still spawn even if the target enemy was already dead");
    }

    // -------------------------------------------------------------------------
    // Mid-flight enemy death: projectile tracks last known position
    // -------------------------------------------------------------------------

    @Test
    void plasma_tracksLiveEnemyPosition_updatesLastKnown() {
        // Enemy starts at (400, 0). On first update (0.1s) enemy is still alive so last-known
        // position is refreshed. Projectile moves 40px closer. Enemy is then killed.
        // Second update: projectile continues to last-known (400, 0) rather than stopping.
        EnemyInstance target = enemyAt(400f, 0f);
        PlasmaProjectileEffect plasma = new PlasmaProjectileEffect(0f, 0f, target, 30, effectsList);

        // First tick — target is still alive, projectile moves 40px
        plasma.update(0.1f);
        assertTrue(plasma.isAlive(), "Projectile should still be in-flight after 0.1s");
        assertEquals(1.0f, target.getHealthFraction(), 0.001f,
            "Enemy should not be damaged mid-flight (only 40/400 px covered)");

        // Kill the target after the first tick
        target.takeDamage(100);

        // Remaining ticks until arrival at (400, 0) — need (400-40)/400 = 0.9s more
        plasma.update(0.9f);

        assertFalse(plasma.isAlive(), "Projectile should reach last-known position and die");
    }

    @Test
    void plasma_noExtraDamageWhenEnemyDiedMidFlight() {
        EnemyInstance target = enemyAt(400f, 0f);
        PlasmaProjectileEffect plasma = new PlasmaProjectileEffect(0f, 0f, target, 30, effectsList);

        plasma.update(0.1f);   // partial travel while alive
        target.takeDamage(100); // enemy dies mid-flight
        plasma.update(0.9f);   // projectile reaches last-known pos

        // Enemy health fraction is exactly 0 from the pre-kill takeDamage; no additional damage
        assertEquals(0.0f, target.getHealthFraction(), 0.001f,
            "Health should remain at 0 — no extra damage from the plasma on arrival");
    }

    // -------------------------------------------------------------------------
    // Incremental movement: projectile moves step-by-step
    // -------------------------------------------------------------------------

    @Test
    void plasma_movesIncrementallyEachFrame() {
        // Target at (400, 0). Each 0.1s update moves 40px. After 5 frames (200px) projectile
        // is still alive and has not dealt damage.
        EnemyInstance target = enemyAt(400f, 0f);
        PlasmaProjectileEffect plasma = new PlasmaProjectileEffect(0f, 0f, target, 30, effectsList);

        for (int i = 0; i < 5; i++) {
            plasma.update(0.1f);
        }

        assertTrue(plasma.isAlive(), "Projectile should still be alive after travelling 200/400 px");
        assertEquals(1.0f, target.getHealthFraction(), 0.001f,
            "Enemy should be undamaged while projectile is still in-flight");
        assertTrue(effectsList.isEmpty(), "No ImpactEffect should exist mid-flight");
    }

    @Test
    void plasma_arrivesAndDamagesAfterEnoughFrames() {
        // 10 frames of 0.1s = 400px total travel == 400px distance
        EnemyInstance target = enemyAt(400f, 0f);
        PlasmaProjectileEffect plasma = new PlasmaProjectileEffect(0f, 0f, target, 30, effectsList);

        for (int i = 0; i < 10; i++) {
            plasma.update(0.1f);
        }

        assertFalse(plasma.isAlive(), "Projectile should have arrived and died after 10 frames");
        assertEquals(0.7f, target.getHealthFraction(), 0.001f,
            "Enemy (100hp) should have 70hp remaining after taking 30 damage on arrival");
    }

    // -------------------------------------------------------------------------
    // update() is a no-op when already dead
    // -------------------------------------------------------------------------

    @Test
    void plasma_updateAfterDeathIsNoOp() {
        EnemyInstance target = enemyAt(100f, 0f);
        PlasmaProjectileEffect plasma = new PlasmaProjectileEffect(0f, 0f, target, 25, effectsList);

        plasma.update(0.5f); // arrives, dies, spawns 1 ImpactEffect
        int impactCountAfterArrival = effectsList.size();

        plasma.update(0.5f); // second update on a dead projectile
        plasma.update(0.5f);

        assertFalse(plasma.isAlive(), "Projectile should remain dead");
        assertEquals(impactCountAfterArrival, effectsList.size(),
            "No additional ImpactEffects should be spawned after the projectile is dead");
    }
}
