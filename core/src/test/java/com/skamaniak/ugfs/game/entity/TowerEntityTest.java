package com.skamaniak.ugfs.game.entity;

import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.TestAssetFactory;
import com.skamaniak.ugfs.asset.model.Tower;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TowerEntityTest {

    @Test
    void consume_storesPower() {
        Tower tower = TestAssetFactory.createTower(100, 0, 10, 1.0f);
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);

        float returned = entity.consume(50f, 1.0f);

        assertEquals(0f, returned, 0.001f);
    }

    @Test
    void consume_returnsExcess() {
        Tower tower = TestAssetFactory.createTower(100, 0, 10, 1.0f);
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);

        float returned = entity.consume(150f, 1.0f);

        assertEquals(50f, returned, 0.001f);
    }

    @Test
    void consume_appliesStandbyLossOncePerFrame() {
        Tower tower = TestAssetFactory.createTower(1000, 10, 10, 1.0f);
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);

        // First consume: stores 50, applies loss 10 → bank = max(0-10,0)=0 → then +50 = 50
        entity.consume(50f, 1.0f);

        // Second consume same frame: should NOT apply loss again (propagated=true)
        entity.consume(50f, 1.0f);

        // Total stored should be 100 (50 + 50, loss only applied once at start)
        // Verify by filling to capacity
        float returned = entity.consume(950f, 1.0f);

        // Bank should be at 100 before this call, so can accept 900 more
        assertEquals(50f, returned, 0.001f);
    }

    @Test
    void attemptShot_firesWhenReadyAndPowered() {
        // fireRate=1.0 → 1 shot per second, shotCost=10
        Tower tower = TestAssetFactory.createTower(100, 0, 10, 1.0f);
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);

        // Give power
        entity.consume(50f, 1.0f);

        // Attempt shot after 1 second
        boolean fired = entity.attemptShot(1.0f);

        assertTrue(fired);

        // Bank should be 50 - 10 = 40. Verify by trying to consume to fill.
        float returned = entity.consume(70f, 0f);
        assertEquals(10f, returned, 0.001f); // 100 - 40 = 60 capacity left, 70-60=10 returned
    }

    @Test
    void attemptShot_failsWhenInsufficientPower() {
        Tower tower = TestAssetFactory.createTower(100, 0, 50, 1.0f);
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);

        // Give only 10 power, but shot costs 50
        entity.consume(10f, 1.0f);

        boolean fired = entity.attemptShot(1.0f);

        assertFalse(fired);
    }

    @Test
    void attemptShot_failsWhenTimerNotReady() {
        Tower tower = TestAssetFactory.createTower(100, 0, 10, 1.0f);
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);

        entity.consume(50f, 1.0f);

        // Only 0.5 seconds passed, need 1.0
        boolean fired = entity.attemptShot(0.5f);

        assertFalse(fired);
    }

    @Test
    void attemptShot_multipleFrames() {
        // fireRate=2.0 → 2 shots per second → 0.5s between shots, shotCost=10
        Tower tower = TestAssetFactory.createTower(100, 0, 10, 2.0f);
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);

        entity.consume(100f, 1.0f);

        // Frame 1: 0.3s → not ready
        assertFalse(entity.attemptShot(0.3f));

        // Frame 2: 0.3s more → cumulative 0.6s ≥ 0.5s → fire
        assertTrue(entity.attemptShot(0.3f));

        // Frame 3: 0.3s more → cumulative 0.1s (0.6-0.5) + 0.3 = 0.4s → not ready
        assertFalse(entity.attemptShot(0.3f));

        // Frame 4: 0.2s more → cumulative 0.6s ≥ 0.5s → fire
        assertTrue(entity.attemptShot(0.2f));
    }

    @Test
    void attemptShot_immediateSecondShot() {
        // fireRate=2.0 → 0.5s between shots
        Tower tower = TestAssetFactory.createTower(100, 0, 10, 2.0f);
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);

        entity.consume(100f, 1.0f);

        // Pass enough time for 2 shots at once (1.0s = 2 * 0.5s)
        assertTrue(entity.attemptShot(1.0f));  // First shot, cumulativeDelta becomes 0.5
        assertTrue(entity.attemptShot(0f));    // Leftover delta allows immediate second shot
    }
}
