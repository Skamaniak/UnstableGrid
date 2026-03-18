package com.skamaniak.ugfs.game.effect;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class ImpactEffect extends VisualEffect {
    private static final float DURATION = 0.1f;
    private static final float MAX_RADIUS = 8f;

    private final float x, y;
    private final Color color = new Color(Color.WHITE);

    public ImpactEffect(float x, float y) {
        super(DURATION);
        this.x = x;
        this.y = y;
    }

    @Override
    public void drawShapes(ShapeRenderer shapeRenderer) {
        float progress = getProgress();
        float radius = MAX_RADIUS * (1f - progress);
        color.a = 1f - progress;
        shapeRenderer.setColor(color);
        shapeRenderer.circle(x, y, radius);
    }
}
