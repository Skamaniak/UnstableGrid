package com.skamaniak.ugfs.game.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.GameConstants;

public abstract class GameEntity implements Drawable {

    private static final float ENERGY_BAR_WIDTH = 52f;
    private static final float ENERGY_BAR_HEIGHT = 5f;
    private static final float ENERGY_BAR_X_OFFSET = 6f;
    private static final float ENERGY_BAR_Y_OFFSET = 2f;

    private static final Color CHEVRON_COLOR = new Color(1f, 0.84f, 0f, 1f);
    private static final float CHEVRON_WIDTH = 12f;
    private static final float CHEVRON_HEIGHT = 6f;
    private static final float CHEVRON_LINE_WIDTH = 1.5f;

    protected Vector2 position;
    protected int totalScrapInvested;

    protected GameEntity(Vector2 position) {
        this.position = position;
    }

    public Vector2 getPosition() {
        return position;
    }

    protected void drawEnergyBar(ShapeRenderer shapeRenderer, float currentPower, float maxPower) {
        if (maxPower <= 0) {
            return;
        }

        float fraction = currentPower / maxPower;
        fraction = Math.max(0f, Math.min(fraction, 1f));

        float barX = position.x * GameConstants.TILE_SIZE_PX + ENERGY_BAR_X_OFFSET;
        float barY = position.y * GameConstants.TILE_SIZE_PX + ENERGY_BAR_Y_OFFSET;

        // Background (dark gray, full width)
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(barX, barY, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT);

        // Foreground (colored by fill fraction, fills from left)
        float r, g;
        if (fraction <= 0.5f) {
            r = 1f;
            g = fraction * 2f;
        } else {
            r = 1f - (fraction - 0.5f) * 2f;
            g = 1f;
        }
        shapeRenderer.setColor(r, g, 0f, 1f);
        shapeRenderer.rect(barX, barY, ENERGY_BAR_WIDTH * fraction, ENERGY_BAR_HEIGHT);
    }

    public int getTotalScrapInvested() {
        return totalScrapInvested;
    }

    public abstract int getLevel();

    public abstract int getMaxLevel();

    public abstract boolean canUpgrade();

    public abstract int getUpgradeCost();

    public abstract void applyUpgrade();

    public abstract String getDetails();

    public abstract int getScrapCost();

    public void drawChevrons(ShapeRenderer shapeRenderer) {
        int chevronCount = getLevel() - 1;
        if (chevronCount <= 0) {
            return;
        }

        float tileX = position.x * GameConstants.TILE_SIZE_PX;
        float tileY = position.y * GameConstants.TILE_SIZE_PX;

        float quadrantCenterX = tileX + GameConstants.TILE_SIZE_PX * 0.75f;
        float quadrantTop = tileY + GameConstants.TILE_SIZE_PX;
        float totalHeight = chevronCount * CHEVRON_HEIGHT + (chevronCount - 1) * 2f;
        float startY = quadrantTop - (GameConstants.TILE_SIZE_PX * 0.5f - totalHeight) / 2f - CHEVRON_HEIGHT;

        shapeRenderer.setColor(CHEVRON_COLOR);
        for (int i = 0; i < chevronCount; i++) {
            float chevronY = startY - i * (CHEVRON_HEIGHT + 2f);
            float leftX = quadrantCenterX - CHEVRON_WIDTH / 2f;
            float rightX = quadrantCenterX + CHEVRON_WIDTH / 2f;
            float peakY = chevronY + CHEVRON_HEIGHT;

            shapeRenderer.rectLine(leftX, chevronY, quadrantCenterX, peakY, CHEVRON_LINE_WIDTH);
            shapeRenderer.rectLine(quadrantCenterX, peakY, rightX, chevronY, CHEVRON_LINE_WIDTH);
        }
    }
}
