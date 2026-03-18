package com.skamaniak.ugfs.game.entity;

import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.GameConstants;
import com.skamaniak.ugfs.TestAssetFactory;
import com.skamaniak.ugfs.asset.model.Enemy;
import com.skamaniak.ugfs.asset.model.Tower;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TowerEntity targeting and damage-dealing logic introduced by the
 * enemy-simulation feature. Each test verifies the behaviour of shoot() as
 * exercised through attemptShot(delta, enemies).
 *
 * Tower is placed at tile (0,0) for all tests unless otherwise stated.
 * Tower center in world pixels = (0.5 * 64, 0.5 * 64) = (32, 32).
 */
class TowerEntityShootingTest {

    private static final int TILE = GameConstants.TILE_SIZE_PX; // 64

    // Tower at tile (0,0): center = (32, 32). Range = 3 tiles = 192 px.
    // Damage = 20. FireRate = 1 shot/s. ShotCost = 0 (no power cost simplifies tests).
    private Tower tower;
    private TowerEntity entity;

    @BeforeEach
    void setUp() {
        tower = TestAssetFactory.createTowerWithRange(
            /*capacity*/ 1000, /*standbyLoss*/ 0, /*shotCost*/ 0,
            /*fireRate*/ 1.0f, /*towerRange*/ 3.0f, /*damage*/ 20);
        entity = new TowerEntity(new Vector2(0, 0), tower);
        // Pre-fill power bank
        entity.consume(1000f, 0f);
    }

    private EnemyInstance aliveEnemyAt(float worldX, float worldY) {
        Enemy enemy = TestAssetFactory.createEnemy(100, 1.0f, 3, false);
        // Path just needs to be non-null to avoid NPE in move(); we won't call move() here.
        return new EnemyInstance(enemy, new Vector2(worldX, worldY), null);
    }

    // -------------------------------------------------------------------------
    // No enemies in list
    // -------------------------------------------------------------------------

    @Test
    void attemptShot_emptyEnemyList_returnsFalse() {
        // No enemies in range — tower should not fire even when powered and timer is ready
        float powerBefore = entity.getPowerBank();
        boolean fired = entity.attemptShot(1.0f, Collections.emptyList());

        assertFalse(fired, "Tower should not fire when there are no enemies");
        assertEquals(powerBefore, entity.getPowerBank(), 0.001f,
            "Power should not be consumed when no enemies are present");
    }

    // -------------------------------------------------------------------------
    // Enemy within range is damaged
    // -------------------------------------------------------------------------

    @Test
    void shoot_enemyWithinRange_receivesDamage() {
        // Tower center (32,32), range 3 tiles = 192px. Enemy at (32,32) = distance 0.
        EnemyInstance nearby = aliveEnemyAt(32f, 32f);

        entity.attemptShot(1.0f, Collections.singletonList(nearby));

        assertTrue(nearby.getHealthFraction() < 1.0f,
            "Enemy within range should receive damage");
    }

    @Test
    void shoot_enemyWithinRange_receivesExactDamageAmount() {
        // Damage = 20 out of 100 health → 80% health remaining
        EnemyInstance nearby = aliveEnemyAt(32f, 32f);

        entity.attemptShot(1.0f, Collections.singletonList(nearby));

        assertEquals(0.8f, nearby.getHealthFraction(), 0.001f,
            "Enemy should lose exactly 20 hp (damage value) when shot");
    }

    // -------------------------------------------------------------------------
    // Enemy outside range is not damaged
    // -------------------------------------------------------------------------

    @Test
    void shoot_enemyOutsideRange_receivesNoDamage() {
        // Tower center (32,32), range 192px. Enemy at (32 + 300, 32) = 300px away > 192px.
        EnemyInstance farAway = aliveEnemyAt(32f + 300f, 32f);

        entity.attemptShot(1.0f, Collections.singletonList(farAway));

        assertEquals(1.0f, farAway.getHealthFraction(), 0.001f,
            "Enemy outside range should not receive damage");
    }

    // -------------------------------------------------------------------------
    // Closest enemy is targeted (not arbitrary)
    // -------------------------------------------------------------------------

    @Test
    void shoot_targetsClosestAliveEnemy() {
        // Two enemies within range: one at distance ~30px, one at ~100px
        EnemyInstance closer = aliveEnemyAt(32f + 30f, 32f);   // 30px from tower center
        EnemyInstance farther = aliveEnemyAt(32f + 100f, 32f); // 100px from tower center

        entity.attemptShot(1.0f, Arrays.asList(farther, closer));

        assertTrue(closer.getHealthFraction() < 1.0f,
            "Closer enemy should be the target and receive damage");
        assertEquals(1.0f, farther.getHealthFraction(), 0.001f,
            "Farther enemy should not be damaged when closer one is in range");
    }

    @Test
    void shoot_targetsClosestAliveEnemy_orderIndependent() {
        // Same scenario but enemies are in reversed order in the list
        EnemyInstance closer = aliveEnemyAt(32f + 30f, 32f);
        EnemyInstance farther = aliveEnemyAt(32f + 100f, 32f);

        entity.attemptShot(1.0f, Arrays.asList(closer, farther));

        assertTrue(closer.getHealthFraction() < 1.0f,
            "Closest enemy should be targeted regardless of list order");
        assertEquals(1.0f, farther.getHealthFraction(), 0.001f,
            "Farther enemy should still not be damaged");
    }

    // -------------------------------------------------------------------------
    // Dead enemies are skipped
    // -------------------------------------------------------------------------

    @Test
    void shoot_skipsDeadEnemies() {
        // Dead enemy within range (closer), live enemy also within range
        EnemyInstance dead = aliveEnemyAt(32f, 32f);
        dead.takeDamage(100); // kill it — health fraction is now 0

        EnemyInstance alive = aliveEnemyAt(32f + 50f, 32f);

        entity.attemptShot(1.0f, Arrays.asList(dead, alive));

        // Dead enemy's health fraction stays at 0 (it was already dead before the shot)
        assertEquals(0.0f, dead.getHealthFraction(), 0.001f,
            "Dead enemy health should remain at 0, not go below");
        assertTrue(alive.getHealthFraction() < 1.0f,
            "Live enemy within range should be targeted instead of the dead one");
    }

    @Test
    void shoot_allEnemiesDead_noCrashAndNoDamage() {
        EnemyInstance dead1 = aliveEnemyAt(32f, 32f);
        EnemyInstance dead2 = aliveEnemyAt(32f + 10f, 32f);
        dead1.takeDamage(100);
        dead2.takeDamage(100);

        // Should not throw
        assertDoesNotThrow(() -> entity.attemptShot(1.0f, Arrays.asList(dead1, dead2)),
            "shoot() with only dead enemies should not crash");
    }

    // -------------------------------------------------------------------------
    // Tower world center is computed correctly from tile position
    // -------------------------------------------------------------------------

    @Test
    void shoot_towerAtNonZeroTile_correctlyCalculatesRange() {
        // Tower at tile (2, 3): center = (2.5 * 64, 3.5 * 64) = (160, 224)
        // Range 1 tile = 64px. Enemy at (160, 224 + 50) = 50px away (within range).
        Tower towerAt2_3 = TestAssetFactory.createTowerWithRange(1000, 0, 0, 1.0f, 1.0f, 25);
        TowerEntity entityAt2_3 = new TowerEntity(new Vector2(2, 3), towerAt2_3);
        entityAt2_3.consume(1000f, 0f);

        float centerX = (2f + 0.5f) * TILE; // 160
        float centerY = (3f + 0.5f) * TILE; // 224
        EnemyInstance nearbyEnemy = aliveEnemyAt(centerX, centerY + 50f);

        entityAt2_3.attemptShot(1.0f, Collections.singletonList(nearbyEnemy));

        assertTrue(nearbyEnemy.getHealthFraction() < 1.0f,
            "Enemy 50px from tower center should be within 1-tile (64px) range");
    }

    @Test
    void shoot_towerAtNonZeroTile_enemyOutsideRange_notDamaged() {
        Tower towerAt2_3 = TestAssetFactory.createTowerWithRange(1000, 0, 0, 1.0f, 1.0f, 25);
        TowerEntity entityAt2_3 = new TowerEntity(new Vector2(2, 3), towerAt2_3);
        entityAt2_3.consume(1000f, 0f);

        float centerX = (2f + 0.5f) * TILE;
        float centerY = (3f + 0.5f) * TILE;
        EnemyInstance farEnemy = aliveEnemyAt(centerX, centerY + 100f); // 100px > 64px range

        entityAt2_3.attemptShot(1.0f, Collections.singletonList(farEnemy));

        assertEquals(1.0f, farEnemy.getHealthFraction(), 0.001f,
            "Enemy 100px away should be outside 1-tile (64px) range");
    }

    // -------------------------------------------------------------------------
    // Power gating: shoot() is only called when there is enough power
    // -------------------------------------------------------------------------

    @Test
    void attemptShot_insufficientPower_doesNotDamageEnemy() {
        // Tower with shot cost 50 but bank is empty
        Tower expensiveTower = TestAssetFactory.createTowerWithRange(1000, 0, 50, 1.0f, 5.0f, 100);
        TowerEntity poor = new TowerEntity(new Vector2(0, 0), expensiveTower);
        // Do NOT fill the power bank

        EnemyInstance nearby = aliveEnemyAt(32f, 32f);
        poor.attemptShot(1.0f, Collections.singletonList(nearby));

        assertEquals(1.0f, nearby.getHealthFraction(), 0.001f,
            "Tower with no power should not deal damage even when enemy is in range");
    }

    // -------------------------------------------------------------------------
    // Enemy exactly at range boundary
    // -------------------------------------------------------------------------

    @Test
    void shoot_enemyAtExactRangeBoundary_isDamaged() {
        // Tower range 3 tiles = 192px. Enemy placed exactly 192px from tower center.
        // The implementation uses <=, so the boundary enemy IS in range.
        float centerX = (0f + 0.5f) * TILE; // 32
        float centerY = (0f + 0.5f) * TILE; // 32
        float rangePx = 3.0f * TILE;         // 192
        EnemyInstance boundary = aliveEnemyAt(centerX + rangePx, centerY);

        entity.attemptShot(1.0f, Collections.singletonList(boundary));

        assertTrue(boundary.getHealthFraction() < 1.0f,
            "Enemy at the exact range boundary should be included (distSq <= rangePx^2)");
    }

    // -------------------------------------------------------------------------
    // ShotResult population
    // -------------------------------------------------------------------------

    @Test
    void shotResult_singleTarget_populatedCorrectly() {
        EnemyInstance nearby = aliveEnemyAt(32f + 50f, 32f);

        entity.attemptShot(1.0f, Collections.singletonList(nearby));

        ShotResult sr = entity.getShotResult();
        assertTrue(sr.fired);
        assertEquals(32f, sr.towerX, 0.001f);
        assertEquals(32f, sr.towerY, 0.001f);
        assertEquals(nearby, sr.targetEnemy);
        assertEquals("single", sr.targeting);
    }

    // -------------------------------------------------------------------------
    // AOE targeting
    // -------------------------------------------------------------------------

    @Nested
    class AoeTargeting {
        private TowerEntity aoeTower;

        @BeforeEach
        void setUp() {
            Tower aoe = TestAssetFactory.createTowerWithRange(
                1000, 0, 0, 1.0f, 3.0f, 10, "aoe", false);
            aoeTower = new TowerEntity(new Vector2(0, 0), aoe);
            aoeTower.consume(1000f, 0f);
        }

        @Test
        void aoe_damagesAllEnemiesInRange() {
            EnemyInstance e1 = aliveEnemyAt(32f, 32f);
            EnemyInstance e2 = aliveEnemyAt(32f + 50f, 32f);
            EnemyInstance e3 = aliveEnemyAt(32f + 100f, 32f);

            aoeTower.attemptShot(1.0f, Arrays.asList(e1, e2, e3));

            assertTrue(e1.getHealthFraction() < 1.0f, "e1 should be damaged");
            assertTrue(e2.getHealthFraction() < 1.0f, "e2 should be damaged");
            assertTrue(e3.getHealthFraction() < 1.0f, "e3 should be damaged");
        }

        @Test
        void aoe_skipsEnemiesOutOfRange() {
            EnemyInstance inRange = aliveEnemyAt(32f, 32f);
            EnemyInstance outOfRange = aliveEnemyAt(32f + 300f, 32f);

            aoeTower.attemptShot(1.0f, Arrays.asList(inRange, outOfRange));

            assertTrue(inRange.getHealthFraction() < 1.0f, "In-range enemy should be damaged");
            assertEquals(1.0f, outOfRange.getHealthFraction(), 0.001f, "Out-of-range enemy should not be damaged");
        }

        @Test
        void aoe_skipsDeadEnemies() {
            EnemyInstance dead = aliveEnemyAt(32f, 32f);
            dead.takeDamage(100);
            EnemyInstance alive = aliveEnemyAt(32f + 50f, 32f);

            aoeTower.attemptShot(1.0f, Arrays.asList(dead, alive));

            assertTrue(alive.getHealthFraction() < 1.0f, "Alive enemy should be damaged");
        }

        @Test
        void aoe_returnsFalseForNoEnemies() {
            boolean fired = aoeTower.attemptShot(1.0f, Collections.emptyList());
            assertFalse(fired);
        }

        @Test
        void aoe_shotResult_populatesAoeTargets() {
            EnemyInstance e1 = aliveEnemyAt(32f, 32f);
            EnemyInstance e2 = aliveEnemyAt(32f + 50f, 32f);

            aoeTower.attemptShot(1.0f, Arrays.asList(e1, e2));

            ShotResult sr = aoeTower.getShotResult();
            assertTrue(sr.fired);
            assertEquals("aoe", sr.targeting);
            assertEquals(2, sr.aoeTargetCount);
        }
    }

    // -------------------------------------------------------------------------
    // Deferred damage (Plasma-style)
    // -------------------------------------------------------------------------

    @Nested
    class DeferredDamage {
        private TowerEntity deferredTower;

        @BeforeEach
        void setUp() {
            Tower deferred = TestAssetFactory.createTowerWithRange(
                1000, 0, 0, 1.0f, 3.0f, 20, "single", true);
            deferredTower = new TowerEntity(new Vector2(0, 0), deferred);
            deferredTower.consume(1000f, 0f);
        }

        @Test
        void deferredDamage_doesNotDamageEnemyImmediately() {
            EnemyInstance nearby = aliveEnemyAt(32f, 32f);

            boolean fired = deferredTower.attemptShot(1.0f, Collections.singletonList(nearby));

            assertTrue(fired, "Tower should fire");
            assertEquals(1.0f, nearby.getHealthFraction(), 0.001f,
                "Enemy should NOT be damaged immediately when deferDamage is true");
        }

        @Test
        void deferredDamage_shotResultHasTargetEnemy() {
            EnemyInstance nearby = aliveEnemyAt(32f, 32f);

            deferredTower.attemptShot(1.0f, Collections.singletonList(nearby));

            ShotResult sr = deferredTower.getShotResult();
            assertTrue(sr.fired);
            assertEquals(nearby, sr.targetEnemy);
            assertEquals(20, sr.damage);
        }
    }
}
