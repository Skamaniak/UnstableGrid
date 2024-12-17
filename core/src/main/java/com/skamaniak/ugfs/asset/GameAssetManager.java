package com.skamaniak.ugfs.asset;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.skamaniak.ugfs.asset.model.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GameAssetManager {
    public static final int TILE_SIZE_PX = 64;

    // Game Objects
    private final Map<String, Tower> towers;
    private final Map<String, Generator> generators;
    private final Map<String, EnergyStorage> energyStorages;
    private final Map<String, Conduit> conduits;
    private final Map<String, Scene> scenes;
    private final Map<String, Terrain> terrains;

    // Textures
    private final Map<String, Texture> textures = new HashMap<>();
    private final Map<String, TextureRegion[][]> tailSets = new HashMap<>();

    public GameAssetManager() {
        JsonAssetLoader jsonAssetLoader = new JsonAssetLoader();

        this.towers = jsonAssetLoader.loadTowers();
        this.generators = jsonAssetLoader.loadGenerators();
        this.energyStorages = jsonAssetLoader.loadEnergyStorages();
        this.conduits = jsonAssetLoader.loadConduits();
        this.scenes = jsonAssetLoader.loadScenes();
        this.terrains = jsonAssetLoader.loadTerrains();
    }

    public Collection<Tower> getTowers() {
        return towers.values();
    }

    public Collection<Generator> getGenerators() {
        return generators.values();
    }

    public Collection<EnergyStorage> getEnergyStorages() {
        return energyStorages.values();
    }

    public Collection<Conduit> getConduits() {
        return conduits.values();
    }

    public Collection<Scene> getScenes() {
        return scenes.values();
    }

    public Collection<Terrain> getTerrains() {
        return terrains.values();
    }

    public Terrain getTerrain(String id) {
        return terrains.get(id);
    }

    public Scene getScene(String id) {
        return scenes.get(id);
    }

    public Texture loadTexture(String texturePath) {
        return textures.computeIfAbsent(texturePath, Texture::new);
    }

    public TextureRegion loadTerrainTileTexture(Scene.Tile tile) {
        Terrain terrain = getTerrain(tile.getTerrainId());
        String tileSetPath = terrain.getTileSet();

        return tailSets.computeIfAbsent(tileSetPath, p -> {
            Texture texture = loadTexture(tileSetPath);
            TextureRegion textureRegion = new TextureRegion(texture);
            return textureRegion.split(TILE_SIZE_PX, TILE_SIZE_PX);
        })[tile.getVariant()][tile.getTileNumber()];
    }
}
