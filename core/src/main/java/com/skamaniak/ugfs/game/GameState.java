package com.skamaniak.ugfs.game;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.skamaniak.ugfs.UnstableGrid;
import com.skamaniak.ugfs.asset.GameAssetManager;
import com.skamaniak.ugfs.asset.model.Conduit;
import com.skamaniak.ugfs.asset.model.Level;
import com.skamaniak.ugfs.game.entity.ConduitEntity;
import com.skamaniak.ugfs.game.entity.GeneratorEntity;
import com.skamaniak.ugfs.game.entity.PowerStorageEntity;
import com.skamaniak.ugfs.game.entity.TowerEntity;
import com.skamaniak.ugfs.simulation.PowerConsumer;
import com.skamaniak.ugfs.simulation.PowerGrid;
import com.skamaniak.ugfs.simulation.PowerSource;

import java.util.HashSet;
import java.util.Set;

public class GameState {
    public final UnstableGrid game;
    private final Level level;
    private final PowerGrid grid;

    private final Set<GeneratorEntity> generators = new HashSet<>();
    private final Set<PowerStorageEntity> storages = new HashSet<>();
    private final Set<TowerEntity> towers = new HashSet<>();
    private final Set<ConduitEntity> conduits = new HashSet<>();

    private int scrap;

    public GameState(UnstableGrid game, Level level) {
        this.game = game;
        this.level = level;

        this.scrap = level.getScrap();
        this.grid = new PowerGrid();
    }

    public void registerTower(TowerEntity tower) {
        towers.add(tower);
        grid.addSink(tower);
    }

    public void registerGenerator(GeneratorEntity generator) {
        generators.add(generator);
        grid.addSource(generator);
    }

    public void registerPowerStorage(PowerStorageEntity storage) {
        storages.add(storage);
        grid.addStorage(storage);
    }

    public void registerLink(Conduit conduit, PowerSource source, PowerConsumer destination) {
        ConduitEntity conduitEntity = new ConduitEntity(conduit, source, destination);
        conduits.add(conduitEntity);
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

    public Level.Tile getTerrainTile(int x, int y) {
        x = x / GameAssetManager.TILE_SIZE_PX;
        y = y / GameAssetManager.TILE_SIZE_PX;
        for (Level.Tile tile : level.getMap()) {
            if (tile.getX() == x && tile.getY() == y) {
                return tile;
            }
        }
        return null;
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

    public void draw(float delta) {
        drawTerrain();
        drawGameEntities();

        // TODO Draw UI
        // TODO Draw Game entities
        // TODO Draw projectiles, effects, etc.
    }

    private void drawTerrain() {
        level.getMap().forEach(tile -> {
            TextureRegion texture = GameAssetManager.INSTANCE.loadTerrainTileTexture(tile);
            game.batch.draw(texture, tile.getX() * GameAssetManager.TILE_SIZE_PX,
                tile.getY() * GameAssetManager.TILE_SIZE_PX);
        });
    }

    private void drawGameEntities() {
        for (ConduitEntity conduit: conduits) {
            conduit.draw(game.batch);
        }

        for (GeneratorEntity generator: generators) {
            generator.draw(game.batch);
        }

        for (PowerStorageEntity storage: storages) {
            storage.draw(game.batch);
        }

        for (TowerEntity tower: towers) {
            tower.draw(game.batch);
        }
    }

}
