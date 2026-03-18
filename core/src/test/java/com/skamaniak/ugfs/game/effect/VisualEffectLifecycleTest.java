package com.skamaniak.ugfs.game.effect;

import com.badlogic.gdx.graphics.Color;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the VisualEffect base-class lifecycle (update, getProgress, isAlive)
 * and for each concrete timed effect (LaserBeamEffect, LightningArcEffect,
 * AoePulseEffect, ImpactEffect). Draw methods are NOT tested — they call
 * ShapeRenderer and are outside the unit-testing boundary.
 */
class VisualEffectLifecycleTest {

    // -------------------------------------------------------------------------
    // VisualEffect base class — update / getProgress / isAlive
    // -------------------------------------------------------------------------

    /**
     * Minimal concrete subclass that exposes no drawing logic, used to test
     * the base-class lifecycle without involving any concrete effect.
     */
    private static class StubEffect extends VisualEffect {
        StubEffect(float duration) {
            super(duration);
        }
    }

    @Test
    void update_freshEffect_isAliveAndProgressZero() {
        StubEffect effect = new StubEffect(1.0f);

        assertTrue(effect.isAlive(), "Newly created effect should be alive");
        assertEquals(0f, effect.getProgress(), 0.001f,
            "Progress should be 0 before any update is called");
    }

    @Test
    void update_halfwayThrough_progressIsHalf() {
        StubEffect effect = new StubEffect(1.0f);

        effect.update(0.5f);

        assertEquals(0.5f, effect.getProgress(), 0.001f,
            "After half the duration has elapsed, progress should be 0.5");
    }

    @Test
    void update_exactlyAtDuration_effectDies() {
        StubEffect effect = new StubEffect(0.2f);

        effect.update(0.2f);

        assertFalse(effect.isAlive(), "Effect should die when elapsed == duration");
    }

    @Test
    void update_pastDuration_effectDies() {
        StubEffect effect = new StubEffect(0.2f);

        effect.update(0.5f); // well past the 0.2s duration

        assertFalse(effect.isAlive(), "Effect should die when elapsed exceeds duration");
    }

    @Test
    void update_pastDuration_progressClampedToOne() {
        StubEffect effect = new StubEffect(0.2f);

        effect.update(1.0f);

        assertEquals(1.0f, effect.getProgress(), 0.001f,
            "getProgress() must be clamped to 1.0 even when elapsed >> duration");
    }

    @Test
    void update_accumulatesOverMultipleCalls() {
        StubEffect effect = new StubEffect(1.0f);

        effect.update(0.3f);
        effect.update(0.3f);

        assertEquals(0.6f, effect.getProgress(), 0.001f,
            "Progress should accumulate across multiple update() calls");
        assertTrue(effect.isAlive(), "Effect should still be alive at 60% progress");
    }

    @Test
    void update_accumulatedCallsKillEffectAtDuration() {
        StubEffect effect = new StubEffect(1.0f);

        effect.update(0.5f);
        effect.update(0.5f);

        assertFalse(effect.isAlive(), "Effect should die after accumulated delta reaches duration");
    }

    // -------------------------------------------------------------------------
    // LaserBeamEffect — duration 0.15s
    // -------------------------------------------------------------------------

    @Test
    void laserBeam_aliveBeforeDurationExpires() {
        LaserBeamEffect laser = new LaserBeamEffect(0f, 0f, 100f, 100f);

        laser.update(0.10f); // less than 0.15s

        assertTrue(laser.isAlive(), "LaserBeamEffect should still be alive at 0.10s");
    }

    @Test
    void laserBeam_diesAfterDuration() {
        LaserBeamEffect laser = new LaserBeamEffect(0f, 0f, 100f, 100f);

        laser.update(0.15f);

        assertFalse(laser.isAlive(), "LaserBeamEffect should die after its 0.15s duration");
    }

    @Test
    void laserBeam_progressAtHalfDuration() {
        LaserBeamEffect laser = new LaserBeamEffect(0f, 0f, 100f, 100f);

        laser.update(0.075f); // half of 0.15s

        assertEquals(0.5f, laser.getProgress(), 0.001f,
            "LaserBeamEffect progress at half duration should be 0.5");
    }

    // -------------------------------------------------------------------------
    // LightningArcEffect — duration 0.2s, fixed endpoints, jittered interior
    // -------------------------------------------------------------------------

    @Test
    void lightningArc_diesAfterDuration() {
        LightningArcEffect arc = new LightningArcEffect(0f, 0f, 200f, 0f, new Random(42L));

        arc.update(0.2f);

        assertFalse(arc.isAlive(), "LightningArcEffect should die after its 0.2s duration");
    }

    @Test
    void lightningArc_aliveBeforeDuration() {
        LightningArcEffect arc = new LightningArcEffect(0f, 0f, 200f, 0f, new Random(42L));

        arc.update(0.1f);

        assertTrue(arc.isAlive(), "LightningArcEffect should be alive at 0.1s (duration is 0.2s)");
    }

    @Test
    void lightningArc_progressAtHalfDuration() {
        LightningArcEffect arc = new LightningArcEffect(0f, 0f, 200f, 0f, new Random(42L));

        arc.update(0.1f); // half of 0.2s

        assertEquals(0.5f, arc.getProgress(), 0.001f,
            "LightningArcEffect progress at 0.1s should be 0.5");
    }

    @Test
    void lightningArc_constructionWithZeroLengthArc_doesNotThrow() {
        // Degenerate case: start == end. The perp vector computation divides by length — check no NaN/crash.
        assertDoesNotThrow(
            () -> new LightningArcEffect(50f, 50f, 50f, 50f, new Random(0L)),
            "LightningArcEffect constructor should not throw for zero-length arc");
    }

    // -------------------------------------------------------------------------
    // AoePulseEffect — duration 0.3s
    // -------------------------------------------------------------------------

    @Test
    void aoePulse_diesAfterDuration() {
        AoePulseEffect pulse = new AoePulseEffect(0f, 0f, 200f, new Color(Color.ORANGE));

        pulse.update(0.3f);

        assertFalse(pulse.isAlive(), "AoePulseEffect should die after its 0.3s duration");
    }

    @Test
    void aoePulse_aliveBeforeDuration() {
        AoePulseEffect pulse = new AoePulseEffect(0f, 0f, 200f, new Color(Color.ORANGE));

        pulse.update(0.15f);

        assertTrue(pulse.isAlive(), "AoePulseEffect should be alive at 0.15s (duration is 0.3s)");
    }

    @Test
    void aoePulse_progressAtHalfDuration() {
        AoePulseEffect pulse = new AoePulseEffect(0f, 0f, 200f, new Color(Color.BLUE));

        pulse.update(0.15f); // half of 0.3s

        assertEquals(0.5f, pulse.getProgress(), 0.001f,
            "AoePulseEffect progress at 0.15s should be 0.5");
    }

    // -------------------------------------------------------------------------
    // ImpactEffect — duration 0.1s
    // -------------------------------------------------------------------------

    @Test
    void impact_diesAfterDuration() {
        ImpactEffect impact = new ImpactEffect(100f, 200f);

        impact.update(0.1f);

        assertFalse(impact.isAlive(), "ImpactEffect should die after its 0.1s duration");
    }

    @Test
    void impact_aliveBeforeDuration() {
        ImpactEffect impact = new ImpactEffect(100f, 200f);

        impact.update(0.05f);

        assertTrue(impact.isAlive(), "ImpactEffect should be alive at 0.05s (duration is 0.1s)");
    }

    @Test
    void impact_progressAtHalfDuration() {
        ImpactEffect impact = new ImpactEffect(100f, 200f);

        impact.update(0.05f); // half of 0.1s

        assertEquals(0.5f, impact.getProgress(), 0.001f,
            "ImpactEffect progress at 0.05s should be 0.5");
    }
}
