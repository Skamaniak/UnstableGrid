package com.skamaniak.ugfs.asset;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.skamaniak.ugfs.asset.model.*;

import java.util.HashMap;
import java.util.Map;

public class JsonAssetLoader {
    private final Json json = new Json();

    public Map<String, Tower> loadTowers() {
        return loadAssets(Tower.class, AssetType.TOWER.getAssetPath());
    }

    public Map<String, Generator> loadGenerators() {
        return loadAssets(Generator.class, AssetType.GENERATOR.getAssetPath());
    }

    public Map<String, PowerStorage> loadPowerStorages() {
        return loadAssets(PowerStorage.class, AssetType.POWER_STORAGE.getAssetPath());
    }

    public Map<String, Conduit> loadConduits() {
        return loadAssets(Conduit.class, AssetType.CONDUIT.getAssetPath());
    }

    public Map<String, Terrain> loadTerrains() {
        return loadAssets(Terrain.class, AssetType.TERRAIN.getAssetPath());
    }

    public Map<String, Level> loadLevels() {
        return loadAssets(Level.class, AssetType.LEVEL.getAssetPath());
    }

    private <T extends GameAsset> Map<String, T> loadAssets(Class<T> type, String directory) {
        FileHandle[] assetFiles = Gdx.files.internal(directory).list();
        Map<String, T> assets = new HashMap<>();

        for (FileHandle assetFile : assetFiles) {
            T asset = json.fromJson(type, assetFile);
            assets.put(asset.getId(), asset);
        }
        return assets;
    }
}
