package com.skamaniak.ugfs.game;

import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.TestAssetFactory;
import com.skamaniak.ugfs.asset.model.Conduit;
import com.skamaniak.ugfs.asset.model.Generator;
import com.skamaniak.ugfs.asset.model.Level;
import com.skamaniak.ugfs.asset.model.PowerStorage;
import com.skamaniak.ugfs.asset.model.Tower;
import com.skamaniak.ugfs.game.entity.GeneratorEntity;
import com.skamaniak.ugfs.game.entity.PowerStorageEntity;
import com.skamaniak.ugfs.game.entity.TowerEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for GameState.upgradeEntity(), sell-after-upgrade scrap refunds, and
 * computeSellValue() after upgrades.
 *
 * GameState is constructed with a mocked Level and null UnstableGrid (only draw/sound
 * paths access the game object — not exercised here).
 */
class GameStateUpgradeTest {

    private static final int STARTING_SCRAP = 500;

    private GameState gameState;

    @BeforeEach
    void setUp() {
        Level level = mock(Level.class);
        when(level.getScrap()).thenReturn(STARTING_SCRAP);
        when(level.getMap()).thenReturn(Collections.emptyList());
        gameState = new GameState(null, level);
    }

    // ============================================================
    // upgradeEntity: success path
    // ============================================================

    @Test
    void upgradeEntity_withSufficientScrap_returnsTrue() {
        Generator gen = TestAssetFactory.createMultiLevelGenerator(
            new int[]{200, 50, 50},
            new int[]{300, 100, 75}
        );
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);
        gameState.registerGenerator(entity);

        boolean result = gameState.upgradeEntity(entity);

        assertTrue(result, "upgradeEntity() should return true when scrap is sufficient");
    }

    @Test
    void upgradeEntity_withSufficientScrap_deductsUpgradeCost() {
        Generator gen = TestAssetFactory.createMultiLevelGenerator(
            new int[]{200, 50, 50},
            new int[]{300, 100, 75}
        );
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);
        gameState.registerGenerator(entity);

        gameState.upgradeEntity(entity); // upgrade cost = 75

        assertEquals(STARTING_SCRAP - 75, gameState.getScrap(),
            "upgradeEntity() should deduct the upgrade cost from the scrap balance");
    }

    @Test
    void upgradeEntity_withSufficientScrap_incrememntsEntityLevel() {
        Generator gen = TestAssetFactory.createMultiLevelGenerator(
            new int[]{200, 50, 50},
            new int[]{300, 100, 75}
        );
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);
        gameState.registerGenerator(entity);

        gameState.upgradeEntity(entity);

        assertEquals(2, entity.getLevel(),
            "upgradeEntity() should increment the entity's level on success");
    }

    // ============================================================
    // upgradeEntity: insufficient scrap
    // ============================================================

    @Test
    void upgradeEntity_withInsufficientScrap_returnsFalse() {
        Generator gen = TestAssetFactory.createMultiLevelGenerator(
            new int[]{200, 50, 50},
            new int[]{300, 100, 600}  // upgrade costs 600, more than starting scrap
        );
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);
        gameState.registerGenerator(entity);

        boolean result = gameState.upgradeEntity(entity);

        assertFalse(result, "upgradeEntity() should return false when scrap is insufficient");
    }

    @Test
    void upgradeEntity_withInsufficientScrap_doesNotChangeScrap() {
        Generator gen = TestAssetFactory.createMultiLevelGenerator(
            new int[]{200, 50, 50},
            new int[]{300, 100, 600}
        );
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);
        gameState.registerGenerator(entity);

        gameState.upgradeEntity(entity);

        assertEquals(STARTING_SCRAP, gameState.getScrap(),
            "upgradeEntity() should not change the scrap balance on failure");
    }

    @Test
    void upgradeEntity_withInsufficientScrap_doesNotChangeEntityLevel() {
        Generator gen = TestAssetFactory.createMultiLevelGenerator(
            new int[]{200, 50, 50},
            new int[]{300, 100, 600}
        );
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);
        gameState.registerGenerator(entity);

        gameState.upgradeEntity(entity);

        assertEquals(1, entity.getLevel(),
            "upgradeEntity() should not change the entity level on failure");
    }

    // ============================================================
    // upgradeEntity: at max level
    // ============================================================

    @Test
    void upgradeEntity_atMaxLevel_returnsFalse() {
        Generator gen = TestAssetFactory.createGenerator(200, 50, 50); // single level = max
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);
        gameState.registerGenerator(entity);

        boolean result = gameState.upgradeEntity(entity);

        assertFalse(result, "upgradeEntity() should return false when entity is at max level");
    }

    @Test
    void upgradeEntity_atMaxLevel_doesNotChangeScrap() {
        Generator gen = TestAssetFactory.createGenerator(200, 50, 50); // single level = max
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);
        gameState.registerGenerator(entity);

        gameState.upgradeEntity(entity);

        assertEquals(STARTING_SCRAP, gameState.getScrap(),
            "upgradeEntity() should leave scrap unchanged when entity is at max level");
    }

    // ============================================================
    // upgradeEntity: PowerStorageEntity
    // ============================================================

    @Test
    void upgradeEntity_powerStorage_withSufficientScrap_succeeds() {
        PowerStorage storage = TestAssetFactory.createMultiLevelPowerStorage(
            new int[]{500, 0, 25},
            new int[]{1000, 0, 50}
        );
        PowerStorageEntity entity = new PowerStorageEntity(new Vector2(0, 0), storage);
        gameState.registerPowerStorage(entity);

        boolean result = gameState.upgradeEntity(entity);

        assertTrue(result, "upgradeEntity() should return true for PowerStorageEntity with sufficient scrap");
        assertEquals(2, entity.getLevel(), "Entity level should be 2 after successful upgrade");
        assertEquals(STARTING_SCRAP - 50, gameState.getScrap(), "Scrap should decrease by upgrade cost");
    }

    // ============================================================
    // upgradeEntity: TowerEntity
    // ============================================================

    @Test
    void upgradeEntity_tower_withSufficientScrap_succeeds() {
        Tower tower = TestAssetFactory.createMultiLevelTower(
            new int[]{100, 0, 10, 30},
            new int[]{200, 0, 15, 60}
        );
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);
        gameState.registerTower(entity);

        boolean result = gameState.upgradeEntity(entity);

        assertTrue(result, "upgradeEntity() should return true for TowerEntity with sufficient scrap");
        assertEquals(2, entity.getLevel(), "Entity level should be 2 after successful upgrade");
        assertEquals(STARTING_SCRAP - 60, gameState.getScrap(), "Scrap should decrease by upgrade cost");
    }

    // ============================================================
    // upgradeEntity: exact scrap boundary
    // ============================================================

    @Test
    void upgradeEntity_withExactScrap_succeeds() {
        // Drain scrap down to exactly the upgrade cost
        gameState.spendScrap(STARTING_SCRAP - 75);

        Generator gen = TestAssetFactory.createMultiLevelGenerator(
            new int[]{200, 50, 50},
            new int[]{300, 100, 75}
        );
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);
        gameState.registerGenerator(entity);

        boolean result = gameState.upgradeEntity(entity);

        assertTrue(result, "upgradeEntity() should succeed when scrap equals upgrade cost exactly");
        assertEquals(0, gameState.getScrap(), "Scrap should be 0 after spending exact balance");
    }

    // ============================================================
    // sell after upgrade: full refund of total invested scrap
    // ============================================================

    @Test
    void sellGenerator_afterUpgrade_refundsTotalInvestedScrap() {
        // level 1 cost = 50, upgrade cost = 75; total invested = 125
        Generator gen = TestAssetFactory.createMultiLevelGenerator(
            new int[]{200, 50, 50},
            new int[]{300, 100, 75}
        );
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);
        gameState.registerGenerator(entity);

        gameState.upgradeEntity(entity); // spends 75, scrap = 500 - 75 = 425
        int scrapBeforeSell = gameState.getScrap(); // 425

        gameState.sellGenerator(entity);

        // Refund should be 125 (50 + 75)
        assertEquals(scrapBeforeSell + 125, gameState.getScrap(),
            "sellGenerator() after upgrade should refund the full totalScrapInvested (placement + upgrade costs)");
    }

    @Test
    void sellStorage_afterUpgrade_refundsTotalInvestedScrap() {
        // level 1 cost = 25, upgrade cost = 50; total invested = 75
        PowerStorage storage = TestAssetFactory.createMultiLevelPowerStorage(
            new int[]{500, 0, 25},
            new int[]{1000, 0, 50}
        );
        PowerStorageEntity entity = new PowerStorageEntity(new Vector2(0, 0), storage);
        gameState.registerPowerStorage(entity);

        gameState.upgradeEntity(entity); // spends 50
        int scrapBeforeSell = gameState.getScrap();

        gameState.sellStorage(entity);

        assertEquals(scrapBeforeSell + 75, gameState.getScrap(),
            "sellStorage() after upgrade should refund the full totalScrapInvested");
    }

    @Test
    void sellTower_afterUpgrade_refundsTotalInvestedScrap() {
        // level 1 cost = 30, upgrade cost = 60; total invested = 90
        Tower tower = TestAssetFactory.createMultiLevelTower(
            new int[]{100, 0, 10, 30},
            new int[]{200, 0, 15, 60}
        );
        TowerEntity entity = new TowerEntity(new Vector2(0, 0), tower);
        gameState.registerTower(entity);

        gameState.upgradeEntity(entity); // spends 60
        int scrapBeforeSell = gameState.getScrap();

        gameState.sellTower(entity);

        assertEquals(scrapBeforeSell + 90, gameState.getScrap(),
            "sellTower() after upgrade should refund the full totalScrapInvested");
    }

    @Test
    void sellGenerator_withoutUpgrade_refundsPlacementCostOnly() {
        Generator gen = TestAssetFactory.createMultiLevelGenerator(
            new int[]{200, 50, 50},
            new int[]{300, 100, 75}
        );
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);
        gameState.registerGenerator(entity);

        int scrapBeforeSell = gameState.getScrap();
        gameState.sellGenerator(entity);

        assertEquals(scrapBeforeSell + 50, gameState.getScrap(),
            "sellGenerator() on a non-upgraded entity should refund only the placement cost");
    }

    // ============================================================
    // computeSellValue: includes total scrap invested + connected conduits
    // ============================================================

    @Test
    void computeSellValue_withoutUpgrade_equalsPlacementCost() {
        Generator gen = TestAssetFactory.createMultiLevelGenerator(
            new int[]{200, 50, 50},
            new int[]{300, 100, 75}
        );
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);
        gameState.registerGenerator(entity);

        int sellValue = gameState.computeSellValue(entity);

        assertEquals(50, sellValue,
            "computeSellValue() on a level-1 entity with no wires should equal the placement cost");
    }

    @Test
    void computeSellValue_afterUpgrade_equalsTotalInvestedScrap() {
        Generator gen = TestAssetFactory.createMultiLevelGenerator(
            new int[]{200, 50, 50},
            new int[]{300, 100, 75}
        );
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);
        gameState.registerGenerator(entity);

        gameState.upgradeEntity(entity);

        int sellValue = gameState.computeSellValue(entity);

        assertEquals(125, sellValue,
            "computeSellValue() after upgrade should return placement cost + upgrade cost (125 = 50 + 75)");
    }

    @Test
    void computeSellValue_afterUpgrade_includesConnectedConduitCosts() {
        // Generator: level 1 cost = 50, upgrade cost = 75; total = 125
        Generator gen = TestAssetFactory.createMultiLevelGenerator(
            new int[]{200, 50, 50},
            new int[]{300, 100, 75}
        );
        Tower towerAsset = TestAssetFactory.createTower(1000, 0, 10, 1.0f, 0);
        Conduit conduitAsset = TestAssetFactory.createConduit(1000, 0, 10, 40);

        GeneratorEntity generator = new GeneratorEntity(new Vector2(0, 0), gen);
        TowerEntity tower = new TowerEntity(new Vector2(1, 0), towerAsset);

        gameState.registerGenerator(generator);
        gameState.registerTower(tower);
        gameState.registerLinkFree(conduitAsset, generator, tower); // conduit cost = 40, no scrap spend

        gameState.upgradeEntity(generator); // spends 75

        int sellValue = gameState.computeSellValue(generator);

        // Expected: totalScrapInvested(125) + conduit cost(40) = 165
        assertEquals(165, sellValue,
            "computeSellValue() should include both totalScrapInvested and connected conduit costs");
    }

    // ============================================================
    // Multiple upgrades: cumulative totalScrapInvested
    // ============================================================

    @Test
    void sellGenerator_afterTwoUpgrades_refundsAllThreeLevelCosts() {
        // level 1 = 50, level 2 = 75, level 3 = 100; total = 225
        Generator gen = TestAssetFactory.createMultiLevelGenerator(
            new int[]{200, 50, 50},
            new int[]{300, 100, 75},
            new int[]{400, 150, 100}
        );
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);
        gameState.registerGenerator(entity);

        gameState.upgradeEntity(entity); // spends 75
        gameState.upgradeEntity(entity); // spends 100
        int scrapBeforeSell = gameState.getScrap();

        gameState.sellGenerator(entity);

        assertEquals(scrapBeforeSell + 225, gameState.getScrap(),
            "sellGenerator() after two upgrades should refund the sum of all three level costs");
    }
}
