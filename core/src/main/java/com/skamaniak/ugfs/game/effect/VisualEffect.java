package com.skamaniak.ugfs.game.effect;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public abstract class VisualEffect {
    protected float elapsed;
    protected final float duration;
    protected boolean alive = true;

    protected VisualEffect(float duration) {
        this.duration = duration;
    }

    public void update(float delta) {
        elapsed += delta;
        if (elapsed >= duration) {
            alive = false;
        }
    }

    public float getProgress() {
        return Math.min(elapsed / duration, 1f);
    }

    public boolean isAlive() {
        return alive;
    }

    public void drawTextures(SpriteBatch batch) {
    }

    public void drawShapes(ShapeRenderer shapeRenderer) {
    }
}
