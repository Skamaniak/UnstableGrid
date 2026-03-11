package com.skamaniak.ugfs;

import com.skamaniak.ugfs.asset.model.Conduit;
import com.skamaniak.ugfs.asset.model.Generator;
import com.skamaniak.ugfs.asset.model.PowerStorage;
import com.skamaniak.ugfs.asset.model.Tower;

import java.util.Collections;

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
