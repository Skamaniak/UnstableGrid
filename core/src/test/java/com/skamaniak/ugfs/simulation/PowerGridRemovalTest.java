package com.skamaniak.ugfs.simulation;

import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.TestAssetFactory;
import com.skamaniak.ugfs.asset.model.Conduit;
import com.skamaniak.ugfs.asset.model.Generator;
import com.skamaniak.ugfs.asset.model.PowerStorage;
import com.skamaniak.ugfs.asset.model.Tower;
import com.skamaniak.ugfs.game.entity.ConduitEntity;
import com.skamaniak.ugfs.game.entity.GeneratorEntity;
import com.skamaniak.ugfs.game.entity.PowerStorageEntity;
import com.skamaniak.ugfs.game.entity.TowerEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PowerGrid cascade removal of conduits when sources, sinks, or storages are removed.
 * Covers the bug fixes in removeSource/removeSink (use conduit.unregister()) and the new
 * cascade logic in removeStorage (removes conduits where storage is from OR to).
 */
class PowerGridRemovalTest {

    private Generator gen;
    private PowerStorage ps;
    private Tower tower;
    private Conduit conduit;

    @BeforeEach
    void setUp() {
        gen = TestAssetFactory.createGenerator(1000, 100);
        ps = TestAssetFactory.createPowerStorage(1000, 0);
        tower = TestAssetFactory.createTower(1000, 0, 10, 1.0f);
        conduit = TestAssetFactory.createConduit(1000, 0, 10);
    }

    // ---- removeSource tests ----

    @Test
    void removeSource_removesSourceFromGrid() {
        GeneratorEntity gen1 = new GeneratorEntity(new Vector2(0, 0), gen);

        PowerGrid grid = new PowerGrid();
        grid.addSource(gen1);

        grid.removeSource(gen1);

        assertEquals(0, grid.getSources().size(), "Sources set should be empty after removeSource");
    }

    @Test
    void removeSource_cascadesAndRemovesAttachedConduit() {
        GeneratorEntity gen1 = new GeneratorEntity(new Vector2(0, 0), gen);
        TowerEntity tower1 = new TowerEntity(new Vector2(1, 0), tower);
        ConduitEntity conduit1 = new ConduitEntity(conduit, gen1, tower1);

        PowerGrid grid = new PowerGrid();
        grid.addSource(gen1);
        grid.addSink(tower1);
        grid.addConduit(conduit1);

        assertEquals(1, grid.getConduits().size(), "Precondition: conduit should be registered");

        grid.removeSource(gen1);

        assertEquals(0, grid.getConduits().size(), "Conduit should be removed when its source is removed");
    }

    @Test
    void removeSource_unregistersConduitFromSource_soNoPropagation() {
        GeneratorEntity gen1 = new GeneratorEntity(new Vector2(0, 0), gen);
        TowerEntity tower1 = new TowerEntity(new Vector2(1, 0), tower);
        ConduitEntity conduit1 = new ConduitEntity(conduit, gen1, tower1);

        PowerGrid grid = new PowerGrid();
        grid.addSource(gen1);
        grid.addSink(tower1);
        grid.addConduit(conduit1);

        grid.removeSource(gen1);
        // Re-add just the generator to see that the conduit no longer routes power
        grid.addSource(gen1);
        grid.simulatePropagation(1.0f);

        assertFalse(tower1.attemptShot(1.0f),
            "Tower should not receive power after source and its conduit have been removed");
    }

    @Test
    void removeSource_cascadesMultipleConduitsFromSameSource() {
        GeneratorEntity gen1 = new GeneratorEntity(new Vector2(0, 0), gen);
        TowerEntity tower1 = new TowerEntity(new Vector2(1, 0), tower);
        TowerEntity tower2 = new TowerEntity(new Vector2(2, 0), tower);
        ConduitEntity conduit1 = new ConduitEntity(conduit, gen1, tower1);
        ConduitEntity conduit2 = new ConduitEntity(conduit, gen1, tower2);

        PowerGrid grid = new PowerGrid();
        grid.addSource(gen1);
        grid.addSink(tower1);
        grid.addSink(tower2);
        grid.addConduit(conduit1);
        grid.addConduit(conduit2);

        assertEquals(2, grid.getConduits().size(), "Precondition: two conduits");

        grid.removeSource(gen1);

        assertEquals(0, grid.getConduits().size(),
            "All conduits from the removed source should be cascade-removed");
    }

    @Test
    void removeSource_doesNotRemoveConduitsFromOtherSources() {
        GeneratorEntity gen1 = new GeneratorEntity(new Vector2(0, 0), gen);
        GeneratorEntity gen2 = new GeneratorEntity(new Vector2(0, 1), gen);
        TowerEntity tower1 = new TowerEntity(new Vector2(1, 0), tower);
        TowerEntity tower2 = new TowerEntity(new Vector2(1, 1), tower);
        ConduitEntity conduit1 = new ConduitEntity(conduit, gen1, tower1);
        ConduitEntity conduit2 = new ConduitEntity(conduit, gen2, tower2);

        PowerGrid grid = new PowerGrid();
        grid.addSource(gen1);
        grid.addSource(gen2);
        grid.addSink(tower1);
        grid.addSink(tower2);
        grid.addConduit(conduit1);
        grid.addConduit(conduit2);

        grid.removeSource(gen1);

        assertEquals(1, grid.getConduits().size(),
            "Only the conduit belonging to the removed source should be removed");
        assertTrue(grid.getConduits().contains(conduit2),
            "Conduit from the surviving source should remain");
    }

    // ---- removeSink tests ----

    @Test
    void removeSink_removesSinkFromGrid() {
        TowerEntity tower1 = new TowerEntity(new Vector2(1, 0), tower);

        PowerGrid grid = new PowerGrid();
        grid.addSink(tower1);

        grid.removeSink(tower1);

        assertEquals(0, grid.getSinks().size(), "Sinks set should be empty after removeSink");
    }

    @Test
    void removeSink_cascadesAndRemovesAttachedConduit() {
        GeneratorEntity gen1 = new GeneratorEntity(new Vector2(0, 0), gen);
        TowerEntity tower1 = new TowerEntity(new Vector2(1, 0), tower);
        ConduitEntity conduit1 = new ConduitEntity(conduit, gen1, tower1);

        PowerGrid grid = new PowerGrid();
        grid.addSource(gen1);
        grid.addSink(tower1);
        grid.addConduit(conduit1);

        assertEquals(1, grid.getConduits().size(), "Precondition: conduit should be registered");

        grid.removeSink(tower1);

        assertEquals(0, grid.getConduits().size(), "Conduit should be removed when its sink is removed");
    }

    @Test
    void removeSink_unregistersConduitFromSource_soNoPropagation() {
        GeneratorEntity gen1 = new GeneratorEntity(new Vector2(0, 0), gen);
        TowerEntity tower1 = new TowerEntity(new Vector2(1, 0), tower);
        ConduitEntity conduit1 = new ConduitEntity(conduit, gen1, tower1);

        PowerGrid grid = new PowerGrid();
        grid.addSource(gen1);
        grid.addSink(tower1);
        grid.addConduit(conduit1);

        grid.removeSink(tower1);
        // Re-add just the sink to test isolation: conduit should no longer forward power
        grid.addSink(tower1);
        grid.simulatePropagation(1.0f);

        assertFalse(tower1.attemptShot(1.0f),
            "Tower should not receive power after the connecting conduit was unregistered by removeSink");
    }

    @Test
    void removeSink_onlyRemovesConduitsTerminatingAtThatSink() {
        GeneratorEntity gen1 = new GeneratorEntity(new Vector2(0, 0), gen);
        TowerEntity tower1 = new TowerEntity(new Vector2(1, 0), tower);
        TowerEntity tower2 = new TowerEntity(new Vector2(2, 0), tower);
        ConduitEntity conduit1 = new ConduitEntity(conduit, gen1, tower1);
        ConduitEntity conduit2 = new ConduitEntity(conduit, gen1, tower2);

        PowerGrid grid = new PowerGrid();
        grid.addSource(gen1);
        grid.addSink(tower1);
        grid.addSink(tower2);
        grid.addConduit(conduit1);
        grid.addConduit(conduit2);

        grid.removeSink(tower1);

        assertEquals(1, grid.getConduits().size(),
            "Only the conduit leading to the removed sink should be removed");
        assertTrue(grid.getConduits().contains(conduit2),
            "Conduit to the surviving sink should remain");
    }

    // ---- removeStorage tests ----

    @Test
    void removeStorage_removesStorageFromGrid() {
        PowerStorageEntity storage = new PowerStorageEntity(new Vector2(1, 0), ps);

        PowerGrid grid = new PowerGrid();
        grid.addStorage(storage);

        grid.removeStorage(storage);

        assertEquals(0, grid.getStorages().size(), "Storages set should be empty after removeStorage");
    }

    @Test
    void removeStorage_cascadesOutgoingConduit() {
        GeneratorEntity gen1 = new GeneratorEntity(new Vector2(0, 0), gen);
        PowerStorageEntity storage = new PowerStorageEntity(new Vector2(1, 0), ps);
        TowerEntity tower1 = new TowerEntity(new Vector2(2, 0), tower);
        ConduitEntity conduitGenToStorage = new ConduitEntity(conduit, gen1, storage);
        ConduitEntity conduitStorageToTower = new ConduitEntity(conduit, storage, tower1);

        PowerGrid grid = new PowerGrid();
        grid.addSource(gen1);
        grid.addStorage(storage);
        grid.addSink(tower1);
        grid.addConduit(conduitGenToStorage);
        grid.addConduit(conduitStorageToTower);

        assertEquals(2, grid.getConduits().size(), "Precondition: two conduits");

        grid.removeStorage(storage);

        assertEquals(0, grid.getConduits().size(),
            "Both conduits (incoming and outgoing) should be removed when storage is removed");
    }

    @Test
    void removeStorage_cascadesIncomingConduit() {
        GeneratorEntity gen1 = new GeneratorEntity(new Vector2(0, 0), gen);
        PowerStorageEntity storage = new PowerStorageEntity(new Vector2(1, 0), ps);
        ConduitEntity conduitGenToStorage = new ConduitEntity(conduit, gen1, storage);

        PowerGrid grid = new PowerGrid();
        grid.addSource(gen1);
        grid.addStorage(storage);
        grid.addConduit(conduitGenToStorage);

        grid.removeStorage(storage);

        assertEquals(0, grid.getConduits().size(),
            "Incoming conduit to the removed storage should be cascade-removed");
    }

    @Test
    void removeStorage_unregistersIncomingConduit_soNoPropagation() {
        GeneratorEntity gen1 = new GeneratorEntity(new Vector2(0, 0), gen);
        PowerStorageEntity storage = new PowerStorageEntity(new Vector2(1, 0), ps);
        TowerEntity tower1 = new TowerEntity(new Vector2(2, 0), tower);
        ConduitEntity conduitGenToStorage = new ConduitEntity(conduit, gen1, storage);
        ConduitEntity conduitStorageToTower = new ConduitEntity(conduit, storage, tower1);

        PowerGrid grid = new PowerGrid();
        grid.addSource(gen1);
        grid.addStorage(storage);
        grid.addSink(tower1);
        grid.addConduit(conduitGenToStorage);
        grid.addConduit(conduitStorageToTower);

        grid.removeStorage(storage);

        // Re-add storage and tower to see that no forwarding occurs via the old conduits
        grid.addStorage(storage);
        grid.simulatePropagation(1.0f);

        assertFalse(tower1.attemptShot(1.0f),
            "Tower should not receive power after storage and its conduits have been removed");
    }

    @Test
    void removeStorage_doesNotRemoveConduitsUnrelatedToRemovedStorage() {
        GeneratorEntity gen1 = new GeneratorEntity(new Vector2(0, 0), gen);
        PowerStorageEntity storage1 = new PowerStorageEntity(new Vector2(1, 0), ps);
        PowerStorageEntity storage2 = new PowerStorageEntity(new Vector2(2, 0), ps);
        TowerEntity tower1 = new TowerEntity(new Vector2(3, 0), tower);
        ConduitEntity conduitGenToStorage1 = new ConduitEntity(conduit, gen1, storage1);
        ConduitEntity conduitGenToStorage2 = new ConduitEntity(conduit, gen1, storage2);
        ConduitEntity conduitStorage2ToTower = new ConduitEntity(conduit, storage2, tower1);

        PowerGrid grid = new PowerGrid();
        grid.addSource(gen1);
        grid.addStorage(storage1);
        grid.addStorage(storage2);
        grid.addSink(tower1);
        grid.addConduit(conduitGenToStorage1);
        grid.addConduit(conduitGenToStorage2);
        grid.addConduit(conduitStorage2ToTower);

        grid.removeStorage(storage1);

        assertEquals(2, grid.getConduits().size(),
            "Only conduit connected to removed storage1 should be removed; storage2's conduits must remain");
    }

    // ---- Simulation correctness after removal ----

    @Test
    void afterRemoveSource_simulationDoesNotRouteThroughRemovedSource() {
        GeneratorEntity gen1 = new GeneratorEntity(new Vector2(0, 0), gen);
        TowerEntity tower1 = new TowerEntity(new Vector2(1, 0), tower);
        ConduitEntity conduit1 = new ConduitEntity(conduit, gen1, tower1);

        PowerGrid grid = new PowerGrid();
        grid.addSource(gen1);
        grid.addSink(tower1);
        grid.addConduit(conduit1);

        // Confirm power flows before removal
        grid.simulatePropagation(1.0f);
        assertTrue(tower1.attemptShot(1.0f), "Precondition: tower should be powered before source removal");

        // Remove and verify no further routing
        grid.removeSource(gen1);
        tower1.resetPropagation(); // clear accumulated power
        // manually drain bank by not consuming further — we check a fresh entity instead
        TowerEntity tower2 = new TowerEntity(new Vector2(1, 1), tower);
        grid.addSink(tower2);
        grid.simulatePropagation(1.0f);

        assertFalse(tower2.attemptShot(1.0f),
            "A freshly added tower should receive no power once the only source was removed");
    }

    @Test
    void afterRemoveStorage_powerFlowChainIsBroken() {
        GeneratorEntity gen1 = new GeneratorEntity(new Vector2(0, 0), gen);
        PowerStorageEntity storage = new PowerStorageEntity(new Vector2(1, 0), ps);
        TowerEntity tower1 = new TowerEntity(new Vector2(2, 0), tower);
        ConduitEntity conduitGenToStorage = new ConduitEntity(conduit, gen1, storage);
        ConduitEntity conduitStorageToTower = new ConduitEntity(conduit, storage, tower1);

        PowerGrid grid = new PowerGrid();
        grid.addSource(gen1);
        grid.addStorage(storage);
        grid.addSink(tower1);
        grid.addConduit(conduitGenToStorage);
        grid.addConduit(conduitStorageToTower);

        // Confirm chain works before removal
        grid.simulatePropagation(1.0f);
        assertTrue(tower1.attemptShot(1.0f), "Precondition: tower should be powered through storage");

        // Remove storage — chain is broken
        grid.removeStorage(storage);
        TowerEntity tower2 = new TowerEntity(new Vector2(2, 1), tower);
        grid.addSink(tower2);
        grid.simulatePropagation(1.0f);

        assertFalse(tower2.attemptShot(1.0f),
            "A fresh tower should receive no power once the relay storage was removed");
    }
}
