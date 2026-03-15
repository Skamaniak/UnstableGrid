package com.skamaniak.ugfs.game;

import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.GameConstants;
import com.skamaniak.ugfs.asset.model.TerrainType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class TilePathfinder {
    private static final int[][] DIRECTIONS = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

    private final TerrainType[][] terrainGrid;
    private final int width;
    private final int height;
    private final Map<String, ?> entityByPosition;

    public TilePathfinder(TerrainType[][] terrainGrid, int width, int height, Map<String, ?> entityByPosition) {
        this.terrainGrid = terrainGrid;
        this.width = width;
        this.height = height;
        this.entityByPosition = entityByPosition;
    }

    public List<Vector2> findPath(int startX, int startY, int endX, int endY) {
        if (startX < 0 || startX >= width || startY < 0 || startY >= height
            || endX < 0 || endX >= width || endY < 0 || endY >= height) {
            return null;
        }

        int[][] gScore = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                gScore[x][y] = Integer.MAX_VALUE;
            }
        }
        gScore[startX][startY] = 0;

        int[][] cameFromX = new int[width][height];
        int[][] cameFromY = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                cameFromX[x][y] = -1;
                cameFromY[x][y] = -1;
            }
        }

        boolean[][] closed = new boolean[width][height];

        PriorityQueue<int[]> openSet = new PriorityQueue<>(Comparator.comparingInt(a -> a[2]));
        int h = Math.abs(endX - startX) + Math.abs(endY - startY);
        openSet.add(new int[]{startX, startY, h});

        while (!openSet.isEmpty()) {
            int[] current = openSet.poll();
            int cx = current[0];
            int cy = current[1];

            if (cx == endX && cy == endY) {
                return reconstructPath(cameFromX, cameFromY, endX, endY, startX, startY);
            }

            if (closed[cx][cy]) {
                continue;
            }
            closed[cx][cy] = true;

            for (int[] dir : DIRECTIONS) {
                int nx = cx + dir[0];
                int ny = cy + dir[1];

                if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
                    continue;
                }
                if (closed[nx][ny]) {
                    continue;
                }
                if (!isWalkable(nx, ny, startX, startY, endX, endY)) {
                    continue;
                }

                int tentativeG = gScore[cx][cy] + 1;
                if (tentativeG < gScore[nx][ny]) {
                    gScore[nx][ny] = tentativeG;
                    cameFromX[nx][ny] = cx;
                    cameFromY[nx][ny] = cy;
                    int fScore = tentativeG + Math.abs(endX - nx) + Math.abs(endY - ny);
                    openSet.add(new int[]{nx, ny, fScore});
                }
            }
        }

        return null;
    }

    private boolean isWalkable(int x, int y, int startX, int startY, int endX, int endY) {
        if ((x == startX && y == startY) || (x == endX && y == endY)) {
            return true;
        }
        if (terrainGrid[x][y] != TerrainType.LAND) {
            return false;
        }
        return !entityByPosition.containsKey(x + "," + y);
    }

    private List<Vector2> reconstructPath(int[][] cameFromX, int[][] cameFromY, int endX, int endY, int startX, int startY) {
        List<Vector2> path = new ArrayList<>();
        int cx = endX;
        int cy = endY;
        while (cx != startX || cy != startY) {
            path.add(tileCenterWorld(cx, cy));
            int px = cameFromX[cx][cy];
            int py = cameFromY[cx][cy];
            cx = px;
            cy = py;
        }
        path.add(tileCenterWorld(startX, startY));
        Collections.reverse(path);
        return path;
    }

    private static Vector2 tileCenterWorld(int tileX, int tileY) {
        return new Vector2(
            (tileX + 0.5f) * GameConstants.TILE_SIZE_PX,
            (tileY + 0.5f) * GameConstants.TILE_SIZE_PX
        );
    }
}
