package com.skamaniak.ugfs.game.entity;

import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.TestAssetFactory;
import com.skamaniak.ugfs.asset.model.Conduit;
import com.skamaniak.ugfs.asset.model.Generator;
import com.skamaniak.ugfs.asset.model.PowerStorage;
import com.skamaniak.ugfs.asset.model.Tower;
import com.skamaniak.ugfs.simulation.PowerConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.*;

/**
 * Tests that ConduitEntity.unregister() correctly detaches the conduit from its
 * source's consumer set when the source is a real entity (not a mock), confirming
 * that the source no longer forwards power through the unregistered conduit.
 */
class ConduitEntityUnregisterTest {

    private Conduit conduitAsset;

    @BeforeEach
    void setUp() {
        conduitAsset = TestAssetFactory.createConduit(1000, 0, 10);
    }

    @Test
    void unregister_fromGeneratorSource_conduitNoLongerReceivesPower() {
        Generator gen = TestAssetFactory.createGenerator(1000, 100);
        GeneratorEntity generatorEntity = new GeneratorEntity(new Vector2(0, 0), gen);

        PowerConsumer mockTarget = mock(PowerConsumer.class);
        when(mockTarget.consume(anyFloat(), anyFloat())).thenReturn(0f);

        ConduitEntity conduit = new ConduitEntity(conduitAsset, generatorEntity, mockTarget);
        conduit.register();

        // Confirm power flows before unregister
        generatorEntity.produce(1.0f);
        verify(mockTarget, atLeastOnce()).consume(anyFloat(), anyFloat());

        conduit.unregister();

        // Reset call count and produce again — conduit should no longer be called
        reset(mockTarget);
        generatorEntity.produce(1.0f);

        verify(mockTarget, never()).consume(anyFloat(), anyFloat());
    }

    @Test
    void unregister_fromStorageSource_conduitNoLongerReceivesPower() {
        PowerStorage ps = TestAssetFactory.createPowerStorage(1000, 0);
        PowerStorageEntity storageEntity = new PowerStorageEntity(new Vector2(0, 0), ps);

        PowerConsumer mockTarget = mock(PowerConsumer.class);
        when(mockTarget.consume(anyFloat(), anyFloat())).thenReturn(0f);

        ConduitEntity conduit = new ConduitEntity(conduitAsset, storageEntity, mockTarget);
        conduit.register();

        // Prime the storage bank with some power
        storageEntity.consume(100f, 1.0f);
        storageEntity.resetPropagation();
        storageEntity.produce(1.0f);
        verify(mockTarget, atLeastOnce()).consume(anyFloat(), anyFloat());

        conduit.unregister();
        reset(mockTarget);

        storageEntity.resetPropagation();
        storageEntity.consume(100f, 0f);
        storageEntity.resetPropagation();
        storageEntity.produce(0f);

        verify(mockTarget, never()).consume(anyFloat(), anyFloat());
    }

    @Test
    void register_thenUnregister_conduitCanBeReregistered() {
        Generator gen = TestAssetFactory.createGenerator(1000, 100);
        GeneratorEntity generatorEntity = new GeneratorEntity(new Vector2(0, 0), gen);

        PowerConsumer mockTarget = mock(PowerConsumer.class);
        when(mockTarget.consume(anyFloat(), anyFloat())).thenReturn(0f);

        ConduitEntity conduit = new ConduitEntity(conduitAsset, generatorEntity, mockTarget);

        conduit.register();
        conduit.unregister();
        conduit.register(); // re-register

        generatorEntity.produce(1.0f);

        verify(mockTarget, atLeastOnce()).consume(anyFloat(), anyFloat());
    }

    @Test
    void unregister_calledTwice_doesNotThrow() {
        Generator gen = TestAssetFactory.createGenerator(1000, 100);
        GeneratorEntity generatorEntity = new GeneratorEntity(new Vector2(0, 0), gen);

        Tower tower = TestAssetFactory.createTower(1000, 0, 10, 1.0f);
        TowerEntity towerEntity = new TowerEntity(new Vector2(1, 0), tower);

        ConduitEntity conduit = new ConduitEntity(conduitAsset, generatorEntity, towerEntity);
        conduit.register();
        conduit.unregister();

        // Second unregister should not throw (removeTo on a set returns false for absent element)
        assertDoesNotThrow(conduit::unregister,
            "Calling unregister() twice should not throw an exception");
    }
}
