package com.skamaniak.ugfs.asset.model;

import java.util.List;

public class Scene extends GameAsset {
    private List<Tile> map;
    private int sceneWidth;
    private int sceneHeight;

    public int getSceneWidth() {
        return sceneWidth;
    }

    public int getSceneHeight() {
        return sceneHeight;
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
