package com.skamaniak.ugfs.game;

import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.GameConstants;
import com.skamaniak.ugfs.asset.model.Enemy;
import com.skamaniak.ugfs.asset.model.Level;
import com.skamaniak.ugfs.game.entity.EnemyInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WaveManager {
    private final List<Level.Wave> waves;
    private final List<Level.SpawnLocation> spawnLocations;
    private final EnemyLookup enemyLookup;
    private final PathComputer pathComputer;

    private int currentWaveNumber;
    private float waveTimer;
    private boolean waveActive;
    private final List<SpawnTimer> activeSpawnTimers = new ArrayList<>();

    public interface EnemyLookup {
        Enemy getEnemy(String id);
    }

    public interface PathComputer {
        List<Vector2> computePath(Vector2 spawnWorldPos, boolean flying);
    }

    public WaveManager(List<Level.Wave> waves, List<Level.SpawnLocation> spawnLocations,
                       EnemyLookup enemyLookup, PathComputer pathComputer) {
        this.waves = waves;
        this.spawnLocations = spawnLocations;
        this.enemyLookup = enemyLookup;
        this.pathComputer = pathComputer;
        this.currentWaveNumber = 0;
        this.waveActive = false;

        if (waves != null && !waves.isEmpty()) {
            this.waveTimer = waves.get(0).getDelay();
        }
    }

    public List<EnemyInstance> update(float delta, int aliveEnemyCount) {
        if (waves == null || waves.isEmpty()) {
            return Collections.emptyList();
        }

        List<EnemyInstance> spawned = new ArrayList<>();

        if (!waveActive) {
            if (currentWaveNumber >= waves.size()) {
                return Collections.emptyList();
            }

            waveTimer -= delta;
            if (waveTimer <= 0) {
                startNextWave();
            } else {
                return Collections.emptyList();
            }
        }

        for (int i = activeSpawnTimers.size() - 1; i >= 0; i--) {
            SpawnTimer timer = activeSpawnTimers.get(i);
            List<EnemyInstance> timerSpawned = timer.update(delta);
            spawned.addAll(timerSpawned);
            if (timer.isExhausted()) {
                activeSpawnTimers.remove(i);
            }
        }

        if (activeSpawnTimers.isEmpty() && aliveEnemyCount == 0 && spawned.isEmpty()) {
            waveActive = false;
            if (currentWaveNumber < waves.size()) {
                waveTimer = waves.get(currentWaveNumber).getDelay();
            }
        }

        return spawned;
    }

    private void startNextWave() {
        Level.Wave wave = waves.get(currentWaveNumber);
        int waveNumber = wave.getWave();
        currentWaveNumber++;
        waveActive = true;

        if (spawnLocations == null) {
            return;
        }

        for (Level.SpawnLocation location : spawnLocations) {
            if (location.getSpawnPlan() == null) {
                continue;
            }
            float spawnWorldX = (location.getX() + 0.5f) * GameConstants.TILE_SIZE_PX;
            float spawnWorldY = (location.getY() + 0.5f) * GameConstants.TILE_SIZE_PX;
            Vector2 spawnPos = new Vector2(spawnWorldX, spawnWorldY);

            for (Level.SpawnEntry entry : location.getSpawnPlan()) {
                if (entry.getWave() == waveNumber) {
                    activeSpawnTimers.add(new SpawnTimer(entry.getEnemies(), spawnPos));
                }
            }
        }
    }

    private class SpawnTimer {
        private final List<Level.EnemySpawn> enemies;
        private final Vector2 spawnWorldPos;
        private int nextIndex;
        private float delayAccumulator;

        SpawnTimer(List<Level.EnemySpawn> enemies, Vector2 spawnWorldPos) {
            this.enemies = enemies;
            this.spawnWorldPos = spawnWorldPos;
            this.nextIndex = 0;
            this.delayAccumulator = 0;
        }

        List<EnemyInstance> update(float delta) {
            if (isExhausted()) {
                return Collections.emptyList();
            }

            List<EnemyInstance> spawned = new ArrayList<>();
            delayAccumulator += delta * 1000f;

            while (nextIndex < enemies.size()) {
                Level.EnemySpawn enemySpawn = enemies.get(nextIndex);
                float requiredDelay = enemySpawn.getDelay();

                if (delayAccumulator >= requiredDelay) {
                    delayAccumulator -= requiredDelay;
                    Enemy enemy = enemyLookup.getEnemy(enemySpawn.getEnemyId());
                    List<Vector2> path = pathComputer.computePath(spawnWorldPos, enemy.isFlying());
                    EnemyInstance instance = new EnemyInstance(enemy, spawnWorldPos, path);
                    spawned.add(instance);
                    nextIndex++;
                } else {
                    break;
                }
            }

            return spawned;
        }

        boolean isExhausted() {
            return nextIndex >= enemies.size();
        }
    }
}
