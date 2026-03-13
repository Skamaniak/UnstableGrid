package com.skamaniak.ugfs.game;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.GameConstants;
import com.skamaniak.ugfs.UnstableGrid;
import com.skamaniak.ugfs.asset.GameAssetManager;
import com.skamaniak.ugfs.asset.model.Conduit;
import com.skamaniak.ugfs.asset.model.Level;
import com.skamaniak.ugfs.game.entity.*;
import com.skamaniak.ugfs.simulation.PowerConsumer;
import com.skamaniak.ugfs.simulation.PowerGrid;
import com.skamaniak.ugfs.simulation.PowerSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

    public void registerLinkFree(Conduit conduit, PowerSource source, PowerConsumer destination) {
        ConduitEntity existing = findConduit(source, destination);
        if (existing != null) {
            if (existing.conduit.equals(conduit)) {
                return;
            }
            conduits.remove(existing);
            grid.removeConduit(existing);
        }
        ConduitEntity conduitEntity = new ConduitEntity(conduit, source, destination);
        conduits.add(conduitEntity);
        grid.addConduit(conduitEntity);
    }

    public void registerLink(Conduit conduit, PowerSource source, PowerConsumer destination) {
        // Replace existing wire between same endpoints (refund old, charge difference)
        ConduitEntity existing = findConduit(source, destination);
        if (existing != null) {
            if (existing.conduit.equals(conduit)) {
                return; // exact same wire type, nothing to do
            }
            sellConduit(existing);
        }

        if (!spendScrap(conduit.getScrapCost())) {
            return;
        }
        ConduitEntity conduitEntity = new ConduitEntity(conduit, source, destination);
        conduits.add(conduitEntity);
        grid.addConduit(conduitEntity);
    }

    public int getScrap() {
        return scrap;
    }

    public void addScrap(int amount) {
        scrap += amount;
    }

    public boolean spendScrap(int amount) {
        if (scrap < amount) {
            return false;
        }
        scrap -= amount;
        return true;
    }

    public void sellConduit(ConduitEntity conduit) {
        conduits.remove(conduit);
        grid.removeConduit(conduit);
        addScrap(conduit.conduit.getScrapCost());
    }

    public void sellGenerator(GeneratorEntity generator) {
        List<ConduitEntity> connected = findConnectedConduits(generator);
        for (ConduitEntity conduit : connected) {
            sellConduit(conduit);
        }
        generators.remove(generator);
        entityByPosition.remove(positionKey(generator));
        grid.removeSource(generator);
        addScrap(generator.getScrapCost());
    }

    public void sellStorage(PowerStorageEntity storage) {
        List<ConduitEntity> connected = findConnectedConduits(storage);
        for (ConduitEntity conduit : connected) {
            sellConduit(conduit);
        }
        storages.remove(storage);
        entityByPosition.remove(positionKey(storage));
        grid.removeStorage(storage);
        addScrap(storage.getScrapCost());
    }

    public void sellTower(TowerEntity tower) {
        List<ConduitEntity> connected = findConnectedConduits(tower);
        for (ConduitEntity conduit : connected) {
            sellConduit(conduit);
        }
        towers.remove(tower);
        entityByPosition.remove(positionKey(tower));
        grid.removeSink(tower);
        addScrap(tower.getScrapCost());
    }

    public void sellEntity(GameEntity entity) {
        if (entity instanceof TowerEntity) {
            sellTower((TowerEntity) entity);
        } else if (entity instanceof PowerStorageEntity) {
            sellStorage((PowerStorageEntity) entity);
        } else if (entity instanceof GeneratorEntity) {
            sellGenerator((GeneratorEntity) entity);
        } else {
            throw new RuntimeException("Unknown entity " + entity);
        }
    }

    public ConduitEntity findConduit(PowerSource from, PowerConsumer to) {
        for (ConduitEntity conduit : conduits) {
            if (conduit.from == from && conduit.to == to) {
                return conduit;
            }
        }
        return null;
    }

    public boolean hasOutgoingConduits(PowerSource source) {
        for (ConduitEntity conduit : conduits) {
            if (conduit.from == source) {
                return true;
            }
        }
        return false;
    }

    public List<ConduitEntity> findConnectedConduits(Object entity) {
        List<ConduitEntity> result = new ArrayList<>();
        for (ConduitEntity conduit : conduits) {
            if (conduit.from == entity || conduit.to == entity) {
                result.add(conduit);
            }
        }
        return result;
    }

    public int computeSellValue(GameEntity entity) {
        int total = entity.getScrapCost();
        for (ConduitEntity conduit : findConnectedConduits(entity)) {
            total += conduit.conduit.getScrapCost();
        }
        return total;
    }

    public void readInputs() {
        // TODO read inputs
    }

    public Level.Tile getTerrainTile(Vector2 coordinates) {
        return getTerrainTile((int) coordinates.x, (int) coordinates.y);
    }

    public Level.Tile getTerrainTile(int x, int y) {
        return tileByPosition.get(tileKey(x / GameConstants.TILE_SIZE_PX, y / GameConstants.TILE_SIZE_PX));
    }

    public GameEntity getEntityAt(Vector2 coordinates) {
        return getEntityAt((int) coordinates.x, (int) coordinates.y);
    }

    public GameEntity getEntityAt(int x, int y) {
        return entityByPosition.get(tileKey(x / GameConstants.TILE_SIZE_PX, y / GameConstants.TILE_SIZE_PX));
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
            game.batch.draw(texture, tile.getX() * GameConstants.TILE_SIZE_PX,
                tile.getY() * GameConstants.TILE_SIZE_PX);
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
