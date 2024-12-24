package com.skamaniak.ugfs.asset.model;

public class Terrain extends GameAsset {
    private String tileSet;
    private TerrainType terrainType;

    public String getTileSet() {
        return tileSet;
    }

    public TerrainType getTerrainType() {
        return terrainType;
    }
}
