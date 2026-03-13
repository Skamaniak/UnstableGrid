package com.skamaniak.ugfs.game;

import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.TestAssetFactory;
import com.skamaniak.ugfs.asset.model.Conduit;
import com.skamaniak.ugfs.asset.model.Generator;
import com.skamaniak.ugfs.asset.model.Level;
import com.skamaniak.ugfs.asset.model.Tower;
import com.skamaniak.ugfs.game.entity.ConduitEntity;
import com.skamaniak.ugfs.game.entity.GeneratorEntity;
import com.skamaniak.ugfs.game.entity.TowerEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for GameState.registerLink() scrap enforcement and GameState.registerLinkFree().
 *
 * registerLink() was changed to check the return value of spendScrap() and skip conduit
 * creation when the balance is insufficient. registerLinkFree() was added as the same
 * operation but without any scrap cost.
 *
 * GameState is constructed with a Mockito-mocked Level. UnstableGrid is null because
 * none of the tested methods touch draw or sound paths.
 */
class GameStateRegisterLinkTest {

    private static final int STARTING_SCRAP = 100;
    private static final int CONDUIT_COST = 50;

    private GameState gameState;
    private GeneratorEntity gen;
    private TowerEntity tower;
    private Conduit conduit;

    @BeforeEach
    void setUp() {
        Level level = mock(Level.class);
        when(level.getScrap()).thenReturn(STARTING_SCRAP);
        when(level.getMap()).thenReturn(Collections.emptyList());

        gameState = new GameState(null, level);

        Generator genAsset = TestAssetFactory.createGenerator(1000, 100);
        Tower towerAsset = TestAssetFactory.createTower(1000, 0, 10, 1.0f);
        conduit = TestAssetFactory.createConduit(1000, 0, 10, CONDUIT_COST);

        gen = new GeneratorEntity(new Vector2(0, 0), genAsset);
        tower = new TowerEntity(new Vector2(1, 0), towerAsset);

        gameState.registerGenerator(gen);
        gameState.registerTower(tower);
    }

    // ---- registerLink: sufficient scrap ----

    @Test
    void registerLink_withSufficientScrap_createsConduit() {
        gameState.registerLink(conduit, gen, tower);

        assertNotNull(gameState.findConduit(gen, tower),
            "registerLink() should create a conduit when scrap is sufficient");
    }

    @Test
    void registerLink_withSufficientScrap_deductsScrap() {
        gameState.registerLink(conduit, gen, tower);

        assertEquals(STARTING_SCRAP - CONDUIT_COST, gameState.getScrap(),
            "registerLink() should deduct the conduit's scrap cost from the balance");
    }

    // ---- registerLink: insufficient scrap ----

    @Test
    void registerLink_withInsufficientScrap_doesNotCreateConduit() {
        // Drain balance below conduit cost
        gameState.spendScrap(STARTING_SCRAP - CONDUIT_COST + 1);

        gameState.registerLink(conduit, gen, tower);

        assertNull(gameState.findConduit(gen, tower),
            "registerLink() should not create a conduit when scrap is insufficient");
    }

    @Test
    void registerLink_withInsufficientScrap_doesNotChangeBalance() {
        // Drain so only CONDUIT_COST - 1 remains (one below the threshold)
        int remainingBalance = CONDUIT_COST - 1;
        gameState.spendScrap(STARTING_SCRAP - remainingBalance);

        gameState.registerLink(conduit, gen, tower);

        assertEquals(remainingBalance, gameState.getScrap(),
            "registerLink() should leave the balance unchanged when it cannot afford the conduit");
    }

    @Test
    void registerLink_withExactBalance_createsConduit() {
        // Drain to exactly the conduit cost
        gameState.spendScrap(STARTING_SCRAP - CONDUIT_COST);

        gameState.registerLink(conduit, gen, tower);

        assertNotNull(gameState.findConduit(gen, tower),
            "registerLink() should succeed when scrap equals the conduit cost exactly");
    }

    @Test
    void registerLink_withExactBalance_leavesZeroBalance() {
        gameState.spendScrap(STARTING_SCRAP - CONDUIT_COST);

        gameState.registerLink(conduit, gen, tower);

        assertEquals(0, gameState.getScrap(),
            "registerLink() spending the exact balance should leave 0 scrap");
    }

    // ---- registerLink: same wire type is a no-op ----

    @Test
    void registerLink_sameConductType_isNoOp_doesNotDeductScrap() {
        gameState.registerLink(conduit, gen, tower); // first placement
        int scrapAfterFirst = gameState.getScrap();

        gameState.registerLink(conduit, gen, tower); // repeat with same type

        assertEquals(scrapAfterFirst, gameState.getScrap(),
            "registerLink() with the exact same conduit type should be a no-op and not deduct scrap twice");
    }

    // ---- registerLink: replacing a different wire type ----

    @Test
    void registerLink_differentConduitType_refundsOldAndChargesNew() {
        Conduit cheapConduit = TestAssetFactory.createConduit(500, 0, 10, 20);
        Conduit expensiveConduit = TestAssetFactory.createConduit(1000, 0, 10, 60);

        gameState.registerLink(cheapConduit, gen, tower); // costs 20, balance = 80
        int scrapBeforeUpgrade = gameState.getScrap(); // 80

        gameState.registerLink(expensiveConduit, gen, tower); // refunds 20, charges 60, net -40

        // After replacing: scrap = 80 + 20 - 60 = 40
        assertEquals(scrapBeforeUpgrade + cheapConduit.getScrapCost() - expensiveConduit.getScrapCost(),
            gameState.getScrap(),
            "Replacing a conduit should refund the old cost and charge the new cost");
    }

    @Test
    void registerLink_differentConduitType_replacesConduit() {
        Conduit cheapConduit = TestAssetFactory.createConduit(500, 0, 10, 20);
        Conduit expensiveConduit = TestAssetFactory.createConduit(1000, 0, 10, 60);

        gameState.registerLink(cheapConduit, gen, tower);
        gameState.registerLink(expensiveConduit, gen, tower);

        ConduitEntity found = gameState.findConduit(gen, tower);
        assertNotNull(found, "A conduit should still exist after replacement");
        assertSame(expensiveConduit, found.conduit,
            "The conduit between the same endpoints should be the newly placed type after replacement");
    }

    // ---- registerLinkFree ----

    @Test
    void registerLinkFree_createsConduit() {
        gameState.registerLinkFree(conduit, gen, tower);

        assertNotNull(gameState.findConduit(gen, tower),
            "registerLinkFree() should create a conduit");
    }

    @Test
    void registerLinkFree_doesNotDeductScrap() {
        gameState.registerLinkFree(conduit, gen, tower);

        assertEquals(STARTING_SCRAP, gameState.getScrap(),
            "registerLinkFree() should not deduct any scrap");
    }

    @Test
    void registerLinkFree_withZeroBalance_stillCreatesConduit() {
        gameState.spendScrap(STARTING_SCRAP); // drain to 0

        gameState.registerLinkFree(conduit, gen, tower);

        assertNotNull(gameState.findConduit(gen, tower),
            "registerLinkFree() should create the conduit even when scrap balance is 0");
    }

    @Test
    void registerLinkFree_sameConductType_isNoOp() {
        gameState.registerLinkFree(conduit, gen, tower);
        gameState.registerLinkFree(conduit, gen, tower);

        // Still exactly one conduit between those endpoints
        assertNotNull(gameState.findConduit(gen, tower),
            "registerLinkFree() with the same conduit type a second time should not fail");
        assertEquals(STARTING_SCRAP, gameState.getScrap(),
            "registerLinkFree() no-op should leave the balance unchanged");
    }
}
