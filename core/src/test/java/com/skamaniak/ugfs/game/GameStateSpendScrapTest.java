package com.skamaniak.ugfs.game;

import com.skamaniak.ugfs.asset.model.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for GameState.spendScrap() covering all edge cases described in the
 * end-to-end scrap mechanics spec: exact balance spend, overdraft prevention,
 * zero-cost spend, and addScrap interaction.
 *
 * GameState receives its starting scrap from the Level asset. UnstableGrid is
 * passed as null because it is only accessed by draw/sound methods that are
 * not exercised in these pure-logic tests.
 */
class GameStateSpendScrapTest {

    private static final int STARTING_SCRAP = 100;

    private GameState gameState;

    @BeforeEach
    void setUp() {
        Level level = mock(Level.class);
        when(level.getScrap()).thenReturn(STARTING_SCRAP);
        when(level.getMap()).thenReturn(Collections.emptyList());

        // UnstableGrid is only accessed in draw/sound paths — safe to pass null here.
        gameState = new GameState(null, level);
    }

    // ---- Initial state ----

    @Test
    void initialScrap_matchesLevelStartingScrap() {
        assertEquals(STARTING_SCRAP, gameState.getScrap(),
            "GameState should initialise scrap from Level.getScrap()");
    }

    // ---- spendScrap: success cases ----

    @Test
    void spendScrap_withAffordableAmount_returnsTrue() {
        boolean result = gameState.spendScrap(50);

        assertTrue(result, "spendScrap() should return true when balance is sufficient");
    }

    @Test
    void spendScrap_withAffordableAmount_deductsCorrectly() {
        gameState.spendScrap(50);

        assertEquals(50, gameState.getScrap(),
            "spendScrap() should deduct the spent amount from the balance");
    }

    @Test
    void spendScrap_withExactBalance_returnsTrue() {
        boolean result = gameState.spendScrap(STARTING_SCRAP);

        assertTrue(result, "spendScrap() should return true when spending exactly the full balance");
    }

    @Test
    void spendScrap_withExactBalance_leavesZeroBalance() {
        gameState.spendScrap(STARTING_SCRAP);

        assertEquals(0, gameState.getScrap(),
            "spendScrap() spending the exact balance should leave 0 scrap");
    }

    @Test
    void spendScrap_withZeroAmount_returnsTrue() {
        boolean result = gameState.spendScrap(0);

        assertTrue(result, "spendScrap(0) should always succeed");
    }

    @Test
    void spendScrap_withZeroAmount_doesNotChangeBalance() {
        gameState.spendScrap(0);

        assertEquals(STARTING_SCRAP, gameState.getScrap(),
            "spendScrap(0) should not change the scrap balance");
    }

    // ---- spendScrap: overdraft prevention ----

    @Test
    void spendScrap_withInsufficientBalance_returnsFalse() {
        boolean result = gameState.spendScrap(STARTING_SCRAP + 1);

        assertFalse(result, "spendScrap() should return false when balance is insufficient");
    }

    @Test
    void spendScrap_withInsufficientBalance_doesNotChangeBalance() {
        gameState.spendScrap(STARTING_SCRAP + 1);

        assertEquals(STARTING_SCRAP, gameState.getScrap(),
            "spendScrap() should leave balance unchanged when it fails due to overdraft");
    }

    @Test
    void spendScrap_withZeroBalanceAndNonZeroAmount_returnsFalse() {
        gameState.spendScrap(STARTING_SCRAP); // drain to 0
        boolean result = gameState.spendScrap(1);

        assertFalse(result, "spendScrap() should return false when balance is 0 and amount > 0");
    }

    @Test
    void spendScrap_withZeroBalanceAndNonZeroAmount_leavesZeroBalance() {
        gameState.spendScrap(STARTING_SCRAP); // drain to 0
        gameState.spendScrap(1);              // attempt overdraft

        assertEquals(0, gameState.getScrap(),
            "Failed spendScrap() should not push balance below 0");
    }

    // ---- addScrap / spendScrap interaction ----

    @Test
    void addScrap_increasesBalance() {
        gameState.addScrap(25);

        assertEquals(STARTING_SCRAP + 25, gameState.getScrap(),
            "addScrap() should increase the balance by the given amount");
    }

    @Test
    void addScrap_thenSpendScrap_allowsSpendingMoreThanStartingBalance() {
        gameState.addScrap(50);        // balance = 150
        boolean result = gameState.spendScrap(120); // would fail without addScrap

        assertTrue(result, "spendScrap() should succeed after addScrap() increases the balance");
        assertEquals(30, gameState.getScrap(),
            "Balance after spending should be addScrap amount minus excess spent");
    }

    @Test
    void spendScrap_afterFailedSpend_balanceIsUnchanged() {
        gameState.spendScrap(STARTING_SCRAP + 999); // fails
        boolean result = gameState.spendScrap(STARTING_SCRAP); // should still succeed

        assertTrue(result, "A failed spendScrap() should not corrupt the balance for subsequent calls");
        assertEquals(0, gameState.getScrap(),
            "Successful spend after a failed spend should use the original balance");
    }
}
