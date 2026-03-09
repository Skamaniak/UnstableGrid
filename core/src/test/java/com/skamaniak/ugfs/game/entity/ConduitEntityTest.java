package com.skamaniak.ugfs.game.entity;

import com.skamaniak.ugfs.TestAssetFactory;
import com.skamaniak.ugfs.asset.model.Conduit;
import com.skamaniak.ugfs.simulation.PowerConsumer;
import com.skamaniak.ugfs.simulation.PowerSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ConduitEntityTest {

    @Test
    void consume_forwardsPowerToTarget() {
        Conduit conduit = TestAssetFactory.createConduit(1000, 0, 5);
        PowerSource source = mock(PowerSource.class);
        PowerConsumer target = mock(PowerConsumer.class);
        when(target.consume(anyFloat(), anyFloat())).thenReturn(0f);

        ConduitEntity entity = new ConduitEntity(conduit, source, target);

        float returned = entity.consume(50f, 1.0f);

        verify(target).consume(eq(50f), eq(1.0f));
        assertEquals(0f, returned, 0.001f);
    }

    @Test
    void consume_appliesTransferLoss() {
        // loss=10 per second
        Conduit conduit = TestAssetFactory.createConduit(1000, 10, 5);
        PowerSource source = mock(PowerSource.class);
        PowerConsumer target = mock(PowerConsumer.class);
        when(target.consume(anyFloat(), anyFloat())).thenReturn(0f);

        ConduitEntity entity = new ConduitEntity(conduit, source, target);

        float returned = entity.consume(50f, 1.0f);

        // 50 - 10*1.0 = 40 usable
        verify(target).consume(eq(40f), eq(1.0f));
        assertEquals(0f, returned, 0.001f);
    }

    @Test
    void consume_rateLimit() {
        // rate=30 per second
        Conduit conduit = TestAssetFactory.createConduit(30, 0, 5);
        PowerSource source = mock(PowerSource.class);
        PowerConsumer target = mock(PowerConsumer.class);
        when(target.consume(anyFloat(), anyFloat())).thenReturn(0f);

        ConduitEntity entity = new ConduitEntity(conduit, source, target);

        float returned = entity.consume(50f, 1.0f);

        // Can only transfer 30, remainder is 50-30=20
        verify(target).consume(eq(30f), eq(1.0f));
        assertEquals(20f, returned, 0.001f);
    }

    @Test
    void consume_rateLimitAcrossMultipleCalls() {
        // rate=30 per second
        Conduit conduit = TestAssetFactory.createConduit(30, 0, 5);
        PowerSource source = mock(PowerSource.class);
        PowerConsumer target = mock(PowerConsumer.class);
        when(target.consume(anyFloat(), anyFloat())).thenReturn(0f);

        ConduitEntity entity = new ConduitEntity(conduit, source, target);

        // First call: transfers 20 out of 30 capacity
        entity.consume(20f, 1.0f);

        // Second call: only 10 capacity remaining
        float returned = entity.consume(20f, 1.0f);

        assertEquals(10f, returned, 0.001f);
    }

    @Test
    void consume_returnsUnusedPower() {
        Conduit conduit = TestAssetFactory.createConduit(1000, 0, 5);
        PowerSource source = mock(PowerSource.class);
        PowerConsumer target = mock(PowerConsumer.class);
        // Target rejects 30 out of 50
        when(target.consume(anyFloat(), anyFloat())).thenReturn(30f);

        ConduitEntity entity = new ConduitEntity(conduit, source, target);

        float returned = entity.consume(50f, 1.0f);

        // 30 rejected by target → returned to sender
        assertEquals(30f, returned, 0.001f);
    }

    @Test
    void consume_zeroLossHighRate() {
        Conduit conduit = TestAssetFactory.createConduit(10000, 0, 5);
        PowerSource source = mock(PowerSource.class);
        PowerConsumer target = mock(PowerConsumer.class);
        when(target.consume(anyFloat(), anyFloat())).thenReturn(0f);

        ConduitEntity entity = new ConduitEntity(conduit, source, target);

        float returned = entity.consume(500f, 0.5f);

        // No loss, high rate → all 500 passed through
        verify(target).consume(eq(500f), eq(0.5f));
        assertEquals(0f, returned, 0.001f);
    }

    @Test
    void resetPropagation_savesLastTransferred() {
        Conduit conduit = TestAssetFactory.createConduit(1000, 0, 5);
        PowerSource source = mock(PowerSource.class);
        PowerConsumer target = mock(PowerConsumer.class);
        when(target.consume(anyFloat(), anyFloat())).thenReturn(0f);

        ConduitEntity entity = new ConduitEntity(conduit, source, target);

        entity.consume(50f, 1.0f);
        entity.resetPropagation();

        // After reset, powerTransferred should be 0 again (ready for next frame)
        // Verify by consuming again — should have full rate capacity
        entity.consume(50f, 1.0f);
        verify(target, times(2)).consume(eq(50f), eq(1.0f));
    }

    @Test
    void register_unregister() {
        Conduit conduit = TestAssetFactory.createConduit(100, 0, 5);
        PowerSource source = mock(PowerSource.class);
        PowerConsumer target = mock(PowerConsumer.class);

        ConduitEntity entity = new ConduitEntity(conduit, source, target);

        entity.register();
        verify(source).addTo(entity);

        entity.unregister();
        verify(source).removeTo(entity);
    }

    @Test
    void equals_hashCode() {
        Conduit conduit = TestAssetFactory.createConduit(100, 0, 5);
        PowerSource source = mock(PowerSource.class);
        PowerConsumer target = mock(PowerConsumer.class);

        ConduitEntity entity1 = new ConduitEntity(conduit, source, target);
        ConduitEntity entity2 = new ConduitEntity(conduit, source, target);

        assertEquals(entity1, entity2);
        assertEquals(entity1.hashCode(), entity2.hashCode());

        // Different target → not equal
        PowerConsumer otherTarget = mock(PowerConsumer.class);
        ConduitEntity entity3 = new ConduitEntity(conduit, source, otherTarget);
        assertNotEquals(entity1, entity3);
    }
}
