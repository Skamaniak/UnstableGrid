package com.skamaniak.ugfs.game.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.GameConstants;
import com.skamaniak.ugfs.asset.GameAssetManager;

public abstract class GameEntity implements Drawable {

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

    protected void drawEnergyLevel(SpriteBatch batch, float current, float max) { //FIXME remove, this is only for debugging
        GameAssetManager.INSTANCE.getFont()
            .draw(batch,
                (Math.round(current * 100 / max)) + "%",
                position.x * GameAssetManager.TILE_SIZE_PX + 4,
                position.y * GameAssetManager.TILE_SIZE_PX + 16);
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
