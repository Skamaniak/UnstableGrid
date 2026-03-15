package com.skamaniak.ugfs.game.entity;

import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.GameConstants;
import com.skamaniak.ugfs.TestAssetFactory;
import com.skamaniak.ugfs.asset.model.Enemy;
import com.skamaniak.ugfs.asset.model.Tower;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TowerEntityTest {

    /** Creates an alive enemy positioned at the given tower entity's center tile. */
    private static List<EnemyInstance> enemyAtTowerCenter(TowerEntity tower) {
        Enemy enemy = TestAssetFactory.createEnemy(100, 1.0f, 0, false);
        float cx = (tower.getPosition().x + 0.5f) * GameConstants.TILE_SIZE_PX;
        float cy = (tower.getPosition().y + 0.5f) * GameConstants.TILE_SIZE_PX;
        return Arrays.asList(new EnemyInstance(enemy, new Vector2(cx, cy), null));
    }

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
        Tower tower = TestAssetFactory.createTowerWithRange(100, 0, 10, 1.0f, 5.0f, 0);
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);

        // Give power
        entity.consume(50f, 1.0f);

        // Attempt shot after 1 second with enemy in range
        boolean fired = entity.attemptShot(1.0f, enemyAtTowerCenter(entity));

        assertTrue(fired);

        // Bank should be 50 - 10 = 40. Verify by trying to consume to fill.
        float returned = entity.consume(70f, 0f);
        assertEquals(10f, returned, 0.001f); // 100 - 40 = 60 capacity left, 70-60=10 returned
    }

    @Test
    void attemptShot_failsWhenInsufficientPower() {
        // shotCost=50, range=5 tiles
        Tower tower = TestAssetFactory.createTowerWithRange(100, 0, 50, 1.0f, 5.0f, 10);
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);

        // Give only 10 power, but shot costs 50
        entity.consume(10f, 1.0f);

        // Enemy in range so the only reason to fail is insufficient power
        boolean fired = entity.attemptShot(1.0f, enemyAtTowerCenter(entity));

        assertFalse(fired);
        assertEquals(10f, entity.getPowerBank(), 0.001f, "Power should not be consumed when shot fails");
    }

    @Test
    void attemptShot_failsWhenTimerNotReady() {
        // range=5 tiles
        Tower tower = TestAssetFactory.createTowerWithRange(100, 0, 10, 1.0f, 5.0f, 10);
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);

        entity.consume(50f, 1.0f);

        // Enemy in range, power available — only 0.5 seconds passed but need 1.0
        boolean fired = entity.attemptShot(0.5f, enemyAtTowerCenter(entity));

        assertFalse(fired);
        assertEquals(50f, entity.getPowerBank(), 0.001f, "Power should not be consumed when timer not ready");
    }

    @Test
    void attemptShot_multipleFrames() {
        // fireRate=2.0 → 2 shots per second → 0.5s between shots, shotCost=10
        Tower tower = TestAssetFactory.createTowerWithRange(100, 0, 10, 2.0f, 5.0f, 0);
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);
        List<EnemyInstance> enemies = enemyAtTowerCenter(entity);

        entity.consume(100f, 1.0f);

        // Frame 1: 0.3s → not ready
        assertFalse(entity.attemptShot(0.3f, enemies));

        // Frame 2: 0.3s more → cumulative 0.6s ≥ 0.5s → fire
        assertTrue(entity.attemptShot(0.3f, enemies));

        // Frame 3: 0.3s more → cumulative 0.1s (0.6-0.5) + 0.3 = 0.4s → not ready
        assertFalse(entity.attemptShot(0.3f, enemies));

        // Frame 4: 0.2s more → cumulative 0.6s ≥ 0.5s → fire
        assertTrue(entity.attemptShot(0.2f, enemies));
    }

    @Test
    void attemptShot_immediateSecondShot() {
        // fireRate=2.0 → 0.5s between shots
        Tower tower = TestAssetFactory.createTowerWithRange(100, 0, 10, 2.0f, 5.0f, 0);
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);
        List<EnemyInstance> enemies = enemyAtTowerCenter(entity);

        entity.consume(100f, 1.0f);

        // Pass enough time for 2 shots at once (1.0s = 2 * 0.5s)
        assertTrue(entity.attemptShot(1.0f, enemies));  // First shot, cumulativeDelta becomes 0.5
        assertTrue(entity.attemptShot(0f, enemies));    // Leftover delta allows immediate second shot
    }
}
