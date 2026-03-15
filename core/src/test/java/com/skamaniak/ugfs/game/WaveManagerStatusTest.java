package com.skamaniak.ugfs.game;

import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.GameConstants;
import com.skamaniak.ugfs.TestAssetFactory;
import com.skamaniak.ugfs.asset.model.Enemy;
import com.skamaniak.ugfs.asset.model.Level;
import com.skamaniak.ugfs.game.entity.EnemyInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for WaveStatus population across the wave lifecycle.
 * Covers: waveActive flag, currentWaveNumber, totalWaves, countdown,
 * allWavesExhausted, and pendingSpawnCount.
 */
class WaveManagerStatusTest {

    private static final int TILE = GameConstants.TILE_SIZE_PX;

    private Enemy zombie;
    private WaveManager.EnemyLookup enemyLookup;
    private WaveManager.PathComputer pathComputer;

    @BeforeEach
    void setUp() {
        zombie = TestAssetFactory.createEnemy(100, 1.0f, 3, false);
        enemyLookup = id -> zombie;
        pathComputer = (pos, flying) -> Collections.singletonList(new Vector2(TILE * 8.5f, TILE * 8.5f));
    }

    // -------------------------------------------------------------------------
    // Helpers — building Level data via mocks (mirrors WaveManagerTest pattern)
    // -------------------------------------------------------------------------

    private static Level.Wave wave(int waveNum, float delay) {
        Level.Wave w = mock(Level.Wave.class);
        when(w.getWave()).thenReturn(waveNum);
        when(w.getDelay()).thenReturn(delay);
        return w;
    }

    private static Level.EnemySpawn enemySpawn(String id, int delayMs) {
        Level.EnemySpawn es = mock(Level.EnemySpawn.class);
        when(es.getEnemyId()).thenReturn(id);
        when(es.getDelay()).thenReturn(delayMs);
        return es;
    }

    private static Level.SpawnEntry spawnEntry(int waveNum, Level.EnemySpawn... spawns) {
        Level.SpawnEntry se = mock(Level.SpawnEntry.class);
        when(se.getWave()).thenReturn(waveNum);
        when(se.getEnemies()).thenReturn(Arrays.asList(spawns));
        return se;
    }

    private static Level.SpawnLocation spawnLocation(int x, int y, Level.SpawnEntry... entries) {
        Level.SpawnLocation sl = mock(Level.SpawnLocation.class);
        when(sl.getX()).thenReturn(x);
        when(sl.getY()).thenReturn(y);
        when(sl.getSpawnPlan()).thenReturn(Arrays.asList(entries));
        return sl;
    }

    // -------------------------------------------------------------------------
    // Initial state — build phase (before wave 1 countdown expires)
    // -------------------------------------------------------------------------

    @Test
    void getWaveStatus_initialBuildPhase_waveActiveIsFalse() {
        Level.Wave w1 = wave(1, 10.0f);
        Level.SpawnEntry se = spawnEntry(1, enemySpawn("zombie", 0));
        Level.SpawnLocation sl = spawnLocation(1, 14, se);
        WaveManager wm = new WaveManager(Collections.singletonList(w1), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        // Advance a small amount — wave has not started
        wm.update(1.0f, 0);

        assertFalse(wm.getWaveStatus().isWaveActive(),
            "waveActive should be false during initial build-phase countdown");
    }

    @Test
    void getWaveStatus_initialBuildPhase_currentWaveNumberIsZero() {
        Level.Wave w1 = wave(1, 10.0f);
        Level.SpawnEntry se = spawnEntry(1, enemySpawn("zombie", 0));
        Level.SpawnLocation sl = spawnLocation(1, 14, se);
        WaveManager wm = new WaveManager(Collections.singletonList(w1), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        wm.update(1.0f, 0);

        assertEquals(0, wm.getWaveStatus().getCurrentWaveNumber(),
            "currentWaveNumber should be 0 before any wave has started");
    }

    @Test
    void getWaveStatus_initialBuildPhase_countdownDecreasesWithDelta() {
        Level.Wave w1 = wave(1, 10.0f);
        Level.SpawnEntry se = spawnEntry(1, enemySpawn("zombie", 0));
        Level.SpawnLocation sl = spawnLocation(1, 14, se);
        WaveManager wm = new WaveManager(Collections.singletonList(w1), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        wm.update(3.0f, 0);

        assertEquals(7.0f, wm.getWaveStatus().getCountdown(), 0.001f,
            "countdown should be initial delay minus elapsed time");
    }

    @Test
    void getWaveStatus_initialBuildPhase_totalWavesReflectsWaveListSize() {
        Level.Wave w1 = wave(1, 10.0f);
        Level.Wave w2 = wave(2, 5.0f);
        WaveManager wm = new WaveManager(Arrays.asList(w1, w2), Collections.emptyList(),
            enemyLookup, pathComputer);

        wm.update(0.1f, 0);

        assertEquals(2, wm.getWaveStatus().getTotalWaves(),
            "totalWaves should equal the number of configured waves");
    }

    @Test
    void getWaveStatus_initialBuildPhase_allWavesExhaustedIsFalse() {
        Level.Wave w1 = wave(1, 10.0f);
        Level.SpawnEntry se = spawnEntry(1, enemySpawn("zombie", 0));
        Level.SpawnLocation sl = spawnLocation(1, 14, se);
        WaveManager wm = new WaveManager(Collections.singletonList(w1), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        wm.update(1.0f, 0);

        assertFalse(wm.getWaveStatus().isAllWavesExhausted(),
            "allWavesExhausted should be false when wave 1 has not yet started");
    }

    // -------------------------------------------------------------------------
    // Wave 1 becomes active
    // -------------------------------------------------------------------------

    @Test
    void getWaveStatus_onceWaveStarts_waveActiveIsTrue() {
        Level.Wave w1 = wave(1, 0.0f);
        Level.SpawnEntry se = spawnEntry(1, enemySpawn("zombie", 0));
        Level.SpawnLocation sl = spawnLocation(1, 14, se);
        WaveManager wm = new WaveManager(Collections.singletonList(w1), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        wm.update(0.001f, 0);

        assertTrue(wm.getWaveStatus().isWaveActive(),
            "waveActive should be true once a wave has started");
    }

    @Test
    void getWaveStatus_onceWaveStarts_currentWaveNumberIsOne() {
        Level.Wave w1 = wave(1, 0.0f);
        Level.SpawnEntry se = spawnEntry(1, enemySpawn("zombie", 0));
        Level.SpawnLocation sl = spawnLocation(1, 14, se);
        WaveManager wm = new WaveManager(Collections.singletonList(w1), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        wm.update(0.001f, 0);

        assertEquals(1, wm.getWaveStatus().getCurrentWaveNumber(),
            "currentWaveNumber should be 1 after wave 1 has started");
    }

    // -------------------------------------------------------------------------
    // pendingSpawnCount during an active wave
    // -------------------------------------------------------------------------

    @Test
    void getWaveStatus_midWave_pendingSpawnCountEqualsRemainingEnemies() {
        // Two enemies in wave 1: first at 0ms, second at very large delay (won't spawn in test)
        Level.Wave w1 = wave(1, 0.0f);
        Level.SpawnEntry se = spawnEntry(1,
            enemySpawn("zombie", 0),
            enemySpawn("zombie", 60_000)   // 60 seconds — far future
        );
        Level.SpawnLocation sl = spawnLocation(1, 14, se);
        WaveManager wm = new WaveManager(Collections.singletonList(w1), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        // Tick enough to spawn only the first enemy (delay 0)
        wm.update(0.001f, 0);

        // SpawnTimer still has 1 remaining (the 60s enemy)
        assertEquals(1, wm.getWaveStatus().getPendingSpawnCount(),
            "pendingSpawnCount should be 1 when one of two enemies is yet to spawn");
    }

    @Test
    void getWaveStatus_afterAllEnemiesSpawned_pendingSpawnCountIsZero() {
        // Single enemy with delay 0; after spawning pendingSpawnCount should drop to 0
        Level.Wave w1 = wave(1, 0.0f);
        Level.SpawnEntry se = spawnEntry(1, enemySpawn("zombie", 0));
        Level.SpawnLocation sl = spawnLocation(1, 14, se);
        WaveManager wm = new WaveManager(Collections.singletonList(w1), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        wm.update(0.001f, 0);

        assertEquals(0, wm.getWaveStatus().getPendingSpawnCount(),
            "pendingSpawnCount should be 0 after the only enemy has spawned");
    }

    @Test
    void getWaveStatus_pendingSpawnCount_decreasesAsEnemiesSpawn() {
        // Three enemies: first at 0ms, second at 500ms, third at 1000ms
        Level.Wave w1 = wave(1, 0.0f);
        Level.SpawnEntry se = spawnEntry(1,
            enemySpawn("zombie", 0),
            enemySpawn("zombie", 500),
            enemySpawn("zombie", 500)
        );
        Level.SpawnLocation sl = spawnLocation(1, 14, se);
        WaveManager wm = new WaveManager(Collections.singletonList(w1), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        // After very small tick: first enemy spawns, two remain
        wm.update(0.001f, 0);
        int afterFirst = wm.getWaveStatus().getPendingSpawnCount();

        // After 0.6s: second enemy spawns (accumulated 500ms), one remains
        wm.update(0.6f, 1);
        int afterSecond = wm.getWaveStatus().getPendingSpawnCount();

        assertAll(
            () -> assertEquals(2, afterFirst, "pendingSpawnCount should be 2 after first enemy spawns"),
            () -> assertEquals(1, afterSecond, "pendingSpawnCount should be 1 after second enemy spawns")
        );
    }

    @Test
    void getWaveStatus_pendingSpawnCount_isZeroBetweenWaves() {
        // Wave 1 with one immediate enemy; after wave ends no more pending
        Level.Wave w1 = wave(1, 0.0f);
        Level.Wave w2 = wave(2, 10.0f);
        Level.SpawnEntry se1 = spawnEntry(1, enemySpawn("zombie", 0));
        Level.SpawnLocation sl = spawnLocation(1, 14, se1);
        WaveManager wm = new WaveManager(Arrays.asList(w1, w2), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        wm.update(0.001f, 0); // spawn wave-1 enemy
        wm.update(0.001f, 0); // wave ends (spawners exhausted, aliveCount=0)

        assertEquals(0, wm.getWaveStatus().getPendingSpawnCount(),
            "pendingSpawnCount should be 0 during the inter-wave countdown");
    }

    // -------------------------------------------------------------------------
    // allWavesExhausted flag
    // -------------------------------------------------------------------------

    @Test
    void getWaveStatus_allWavesExhausted_isFalseWhileWaveIsActive() {
        Level.Wave w1 = wave(1, 0.0f);
        Level.SpawnEntry se = spawnEntry(1, enemySpawn("zombie", 0));
        Level.SpawnLocation sl = spawnLocation(1, 14, se);
        WaveManager wm = new WaveManager(Collections.singletonList(w1), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        wm.update(0.001f, 0); // wave 1 starts, one enemy spawned

        // Wave is still active (spawned enemy still alive in waveManager perspective: we report aliveCount=1)
        // We need to check that allWavesExhausted is false while waveActive is true
        assertTrue(wm.getWaveStatus().isWaveActive(),
            "Precondition: wave should still be active");
        assertFalse(wm.getWaveStatus().isAllWavesExhausted(),
            "allWavesExhausted should be false while the wave is still active");
    }

    @Test
    void getWaveStatus_allWavesExhausted_becomesTrueAfterFinalWaveEnds() {
        // Single-wave setup; wave ends when spawners exhausted and alive=0
        Level.Wave w1 = wave(1, 0.0f);
        Level.SpawnEntry se = spawnEntry(1, enemySpawn("zombie", 0));
        Level.SpawnLocation sl = spawnLocation(1, 14, se);
        WaveManager wm = new WaveManager(Collections.singletonList(w1), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        wm.update(0.001f, 0); // spawn the enemy (spawner becomes exhausted)
        wm.update(0.001f, 0); // wave ends: spawners empty AND aliveCount=0

        assertTrue(wm.getWaveStatus().isAllWavesExhausted(),
            "allWavesExhausted should be true after the only wave ends with no alive enemies");
    }

    @Test
    void getWaveStatus_allWavesExhausted_remainsFalseWhileWave2StillPending() {
        // Two waves; after wave 1 ends, wave 2 is in countdown — not yet exhausted
        Level.Wave w1 = wave(1, 0.0f);
        Level.Wave w2 = wave(2, 10.0f);
        Level.SpawnEntry se1 = spawnEntry(1, enemySpawn("zombie", 0));
        Level.SpawnLocation sl = spawnLocation(1, 14, se1);
        WaveManager wm = new WaveManager(Arrays.asList(w1, w2), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        wm.update(0.001f, 0); // wave 1 starts and enemy spawns
        wm.update(0.001f, 0); // wave 1 ends; wave 2 countdown begins

        assertFalse(wm.getWaveStatus().isAllWavesExhausted(),
            "allWavesExhausted should be false when wave 2 is still pending");
    }

    @Test
    void getWaveStatus_allWavesExhausted_becomesTrueAfterLastOfTwoWavesEnds() {
        Level.Wave w1 = wave(1, 0.0f);
        Level.Wave w2 = wave(2, 0.0f);
        Level.SpawnEntry se1 = spawnEntry(1, enemySpawn("zombie", 0));
        Level.SpawnEntry se2 = spawnEntry(2, enemySpawn("zombie", 0));
        Level.SpawnLocation sl = spawnLocation(1, 14, se1, se2);
        WaveManager wm = new WaveManager(Arrays.asList(w1, w2), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        wm.update(0.001f, 0); // wave 1 starts, enemy spawns
        wm.update(0.001f, 0); // wave 1 ends, wave 2 countdown (0s) starts
        wm.update(0.001f, 0); // wave 2 starts, enemy spawns
        wm.update(0.001f, 0); // wave 2 ends (spawners empty, aliveCount=0)

        assertTrue(wm.getWaveStatus().isAllWavesExhausted(),
            "allWavesExhausted should be true after both waves have ended");
    }

    @Test
    void getWaveStatus_allWavesExhausted_withNullWaves_isTrueImmediately() {
        WaveManager wm = new WaveManager(null, null, enemyLookup, pathComputer);

        wm.update(0.001f, 0);

        assertTrue(wm.getWaveStatus().isAllWavesExhausted(),
            "allWavesExhausted should be true immediately when no waves are configured");
    }

    // -------------------------------------------------------------------------
    // WaveStatus reuse — same instance returned each call
    // -------------------------------------------------------------------------

    @Test
    void getWaveStatus_returnsSameInstanceEachCall() {
        Level.Wave w1 = wave(1, 5.0f);
        WaveManager wm = new WaveManager(Collections.singletonList(w1), Collections.emptyList(),
            enemyLookup, pathComputer);

        WaveManager.WaveStatus first = wm.getWaveStatus();
        wm.update(1.0f, 0);
        WaveManager.WaveStatus second = wm.getWaveStatus();

        assertSame(first, second,
            "getWaveStatus() must return the same mutable instance, not a new object each call");
    }

    // -------------------------------------------------------------------------
    // waveActive transitions back to false after wave ends
    // -------------------------------------------------------------------------

    @Test
    void getWaveStatus_waveActive_returnsFalseAfterWaveEnds() {
        Level.Wave w1 = wave(1, 0.0f);
        Level.Wave w2 = wave(2, 30.0f);
        Level.SpawnEntry se = spawnEntry(1, enemySpawn("zombie", 0));
        Level.SpawnLocation sl = spawnLocation(1, 14, se);
        WaveManager wm = new WaveManager(Arrays.asList(w1, w2), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        wm.update(0.001f, 0); // wave 1 starts, enemy spawns
        wm.update(0.001f, 0); // wave 1 ends (alive=0)

        assertFalse(wm.getWaveStatus().isWaveActive(),
            "waveActive should return to false after the wave ends and all enemies are dead");
    }

    // -------------------------------------------------------------------------
    // pendingSpawnCount with multiple spawn locations
    // -------------------------------------------------------------------------

    @Test
    void getWaveStatus_pendingSpawnCount_summedAcrossAllActiveSpawnTimers() {
        // Two spawn locations: each has one future enemy in wave 1 (beyond test time)
        Level.Wave w1 = wave(1, 0.0f);
        Level.SpawnEntry se1 = spawnEntry(1, enemySpawn("zombie", 60_000));
        Level.SpawnEntry se2 = spawnEntry(1, enemySpawn("zombie", 60_000));
        Level.SpawnLocation sl1 = spawnLocation(1, 14, se1);
        Level.SpawnLocation sl2 = spawnLocation(14, 14, se2);
        WaveManager wm = new WaveManager(Collections.singletonList(w1),
            Arrays.asList(sl1, sl2), enemyLookup, pathComputer);

        wm.update(0.001f, 0); // wave starts; both timers created but no enemy spawns yet

        assertEquals(2, wm.getWaveStatus().getPendingSpawnCount(),
            "pendingSpawnCount should sum remaining counts across all active spawn timers");
    }
}
