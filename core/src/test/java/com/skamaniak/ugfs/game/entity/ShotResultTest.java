package com.skamaniak.ugfs.game.entity;

import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.TestAssetFactory;
import com.skamaniak.ugfs.asset.model.Enemy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ShotResult — the mutable, reusable shot-data struct.
 * Focuses on reset() clearing all fields to their defaults.
 */
class ShotResultTest {

    private ShotResult sr;

    @BeforeEach
    void setUp() {
        sr = new ShotResult();
    }

    // -------------------------------------------------------------------------
    // Default (post-construction) state
    // -------------------------------------------------------------------------

    @Test
    void defaultState_firedIsFalse() {
        assertFalse(sr.fired, "Freshly constructed ShotResult should have fired == false");
    }

    @Test
    void defaultState_targetingIsSingle() {
        // Default targeting is set by reset(); constructor leaves it null —
        // confirm reset() restores "single".
        sr.reset();
        assertEquals("single", sr.targeting,
            "reset() should restore targeting to \"single\"");
    }

    @Test
    void defaultState_aoeTargetCountIsZero() {
        sr.reset();
        assertEquals(0, sr.aoeTargetCount,
            "reset() should restore aoeTargetCount to 0");
    }

    // -------------------------------------------------------------------------
    // reset() clears all scalar fields
    // -------------------------------------------------------------------------

    @Test
    void reset_clearsFiredFlag() {
        sr.fired = true;
        sr.reset();
        assertFalse(sr.fired, "reset() should clear the fired flag");
    }

    @Test
    void reset_clearsTowerCoordinates() {
        sr.towerX = 128f;
        sr.towerY = 256f;
        sr.reset();
        assertEquals(0f, sr.towerX, 0.001f, "reset() should zero towerX");
        assertEquals(0f, sr.towerY, 0.001f, "reset() should zero towerY");
    }

    @Test
    void reset_clearsTargetCoordinates() {
        sr.targetX = 500f;
        sr.targetY = 300f;
        sr.reset();
        assertEquals(0f, sr.targetX, 0.001f, "reset() should zero targetX");
        assertEquals(0f, sr.targetY, 0.001f, "reset() should zero targetY");
    }

    @Test
    void reset_clearsRangePx() {
        sr.rangePx = 192f;
        sr.reset();
        assertEquals(0f, sr.rangePx, 0.001f, "reset() should zero rangePx");
    }

    @Test
    void reset_clearsDamage() {
        sr.damage = 50;
        sr.reset();
        assertEquals(0, sr.damage, "reset() should zero the damage field");
    }

    @Test
    void reset_clearsTargetEnemy() {
        Enemy asset = TestAssetFactory.createEnemy(100, 1.0f, 5, false);
        sr.targetEnemy = new EnemyInstance(asset, new Vector2(0f, 0f), null);
        sr.reset();
        assertNull(sr.targetEnemy, "reset() should null the targetEnemy reference");
    }

    @Test
    void reset_clearsAoeTargetCount() {
        sr.aoeTargetCount = 7;
        sr.reset();
        assertEquals(0, sr.aoeTargetCount, "reset() should zero aoeTargetCount");
    }

    @Test
    void reset_restoresTargetingToSingle() {
        sr.targeting = "aoe";
        sr.reset();
        assertEquals("single", sr.targeting,
            "reset() should restore targeting to \"single\"");
    }

    // -------------------------------------------------------------------------
    // aoeTargets array capacity
    // -------------------------------------------------------------------------

    @Test
    void aoeTargets_arrayLengthIs64() {
        assertEquals(64, sr.aoeTargets.length,
            "aoeTargets fixed array must have capacity 64 (MAX_AOE_TARGETS)");
    }

    // -------------------------------------------------------------------------
    // Reset is idempotent
    // -------------------------------------------------------------------------

    @Test
    void reset_calledTwice_remainsClean() {
        sr.fired = true;
        sr.towerX = 100f;
        sr.damage = 20;
        sr.reset();
        sr.reset(); // second reset should be harmless

        assertFalse(sr.fired, "fired should still be false after double reset");
        assertEquals(0f, sr.towerX, 0.001f, "towerX should still be 0 after double reset");
        assertEquals(0, sr.damage, "damage should still be 0 after double reset");
    }
}
