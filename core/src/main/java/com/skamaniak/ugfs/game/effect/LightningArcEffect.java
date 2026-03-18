package com.skamaniak.ugfs.game.effect;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.Random;

public class LightningArcEffect extends VisualEffect {
    private static final float DURATION = 0.2f;
    private static final int SEGMENTS = 8;

    private final float[] pointsX;
    private final float[] pointsY;
    private final Color color = new Color(0.4f, 0.9f, 1f, 1f);

    public LightningArcEffect(float x1, float y1, float x2, float y2, Random random) {
        super(DURATION);
        pointsX = new float[SEGMENTS + 1];
        pointsY = new float[SEGMENTS + 1];

        pointsX[0] = x1;
        pointsY[0] = y1;
        pointsX[SEGMENTS] = x2;
        pointsY[SEGMENTS] = y2;

        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        for (int i = 1; i < SEGMENTS; i++) {
            float t = (float) i / SEGMENTS;
            if (length < 0.001f) {
                pointsX[i] = x1;
                pointsY[i] = y1;
            } else {
                float perpX = -dy / length;
                float perpY = dx / length;
                float jitter = length * 0.1f;
                float offset = (random.nextFloat() * 2f - 1f) * jitter;
                pointsX[i] = x1 + dx * t + perpX * offset;
                pointsY[i] = y1 + dy * t + perpY * offset;
            }
        }
    }

    @Override
    public void drawShapes(ShapeRenderer shapeRenderer) {
        float alpha = 1f - getProgress();
        color.a = alpha;
        shapeRenderer.setColor(color);
        for (int i = 0; i < SEGMENTS; i++) {
            shapeRenderer.rectLine(pointsX[i], pointsY[i], pointsX[i + 1], pointsY[i + 1], 2f);
        }
    }
}
