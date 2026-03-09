package com.skamaniak.ugfs.game;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.UnstableGrid;
import com.skamaniak.ugfs.asset.GameAssetManager;
import com.skamaniak.ugfs.asset.model.Conduit;
import com.skamaniak.ugfs.asset.model.Level;
import com.skamaniak.ugfs.game.entity.*;
import com.skamaniak.ugfs.simulation.PowerConsumer;
import com.skamaniak.ugfs.simulation.PowerGrid;
import com.skamaniak.ugfs.simulation.PowerSource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GameState {
    public final UnstableGrid game;
    private final Level level;
    private final PowerGrid grid;

    private final Set<GeneratorEntity> generators = new HashSet<>();
    private final Set<PowerStorageEntity> storages = new HashSet<>();
    private final Set<TowerEntity> towers = new HashSet<>();
    private final Set<ConduitEntity> conduits = new HashSet<>();

    private final Map<String, GameEntity> entityByPosition = new HashMap<>();
    private final Map<String, Level.Tile> tileByPosition = new HashMap<>();

    private int scrap;

    public GameState(UnstableGrid game, Level level) {
        this.game = game;
        this.level = level;

        this.scrap = level.getScrap();
        this.grid = new PowerGrid();

        for (Level.Tile tile : level.getMap()) {
            tileByPosition.put(tileKey(tile.getX(), tile.getY()), tile);
        }

        GameAssetManager.INSTANCE.loadSound(level.getMusic()).loop(0.15f); //TODO take volume from settings

    }

    public void registerEntity(GameEntity entity) {
        if (entity instanceof TowerEntity) {
            registerTower((TowerEntity) entity);
        } else if (entity instanceof PowerStorageEntity) {
            registerPowerStorage((PowerStorageEntity) entity);
        } else if (entity instanceof GeneratorEntity) {
            registerGenerator((GeneratorEntity) entity);
        } else {
            throw new RuntimeException("Unknown entity " + entity);
        }
    }

    public void registerTower(TowerEntity tower) {
        towers.add(tower);
        entityByPosition.put(positionKey(tower), tower);
        grid.addSink(tower);
    }

    public void registerGenerator(GeneratorEntity generator) {
        generators.add(generator);
        entityByPosition.put(positionKey(generator), generator);
        grid.addSource(generator);
    }

    public void registerPowerStorage(PowerStorageEntity storage) {
        storages.add(storage);
        entityByPosition.put(positionKey(storage), storage);
        grid.addStorage(storage);
    }

    public void registerLink(Conduit conduit, PowerSource source, PowerConsumer destination) {
        ConduitEntity conduitEntity = new ConduitEntity(conduit, source, destination);
        if (!conduits.add(conduitEntity)) {
            return; // duplicate link, already exists
        }
        grid.addConduit(conduitEntity);
    }

    public void sellConduit(ConduitEntity conduit) {
        conduits.remove(conduit);
        grid.removeConduit(conduit);
        // TODO add scrap back
    }

    public void readInputs() {
        // TODO read inputs
    }

    public Level.Tile getTerrainTile(Vector2 coordinates) {
        return getTerrainTile((int) coordinates.x, (int) coordinates.y);
    }

    public Level.Tile getTerrainTile(int x, int y) {
        return tileByPosition.get(tileKey(x / GameAssetManager.TILE_SIZE_PX, y / GameAssetManager.TILE_SIZE_PX));
    }

    public GameEntity getEntityAt(Vector2 coordinates) {
        return getEntityAt((int) coordinates.x, (int) coordinates.y);
    }

    public GameEntity getEntityAt(int x, int y) {
        return entityByPosition.get(tileKey(x / GameAssetManager.TILE_SIZE_PX, y / GameAssetManager.TILE_SIZE_PX));
    }

    private static String positionKey(GameEntity entity) {
        return tileKey((int) entity.getPosition().x, (int) entity.getPosition().y);
    }

    private static String tileKey(int x, int y) {
        return x + "," + y;
    }

    public void simulate(float delta) {
        grid.simulatePropagation(delta);
        simulateShooting(delta);
        simulateEnemies(delta);
    }

    private void simulateShooting(float delta) {
        for (TowerEntity tower : towers) {
            if (tower.attemptShot(delta)) {
                GameAssetManager.INSTANCE.loadSound(tower.tower.getShotSound()).play();
            }
        }
    }

    private void simulateEnemies(float delta) {

    }

    public void drawTextures(float delta) {
        drawTerrain();
        drawGameEntities();

        // TODO Draw UI
        // TODO Draw projectiles, effects, etc.
    }

    public void drawShapes(ShapeRenderer shapeRenderer) {
        // TODO
    }

    private void drawTerrain() {
        level.getMap().forEach(tile -> {
            TextureRegion texture = GameAssetManager.INSTANCE.loadTerrainTileTexture(tile);
            game.batch.draw(texture, tile.getX() * GameAssetManager.TILE_SIZE_PX,
                tile.getY() * GameAssetManager.TILE_SIZE_PX);
        });
    }

    private void drawGameEntities() {
        for (ConduitEntity conduit : conduits) {
            conduit.draw(game.batch);
        }

        for (GeneratorEntity generator : generators) {
            generator.draw(game.batch);
        }

        for (PowerStorageEntity storage : storages) {
            storage.draw(game.batch);
        }

        for (TowerEntity tower : towers) {
            tower.draw(game.batch);
        }
    }

}
