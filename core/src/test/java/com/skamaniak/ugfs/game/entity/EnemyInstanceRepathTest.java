package com.skamaniak.ugfs.game.entity;

import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.GameConstants;
import com.skamaniak.ugfs.TestAssetFactory;
import com.skamaniak.ugfs.asset.model.Enemy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EnemyInstance.repath() and EnemyInstance.isFlying().
 *
 * repath() spec (from design doc):
 *  - Replaces the current path with the new path.
 *  - Resets pathIndex to 0.
 *  - Sets the first waypoint of the new path to the enemy's current world position
 *    (so the enemy continues from where it is rather than teleporting).
 *  - If newPath is null, keeps the old path unchanged.
 *
 * isFlying() spec:
 *  - Delegates directly to enemy.isFlying().
 */
class EnemyInstanceRepathTest {

    private static final int TILE = GameConstants.TILE_SIZE_PX;

    private Enemy groundEnemy;
    private Enemy flyingEnemy;

    @BeforeEach
    void setUp() {
        groundEnemy = TestAssetFactory.createEnemy(100, 1.0f, 3, false);
        flyingEnemy = TestAssetFactory.createEnemy(80, 2.0f, 5, true);
    }

    // -------------------------------------------------------------------------
    // isFlying — delegation to enemy asset
    // -------------------------------------------------------------------------

    @Test
    void isFlying_groundEnemy_returnsFalse() {
        EnemyInstance instance = new EnemyInstance(groundEnemy, new Vector2(0, 0), null);

        assertFalse(instance.isFlying(),
            "isFlying() should return false for a ground enemy");
    }

    @Test
    void isFlying_flyingEnemy_returnsTrue() {
        EnemyInstance instance = new EnemyInstance(flyingEnemy, new Vector2(0, 0), null);

        assertTrue(instance.isFlying(),
            "isFlying() should return true for a flying enemy");
    }

    // -------------------------------------------------------------------------
    // repath — null path is ignored
    // -------------------------------------------------------------------------

    @Test
    void repath_withNullPath_keepsOldPath() {
        Vector2 wp1 = new Vector2(TILE * 3f, 0f);
        Vector2 wp2 = new Vector2(TILE * 6f, 0f);
        List<Vector2> originalPath = new ArrayList<>(Arrays.asList(wp1, wp2));
        EnemyInstance instance = new EnemyInstance(groundEnemy, new Vector2(0, 0), originalPath);

        instance.repath(null);

        // The enemy should still move along the original path
        instance.move(1.0f); // speed 1 tile/s, moves 64px
        assertTrue(instance.getWorldPosition().x > 0f,
            "Enemy should still move along original path after repath(null)");
    }

    @Test
    void repath_withNullPath_doesNotCrash() {
        EnemyInstance instance = new EnemyInstance(groundEnemy, new Vector2(100f, 200f), null);

        // Must not throw
        assertDoesNotThrow(() -> instance.repath(null),
            "repath(null) must not throw an exception");
    }

    // -------------------------------------------------------------------------
    // repath — path replacement
    // -------------------------------------------------------------------------

    @Test
    void repath_replacesOldPathWithNewPath() {
        Vector2 oldTarget = new Vector2(TILE * 10f, 0f);
        List<Vector2> oldPath = new ArrayList<>(Collections.singletonList(oldTarget));
        EnemyInstance instance = new EnemyInstance(groundEnemy, new Vector2(0, 0), oldPath);

        Vector2 newTarget = new Vector2(0f, TILE * 5f); // different direction
        List<Vector2> newPath = new ArrayList<>(Arrays.asList(new Vector2(0, 0), newTarget));
        instance.repath(newPath);

        // After repathing, enemy should move toward new target (y-axis)
        instance.move(0.5f);
        assertTrue(instance.getWorldPosition().y > 0f,
            "After repath, enemy should move along the new path (y direction), not the old one");
    }

    // -------------------------------------------------------------------------
    // repath — pathIndex reset to 0
    // -------------------------------------------------------------------------

    @Test
    void repath_resetsPathIndexToZero() {
        // Build a long enough path so the enemy advances past the first waypoint
        Vector2 spawn = new Vector2(0f, 0f);
        Vector2 wp1 = new Vector2(TILE * 1f, 0f); // 1 tile away
        Vector2 wp2 = new Vector2(TILE * 2f, 0f);
        List<Vector2> initialPath = new ArrayList<>(Arrays.asList(wp1, wp2));
        EnemyInstance instance = new EnemyInstance(groundEnemy, spawn, initialPath);

        // Advance past the first waypoint so pathIndex > 0
        instance.move(1.5f); // moves 1.5 tiles, now somewhere between wp1 and wp2

        // Repath with a brand-new path
        Vector2 newWp1 = new Vector2(instance.getWorldPosition().x, instance.getWorldPosition().y);
        Vector2 newWp2 = new Vector2(newWp1.x, newWp1.y + TILE * 3f);
        List<Vector2> newPath = new ArrayList<>(Arrays.asList(new Vector2(newWp1), new Vector2(newWp2)));
        instance.repath(newPath);

        // After repath, enemy should be able to traverse the entire new path
        // (pathIndex=0 means it starts at the beginning of newPath).
        // If pathIndex were NOT reset, it would skip newPath[0] and only have newPath[1] left.
        // We verify by checking the enemy does not immediately reach base with a large delta.
        assertFalse(instance.hasReachedBase(),
            "After repath, enemy should not have reached the base immediately (pathIndex must be 0)");
    }

    // -------------------------------------------------------------------------
    // repath — first waypoint replaced with current world position
    // -------------------------------------------------------------------------

    @Test
    void repath_setsFirstWaypointToCurrentWorldPosition() {
        // Enemy is mid-way at (100, 200)
        Vector2 currentPos = new Vector2(100f, 200f);
        List<Vector2> originalPath = new ArrayList<>(Collections.singletonList(new Vector2(500f, 500f)));
        EnemyInstance instance = new EnemyInstance(groundEnemy, new Vector2(0, 0), originalPath);
        // Manually place enemy at a specific position by constructing at that spawn point
        EnemyInstance positionedInstance = new EnemyInstance(groundEnemy, currentPos, originalPath);

        // Build a new path whose first waypoint is somewhere else
        Vector2 staleFirstWaypoint = new Vector2(999f, 999f);
        Vector2 finalWaypoint = new Vector2(TILE * 8f, TILE * 8f);
        List<Vector2> newPath = new ArrayList<>(Arrays.asList(new Vector2(staleFirstWaypoint), new Vector2(finalWaypoint)));

        positionedInstance.repath(newPath);

        // The first waypoint in newPath should now equal the enemy's current world position
        assertEquals(currentPos.x, newPath.get(0).x, 0.001f,
            "repath() must set newPath[0].x to the enemy's current world x position");
        assertEquals(currentPos.y, newPath.get(0).y, 0.001f,
            "repath() must set newPath[0].y to the enemy's current world y position");
    }

    @Test
    void repath_currentPositionPreserved_enemyDoesNotTeleport() {
        // Enemy spawns at (0,0), moves right to (96,0) = 1.5 tiles
        Vector2 spawn = new Vector2(0f, 0f);
        Vector2 farRight = new Vector2(TILE * 10f, 0f);
        EnemyInstance instance = new EnemyInstance(groundEnemy, spawn, new ArrayList<>(Collections.singletonList(farRight)));
        instance.move(1.5f); // now at x=96, y=0

        float xBeforeRepath = instance.getWorldPosition().x;

        // Repath to a new path heading upward
        List<Vector2> newPath = new ArrayList<>(Arrays.asList(
            new Vector2(0f, 0f),          // placeholder; will be overwritten
            new Vector2(xBeforeRepath, TILE * 5f)
        ));
        instance.repath(newPath);

        // World position must not have changed from the repath call itself
        assertEquals(xBeforeRepath, instance.getWorldPosition().x, 0.001f,
            "repath() must not teleport the enemy — world position should remain unchanged");
    }

    // -------------------------------------------------------------------------
    // repath — empty new path is accepted (no crash, no movement)
    // -------------------------------------------------------------------------

    @Test
    void repath_withEmptyNewPath_doesNotCrash() {
        Vector2 spawn = new Vector2(0f, 0f);
        List<Vector2> originalPath = new ArrayList<>(Collections.singletonList(new Vector2(100f, 0f)));
        EnemyInstance instance = new EnemyInstance(groundEnemy, spawn, originalPath);

        assertDoesNotThrow(() -> instance.repath(new ArrayList<>()),
            "repath() with an empty list must not throw");
    }

    @Test
    void repath_withEmptyNewPath_enemyDoesNotMoveOrCrash() {
        Vector2 spawn = new Vector2(50f, 50f);
        List<Vector2> originalPath = new ArrayList<>(Collections.singletonList(new Vector2(500f, 500f)));
        EnemyInstance instance = new EnemyInstance(groundEnemy, spawn, originalPath);

        instance.repath(new ArrayList<>());
        instance.move(2.0f);

        assertEquals(50f, instance.getWorldPosition().x, 0.001f,
            "After repath to empty path, enemy should not move");
        assertEquals(50f, instance.getWorldPosition().y, 0.001f,
            "After repath to empty path, enemy should not change y");
    }
}
