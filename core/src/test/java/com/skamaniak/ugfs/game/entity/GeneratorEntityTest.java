package com.skamaniak.ugfs.game.entity;

import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.TestAssetFactory;
import com.skamaniak.ugfs.asset.model.Generator;
import com.skamaniak.ugfs.simulation.PowerConsumer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GeneratorEntityTest {

    @Test
    void produce_generatesAndDistributesPower() {
        Generator gen = TestAssetFactory.createGenerator(1000, 100);
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);

        PowerConsumer consumer = mock(PowerConsumer.class);
        when(consumer.consume(anyFloat(), anyFloat())).thenReturn(0f);
        entity.addTo(consumer);

        entity.produce(0.5f);

        // rate=100, delta=0.5 → generates 50
        verify(consumer).consume(eq(50f), eq(0.5f));
    }

    @Test
    void produce_capsAtMaxCapacity() {
        Generator gen = TestAssetFactory.createGenerator(30, 100);
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);

        // No consumers, so power stays in bank
        entity.produce(1.0f); // generates 100 but capacity is 30

        // Now add a consumer and produce again (delta=0 so no new generation)
        PowerConsumer consumer = mock(PowerConsumer.class);
        when(consumer.consume(anyFloat(), anyFloat())).thenReturn(0f);
        entity.addTo(consumer);
        entity.produce(0f);

        // Should only have 30 in bank (capped)
        verify(consumer).consume(eq(30f), eq(0f));
    }

    @Test
    void produce_keepsPowerRejectedByConsumer() {
        Generator gen = TestAssetFactory.createGenerator(1000, 100);
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);

        // Consumer rejects 30 out of 50
        PowerConsumer consumer = mock(PowerConsumer.class);
        when(consumer.consume(anyFloat(), anyFloat())).thenReturn(30f);
        entity.addTo(consumer);

        entity.produce(0.5f); // generates 50, 30 rejected

        // Next frame with delta=0, should try to distribute the remaining 30
        entity.produce(0f);

        verify(consumer, times(2)).consume(anyFloat(), anyFloat());
        // Second call should have 30 in bank
        verify(consumer).consume(eq(30f), eq(0f));
    }

    @Test
    void produce_distributesToMultipleConsumers() {
        Generator gen = TestAssetFactory.createGenerator(1000, 100);
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);

        PowerConsumer consumer1 = mock(PowerConsumer.class);
        PowerConsumer consumer2 = mock(PowerConsumer.class);

        // consumer1 takes 30 out of 100, returns 70
        when(consumer1.consume(anyFloat(), anyFloat())).thenReturn(70f);
        // consumer2 takes all remaining
        when(consumer2.consume(anyFloat(), anyFloat())).thenReturn(0f);

        entity.addTo(consumer1);
        entity.addTo(consumer2);

        entity.produce(1.0f);

        // Both should have been called
        verify(consumer1).consume(anyFloat(), eq(1.0f));
        verify(consumer2).consume(anyFloat(), eq(1.0f));
    }

    @Test
    void produce_clampsNegativePowerToZero() {
        Generator gen = TestAssetFactory.createGenerator(1000, 100);
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);

        // Consumer claims to return negative (simulating rounding error)
        PowerConsumer consumer = mock(PowerConsumer.class);
        when(consumer.consume(anyFloat(), anyFloat())).thenReturn(-5f);
        entity.addTo(consumer);

        entity.produce(0.5f);

        // Next frame: power bank should have been clamped to 0, not negative
        PowerConsumer checker = mock(PowerConsumer.class);
        when(checker.consume(anyFloat(), anyFloat())).thenReturn(0f);
        entity.removeTo(consumer);
        entity.addTo(checker);

        entity.produce(0f); // delta=0, no new generation, bank should be 0

        verify(checker).consume(eq(0f), eq(0f));
    }

    @Test
    void addTo_removeTo_managesConsumerSet() {
        Generator gen = TestAssetFactory.createGenerator(1000, 100);
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);

        PowerConsumer consumer = mock(PowerConsumer.class);

        assertTrue(entity.addTo(consumer));
        assertFalse(entity.addTo(consumer)); // duplicate

        assertTrue(entity.removeTo(consumer));
        assertFalse(entity.removeTo(consumer)); // already removed

        // After removal, consumer should not receive power
        entity.produce(1.0f);
        verify(consumer, never()).consume(anyFloat(), anyFloat());
    }
}
