package com.skamaniak.ugfs.asset.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for getBuildCost() overrides on all GameAsset subclasses.
 *
 * Asset model classes have private fields with no setters (designed for JSON deserialization).
 * Reflection is used here as a test-only technique to populate those fields — no production
 * code is modified. This is the only way to exercise the actual override logic without
 * introducing setters or constructors into the production classes.
 */
class GameAssetBuildCostTest {

    // ---- GameAsset base class ----

    @Test
    void gameAsset_getBuildCost_returnsZeroByDefault() {
        // Use a concrete subclass with no getBuildCost() override — Level extends GameAsset
        // and does not override getBuildCost(), so it inherits the default of 0.
        Level level = new Level();
        assertEquals(0, level.getBuildCost(),
            "GameAsset.getBuildCost() default implementation should return 0");
    }

    // ---- Generator ----

    @Test
    void generator_getBuildCost_returnsLevelZeroScrapCost() throws Exception {
        Generator.Level level = realGeneratorLevel(50);
        Generator generator = realGenerator(Collections.singletonList(level));

        assertEquals(50, generator.getBuildCost(),
            "Generator.getBuildCost() should return the level-1 scrap cost");
    }

    @Test
    void generator_getBuildCost_returnsZeroWhenScrapCostIsZero() throws Exception {
        Generator.Level level = realGeneratorLevel(0);
        Generator generator = realGenerator(Collections.singletonList(level));

        assertEquals(0, generator.getBuildCost(),
            "Generator.getBuildCost() should return 0 when scrap cost is 0");
    }

    @Test
    void generator_getBuildCost_usesFirstLevelOnly() throws Exception {
        Generator.Level levelOne = realGeneratorLevel(30);
        Generator.Level levelTwo = realGeneratorLevel(999);
        Generator generator = realGenerator(java.util.Arrays.asList(levelOne, levelTwo));

        assertEquals(30, generator.getBuildCost(),
            "Generator.getBuildCost() should use levels.get(0), not subsequent levels");
    }

    // ---- PowerStorage ----

    @Test
    void powerStorage_getBuildCost_returnsLevelZeroScrapCost() throws Exception {
        PowerStorage.Level level = realPowerStorageLevel(75);
        PowerStorage storage = realPowerStorage(Collections.singletonList(level));

        assertEquals(75, storage.getBuildCost(),
            "PowerStorage.getBuildCost() should return the level-1 scrap cost");
    }

    @Test
    void powerStorage_getBuildCost_returnsZeroWhenScrapCostIsZero() throws Exception {
        PowerStorage.Level level = realPowerStorageLevel(0);
        PowerStorage storage = realPowerStorage(Collections.singletonList(level));

        assertEquals(0, storage.getBuildCost(),
            "PowerStorage.getBuildCost() should return 0 when scrap cost is 0");
    }

    @Test
    void powerStorage_getBuildCost_usesFirstLevelOnly() throws Exception {
        PowerStorage.Level levelOne = realPowerStorageLevel(40);
        PowerStorage.Level levelTwo = realPowerStorageLevel(888);
        PowerStorage storage = realPowerStorage(java.util.Arrays.asList(levelOne, levelTwo));

        assertEquals(40, storage.getBuildCost(),
            "PowerStorage.getBuildCost() should use levels.get(0), not subsequent levels");
    }

    // ---- Tower ----

    @Test
    void tower_getBuildCost_returnsLevelZeroScrapCost() throws Exception {
        Tower.Level level = realTowerLevel(200);
        Tower tower = realTower(Collections.singletonList(level));

        assertEquals(200, tower.getBuildCost(),
            "Tower.getBuildCost() should return the level-1 scrap cost");
    }

    @Test
    void tower_getBuildCost_returnsZeroWhenScrapCostIsZero() throws Exception {
        Tower.Level level = realTowerLevel(0);
        Tower tower = realTower(Collections.singletonList(level));

        assertEquals(0, tower.getBuildCost(),
            "Tower.getBuildCost() should return 0 when scrap cost is 0");
    }

    @Test
    void tower_getBuildCost_usesFirstLevelOnly() throws Exception {
        Tower.Level levelOne = realTowerLevel(100);
        Tower.Level levelTwo = realTowerLevel(777);
        Tower tower = realTower(java.util.Arrays.asList(levelOne, levelTwo));

        assertEquals(100, tower.getBuildCost(),
            "Tower.getBuildCost() should use levels.get(0), not subsequent levels");
    }

    // ---- Conduit ----

    @Test
    void conduit_getBuildCost_returnsScrapCost() throws Exception {
        Conduit conduit = realConduit(60);

        assertEquals(60, conduit.getBuildCost(),
            "Conduit.getBuildCost() should return the scrapCost field directly");
    }

    @Test
    void conduit_getBuildCost_returnsZeroWhenScrapCostIsZero() throws Exception {
        Conduit conduit = realConduit(0);

        assertEquals(0, conduit.getBuildCost(),
            "Conduit.getBuildCost() should return 0 when scrap cost is 0");
    }

    @Test
    void conduit_getBuildCost_matchesGetScrapCost() throws Exception {
        Conduit conduit = realConduit(35);

        assertEquals(conduit.getScrapCost(), conduit.getBuildCost(),
            "Conduit.getBuildCost() and getScrapCost() should return the same value");
    }

    // ---- Reflection helpers ----

    private static Generator.Level realGeneratorLevel(int scrapCost) throws Exception {
        Generator.Level level = new Generator.Level();
        setField(Generator.Level.class, level, "scrapCost", scrapCost);
        return level;
    }

    private static Generator realGenerator(java.util.List<Generator.Level> levels) throws Exception {
        Generator generator = new Generator();
        setField(Generator.class, generator, "levels", levels);
        return generator;
    }

    private static PowerStorage.Level realPowerStorageLevel(int scrapCost) throws Exception {
        PowerStorage.Level level = new PowerStorage.Level();
        setField(PowerStorage.Level.class, level, "scrapCost", scrapCost);
        return level;
    }

    private static PowerStorage realPowerStorage(java.util.List<PowerStorage.Level> levels) throws Exception {
        PowerStorage storage = new PowerStorage();
        setField(PowerStorage.class, storage, "levels", levels);
        return storage;
    }

    private static Tower.Level realTowerLevel(int scrapCost) throws Exception {
        Tower.Level level = new Tower.Level();
        setField(Tower.Level.class, level, "scrapCost", scrapCost);
        return level;
    }

    private static Tower realTower(java.util.List<Tower.Level> levels) throws Exception {
        Tower tower = new Tower();
        setField(Tower.class, tower, "levels", levels);
        return tower;
    }

    private static Conduit realConduit(int scrapCost) throws Exception {
        Conduit conduit = new Conduit();
        setField(Conduit.class, conduit, "scrapCost", scrapCost);
        return conduit;
    }

    private static void setField(Class<?> clazz, Object target, String fieldName, Object value)
            throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
