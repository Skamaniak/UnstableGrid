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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the canTargetFlying filtering logic added by the comprehensive-rebalance feature.
 *
 * Spec requirement (from docs/specs/20260318-comprehensive-rebalance.md):
 *   "Only Laser and Plasma can target flying enemies. Tesla, Microwave, and EMP cannot.
 *   This is controlled by a canTargetFlying boolean on the Tower JSON — data-driven,
 *   no hardcoded ID checks, no default value."
 *
 * Two scenarios are exercised for both shootSingle and shootAoe modes:
 *   1. canTargetFlying=false  → flying enemies are completely ignored; ground enemies are still hit.
 *   2. canTargetFlying=true   → both flying and ground enemies are targeted.
 *
 * All towers are placed at tile (0,0); tower world center = (32, 32).
 * Enemies are placed within range (< 3 tiles = 192px from the tower center) unless stated otherwise.
 */
class TowerEntityFlyingTargetingTest {

    private static final int TILE = GameConstants.TILE_SIZE_PX; // 64
    // Tower center for tile (0,0)
    private static final float TOWER_X = 0.5f * TILE; // 32
    private static final float TOWER_Y = 0.5f * TILE; // 32

    // Helper: create a live enemy at the given world position
    private static EnemyInstance enemyAt(float worldX, float worldY, boolean flying) {
        Enemy asset = TestAssetFactory.createEnemy(100, 1.0f, 3, flying);
        return new EnemyInstance(asset, new Vector2(worldX, worldY), Collections.emptyList());
    }

    // =========================================================================
    // Single-targeting tower (shootSingle path)
    // =========================================================================

    @Nested
    class SingleTargetingTower {

        /** Tower with canTargetFlying=false — models Tesla/Microwave/EMP. */
        private TowerEntity groundOnlyTower;

        /** Tower with canTargetFlying=true — models Laser/Plasma. */
        private TowerEntity antiAirTower;

        @BeforeEach
        void setUp() {
            Tower groundOnlyAsset = TestAssetFactory.createTowerWithRange(
                /*capacity*/   1000,
                /*standbyLoss*/ 0,
                /*shotCost*/    0,
                /*fireRate*/    1.0f,
                /*towerRange*/  3.0f,
                /*damage*/      20,
                /*targeting*/   "single",
                /*deferDamage*/ false,
                /*canTargetFlying*/ false);
            groundOnlyTower = new TowerEntity(new Vector2(0, 0), groundOnlyAsset);
            groundOnlyTower.consume(1000f, 0f);

            Tower antiAirAsset = TestAssetFactory.createTowerWithRange(
                1000, 0, 0, 1.0f, 3.0f, 20,
                "single", false, /*canTargetFlying*/ true);
            antiAirTower = new TowerEntity(new Vector2(0, 0), antiAirAsset);
            antiAirTower.consume(1000f, 0f);
        }

        // --- canTargetFlying=false tests ---

        @Test
        void shootSingle_groundOnly_flyingEnemyInRange_notDamaged() {
            EnemyInstance flyer = enemyAt(TOWER_X, TOWER_Y, /*flying*/ true);

            groundOnlyTower.attemptShot(1.0f, Collections.singletonList(flyer));

            assertEquals(1.0f, flyer.getHealthFraction(), 0.001f,
                "A ground-only tower must not damage a flying enemy even when it is within range");
        }

        @Test
        void shootSingle_groundOnly_flyingEnemyInRange_doesNotFire() {
            EnemyInstance flyer = enemyAt(TOWER_X, TOWER_Y, true);

            boolean fired = groundOnlyTower.attemptShot(1.0f, Collections.singletonList(flyer));

            assertFalse(fired,
                "A ground-only tower should return false (no shot) when only flying enemies are present");
        }

        @Test
        void shootSingle_groundOnly_groundEnemyInRange_isDamaged() {
            EnemyInstance ground = enemyAt(TOWER_X, TOWER_Y, /*flying*/ false);

            groundOnlyTower.attemptShot(1.0f, Collections.singletonList(ground));

            assertTrue(ground.getHealthFraction() < 1.0f,
                "A ground-only tower must still damage ground enemies normally");
        }

        @Test
        void shootSingle_groundOnly_mixedEnemies_onlyGroundIsDamaged() {
            // Place flyer closer than ground enemy to confirm flying enemy is skipped, not just second-picked
            EnemyInstance flyer = enemyAt(TOWER_X, TOWER_Y, true);              // distance 0 — closest
            EnemyInstance ground = enemyAt(TOWER_X + 50f, TOWER_Y, false);      // distance 50px — farther

            groundOnlyTower.attemptShot(1.0f, Arrays.asList(flyer, ground));

            assertEquals(1.0f, flyer.getHealthFraction(), 0.001f,
                "Flying enemy must remain undamaged when canTargetFlying=false");
            assertTrue(ground.getHealthFraction() < 1.0f,
                "Ground enemy must be targeted even though a closer flying enemy exists");
        }

        @Test
        void shootSingle_groundOnly_onlyFlyingEnemiesPresent_shotResultNotFired() {
            EnemyInstance flyer = enemyAt(TOWER_X, TOWER_Y, true);

            groundOnlyTower.attemptShot(1.0f, Collections.singletonList(flyer));

            assertFalse(groundOnlyTower.getShotResult().fired,
                "ShotResult.fired must be false when the only enemy was a flying one and canTargetFlying=false");
        }

        // --- canTargetFlying=true tests ---

        @Test
        void shootSingle_antiAir_flyingEnemyInRange_isDamaged() {
            EnemyInstance flyer = enemyAt(TOWER_X, TOWER_Y, true);

            antiAirTower.attemptShot(1.0f, Collections.singletonList(flyer));

            assertTrue(flyer.getHealthFraction() < 1.0f,
                "An anti-air tower must damage a flying enemy within range");
        }

        @Test
        void shootSingle_antiAir_groundEnemyInRange_isDamaged() {
            EnemyInstance ground = enemyAt(TOWER_X, TOWER_Y, false);

            antiAirTower.attemptShot(1.0f, Collections.singletonList(ground));

            assertTrue(ground.getHealthFraction() < 1.0f,
                "An anti-air tower must also damage ground enemies normally");
        }

        @Test
        void shootSingle_antiAir_targetsClosestRegardlessOfFlyingStatus() {
            // Flying enemy is closer; ground enemy is farther — anti-air should pick the flyer
            EnemyInstance flyer = enemyAt(TOWER_X + 10f, TOWER_Y, true);
            EnemyInstance ground = enemyAt(TOWER_X + 80f, TOWER_Y, false);

            antiAirTower.attemptShot(1.0f, Arrays.asList(ground, flyer));

            assertTrue(flyer.getHealthFraction() < 1.0f,
                "Anti-air tower should target the closest enemy even if it is flying");
            assertEquals(1.0f, ground.getHealthFraction(), 0.001f,
                "Farther ground enemy should not be hit when the closer flying enemy is in range");
        }
    }

    // =========================================================================
    // AOE-targeting tower (shootAoe path)
    // =========================================================================

    @Nested
    class AoeTargetingTower {

        /** AOE tower with canTargetFlying=false — models EMP/Microwave. */
        private TowerEntity groundOnlyAoe;

        /** AOE tower with canTargetFlying=true — models a hypothetical AOE anti-air. */
        private TowerEntity antiAirAoe;

        @BeforeEach
        void setUp() {
            Tower groundOnlyAsset = TestAssetFactory.createTowerWithRange(
                1000, 0, 0, 1.0f, 3.0f, 10,
                "aoe", false, /*canTargetFlying*/ false);
            groundOnlyAoe = new TowerEntity(new Vector2(0, 0), groundOnlyAsset);
            groundOnlyAoe.consume(1000f, 0f);

            Tower antiAirAsset = TestAssetFactory.createTowerWithRange(
                1000, 0, 0, 1.0f, 3.0f, 10,
                "aoe", false, /*canTargetFlying*/ true);
            antiAirAoe = new TowerEntity(new Vector2(0, 0), antiAirAsset);
            antiAirAoe.consume(1000f, 0f);
        }

        // --- canTargetFlying=false tests ---

        @Test
        void shootAoe_groundOnly_flyingEnemyInRange_notDamaged() {
            EnemyInstance flyer = enemyAt(TOWER_X, TOWER_Y, true);

            groundOnlyAoe.attemptShot(1.0f, Collections.singletonList(flyer));

            assertEquals(1.0f, flyer.getHealthFraction(), 0.001f,
                "Ground-only AOE tower must not damage flying enemies");
        }

        @Test
        void shootAoe_groundOnly_flyingEnemyInRange_doesNotFire() {
            EnemyInstance flyer = enemyAt(TOWER_X, TOWER_Y, true);

            boolean fired = groundOnlyAoe.attemptShot(1.0f, Collections.singletonList(flyer));

            assertFalse(fired,
                "Ground-only AOE tower should return false when only flying enemies are in range");
        }

        @Test
        void shootAoe_groundOnly_groundEnemyInRange_isDamaged() {
            EnemyInstance ground = enemyAt(TOWER_X, TOWER_Y, false);

            groundOnlyAoe.attemptShot(1.0f, Collections.singletonList(ground));

            assertTrue(ground.getHealthFraction() < 1.0f,
                "Ground-only AOE tower must damage ground enemies normally");
        }

        @Test
        void shootAoe_groundOnly_mixedEnemies_onlyGroundEnemiesDamaged() {
            EnemyInstance flyer1 = enemyAt(TOWER_X, TOWER_Y, true);
            EnemyInstance flyer2 = enemyAt(TOWER_X + 40f, TOWER_Y, true);
            EnemyInstance ground1 = enemyAt(TOWER_X + 20f, TOWER_Y, false);
            EnemyInstance ground2 = enemyAt(TOWER_X + 60f, TOWER_Y, false);

            groundOnlyAoe.attemptShot(1.0f,
                Arrays.asList(flyer1, ground1, flyer2, ground2));

            assertEquals(1.0f, flyer1.getHealthFraction(), 0.001f,
                "First flying enemy must not be damaged");
            assertEquals(1.0f, flyer2.getHealthFraction(), 0.001f,
                "Second flying enemy must not be damaged");
            assertTrue(ground1.getHealthFraction() < 1.0f,
                "First ground enemy must be damaged by AOE");
            assertTrue(ground2.getHealthFraction() < 1.0f,
                "Second ground enemy must be damaged by AOE");
        }

        @Test
        void shootAoe_groundOnly_aoeTargetCount_excludesFlyingEnemies() {
            EnemyInstance flyer = enemyAt(TOWER_X, TOWER_Y, true);
            EnemyInstance ground1 = enemyAt(TOWER_X + 20f, TOWER_Y, false);
            EnemyInstance ground2 = enemyAt(TOWER_X + 40f, TOWER_Y, false);

            groundOnlyAoe.attemptShot(1.0f, Arrays.asList(flyer, ground1, ground2));

            assertEquals(2, groundOnlyAoe.getShotResult().aoeTargetCount,
                "aoeTargetCount should count only ground enemies when canTargetFlying=false");
        }

        // --- canTargetFlying=true tests ---

        @Test
        void shootAoe_antiAir_flyingEnemyInRange_isDamaged() {
            EnemyInstance flyer = enemyAt(TOWER_X, TOWER_Y, true);

            antiAirAoe.attemptShot(1.0f, Collections.singletonList(flyer));

            assertTrue(flyer.getHealthFraction() < 1.0f,
                "Anti-air AOE tower must damage flying enemies");
        }

        @Test
        void shootAoe_antiAir_mixedEnemies_allInRangeDamaged() {
            EnemyInstance flyer = enemyAt(TOWER_X, TOWER_Y, true);
            EnemyInstance ground = enemyAt(TOWER_X + 30f, TOWER_Y, false);

            antiAirAoe.attemptShot(1.0f, Arrays.asList(flyer, ground));

            assertTrue(flyer.getHealthFraction() < 1.0f,
                "Anti-air AOE must damage the flying enemy");
            assertTrue(ground.getHealthFraction() < 1.0f,
                "Anti-air AOE must also damage the ground enemy");
        }

        @Test
        void shootAoe_antiAir_aoeTargetCount_includesBothTypes() {
            EnemyInstance flyer = enemyAt(TOWER_X, TOWER_Y, true);
            EnemyInstance ground = enemyAt(TOWER_X + 30f, TOWER_Y, false);

            antiAirAoe.attemptShot(1.0f, Arrays.asList(flyer, ground));

            assertEquals(2, antiAirAoe.getShotResult().aoeTargetCount,
                "aoeTargetCount should include both flying and ground enemies when canTargetFlying=true");
        }
    }
}
