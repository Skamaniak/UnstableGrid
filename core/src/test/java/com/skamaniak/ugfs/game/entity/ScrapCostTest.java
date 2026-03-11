package com.skamaniak.ugfs.game.entity;

import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.TestAssetFactory;
import com.skamaniak.ugfs.asset.model.Conduit;
import com.skamaniak.ugfs.asset.model.Generator;
import com.skamaniak.ugfs.asset.model.PowerStorage;
import com.skamaniak.ugfs.asset.model.Tower;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for getScrapCost() on all entity types.
 * The spec requires each entity to expose its level's scrap cost so the sell/refund
 * system can compute the correct refund amount.
 */
class ScrapCostTest {

    @Test
    void generatorEntity_getScrapCost_returnsLevelScrapCost() {
        Generator gen = TestAssetFactory.createGenerator(1000, 100, 150);
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);

        assertEquals(150, entity.getScrapCost(),
            "GeneratorEntity.getScrapCost() should return the level's scrap cost");
    }

    @Test
    void generatorEntity_getScrapCost_zeroByDefault() {
        Generator gen = TestAssetFactory.createGenerator(1000, 100); // scrapCost defaults to 0
        GeneratorEntity entity = new GeneratorEntity(new Vector2(0, 0), gen);

        assertEquals(0, entity.getScrapCost(),
            "GeneratorEntity.getScrapCost() should return 0 when no scrap cost is configured");
    }

    @Test
    void towerEntity_getScrapCost_returnsLevelScrapCost() {
        Tower twr = TestAssetFactory.createTower(1000, 0, 10, 1.0f, 200);
        TowerEntity entity = new TowerEntity(new Vector2(1, 0), twr);

        assertEquals(200, entity.getScrapCost(),
            "TowerEntity.getScrapCost() should return the level's scrap cost");
    }

    @Test
    void towerEntity_getScrapCost_zeroByDefault() {
        Tower twr = TestAssetFactory.createTower(1000, 0, 10, 1.0f); // scrapCost defaults to 0
        TowerEntity entity = new TowerEntity(new Vector2(1, 0), twr);

        assertEquals(0, entity.getScrapCost(),
            "TowerEntity.getScrapCost() should return 0 when no scrap cost is configured");
    }

    @Test
    void powerStorageEntity_getScrapCost_returnsLevelScrapCost() {
        PowerStorage storage = TestAssetFactory.createPowerStorage(1000, 0, 75);
        PowerStorageEntity entity = new PowerStorageEntity(new Vector2(2, 0), storage);

        assertEquals(75, entity.getScrapCost(),
            "PowerStorageEntity.getScrapCost() should return the level's scrap cost");
    }

    @Test
    void powerStorageEntity_getScrapCost_zeroByDefault() {
        PowerStorage storage = TestAssetFactory.createPowerStorage(1000, 0); // scrapCost defaults to 0
        PowerStorageEntity entity = new PowerStorageEntity(new Vector2(2, 0), storage);

        assertEquals(0, entity.getScrapCost(),
            "PowerStorageEntity.getScrapCost() should return 0 when no scrap cost is configured");
    }

    @Test
    void conduitEntity_getScrapCostFromAsset_returnsConfiguredValue() {
        // ConduitEntity does not extend GameEntity and has no getScrapCost() itself;
        // the sell logic reads conduit.conduit.getScrapCost() directly.
        // Verify the Conduit asset model returns the correct scrap cost.
        Conduit conduitAsset = TestAssetFactory.createConduit(1000, 0, 10, 50);

        assertEquals(50, conduitAsset.getScrapCost(),
            "Conduit asset getScrapCost() should return the configured scrap cost");
    }
}
