package com.skamaniak.ugfs.asset;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.skamaniak.ugfs.asset.model.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GameAssetManager {
    public static final int TILE_SIZE_PX = 64;
    public static GameAssetManager INSTANCE = new GameAssetManager();

    // Game Objects
    private final Map<String, Tower> towers;
    private final Map<String, Generator> generators;
    private final Map<String, PowerStorage> powerStorages;
    private final Map<String, Conduit> conduits;
    private final Map<String, Level> level;
    private final Map<String, Terrain> terrains;

    // Textures
    private final Map<String, Texture> textures = new HashMap<>();
    private final Map<String, TextureRegion[][]> tailSets = new HashMap<>();
    private final Map<String, TextureRegion> repeatingTextures = new HashMap<>();

    // Audio
    private final Map<String, Sound> sounds = new HashMap<>();

    // Skin
    private final Skin skin = new Skin(Gdx.files.internal("assets/skin/uiskin.json"));

    // Font
    private final BitmapFont font;

    private GameAssetManager() {
        JsonAssetLoader jsonAssetLoader = new JsonAssetLoader();

        this.towers = jsonAssetLoader.loadTowers();
        this.generators = jsonAssetLoader.loadGenerators();
        this.powerStorages = jsonAssetLoader.loadPowerStorages();
        this.conduits = jsonAssetLoader.loadConduits();
        this.level = jsonAssetLoader.loadLevels();
        this.terrains = jsonAssetLoader.loadTerrains();

        font = new BitmapFont(); //dispose?
        font.setColor(Color.RED);
    }

    public Tower getTower(String id) {
        return towers.get(id);
    }

    public Collection<Tower> getTowers() {
        return towers.values();
    }

    public Generator getGenerator(String id) {
        return generators.get(id);
    }

    public Collection<Generator> getGenerators() {
        return generators.values();
    }

    public PowerStorage getPowerStorage(String id) {
        return powerStorages.get(id);
    }

    public Collection<PowerStorage> getPowerStorages() {
        return powerStorages.values();
    }

    public Conduit getConduit(String id) {
        return conduits.get(id);
    }

    public Collection<Conduit> getConduits() {
        return conduits.values();
    }

    public Collection<Level> getLevel() {
        return level.values();
    }

    public Collection<Terrain> getTerrains() {
        return terrains.values();
    }

    public Terrain getTerrain(String id) {
        return terrains.get(id);
    }

    public Level getLevel(String id) {
        return level.get(id);
    }

    public Texture loadTexture(String texturePath) {
        return textures.computeIfAbsent(texturePath, Texture::new);
    }

    public TextureRegion loadTerrainTileTexture(Level.Tile tile) {
        Terrain terrain = getTerrain(tile.getTerrainId());
        String tileSetPath = terrain.getTileSet();

        return tailSets.computeIfAbsent(tileSetPath, p -> {
            Texture texture = loadTexture(tileSetPath);
            TextureRegion textureRegion = new TextureRegion(texture);
            return textureRegion.split(TILE_SIZE_PX, TILE_SIZE_PX);
        })[tile.getVariant()][tile.getTileNumber()];
    }

    public TextureRegion loadRepeatingTexture(String repeatingTexturePath) {
        return repeatingTextures.computeIfAbsent(repeatingTexturePath, (path -> {
            Texture texture = loadTexture(path);
            texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge); // Enable horizontal repeat
            return new TextureRegion(texture);
        }));
    }

    public Sound loadSound(String soundPath) {
        return sounds.computeIfAbsent(soundPath, (path) -> Gdx.audio.newSound(Gdx.files.internal(path)));
    }

    public Skin getSkin() {
        return skin;
    }

    public BitmapFont getFont() {
        return font;
    }
}
