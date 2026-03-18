package com.skamaniak.ugfs.game.effect;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class LaserBeamEffect extends VisualEffect {
    private static final float DURATION = 0.15f;

    private final float x1, y1, x2, y2;
    private final Color color = new Color(1f, 0.2f, 0.1f, 1f);

    public LaserBeamEffect(float x1, float y1, float x2, float y2) {
        super(DURATION);
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    @Override
    public void drawShapes(ShapeRenderer shapeRenderer) {
        float alpha = 1f - getProgress();
        color.a = alpha;
        shapeRenderer.setColor(color);
        shapeRenderer.rectLine(x1, y1, x2, y2, 3f * alpha + 1f);
    }
}
