package com.skamaniak.ugfs.game;

import com.skamaniak.ugfs.UnstableGrid;
import com.skamaniak.ugfs.asset.model.Level;
import com.skamaniak.ugfs.asset.model.PowerStorage;
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
    private final UnstableGrid game;
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

    public void registerLink(ConduitEntity conduit, PowerSource source, PowerConsumer destination) {
        conduits.add(conduit);
        grid.addLink(conduit, source, destination);
    }

    public void sellConduit(ConduitEntity conduit) {
        conduits.remove(conduit);
        grid.removeLink(conduit);
        // TODO add scrap back
    }

    public void run(float delta) {
        readInputs();
        simulate(delta);
        draw(delta);
    }

    private void readInputs() {
        // TODO read inputs
    }

    private void simulate(float delta) {
        grid.simulatePropagation(delta);
        simulateShooting(delta);
        simulateEnemies(delta);
    }

    private void simulateShooting(float delta) {
        for (TowerEntity tower: towers) {
            if (tower.attemptShot(delta)) {
                game.gameAssetManager.loadSound(tower.tower.getShotSound()).play();
            }
        }
    }

    private void simulateEnemies(float delta) {

    }

    private void draw(float delta) {
        // TODO Draw UI
        // TODO Draw Game entities
        // TODO Draw Enemies
        // TODO Draw projectiles, effects, etc.
    }
}
