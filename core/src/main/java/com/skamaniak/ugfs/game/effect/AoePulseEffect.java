package com.skamaniak.ugfs.game.effect;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class AoePulseEffect extends VisualEffect {
    private static final float DURATION = 0.3f;

    private final float centerX, centerY;
    private final float maxRadius;
    private final Color color;

    public AoePulseEffect(float centerX, float centerY, float maxRadius, Color baseColor) {
        super(DURATION);
        this.centerX = centerX;
        this.centerY = centerY;
        this.maxRadius = maxRadius;
        this.color = new Color(baseColor);
    }

    @Override
    public void drawShapes(ShapeRenderer shapeRenderer) {
        float progress = getProgress();
        float radius = maxRadius * progress;
        float alpha = 0.3f * (1f - progress);
        color.a = alpha;
        shapeRenderer.setColor(color);
        shapeRenderer.circle(centerX, centerY, radius, 32);
    }
}
