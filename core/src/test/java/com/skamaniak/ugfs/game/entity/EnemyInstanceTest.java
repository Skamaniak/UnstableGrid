package com.skamaniak.ugfs.game.entity;

import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.GameConstants;
import com.skamaniak.ugfs.TestAssetFactory;
import com.skamaniak.ugfs.asset.model.Enemy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnemyInstanceTest {

    private static final int TILE = GameConstants.TILE_SIZE_PX; // 64

    private Enemy groundEnemy; // speed 1.0 tile/s = 64 px/s

    @BeforeEach
    void setUp() {
        groundEnemy = TestAssetFactory.createEnemy(100, 1.0f, 3, false);
    }

    // -------------------------------------------------------------------------
    // takeDamage
    // -------------------------------------------------------------------------

    @Test
    void takeDamage_reducesCurrentHealth() {
        EnemyInstance enemy = new EnemyInstance(groundEnemy, new Vector2(0, 0), null);

        enemy.takeDamage(30);

        assertEquals(0.7f, enemy.getHealthFraction(), 0.001f,
            "Health fraction should be 0.7 after 30 damage on 100hp enemy");
    }

    @Test
    void takeDamage_exactlyZeroKillsEnemy() {
        EnemyInstance enemy = new EnemyInstance(groundEnemy, new Vector2(0, 0), null);

        enemy.takeDamage(100);

        assertFalse(enemy.isAlive(), "Enemy should be dead after damage equals max health");
    }

    @Test
    void takeDamage_overkillDoesNotGoNegative() {
        EnemyInstance enemy = new EnemyInstance(groundEnemy, new Vector2(0, 0), null);

        enemy.takeDamage(999);

        assertEquals(0.0f, enemy.getHealthFraction(), 0.001f,
            "Health fraction should be 0 even when overkilled");
    }

    @Test
    void takeDamage_overkillSetsAliveToFalse() {
        EnemyInstance enemy = new EnemyInstance(groundEnemy, new Vector2(0, 0), null);

        enemy.takeDamage(150);

        assertFalse(enemy.isAlive(), "Enemy should be dead after overkill");
    }

    @Test
    void isAlive_trueAtSpawn() {
        EnemyInstance enemy = new EnemyInstance(groundEnemy, new Vector2(0, 0), null);

        assertTrue(enemy.isAlive(), "Freshly spawned enemy should be alive");
    }

    // -------------------------------------------------------------------------
    // getHealthFraction
    // -------------------------------------------------------------------------

    @Test
    void getHealthFraction_returnsOneAtFullHealth() {
        EnemyInstance enemy = new EnemyInstance(groundEnemy, new Vector2(0, 0), null);

        assertEquals(1.0f, enemy.getHealthFraction(), 0.001f,
            "Health fraction should be 1.0 at full health");
    }

    @Test
    void getHealthFraction_returnsHalfAtHalfHealth() {
        EnemyInstance enemy = new EnemyInstance(groundEnemy, new Vector2(0, 0), null);

        enemy.takeDamage(50);

        assertEquals(0.5f, enemy.getHealthFraction(), 0.001f,
            "Health fraction should be 0.5 after 50 damage on 100hp enemy");
    }

    @Test
    void getHealthFraction_returnsZeroWhenDead() {
        EnemyInstance enemy = new EnemyInstance(groundEnemy, new Vector2(0, 0), null);

        enemy.takeDamage(100);

        assertEquals(0.0f, enemy.getHealthFraction(), 0.001f,
            "Health fraction should be 0.0 when enemy is dead");
    }

    // -------------------------------------------------------------------------
    // move — null / empty path (stuck enemy)
    // -------------------------------------------------------------------------

    @Test
    void move_withNullPath_doesNotMoveOrCrash() {
        Vector2 spawn = new Vector2(100f, 200f);
        EnemyInstance enemy = new EnemyInstance(groundEnemy, spawn, null);

        enemy.move(1.0f);

        assertEquals(100f, enemy.getWorldPosition().x, 0.001f,
            "Stuck enemy (null path) should not change x");
        assertEquals(200f, enemy.getWorldPosition().y, 0.001f,
            "Stuck enemy (null path) should not change y");
    }

    @Test
    void move_withEmptyPath_doesNotMoveOrCrash() {
        Vector2 spawn = new Vector2(100f, 200f);
        EnemyInstance enemy = new EnemyInstance(groundEnemy, spawn, Collections.emptyList());

        enemy.move(1.0f);

        assertEquals(100f, enemy.getWorldPosition().x, 0.001f,
            "Stuck enemy (empty path) should not change x");
    }

    @Test
    void move_withNullPath_doesNotSetReachedBase() {
        EnemyInstance enemy = new EnemyInstance(groundEnemy, new Vector2(0, 0), null);

        enemy.move(1.0f);

        assertFalse(enemy.hasReachedBase(), "Enemy with null path should not reach base");
    }

    // -------------------------------------------------------------------------
    // move — zero delta
    // -------------------------------------------------------------------------

    @Test
    void move_withZeroDelta_doesNotChangePosition() {
        Vector2 spawn = new Vector2(0f, 0f);
        Vector2 target = new Vector2((float) TILE, 0f);
        EnemyInstance enemy = new EnemyInstance(groundEnemy, spawn, Collections.singletonList(target));

        enemy.move(0f);

        assertEquals(0f, enemy.getWorldPosition().x, 0.001f,
            "Zero delta move should not change position");
    }

    // -------------------------------------------------------------------------
    // move — single-segment path
    // -------------------------------------------------------------------------

    @Test
    void move_advancesPositionTowardWaypoint() {
        // Speed 1.0 tile/s, delta 0.5s => moves 0.5 tiles = 32px
        Vector2 spawn = new Vector2(0f, 0f);
        Vector2 waypoint = new Vector2((float) TILE, 0f); // 64 px away
        EnemyInstance enemy = new EnemyInstance(groundEnemy, spawn, Collections.singletonList(waypoint));

        enemy.move(0.5f);

        assertEquals(32f, enemy.getWorldPosition().x, 0.001f,
            "Enemy should move 32px (0.5 tile) in 0.5 seconds at speed 1.0");
    }

    @Test
    void move_reachesWaypointExactly() {
        // Speed 1.0 tile/s, delta 1.0s => moves exactly 1 tile = 64px
        Vector2 spawn = new Vector2(0f, 0f);
        Vector2 waypoint = new Vector2((float) TILE, 0f);
        EnemyInstance enemy = new EnemyInstance(groundEnemy, spawn, Collections.singletonList(waypoint));

        enemy.move(1.0f);

        assertEquals((float) TILE, enemy.getWorldPosition().x, 0.001f,
            "Enemy should snap to waypoint when it exactly reaches it");
    }

    @Test
    void move_singleWaypoint_setsReachedBaseWhenReached() {
        Vector2 spawn = new Vector2(0f, 0f);
        Vector2 base = new Vector2((float) TILE, 0f);
        EnemyInstance enemy = new EnemyInstance(groundEnemy, spawn, Collections.singletonList(base));

        // Move enough to fully traverse the path
        enemy.move(2.0f);

        assertTrue(enemy.hasReachedBase(), "Enemy should have reached base after exhausting single-waypoint path");
    }

    @Test
    void move_doesNotSetReachedBaseBeforeReachingEnd() {
        Vector2 spawn = new Vector2(0f, 0f);
        Vector2 base = new Vector2((float) TILE * 10, 0f); // very far away
        EnemyInstance enemy = new EnemyInstance(groundEnemy, spawn, Collections.singletonList(base));

        enemy.move(0.1f);

        assertFalse(enemy.hasReachedBase(), "Enemy should not reach base before travelling full path");
    }

    // -------------------------------------------------------------------------
    // move — multi-segment path
    // -------------------------------------------------------------------------

    @Test
    void move_multiSegment_incrementsPathIndex() {
        // Two waypoints each 1 tile apart; speed 1.0 tile/s, delta 1.5s => crosses first waypoint
        Vector2 spawn = new Vector2(0f, 0f);
        Vector2 wp1 = new Vector2((float) TILE, 0f);
        Vector2 wp2 = new Vector2((float) TILE * 2, 0f);
        List<Vector2> path = Arrays.asList(wp1, wp2);
        EnemyInstance enemy = new EnemyInstance(groundEnemy, spawn, path);

        enemy.move(1.5f);

        // Should be halfway between wp1 and wp2
        assertEquals(TILE * 1.5f, enemy.getWorldPosition().x, 0.5f,
            "After 1.5 tiles of movement on a 2-tile path, enemy should be past first waypoint");
    }

    @Test
    void move_multiSegment_setsReachedBaseAtFinalWaypoint() {
        Vector2 spawn = new Vector2(0f, 0f);
        Vector2 wp1 = new Vector2((float) TILE, 0f);
        Vector2 wp2 = new Vector2((float) TILE * 2, 0f);
        List<Vector2> path = Arrays.asList(wp1, wp2);
        EnemyInstance enemy = new EnemyInstance(groundEnemy, spawn, path);

        // Move enough to cover the full 2-tile path
        enemy.move(3.0f);

        assertTrue(enemy.hasReachedBase(), "Enemy should reach base after traversing all waypoints");
    }

    @Test
    void move_deadEnemy_doesNotMove() {
        Vector2 spawn = new Vector2(0f, 0f);
        Vector2 waypoint = new Vector2((float) TILE, 0f);
        EnemyInstance enemy = new EnemyInstance(groundEnemy, spawn, Collections.singletonList(waypoint));
        enemy.takeDamage(100); // kill it

        enemy.move(1.0f);

        assertEquals(0f, enemy.getWorldPosition().x, 0.001f,
            "Dead enemy should not move");
    }

    @Test
    void move_speedIsScaledByTileSize() {
        // Speed 2.0 tiles/s => 128 px/s, delta 1.0s => 128px
        Enemy fastEnemy = TestAssetFactory.createEnemy(100, 2.0f, 3, false);
        Vector2 spawn = new Vector2(0f, 0f);
        Vector2 waypoint = new Vector2(200f, 0f); // well beyond 128px
        EnemyInstance enemy = new EnemyInstance(fastEnemy, spawn, Collections.singletonList(waypoint));

        enemy.move(1.0f);

        assertEquals(2.0f * TILE, enemy.getWorldPosition().x, 0.001f,
            "Enemy with speed 2.0 should travel 2 tiles in 1 second");
    }
}
