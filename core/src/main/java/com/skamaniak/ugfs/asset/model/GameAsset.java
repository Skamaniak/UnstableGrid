package com.skamaniak.ugfs.asset.model;

import java.util.List;

public class GameAsset {
    private String id;
    private String name;
    private String description;
    private AssetType assetType;
    private List<TerrainType> buildableOn;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public List<TerrainType> getBuildableOn() {
        return buildableOn;
    }
}
