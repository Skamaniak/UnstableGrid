package com.skamaniak.ugfs.game;

import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.GameConstants;
import com.skamaniak.ugfs.asset.model.TerrainType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TilePathfinderTest {

    private static final int TILE = GameConstants.TILE_SIZE_PX; // 64

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Creates an all-LAND grid of the given dimensions. */
    private static TerrainType[][] allLand(int width, int height) {
        TerrainType[][] grid = new TerrainType[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid[x][y] = TerrainType.LAND;
            }
        }
        return grid;
    }

    /** Returns the expected tile-center world position for tile (tx, ty). */
    private static Vector2 tileCenter(int tx, int ty) {
        return new Vector2((tx + 0.5f) * TILE, (ty + 0.5f) * TILE);
    }

    // -------------------------------------------------------------------------
    // Out-of-bounds coordinates
    // -------------------------------------------------------------------------

    @Test
    void findPath_startOutOfBounds_returnsNull() {
        TilePathfinder pf = new TilePathfinder(allLand(5, 5), 5, 5, new HashMap<>());

        List<Vector2> path = pf.findPath(-1, 0, 4, 4);

        assertNull(path, "Out-of-bounds start should return null");
    }

    @Test
    void findPath_endOutOfBounds_returnsNull() {
        TilePathfinder pf = new TilePathfinder(allLand(5, 5), 5, 5, new HashMap<>());

        List<Vector2> path = pf.findPath(0, 0, 5, 4);

        assertNull(path, "Out-of-bounds end should return null");
    }

    // -------------------------------------------------------------------------
    // Trivial cases
    // -------------------------------------------------------------------------

    @Test
    void findPath_startEqualsEnd_returnsSingleNodePath() {
        TilePathfinder pf = new TilePathfinder(allLand(5, 5), 5, 5, new HashMap<>());

        List<Vector2> path = pf.findPath(2, 2, 2, 2);

        assertNotNull(path, "Path from a tile to itself should not be null");
        assertEquals(1, path.size(), "Path from a tile to itself should have exactly one node");
    }

    @Test
    void findPath_startEqualsEnd_containsTileCenter() {
        TilePathfinder pf = new TilePathfinder(allLand(5, 5), 5, 5, new HashMap<>());

        List<Vector2> path = pf.findPath(2, 2, 2, 2);

        assertEquals(tileCenter(2, 2).x, path.get(0).x, 0.001f,
            "Single-node path should be at the tile center x");
        assertEquals(tileCenter(2, 2).y, path.get(0).y, 0.001f,
            "Single-node path should be at the tile center y");
    }

    // -------------------------------------------------------------------------
    // Open path (no obstacles)
    // -------------------------------------------------------------------------

    @Test
    void findPath_straightLine_returnsCorrectLength() {
        // 5-wide, 1-tall strip. Path from (0,0) to (4,0) should be 5 nodes.
        TilePathfinder pf = new TilePathfinder(allLand(5, 1), 5, 1, new HashMap<>());

        List<Vector2> path = pf.findPath(0, 0, 4, 0);

        assertNotNull(path, "Straight-line path should not be null");
        assertEquals(5, path.size(), "Straight-line path of 4 steps should have 5 nodes");
    }

    @Test
    void findPath_straightLine_startsAtSpawn() {
        TilePathfinder pf = new TilePathfinder(allLand(5, 1), 5, 1, new HashMap<>());

        List<Vector2> path = pf.findPath(0, 0, 4, 0);

        assertEquals(tileCenter(0, 0).x, path.get(0).x, 0.001f, "Path should start at spawn tile center");
        assertEquals(tileCenter(0, 0).y, path.get(0).y, 0.001f, "Path should start at spawn tile center y");
    }

    @Test
    void findPath_straightLine_endsAtBase() {
        TilePathfinder pf = new TilePathfinder(allLand(5, 1), 5, 1, new HashMap<>());

        List<Vector2> path = pf.findPath(0, 0, 4, 0);

        Vector2 last = path.get(path.size() - 1);
        assertEquals(tileCenter(4, 0).x, last.x, 0.001f, "Path should end at base tile center");
        assertEquals(tileCenter(4, 0).y, last.y, 0.001f, "Path should end at base tile center y");
    }

    // -------------------------------------------------------------------------
    // Blocked path (all water between start and end)
    // -------------------------------------------------------------------------

    @Test
    void findPath_completelyBlocked_returnsNull() {
        // 3x1 grid: [start=LAND][WATER][end=LAND]
        TerrainType[][] grid = {
            {TerrainType.LAND},
            {TerrainType.WATER},
            {TerrainType.LAND}
        };
        TilePathfinder pf = new TilePathfinder(grid, 3, 1, new HashMap<>());

        List<Vector2> path = pf.findPath(0, 0, 2, 0);

        assertNull(path, "Path should be null when route is entirely blocked by water");
    }

    // -------------------------------------------------------------------------
    // Non-LAND terrain is impassable
    // -------------------------------------------------------------------------

    @Test
    void findPath_waterTilesAreImpassable() {
        // 3x3 grid, all LAND except center column is WATER
        // Force path to go around via y=1 row or be blocked
        // Layout: column x=1 is WATER for all y. Path from (0,1) to (2,1) must go around
        // but in a 3x3 with column blocked, no detour -> null
        TerrainType[][] grid = new TerrainType[3][3];
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                grid[x][y] = (x == 1) ? TerrainType.WATER : TerrainType.LAND;
            }
        }
        TilePathfinder pf = new TilePathfinder(grid, 3, 3, new HashMap<>());

        List<Vector2> path = pf.findPath(0, 1, 2, 1);

        assertNull(path, "Column of water tiles should fully block the path");
    }

    @Test
    void findPath_rockTilesAreImpassable() {
        // Similar to water — ROCKS is not LAND so it's impassable
        TerrainType[][] grid = {
            {TerrainType.LAND},
            {TerrainType.ROCKS},
            {TerrainType.LAND}
        };
        TilePathfinder pf = new TilePathfinder(grid, 3, 1, new HashMap<>());

        List<Vector2> path = pf.findPath(0, 0, 2, 0);

        assertNull(path, "ROCKS tile in the only route should make path null");
    }

    // -------------------------------------------------------------------------
    // Path around obstacles
    // -------------------------------------------------------------------------

    @Test
    void findPath_aroundSingleObstacle_findsDeroute() {
        // 3x3 all LAND except (1,1) blocked by an entity. Path (0,1)->(2,1) must go around.
        TerrainType[][] grid = allLand(3, 3);
        Map<String, Object> entities = new HashMap<>();
        entities.put("1,1", new Object()); // structure at (1,1)

        TilePathfinder pf = new TilePathfinder(grid, 3, 3, entities);

        List<Vector2> path = pf.findPath(0, 1, 2, 1);

        assertNotNull(path, "Should find a detour around a single occupied tile");
    }

    @Test
    void findPath_occupiedTilesAreImpassable() {
        // Block the entire middle column with entities (all LAND but occupied)
        TerrainType[][] grid = allLand(3, 3);
        Map<String, Object> entities = new HashMap<>();
        entities.put("1,0", new Object());
        entities.put("1,1", new Object());
        entities.put("1,2", new Object());

        TilePathfinder pf = new TilePathfinder(grid, 3, 3, entities);

        List<Vector2> path = pf.findPath(0, 1, 2, 1);

        assertNull(path, "Fully occupied column should block path even on LAND terrain");
    }

    // -------------------------------------------------------------------------
    // Start and end tiles are always walkable (even if occupied or non-LAND)
    // -------------------------------------------------------------------------

    @Test
    void findPath_startTileIsAlwaysWalkable_evenIfOccupied() {
        // Start tile (0,0) has a structure on it — should still be reachable as the path start
        TerrainType[][] grid = allLand(5, 1);
        Map<String, Object> entities = new HashMap<>();
        entities.put("0,0", new Object());

        TilePathfinder pf = new TilePathfinder(grid, 5, 1, entities);

        List<Vector2> path = pf.findPath(0, 0, 4, 0);

        assertNotNull(path, "Start tile occupied by structure should still allow path to start");
    }

    @Test
    void findPath_endTileIsAlwaysWalkable_evenIfOccupied() {
        // End tile (4,0) has a structure on it — should still be the valid destination
        TerrainType[][] grid = allLand(5, 1);
        Map<String, Object> entities = new HashMap<>();
        entities.put("4,0", new Object());

        TilePathfinder pf = new TilePathfinder(grid, 5, 1, entities);

        List<Vector2> path = pf.findPath(0, 0, 4, 0);

        assertNotNull(path, "End tile occupied by structure should still be a valid destination");
    }

    @Test
    void findPath_endTileIsAlwaysWalkable_evenIfWater() {
        // End tile (4,0) is WATER terrain — should still be reachable as base
        TerrainType[][] grid = allLand(5, 1);
        grid[4][0] = TerrainType.WATER;

        TilePathfinder pf = new TilePathfinder(grid, 5, 1, new HashMap<>());

        List<Vector2> path = pf.findPath(0, 0, 4, 0);

        assertNotNull(path, "End tile on WATER terrain should still be a valid destination");
    }

    // -------------------------------------------------------------------------
    // Path returns tile-center world coordinates
    // -------------------------------------------------------------------------

    @Test
    void findPath_pathNodesAreAtTileCenters() {
        TilePathfinder pf = new TilePathfinder(allLand(3, 1), 3, 1, new HashMap<>());

        List<Vector2> path = pf.findPath(0, 0, 2, 0);

        for (Vector2 node : path) {
            // Each node x should be of the form (n + 0.5) * TILE
            float relX = node.x / TILE;
            assertEquals(0.5f, relX - (int) relX, 0.001f,
                "Path node x should be at the tile center (offset 0.5 * TILE_SIZE_PX)");
        }
    }

    // -------------------------------------------------------------------------
    // Finds shortest (Manhattan-optimal) path
    // -------------------------------------------------------------------------

    @Test
    void findPath_openGrid_pathLengthIsManhattanDistancePlusOne() {
        // In an open grid, A* should find the shortest path (Manhattan distance + 1 nodes)
        TilePathfinder pf = new TilePathfinder(allLand(5, 5), 5, 5, new HashMap<>());

        List<Vector2> path = pf.findPath(0, 0, 3, 2);

        // Manhattan distance = |3-0| + |2-0| = 5, so path has 6 nodes
        assertNotNull(path, "Path should exist in open grid");
        assertEquals(6, path.size(), "Path length should be Manhattan distance + 1 in open grid");
    }
}
