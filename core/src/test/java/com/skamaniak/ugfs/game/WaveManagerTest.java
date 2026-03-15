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

class WaveManagerTest {

    private static final int TILE = GameConstants.TILE_SIZE_PX; // 64

    private Enemy zombie;
    private WaveManager.EnemyLookup enemyLookup;
    private WaveManager.PathComputer pathComputer;

    @BeforeEach
    void setUp() {
        zombie = TestAssetFactory.createEnemy(100, 1.0f, 3, false);
        enemyLookup = id -> zombie;
        // Return a trivial one-waypoint path for every spawn
        pathComputer = (pos, flying) -> Collections.singletonList(new Vector2(TILE * 8.5f, TILE * 8.5f));
    }

    // -------------------------------------------------------------------------
    // Helpers — building Level data via mocks
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
    // No waves configured
    // -------------------------------------------------------------------------

    @Test
    void update_withNullWaves_returnsEmptyList() {
        WaveManager wm = new WaveManager(null, null, enemyLookup, pathComputer);

        List<EnemyInstance> spawned = wm.update(1.0f, 0);

        assertTrue(spawned.isEmpty(), "With null waves list, update should return empty list");
    }

    @Test
    void update_withEmptyWaves_returnsEmptyList() {
        WaveManager wm = new WaveManager(Collections.emptyList(), Collections.emptyList(), enemyLookup, pathComputer);

        List<EnemyInstance> spawned = wm.update(1.0f, 0);

        assertTrue(spawned.isEmpty(), "With empty waves list, update should return empty list");
    }

    // -------------------------------------------------------------------------
    // Wave 1 delay before spawning
    // -------------------------------------------------------------------------

    @Test
    void update_beforeWaveOneDelay_spawnsNothing() {
        // Wave 1 has 5-second delay
        Level.Wave w1 = wave(1, 5.0f);
        Level.SpawnEntry se = spawnEntry(1, enemySpawn("enemy.zombie", 0));
        Level.SpawnLocation sl = spawnLocation(1, 14, se);

        WaveManager wm = new WaveManager(Collections.singletonList(w1), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        // Advance 4 seconds — should not yet start
        List<EnemyInstance> spawned = wm.update(4.0f, 0);

        assertTrue(spawned.isEmpty(), "No enemies should spawn before wave 1 delay expires");
    }

    @Test
    void update_exactlyAtWaveOneDelay_startsWave() {
        // Wave 1 has 2-second delay; single enemy with 0ms delay
        Level.Wave w1 = wave(1, 2.0f);
        Level.SpawnEntry se = spawnEntry(1, enemySpawn("enemy.zombie", 0));
        Level.SpawnLocation sl = spawnLocation(1, 14, se);

        WaveManager wm = new WaveManager(Collections.singletonList(w1), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        // Advance exactly 2 seconds
        List<EnemyInstance> spawned = wm.update(2.0f, 0);

        assertFalse(spawned.isEmpty(), "Enemy should spawn when wave delay is exactly reached");
    }

    // -------------------------------------------------------------------------
    // Individual enemy delay within a wave
    // -------------------------------------------------------------------------

    @Test
    void update_firstEnemySpawnsImmediatelyWhenDelayIsZero() {
        Level.Wave w1 = wave(1, 0.0f);
        Level.SpawnEntry se = spawnEntry(1, enemySpawn("enemy.zombie", 0));
        Level.SpawnLocation sl = spawnLocation(1, 14, se);

        WaveManager wm = new WaveManager(Collections.singletonList(w1), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        List<EnemyInstance> spawned = wm.update(0.001f, 0);

        assertEquals(1, spawned.size(), "Single enemy with 0ms delay should spawn on first update");
    }

    @Test
    void update_secondEnemyNotYetReadyBeforeDelay() {
        // Two enemies, 1000ms (1 second) apart
        Level.Wave w1 = wave(1, 0.0f);
        Level.SpawnEntry se = spawnEntry(1,
            enemySpawn("enemy.zombie", 0),
            enemySpawn("enemy.zombie", 1000)
        );
        Level.SpawnLocation sl = spawnLocation(1, 14, se);

        WaveManager wm = new WaveManager(Collections.singletonList(w1), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        // First update at 0.5s — first enemy spawns (delay 0), second (delay 1000ms) not yet
        List<EnemyInstance> firstUpdate = wm.update(0.5f, 0);
        assertEquals(1, firstUpdate.size(), "Only first enemy (0ms delay) should spawn in first 0.5s");

        // Second update at another 0.4s (total 0.9s) — still not enough for second enemy
        List<EnemyInstance> secondUpdate = wm.update(0.4f, 1);
        assertEquals(0, secondUpdate.size(), "Second enemy (1000ms delay) should not spawn at 0.9s total");
    }

    @Test
    void update_secondEnemySpawnsAfterDelay() {
        Level.Wave w1 = wave(1, 0.0f);
        Level.SpawnEntry se = spawnEntry(1,
            enemySpawn("enemy.zombie", 0),
            enemySpawn("enemy.zombie", 1000)
        );
        Level.SpawnLocation sl = spawnLocation(1, 14, se);

        WaveManager wm = new WaveManager(Collections.singletonList(w1), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        // Consume first enemy spawn (delay 0)
        wm.update(0.001f, 0);

        // Advance 1.0 second more — second enemy should now spawn
        List<EnemyInstance> spawned = wm.update(1.0f, 1);
        assertEquals(1, spawned.size(), "Second enemy should spawn 1000ms after first");
    }

    // -------------------------------------------------------------------------
    // Multiple spawn locations fire simultaneously
    // -------------------------------------------------------------------------

    @Test
    void update_multipleSpawnLocations_bothSpawnOnWaveStart() {
        // Wave 1 with 0s delay, two spawn locations each with one enemy (0ms delay)
        Level.Wave w1 = wave(1, 0.0f);
        Level.SpawnEntry se1 = spawnEntry(1, enemySpawn("enemy.zombie", 0));
        Level.SpawnEntry se2 = spawnEntry(1, enemySpawn("enemy.zombie", 0));
        Level.SpawnLocation sl1 = spawnLocation(1, 14, se1);
        Level.SpawnLocation sl2 = spawnLocation(14, 14, se2);

        WaveManager wm = new WaveManager(Collections.singletonList(w1), Arrays.asList(sl1, sl2),
            enemyLookup, pathComputer);

        List<EnemyInstance> spawned = wm.update(0.001f, 0);

        assertEquals(2, spawned.size(), "Both spawn locations should produce an enemy simultaneously on wave start");
    }

    // -------------------------------------------------------------------------
    // Wave transition
    // -------------------------------------------------------------------------

    @Test
    void update_waveEnds_whenAllSpawnersExhaustedAndAliveCountZero() {
        // Wave 1 with 0s delay, one enemy. After spawning and alive=0, wave should end.
        Level.Wave w1 = wave(1, 0.0f);
        Level.Wave w2 = wave(2, 3.0f);
        Level.SpawnEntry se = spawnEntry(1, enemySpawn("enemy.zombie", 0));
        Level.SpawnLocation sl = spawnLocation(1, 14, se);

        WaveManager wm = new WaveManager(Arrays.asList(w1, w2), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        // Spawn the wave-1 enemy
        wm.update(0.001f, 0);

        // Wave-1 spawner is exhausted; alive count is now 0 → wave should end
        List<EnemyInstance> spawned = wm.update(0.001f, 0);

        // After wave ends, the wave-2 delay timer should start; no enemies yet
        assertTrue(spawned.isEmpty(), "No enemies should spawn immediately after wave ends");
    }

    @Test
    void update_wave2StartsAfterItsDelay_followingWave1End() {
        Level.Wave w1 = wave(1, 0.0f);
        Level.Wave w2 = wave(2, 2.0f);

        Level.SpawnEntry se1 = spawnEntry(1, enemySpawn("enemy.zombie", 0));
        Level.SpawnEntry se2 = spawnEntry(2, enemySpawn("enemy.zombie", 0));
        Level.SpawnLocation sl = spawnLocation(1, 14, se1, se2);

        WaveManager wm = new WaveManager(Arrays.asList(w1, w2), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        // Trigger wave 1 start and immediate spawn
        wm.update(0.001f, 0);

        // Let wave 1 end (spawners exhausted, aliveCount=0)
        wm.update(0.001f, 0);

        // Advance 1 second — wave 2 delay is 2 seconds, should not start yet
        List<EnemyInstance> beforeDelay = wm.update(1.0f, 0);
        assertTrue(beforeDelay.isEmpty(), "Wave 2 should not start before its 2-second delay");

        // Advance another 1.1 seconds — now wave 2 should start
        List<EnemyInstance> afterDelay = wm.update(1.1f, 0);
        assertFalse(afterDelay.isEmpty(), "Wave 2 should start after its delay expires");
    }

    @Test
    void update_afterFinalWave_returnsEmptyListForever() {
        Level.Wave w1 = wave(1, 0.0f);
        Level.SpawnEntry se = spawnEntry(1, enemySpawn("enemy.zombie", 0));
        Level.SpawnLocation sl = spawnLocation(1, 14, se);

        WaveManager wm = new WaveManager(Collections.singletonList(w1), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        // Spawn wave 1
        wm.update(0.001f, 0);
        // End wave 1 (aliveCount=0, spawners exhausted)
        wm.update(0.001f, 0);

        // Many ticks after final wave ends — no more waves, should always return empty
        for (int i = 0; i < 5; i++) {
            List<EnemyInstance> result = wm.update(10.0f, 0);
            assertTrue(result.isEmpty(), "No more enemies should spawn after all waves are done");
        }
    }

    // -------------------------------------------------------------------------
    // Wave does not transition while enemies are still alive
    // -------------------------------------------------------------------------

    @Test
    void update_waveDoesNotEndWhileEnemiesAreAlive() {
        Level.Wave w1 = wave(1, 0.0f);
        Level.Wave w2 = wave(2, 0.0f); // 0-second delay so it starts immediately if wave 1 ends
        Level.SpawnEntry se1 = spawnEntry(1, enemySpawn("enemy.zombie", 0));
        Level.SpawnEntry se2 = spawnEntry(2, enemySpawn("enemy.zombie", 0));
        Level.SpawnLocation sl = spawnLocation(1, 14, se1, se2);

        WaveManager wm = new WaveManager(Arrays.asList(w1, w2), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        // Spawn wave-1 enemy
        wm.update(0.001f, 0);

        // Spawner is exhausted but aliveCount > 0 — wave should NOT end
        List<EnemyInstance> result = wm.update(1.0f, 1);
        assertTrue(result.isEmpty(), "Wave 2 should not start while wave 1 enemies are still alive");
    }

    // -------------------------------------------------------------------------
    // Spawn entries that don't match current wave number are ignored
    // -------------------------------------------------------------------------

    @Test
    void update_spawnEntriesForOtherWavesAreIgnored() {
        // Spawn location has entries for both wave 1 and wave 2;
        // only wave-1 entries should fire on wave 1 start.
        Level.Wave w1 = wave(1, 0.0f);
        Level.SpawnEntry se1 = spawnEntry(1, enemySpawn("enemy.zombie", 0));
        Level.SpawnEntry se2 = spawnEntry(2,
            enemySpawn("enemy.zombie", 0),
            enemySpawn("enemy.zombie", 0),
            enemySpawn("enemy.zombie", 0)
        );
        Level.SpawnLocation sl = spawnLocation(1, 14, se1, se2);

        WaveManager wm = new WaveManager(Collections.singletonList(w1), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        // Only wave 1 starts — wave 2 entry should not contribute enemies
        List<EnemyInstance> spawned = wm.update(0.001f, 0);

        assertEquals(1, spawned.size(), "Only wave-1 spawn entries should fire during wave 1");
    }

    // -------------------------------------------------------------------------
    // EnemyLookup and PathComputer integration
    // -------------------------------------------------------------------------

    @Test
    void update_callsEnemyLookupWithCorrectId() {
        WaveManager.EnemyLookup spyLookup = mock(WaveManager.EnemyLookup.class);
        when(spyLookup.getEnemy(anyString())).thenReturn(zombie);

        Level.Wave w1 = wave(1, 0.0f);
        Level.SpawnEntry se = spawnEntry(1, enemySpawn("enemy.zombie", 0));
        Level.SpawnLocation sl = spawnLocation(1, 14, se);

        WaveManager wm = new WaveManager(Collections.singletonList(w1), Collections.singletonList(sl),
            spyLookup, pathComputer);

        wm.update(0.001f, 0);

        verify(spyLookup).getEnemy("enemy.zombie");
    }

    @Test
    void update_callsPathComputerWithFlyingFlag() {
        Enemy flyingEnemy = TestAssetFactory.createEnemy(50, 2.0f, 5, true);
        WaveManager.EnemyLookup flyingLookup = id -> flyingEnemy;
        WaveManager.PathComputer spyPath = mock(WaveManager.PathComputer.class);
        when(spyPath.computePath(any(), anyBoolean()))
            .thenReturn(Collections.singletonList(new Vector2(100, 100)));

        Level.Wave w1 = wave(1, 0.0f);
        Level.SpawnEntry se = spawnEntry(1, enemySpawn("enemy.bat", 0));
        Level.SpawnLocation sl = spawnLocation(1, 14, se);

        WaveManager wm = new WaveManager(Collections.singletonList(w1), Collections.singletonList(sl),
            flyingLookup, spyPath);

        wm.update(0.001f, 0);

        verify(spyPath).computePath(any(Vector2.class), eq(true));
    }

    @Test
    void update_spawnedInstanceIsAlive() {
        Level.Wave w1 = wave(1, 0.0f);
        Level.SpawnEntry se = spawnEntry(1, enemySpawn("enemy.zombie", 0));
        Level.SpawnLocation sl = spawnLocation(1, 14, se);

        WaveManager wm = new WaveManager(Collections.singletonList(w1), Collections.singletonList(sl),
            enemyLookup, pathComputer);

        List<EnemyInstance> spawned = wm.update(0.001f, 0);

        assertTrue(spawned.get(0).isAlive(), "Freshly spawned EnemyInstance should be alive");
    }
}
