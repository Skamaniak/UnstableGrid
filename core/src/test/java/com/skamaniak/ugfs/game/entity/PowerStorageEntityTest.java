package com.skamaniak.ugfs.game.entity;

import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.TestAssetFactory;
import com.skamaniak.ugfs.asset.model.PowerStorage;
import com.skamaniak.ugfs.simulation.PowerConsumer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PowerStorageEntityTest {

    @Test
    void consume_storesAndForwardsPower() {
        PowerStorage ps = TestAssetFactory.createPowerStorage(1000, 0);
        PowerStorageEntity entity = new PowerStorageEntity(new Vector2(0, 0), ps);

        PowerConsumer downstream = mock(PowerConsumer.class);
        when(downstream.consume(anyFloat(), anyFloat())).thenReturn(0f);
        entity.addTo(downstream);

        float returned = entity.consume(100f, 1.0f);

        // Downstream gets 100 (power + 0 bank), takes all, 0 returned
        verify(downstream).consume(eq(100f), eq(1.0f));
        assertEquals(0f, returned, 0.001f);
    }

    @Test
    void consume_appliesStandbyLossOnce() {
        PowerStorage ps = TestAssetFactory.createPowerStorage(1000, 10);
        PowerStorageEntity entity = new PowerStorageEntity(new Vector2(0, 0), ps);

        // First consume: standby loss applied (10 * 1.0 = 10, but bank is 0 so clamped to 0)
        float returned1 = entity.consume(50f, 1.0f);
        // Bank was 0, loss=10 → bank becomes 0, usable = 50+0=50, stored 50, returned 0
        assertEquals(0f, returned1, 0.001f);

        // Reset and call again - second consume with propagated=true should absorb without loss
        entity.resetPropagation();

        // Now bank has 50 from first call. Loss=10*1=10 → bank becomes 40, usable=100+40=140
        PowerConsumer downstream = mock(PowerConsumer.class);
        when(downstream.consume(anyFloat(), anyFloat())).thenReturn(0f);
        entity.addTo(downstream);

        entity.consume(100f, 1.0f);

        verify(downstream).consume(eq(140f), eq(1.0f));
    }

    @Test
    void consume_cycleDetection_refusesPower() {
        PowerStorage ps = TestAssetFactory.createPowerStorage(1000, 0);
        PowerStorageEntity entityA = new PowerStorageEntity(new Vector2(0, 0), ps);
        PowerStorageEntity entityB = new PowerStorageEntity(new Vector2(1, 0), ps);

        // Create cycle: A → B → A
        entityA.addTo(entityB);
        entityB.addTo(entityA);

        // When A consumes, it will forward to B, which will try to forward back to A.
        // A should detect inProgress and refuse the power.
        float returned = entityA.consume(100f, 1.0f);

        // Power should flow A→B, B tries A (refused), B stores remainder
        // No infinite loop should occur
        assertTrue(returned >= 0f);
    }

    @Test
    void consume_secondSource_absorbsWithoutForwarding() {
        PowerStorage ps = TestAssetFactory.createPowerStorage(1000, 0);
        PowerStorageEntity entity = new PowerStorageEntity(new Vector2(0, 0), ps);

        PowerConsumer downstream = mock(PowerConsumer.class);
        when(downstream.consume(anyFloat(), anyFloat())).thenReturn(0f);
        entity.addTo(downstream);

        // First source sends power
        entity.consume(50f, 1.0f);

        // Second source sends power - should absorb without forwarding downstream again
        float returned = entity.consume(30f, 1.0f);

        // Downstream should only be called once (from first consume)
        verify(downstream, times(1)).consume(anyFloat(), anyFloat());
        // Second call absorbs into bank
        assertEquals(0f, returned, 0.001f);
    }

    @Test
    void consume_returnsExcessPower() {
        PowerStorage ps = TestAssetFactory.createPowerStorage(100, 0);
        PowerStorageEntity entity = new PowerStorageEntity(new Vector2(0, 0), ps);

        // No downstream consumers, all power goes to bank
        float returned = entity.consume(150f, 1.0f);

        // Bank can hold 100, so 50 returned
        assertEquals(50f, returned, 0.001f);
    }

    @Test
    void produce_triggersConsumeIfNotPropagated() {
        PowerStorage ps = TestAssetFactory.createPowerStorage(1000, 10);
        PowerStorageEntity entity = new PowerStorageEntity(new Vector2(0, 0), ps);

        // Give it some power first
        entity.consume(100f, 1.0f);
        entity.resetPropagation();

        // Produce should trigger consume(0, delta) which applies standby loss
        entity.produce(1.0f);

        // After produce, the storage should have had standby loss applied
        // Bank was 100, loss=10*1=10 → bank should be 90
        // Verify by doing another consume and checking what's available
        entity.resetPropagation();
        PowerConsumer checker = mock(PowerConsumer.class);
        when(checker.consume(anyFloat(), anyFloat())).thenReturn(0f);
        entity.addTo(checker);
        entity.consume(0f, 0f); // delta=0 so no additional loss

        verify(checker).consume(eq(90f), eq(0f));
    }

    @Test
    void produce_skipsIfAlreadyPropagated() {
        PowerStorage ps = TestAssetFactory.createPowerStorage(1000, 10);
        PowerStorageEntity entity = new PowerStorageEntity(new Vector2(0, 0), ps);

        PowerConsumer downstream = mock(PowerConsumer.class);
        when(downstream.consume(anyFloat(), anyFloat())).thenReturn(0f);
        entity.addTo(downstream);

        // First: consume propagates
        entity.consume(50f, 1.0f);

        // Then produce should skip since already propagated
        entity.produce(1.0f);

        // Downstream should only have been called once (from consume, not from produce)
        verify(downstream, times(1)).consume(anyFloat(), anyFloat());
    }

    @Test
    void resetPropagation_clearsBothFlags() {
        PowerStorage ps = TestAssetFactory.createPowerStorage(1000, 0);
        PowerStorageEntity entity = new PowerStorageEntity(new Vector2(0, 0), ps);

        PowerConsumer downstream = mock(PowerConsumer.class);
        when(downstream.consume(anyFloat(), anyFloat())).thenReturn(0f);
        entity.addTo(downstream);

        // Propagate to set flags
        entity.consume(50f, 1.0f);

        // Reset
        entity.resetPropagation();

        // After reset, consume should forward downstream again (not just absorb)
        entity.consume(30f, 1.0f);

        verify(downstream, times(2)).consume(anyFloat(), anyFloat());
    }
}
