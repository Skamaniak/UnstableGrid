package com.skamaniak.ugfs.asset.model;

import java.util.List;

public class Level extends GameAsset {
    private List<Tile> map;
    private int scrap;
    private int levelWidth;
    private int levelHeight;
    private String music;
    private Position base;
    private List<Wave> waves;
    private List<SpawnLocation> spawnLocations;

    public int getScrap() {
        return scrap;
    }

    public int getLevelWidth() {
        return levelWidth;
    }

    public int getLevelHeight() {
        return levelHeight;
    }

    public String getMusic() {
        return music;
    }

    public List<Tile> getMap() {
        return map;
    }

    public Position getBase() {
        return base;
    }

    public List<Wave> getWaves() {
        return waves;
    }

    public List<SpawnLocation> getSpawnLocations() {
        return spawnLocations;
    }

    public static class Tile {
        private int x;
        private int y;
        private String terrainId;
        private int tileNumber;
        private int variant;

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public String getTerrainId() {
            return terrainId;
        }

        public int getTileNumber() {
            return tileNumber;
        }

        public int getVariant() {
            return variant;
        }
    }

    public static class Position {
        private int x;
        private int y;

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    public static class Wave {
        private int wave;
        private float delay;

        public int getWave() {
            return wave;
        }

        public float getDelay() {
            return delay;
        }
    }

    public static class SpawnLocation {
        private int x;
        private int y;
        private List<SpawnEntry> spawnPlan;

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public List<SpawnEntry> getSpawnPlan() {
            return spawnPlan;
        }
    }

    public static class SpawnEntry {
        private int wave;
        private List<EnemySpawn> enemies;

        public int getWave() {
            return wave;
        }

        public List<EnemySpawn> getEnemies() {
            return enemies;
        }
    }

    public static class EnemySpawn {
        private String enemyId;
        private int delay;

        public String getEnemyId() {
            return enemyId;
        }

        public int getDelay() {
            return delay;
        }
    }
}
