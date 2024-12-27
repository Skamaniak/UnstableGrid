package com.skamaniak.ugfs.asset.model;

import java.util.List;

public class Level extends GameAsset {
    private List<Tile> map;
    private int scrap;
    private int levelWidth;
    private int levelHeight;
    private String music;

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
}
