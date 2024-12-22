package com.skamaniak.ugfs.game.entity;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.asset.GameAssetManager;

public abstract class GameEntity implements Drawable {

    protected Vector2 position;

    protected GameEntity(Vector2 position) {
        this.position = position;
    }

    public Vector2 getPosition() {
        return position;
    }

    protected void drawEnergyLevel(SpriteBatch batch, float current, float max) { //FIXME remove, this is only for debugging
        GameAssetManager.INSTANCE.getFont()
            .draw(batch,
                (Math.floor(current * 100 / max)) + "%",
                position.x * GameAssetManager.TILE_SIZE_PX,
                position.y * GameAssetManager.TILE_SIZE_PX + 16);
    }
}
