package com.skamaniak.ugfs.asset.model;

public enum AssetType {
    TOWER("assets/json/tower"),
    GENERATOR("assets/json/generator"),
    POWER_STORAGE("assets/json/power-storage"),
    CONDUIT("assets/json/conduit"),
    TERRAIN("assets/json/terrain"),
    LEVEL("assets/json/level");

    private String assetPath;

    AssetType(String assetPath) {
        this.assetPath = assetPath;
    }

    public String getAssetPath() {
        return assetPath;
    }
}
