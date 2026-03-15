package com.skamaniak.ugfs.simulation;

import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.TestAssetFactory;
import com.skamaniak.ugfs.asset.model.Conduit;
import com.skamaniak.ugfs.asset.model.Generator;
import com.skamaniak.ugfs.asset.model.PowerStorage;
import com.skamaniak.ugfs.asset.model.Tower;
import com.skamaniak.ugfs.game.entity.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PowerGridTest {

    @Test
    void simpleChain_generatorToConduitToTower() {
        Generator gen = TestAssetFactory.createGenerator(1000, 100);
        Tower tower = TestAssetFactory.createTower(1000, 0, 10, 1.0f);
        Conduit conduit = TestAssetFactory.createConduit(1000, 0, 5);

        GeneratorEntity genEntity = new GeneratorEntity(new Vector2(0, 0), gen);
        TowerEntity towerEntity = new TowerEntity(new Vector2(1, 0), tower);
        ConduitEntity conduitEntity = new ConduitEntity(conduit, genEntity, towerEntity);

        PowerGrid grid = new PowerGrid();
        grid.addSource(genEntity);
        grid.addSink(towerEntity);
        grid.addConduit(conduitEntity);

        grid.simulatePropagation(1.0f);

        // Generator produces 100, sends through conduit to tower
        assertTrue(towerEntity.getPowerBank() > 0, "Tower should have received power from generator");
    }

    @Test
    void generatorToStorageToTower() {
        Generator gen = TestAssetFactory.createGenerator(1000, 100);
        PowerStorage ps = TestAssetFactory.createPowerStorage(1000, 0);
        Tower tower = TestAssetFactory.createTower(1000, 0, 10, 1.0f);
        Conduit conduit = TestAssetFactory.createConduit(1000, 0, 5);

        GeneratorEntity genEntity = new GeneratorEntity(new Vector2(0, 0), gen);
        PowerStorageEntity storageEntity = new PowerStorageEntity(new Vector2(1, 0), ps);
        TowerEntity towerEntity = new TowerEntity(new Vector2(2, 0), tower);

        ConduitEntity conduit1 = new ConduitEntity(conduit, genEntity, storageEntity);
        ConduitEntity conduit2 = new ConduitEntity(conduit, storageEntity, towerEntity);

        PowerGrid grid = new PowerGrid();
        grid.addSource(genEntity);
        grid.addStorage(storageEntity);
        grid.addSink(towerEntity);
        grid.addConduit(conduit1);
        grid.addConduit(conduit2);

        grid.simulatePropagation(1.0f);

        // Power should flow gen → storage → tower
        assertTrue(towerEntity.getPowerBank() > 0, "Tower should have received power through storage");
    }

    @Test
    void cycleHandling_twoStoragesInCycle() {
        PowerStorage ps = TestAssetFactory.createPowerStorage(1000, 0);
        Generator gen = TestAssetFactory.createGenerator(1000, 100);
        Conduit conduit = TestAssetFactory.createConduit(1000, 0, 5);

        GeneratorEntity genEntity = new GeneratorEntity(new Vector2(0, 0), gen);
        PowerStorageEntity storageA = new PowerStorageEntity(new Vector2(1, 0), ps);
        PowerStorageEntity storageB = new PowerStorageEntity(new Vector2(2, 0), ps);

        // Gen → A, A → B, B → A (cycle)
        ConduitEntity genToA = new ConduitEntity(conduit, genEntity, storageA);
        ConduitEntity aToB = new ConduitEntity(conduit, storageA, storageB);
        ConduitEntity bToA = new ConduitEntity(conduit, storageB, storageA);

        PowerGrid grid = new PowerGrid();
        grid.addSource(genEntity);
        grid.addStorage(storageA);
        grid.addStorage(storageB);
        grid.addConduit(genToA);
        grid.addConduit(aToB);
        grid.addConduit(bToA);

        // Should complete without infinite loop
        grid.simulatePropagation(1.0f);
    }

    @Test
    void multipleSourcesOneStorage() {
        Generator gen = TestAssetFactory.createGenerator(1000, 100);
        PowerStorage ps = TestAssetFactory.createPowerStorage(1000, 0);
        Tower tower = TestAssetFactory.createTower(1000, 0, 10, 1.0f);
        Conduit conduit = TestAssetFactory.createConduit(1000, 0, 5);

        GeneratorEntity gen1 = new GeneratorEntity(new Vector2(0, 0), gen);
        GeneratorEntity gen2 = new GeneratorEntity(new Vector2(0, 1), gen);
        PowerStorageEntity storage = new PowerStorageEntity(new Vector2(1, 0), ps);
        TowerEntity towerEntity = new TowerEntity(new Vector2(2, 0), tower);

        ConduitEntity c1 = new ConduitEntity(conduit, gen1, storage);
        ConduitEntity c2 = new ConduitEntity(conduit, gen2, storage);
        ConduitEntity c3 = new ConduitEntity(conduit, storage, towerEntity);

        PowerGrid grid = new PowerGrid();
        grid.addSource(gen1);
        grid.addSource(gen2);
        grid.addStorage(storage);
        grid.addSink(towerEntity);
        grid.addConduit(c1);
        grid.addConduit(c2);
        grid.addConduit(c3);

        grid.simulatePropagation(1.0f);

        // Both generators produce 100 each, tower should receive power
        assertTrue(towerEntity.getPowerBank() > 0, "Tower should have received power from both generators");
    }

    @Test
    void deltaChunking_largeDelta() {
        Generator gen = TestAssetFactory.createGenerator(50, 100);
        Tower tower = TestAssetFactory.createTower(1000, 0, 10, 1.0f);
        Conduit conduit = TestAssetFactory.createConduit(1000, 0, 5);

        GeneratorEntity genEntity = new GeneratorEntity(new Vector2(0, 0), gen);
        TowerEntity towerEntity = new TowerEntity(new Vector2(1, 0), tower);
        ConduitEntity conduitEntity = new ConduitEntity(conduit, genEntity, towerEntity);

        PowerGrid grid = new PowerGrid();
        grid.addSource(genEntity);
        grid.addSink(towerEntity);
        grid.addConduit(conduitEntity);

        // Delta > 1s should be chunked
        grid.simulatePropagation(2.5f);

        // Generator has capacity 50, rate 100. Each 1s chunk: generates 100 capped to 50, sends 50.
        // 3 chunks (1.0 + 1.0 + 0.5): tower should have received power across all chunks
        assertTrue(towerEntity.getPowerBank() > 0, "Tower should have received power across chunked simulation");
    }

    @Test
    void entityRegistration_addRemove() {
        PowerGrid grid = new PowerGrid();

        Generator gen = TestAssetFactory.createGenerator(100, 100);
        GeneratorEntity genEntity = new GeneratorEntity(new Vector2(0, 0), gen);

        assertTrue(grid.addSource(genEntity));
        assertFalse(grid.addSource(genEntity)); // duplicate
        assertEquals(1, grid.getSources().size());

        assertTrue(grid.removeSource(genEntity));
        assertEquals(0, grid.getSources().size());
    }

    @Test
    void removeSource_cleansUpConduits() {
        Generator gen = TestAssetFactory.createGenerator(1000, 100);
        Tower tower = TestAssetFactory.createTower(1000, 0, 10, 1.0f);
        Conduit conduit = TestAssetFactory.createConduit(1000, 0, 5);

        GeneratorEntity genEntity = new GeneratorEntity(new Vector2(0, 0), gen);
        TowerEntity towerEntity = new TowerEntity(new Vector2(1, 0), tower);
        ConduitEntity conduitEntity = new ConduitEntity(conduit, genEntity, towerEntity);

        PowerGrid grid = new PowerGrid();
        grid.addSource(genEntity);
        grid.addSink(towerEntity);
        grid.addConduit(conduitEntity);

        assertEquals(1, grid.getConduits().size());

        grid.removeSource(genEntity);

        assertEquals(0, grid.getConduits().size());
        assertEquals(0, grid.getSources().size());
    }

    @Test
    void removeSink_cleansUpConduits() {
        Generator gen = TestAssetFactory.createGenerator(1000, 100);
        Tower tower = TestAssetFactory.createTower(1000, 0, 10, 1.0f);
        Conduit conduit = TestAssetFactory.createConduit(1000, 0, 5);

        GeneratorEntity genEntity = new GeneratorEntity(new Vector2(0, 0), gen);
        TowerEntity towerEntity = new TowerEntity(new Vector2(1, 0), tower);
        ConduitEntity conduitEntity = new ConduitEntity(conduit, genEntity, towerEntity);

        PowerGrid grid = new PowerGrid();
        grid.addSource(genEntity);
        grid.addSink(towerEntity);
        grid.addConduit(conduitEntity);

        grid.removeSink(towerEntity);

        assertEquals(0, grid.getConduits().size());
        assertEquals(0, grid.getSinks().size());
    }

    @Test
    void resetPropagation_resetsAllEntities() {
        PowerStorage ps = TestAssetFactory.createPowerStorage(1000, 0);
        Tower tower = TestAssetFactory.createTower(1000, 0, 10, 1.0f);
        Conduit conduit = TestAssetFactory.createConduit(1000, 0, 5);
        Generator gen = TestAssetFactory.createGenerator(1000, 100);

        GeneratorEntity genEntity = new GeneratorEntity(new Vector2(0, 0), gen);
        PowerStorageEntity storageEntity = new PowerStorageEntity(new Vector2(1, 0), ps);
        TowerEntity towerEntity = new TowerEntity(new Vector2(2, 0), tower);

        ConduitEntity c1 = new ConduitEntity(conduit, genEntity, storageEntity);
        ConduitEntity c2 = new ConduitEntity(conduit, storageEntity, towerEntity);

        PowerGrid grid = new PowerGrid();
        grid.addSource(genEntity);
        grid.addStorage(storageEntity);
        grid.addSink(towerEntity);
        grid.addConduit(c1);
        grid.addConduit(c2);

        // Simulate twice — second simulation should work correctly (flags reset between frames)
        grid.simulatePropagation(1.0f);
        grid.simulatePropagation(1.0f);

        // Tower should have accumulated power over 2 frames
        assertTrue(towerEntity.getPowerBank() > 0, "Tower should have accumulated power over 2 propagation frames");
    }
}
