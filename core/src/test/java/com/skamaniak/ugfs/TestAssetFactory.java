package com.skamaniak.ugfs;

import com.skamaniak.ugfs.asset.model.Conduit;
import com.skamaniak.ugfs.asset.model.Enemy;
import com.skamaniak.ugfs.asset.model.Generator;
import com.skamaniak.ugfs.asset.model.PowerStorage;
import com.skamaniak.ugfs.asset.model.Tower;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestAssetFactory {

    public static Generator createGenerator(int capacity, int generationRate) {
        return createGenerator(capacity, generationRate, 0);
    }

    public static Generator createGenerator(int capacity, int generationRate, int scrapCost) {
        Generator.Level level = mock(Generator.Level.class);
        when(level.getPowerStorage()).thenReturn(capacity);
        when(level.getPowerGenerationRate()).thenReturn(generationRate);
        when(level.getScrapCost()).thenReturn(scrapCost);

        Generator generator = mock(Generator.class);
        when(generator.getLevels()).thenReturn(Collections.singletonList(level));
        return generator;
    }

    /**
     * Creates a multi-level Generator mock. Each int[] in levelStats is
     * {capacity, generationRate, scrapCost} for that level.
     */
    public static Generator createMultiLevelGenerator(int[]... levelStats) {
        List<Generator.Level> levels = new ArrayList<>();
        for (int[] stats : levelStats) {
            Generator.Level lvl = mock(Generator.Level.class);
            when(lvl.getPowerStorage()).thenReturn(stats[0]);
            when(lvl.getPowerGenerationRate()).thenReturn(stats[1]);
            when(lvl.getScrapCost()).thenReturn(stats[2]);
            levels.add(lvl);
        }
        Generator generator = mock(Generator.class);
        when(generator.getLevels()).thenReturn(levels);
        return generator;
    }

    public static PowerStorage createPowerStorage(int capacity, int standbyLoss) {
        return createPowerStorage(capacity, standbyLoss, 0);
    }

    public static PowerStorage createPowerStorage(int capacity, int standbyLoss, int scrapCost) {
        PowerStorage.Level level = mock(PowerStorage.Level.class);
        when(level.getPowerStorage()).thenReturn(capacity);
        when(level.getPowerLossStandby()).thenReturn(standbyLoss);
        when(level.getScrapCost()).thenReturn(scrapCost);

        PowerStorage storage = mock(PowerStorage.class);
        when(storage.getLevels()).thenReturn(Collections.singletonList(level));
        return storage;
    }

    /**
     * Creates a multi-level PowerStorage mock. Each int[] in levelStats is
     * {capacity, standbyLoss, scrapCost} for that level.
     */
    public static PowerStorage createMultiLevelPowerStorage(int[]... levelStats) {
        List<PowerStorage.Level> levels = new ArrayList<>();
        for (int[] stats : levelStats) {
            PowerStorage.Level lvl = mock(PowerStorage.Level.class);
            when(lvl.getPowerStorage()).thenReturn(stats[0]);
            when(lvl.getPowerLossStandby()).thenReturn(stats[1]);
            when(lvl.getScrapCost()).thenReturn(stats[2]);
            levels.add(lvl);
        }
        PowerStorage storage = mock(PowerStorage.class);
        when(storage.getLevels()).thenReturn(levels);
        return storage;
    }

    public static Tower createTower(int capacity, int standbyLoss, int shotCost, float fireRate) {
        return createTower(capacity, standbyLoss, shotCost, fireRate, 0);
    }

    public static Tower createTower(int capacity, int standbyLoss, int shotCost, float fireRate, int scrapCost) {
        Tower.Level level = mock(Tower.Level.class);
        when(level.getPowerStorage()).thenReturn(capacity);
        when(level.getPowerLossStandby()).thenReturn(standbyLoss);
        when(level.getPowerCostShot()).thenReturn(shotCost);
        when(level.getFireRate()).thenReturn(fireRate);
        when(level.getScrapCost()).thenReturn(scrapCost);

        Tower tower = mock(Tower.class);
        when(tower.getLevels()).thenReturn(Collections.singletonList(level));
        return tower;
    }

    /**
     * Creates a multi-level Tower mock. Each entry is {capacity, standbyLoss, shotCost, scrapCost}
     * (fireRate defaults to 1.0 for simplicity). Use the overload below for custom fireRate.
     */
    public static Tower createMultiLevelTower(int[]... levelStats) {
        List<Tower.Level> levels = new ArrayList<>();
        for (int[] stats : levelStats) {
            Tower.Level lvl = mock(Tower.Level.class);
            when(lvl.getPowerStorage()).thenReturn(stats[0]);
            when(lvl.getPowerLossStandby()).thenReturn(stats[1]);
            when(lvl.getPowerCostShot()).thenReturn(stats[2]);
            when(lvl.getFireRate()).thenReturn(1.0f);
            when(lvl.getScrapCost()).thenReturn(stats[3]);
            levels.add(lvl);
        }
        Tower tower = mock(Tower.class);
        when(tower.getLevels()).thenReturn(levels);
        return tower;
    }

    /**
     * Creates an Enemy mock with the given stats.
     */
    public static Enemy createEnemy(int health, float speed, int scrap, boolean flying) {
        Enemy enemy = mock(Enemy.class);
        when(enemy.getHealth()).thenReturn(health);
        when(enemy.getSpeed()).thenReturn(speed);
        when(enemy.getScrap()).thenReturn(scrap);
        when(enemy.isFlying()).thenReturn(flying);
        return enemy;
    }

    /**
     * Creates a Tower mock that also stubs towerRange and damage on its single level.
     * Defaults to targeting "single" and deferDamage false.
     */
    public static Tower createTowerWithRange(int capacity, int standbyLoss, int shotCost, float fireRate,
                                             float towerRange, int damage) {
        return createTowerWithRange(capacity, standbyLoss, shotCost, fireRate, towerRange, damage, "single", false);
    }

    /**
     * Creates a Tower mock with full targeting configuration.
     * canTargetFlying defaults to true (the tower can hit both ground and flying enemies).
     */
    public static Tower createTowerWithRange(int capacity, int standbyLoss, int shotCost, float fireRate,
                                             float towerRange, int damage, String targeting, boolean deferDamage) {
        return createTowerWithRange(capacity, standbyLoss, shotCost, fireRate, towerRange, damage,
            targeting, deferDamage, true);
    }

    /**
     * Creates a Tower mock with full targeting configuration including the canTargetFlying flag.
     */
    public static Tower createTowerWithRange(int capacity, int standbyLoss, int shotCost, float fireRate,
                                             float towerRange, int damage, String targeting, boolean deferDamage,
                                             boolean canTargetFlying) {
        Tower.Level level = mock(Tower.Level.class);
        when(level.getPowerStorage()).thenReturn(capacity);
        when(level.getPowerLossStandby()).thenReturn(standbyLoss);
        when(level.getPowerCostShot()).thenReturn(shotCost);
        when(level.getFireRate()).thenReturn(fireRate);
        when(level.getTowerRange()).thenReturn(towerRange);
        when(level.getDamage()).thenReturn(damage);
        when(level.getScrapCost()).thenReturn(0);

        Tower tower = mock(Tower.class);
        when(tower.getLevels()).thenReturn(Collections.singletonList(level));
        when(tower.getTargeting()).thenReturn(targeting);
        when(tower.isDeferDamage()).thenReturn(deferDamage);
        when(tower.isCanTargetFlying()).thenReturn(canTargetFlying);
        return tower;
    }

    public static Conduit createConduit(int transferRate, int transferLoss, int range) {
        return createConduit(transferRate, transferLoss, range, 0);
    }

    public static Conduit createConduit(int transferRate, int transferLoss, int range, int scrapCost) {
        Conduit conduit = mock(Conduit.class);
        when(conduit.getPowerTransferRate()).thenReturn(transferRate);
        when(conduit.getPowerTransferLoss()).thenReturn(transferLoss);
        when(conduit.getConnectRange()).thenReturn(range);
        when(conduit.getScrapCost()).thenReturn(scrapCost);
        return conduit;
    }
}
