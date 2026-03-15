package com.skamaniak.ugfs.game.entity;

import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.TestAssetFactory;
import com.skamaniak.ugfs.asset.model.Generator;
import com.skamaniak.ugfs.asset.model.PowerStorage;
import com.skamaniak.ugfs.asset.model.Tower;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the upgrade infrastructure methods added to GameEntity subclasses:
 * getLevel(), getMaxLevel(), canUpgrade(), getUpgradeCost(), applyUpgrade(),
 * getTotalScrapInvested(), and getScrapCost() reflecting the current level.
 *
 * Covers all three upgradeable entity types: GeneratorEntity, PowerStorageEntity,
 * and TowerEntity.
 */
class UpgradeInfrastructureTest {

    // ============================================================
    // GeneratorEntity
    // ============================================================

    @Test
    void generator_getLevel_returnsOneAtCreation() {
        Generator gen = TestAssetFactory.createMultiLevelGenerator(
            new int[]{200, 50, 50},
            new int[]{300, 100, 75}
        );
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);

        assertEquals(1, entity.getLevel(),
            "GeneratorEntity should start at level 1");
    }

    @Test
    void generator_getMaxLevel_reflectsNumberOfLevels() {
        Generator gen = TestAssetFactory.createMultiLevelGenerator(
            new int[]{200, 50, 50},
            new int[]{300, 100, 75}
        );
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);

        assertEquals(2, entity.getMaxLevel(),
            "getMaxLevel() should return the total number of levels in the asset");
    }

    @Test
    void generator_canUpgrade_trueWhenBelowMaxLevel() {
        Generator gen = TestAssetFactory.createMultiLevelGenerator(
            new int[]{200, 50, 50},
            new int[]{300, 100, 75}
        );
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);

        assertTrue(entity.canUpgrade(),
            "canUpgrade() should return true when entity is below max level");
    }

    @Test
    void generator_canUpgrade_falseAtMaxLevel() {
        Generator gen = TestAssetFactory.createGenerator(200, 50, 50); // single level
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);

        assertFalse(entity.canUpgrade(),
            "canUpgrade() should return false when entity is already at max level");
    }

    @Test
    void generator_getUpgradeCost_returnsNextLevelCost() {
        Generator gen = TestAssetFactory.createMultiLevelGenerator(
            new int[]{200, 50, 50},
            new int[]{300, 100, 75}
        );
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);

        assertEquals(75, entity.getUpgradeCost(),
            "getUpgradeCost() should return the scrap cost of the next level");
    }

    @Test
    void generator_getUpgradeCost_returnsNegativeOneAtMaxLevel() {
        Generator gen = TestAssetFactory.createGenerator(200, 50, 50); // single level
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);

        assertEquals(-1, entity.getUpgradeCost(),
            "getUpgradeCost() should return -1 when at max level");
    }

    @Test
    void generator_applyUpgrade_incrementsLevel() {
        Generator gen = TestAssetFactory.createMultiLevelGenerator(
            new int[]{200, 50, 50},
            new int[]{300, 100, 75}
        );
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);

        entity.applyUpgrade();

        assertEquals(2, entity.getLevel(),
            "applyUpgrade() should increment the level by 1");
    }

    @Test
    void generator_applyUpgrade_updatesTotalScrapInvested() {
        Generator gen = TestAssetFactory.createMultiLevelGenerator(
            new int[]{200, 50, 50},
            new int[]{300, 100, 75}
        );
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);

        entity.applyUpgrade();

        assertEquals(50 + 75, entity.getTotalScrapInvested(),
            "applyUpgrade() should add the upgrade cost to totalScrapInvested");
    }

    @Test
    void generator_getScrapCost_reflectsNewLevelAfterUpgrade() {
        Generator gen = TestAssetFactory.createMultiLevelGenerator(
            new int[]{200, 50, 50},
            new int[]{300, 100, 75}
        );
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);

        entity.applyUpgrade();

        assertEquals(75, entity.getScrapCost(),
            "getScrapCost() should return the current (upgraded) level's cost after applyUpgrade()");
    }

    @Test
    void generator_getTotalScrapInvested_initializedToLevelOneCost() {
        Generator gen = TestAssetFactory.createMultiLevelGenerator(
            new int[]{200, 50, 50},
            new int[]{300, 100, 75}
        );
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);

        assertEquals(50, entity.getTotalScrapInvested(),
            "getTotalScrapInvested() should equal the level-1 cost immediately after construction");
    }

    @Test
    void generator_totalScrapInvested_accumulatesAcrossTwoUpgrades() {
        // level 1 cost=50, level 2 cost=75, level 3 cost=100
        Generator gen = TestAssetFactory.createMultiLevelGenerator(
            new int[]{200, 50, 50},
            new int[]{300, 100, 75},
            new int[]{400, 150, 100}
        );
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);

        entity.applyUpgrade(); // +75
        entity.applyUpgrade(); // +100

        assertEquals(50 + 75 + 100, entity.getTotalScrapInvested(),
            "getTotalScrapInvested() should accumulate all invested scrap across multiple upgrades");
    }

    @Test
    void generator_canUpgrade_falseAfterReachingMaxLevel() {
        Generator gen = TestAssetFactory.createMultiLevelGenerator(
            new int[]{200, 50, 50},
            new int[]{300, 100, 75}
        );
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);

        entity.applyUpgrade(); // now at level 2 = max

        assertFalse(entity.canUpgrade(),
            "canUpgrade() should return false after upgrading to max level");
    }

    // ============================================================
    // PowerStorageEntity
    // ============================================================

    @Test
    void storage_getLevel_returnsOneAtCreation() {
        PowerStorage storage = TestAssetFactory.createMultiLevelPowerStorage(
            new int[]{500, 0, 25},
            new int[]{1000, 0, 50}
        );
        PowerStorageEntity entity = new PowerStorageEntity(new Vector2(0, 0), storage);

        assertEquals(1, entity.getLevel(),
            "PowerStorageEntity should start at level 1");
    }

    @Test
    void storage_getMaxLevel_reflectsNumberOfLevels() {
        PowerStorage storage = TestAssetFactory.createMultiLevelPowerStorage(
            new int[]{500, 0, 25},
            new int[]{1000, 0, 50}
        );
        PowerStorageEntity entity = new PowerStorageEntity(new Vector2(0, 0), storage);

        assertEquals(2, entity.getMaxLevel(),
            "getMaxLevel() should return the total number of levels in the asset");
    }

    @Test
    void storage_canUpgrade_trueWhenBelowMaxLevel() {
        PowerStorage storage = TestAssetFactory.createMultiLevelPowerStorage(
            new int[]{500, 0, 25},
            new int[]{1000, 0, 50}
        );
        PowerStorageEntity entity = new PowerStorageEntity(new Vector2(0, 0), storage);

        assertTrue(entity.canUpgrade(),
            "canUpgrade() should return true when entity is below max level");
    }

    @Test
    void storage_canUpgrade_falseAtMaxLevel() {
        PowerStorage storage = TestAssetFactory.createPowerStorage(500, 0, 25); // single level
        PowerStorageEntity entity = new PowerStorageEntity(new Vector2(0, 0), storage);

        assertFalse(entity.canUpgrade(),
            "canUpgrade() should return false when entity is already at max level");
    }

    @Test
    void storage_getUpgradeCost_returnsNextLevelCost() {
        PowerStorage storage = TestAssetFactory.createMultiLevelPowerStorage(
            new int[]{500, 0, 25},
            new int[]{1000, 0, 50}
        );
        PowerStorageEntity entity = new PowerStorageEntity(new Vector2(0, 0), storage);

        assertEquals(50, entity.getUpgradeCost(),
            "getUpgradeCost() should return the scrap cost of the next level");
    }

    @Test
    void storage_getUpgradeCost_returnsNegativeOneAtMaxLevel() {
        PowerStorage storage = TestAssetFactory.createPowerStorage(500, 0, 25); // single level
        PowerStorageEntity entity = new PowerStorageEntity(new Vector2(0, 0), storage);

        assertEquals(-1, entity.getUpgradeCost(),
            "getUpgradeCost() should return -1 when at max level");
    }

    @Test
    void storage_applyUpgrade_incrementsLevel() {
        PowerStorage storage = TestAssetFactory.createMultiLevelPowerStorage(
            new int[]{500, 0, 25},
            new int[]{1000, 0, 50}
        );
        PowerStorageEntity entity = new PowerStorageEntity(new Vector2(0, 0), storage);

        entity.applyUpgrade();

        assertEquals(2, entity.getLevel(),
            "applyUpgrade() should increment the level by 1");
    }

    @Test
    void storage_applyUpgrade_updatesTotalScrapInvested() {
        PowerStorage storage = TestAssetFactory.createMultiLevelPowerStorage(
            new int[]{500, 0, 25},
            new int[]{1000, 0, 50}
        );
        PowerStorageEntity entity = new PowerStorageEntity(new Vector2(0, 0), storage);

        entity.applyUpgrade();

        assertEquals(25 + 50, entity.getTotalScrapInvested(),
            "applyUpgrade() should add the upgrade cost to totalScrapInvested");
    }

    @Test
    void storage_getScrapCost_reflectsNewLevelAfterUpgrade() {
        PowerStorage storage = TestAssetFactory.createMultiLevelPowerStorage(
            new int[]{500, 0, 25},
            new int[]{1000, 0, 50}
        );
        PowerStorageEntity entity = new PowerStorageEntity(new Vector2(0, 0), storage);

        entity.applyUpgrade();

        assertEquals(50, entity.getScrapCost(),
            "getScrapCost() should return the current (upgraded) level's cost after applyUpgrade()");
    }

    @Test
    void storage_getTotalScrapInvested_initializedToLevelOneCost() {
        PowerStorage storage = TestAssetFactory.createMultiLevelPowerStorage(
            new int[]{500, 0, 25},
            new int[]{1000, 0, 50}
        );
        PowerStorageEntity entity = new PowerStorageEntity(new Vector2(0, 0), storage);

        assertEquals(25, entity.getTotalScrapInvested(),
            "getTotalScrapInvested() should equal the level-1 cost immediately after construction");
    }

    // ============================================================
    // TowerEntity
    // ============================================================

    @Test
    void tower_getLevel_returnsOneAtCreation() {
        Tower tower = TestAssetFactory.createMultiLevelTower(
            new int[]{100, 0, 10, 30},
            new int[]{200, 0, 15, 60}
        );
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);

        assertEquals(1, entity.getLevel(),
            "TowerEntity should start at level 1");
    }

    @Test
    void tower_getMaxLevel_reflectsNumberOfLevels() {
        Tower tower = TestAssetFactory.createMultiLevelTower(
            new int[]{100, 0, 10, 30},
            new int[]{200, 0, 15, 60}
        );
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);

        assertEquals(2, entity.getMaxLevel(),
            "getMaxLevel() should return the total number of levels in the asset");
    }

    @Test
    void tower_canUpgrade_trueWhenBelowMaxLevel() {
        Tower tower = TestAssetFactory.createMultiLevelTower(
            new int[]{100, 0, 10, 30},
            new int[]{200, 0, 15, 60}
        );
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);

        assertTrue(entity.canUpgrade(),
            "canUpgrade() should return true when entity is below max level");
    }

    @Test
    void tower_canUpgrade_falseAtMaxLevel() {
        Tower tower = TestAssetFactory.createTower(100, 0, 10, 1.0f, 30); // single level
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);

        assertFalse(entity.canUpgrade(),
            "canUpgrade() should return false when entity is already at max level");
    }

    @Test
    void tower_getUpgradeCost_returnsNextLevelCost() {
        Tower tower = TestAssetFactory.createMultiLevelTower(
            new int[]{100, 0, 10, 30},
            new int[]{200, 0, 15, 60}
        );
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);

        assertEquals(60, entity.getUpgradeCost(),
            "getUpgradeCost() should return the scrap cost of the next level");
    }

    @Test
    void tower_getUpgradeCost_returnsNegativeOneAtMaxLevel() {
        Tower tower = TestAssetFactory.createTower(100, 0, 10, 1.0f, 30); // single level
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);

        assertEquals(-1, entity.getUpgradeCost(),
            "getUpgradeCost() should return -1 when at max level");
    }

    @Test
    void tower_applyUpgrade_incrementsLevel() {
        Tower tower = TestAssetFactory.createMultiLevelTower(
            new int[]{100, 0, 10, 30},
            new int[]{200, 0, 15, 60}
        );
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);

        entity.applyUpgrade();

        assertEquals(2, entity.getLevel(),
            "applyUpgrade() should increment the level by 1");
    }

    @Test
    void tower_applyUpgrade_updatesTotalScrapInvested() {
        Tower tower = TestAssetFactory.createMultiLevelTower(
            new int[]{100, 0, 10, 30},
            new int[]{200, 0, 15, 60}
        );
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);

        entity.applyUpgrade();

        assertEquals(30 + 60, entity.getTotalScrapInvested(),
            "applyUpgrade() should add the upgrade cost to totalScrapInvested");
    }

    @Test
    void tower_getScrapCost_reflectsNewLevelAfterUpgrade() {
        Tower tower = TestAssetFactory.createMultiLevelTower(
            new int[]{100, 0, 10, 30},
            new int[]{200, 0, 15, 60}
        );
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);

        entity.applyUpgrade();

        assertEquals(60, entity.getScrapCost(),
            "getScrapCost() should return the current (upgraded) level's cost after applyUpgrade()");
    }

    @Test
    void tower_getTotalScrapInvested_initializedToLevelOneCost() {
        Tower tower = TestAssetFactory.createMultiLevelTower(
            new int[]{100, 0, 10, 30},
            new int[]{200, 0, 15, 60}
        );
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);

        assertEquals(30, entity.getTotalScrapInvested(),
            "getTotalScrapInvested() should equal the level-1 cost immediately after construction");
    }

    @Test
    void tower_totalScrapInvested_accumulatesAcrossTwoUpgrades() {
        // level 1 cost=30, level 2 cost=60, level 3 cost=90
        Tower tower = TestAssetFactory.createMultiLevelTower(
            new int[]{100, 0, 10, 30},
            new int[]{200, 0, 15, 60},
            new int[]{300, 0, 20, 90}
        );
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);

        entity.applyUpgrade(); // +60
        entity.applyUpgrade(); // +90

        assertEquals(30 + 60 + 90, entity.getTotalScrapInvested(),
            "getTotalScrapInvested() should accumulate all invested scrap across multiple upgrades");
    }
}
