package com.skamaniak.ugfs.asset.model;

public enum AssetType {
    TOWER("assets/json/tower"),
    GENERATOR("assets/json/generator"),
    ENERGY_STORAGE("assets/json/energy-storage"),
    CONDUIT("assets/json/conduit"),
    TERRAIN("assets/json/terrain"),
    SCENE("assets/json/scene");

    private String assetPath;

    AssetType(String assetPath) {
        this.assetPath = assetPath;
    }

    public String getAssetPath() {
        return assetPath;
    }
}
