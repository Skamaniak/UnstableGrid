package com.skamaniak.ugfs.game.effect;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.skamaniak.ugfs.game.entity.EnemyInstance;

import java.util.List;

public class PlasmaProjectileEffect extends VisualEffect {
    private static final float SPEED = 400f;
    private static final float RADIUS = 6f;

    private final float startX, startY;
    private final EnemyInstance targetEnemy;
    private final int damage;
    private final List<VisualEffect> pendingEffects;
    private float currentX, currentY;
    private float lastKnownTargetX, lastKnownTargetY;

    public PlasmaProjectileEffect(float startX, float startY, EnemyInstance targetEnemy,
                                  int damage, List<VisualEffect> pendingEffects) {
        // Duration is an upper bound; we kill it on arrival
        super(10f);
        this.startX = startX;
        this.startY = startY;
        this.currentX = startX;
        this.currentY = startY;
        this.targetEnemy = targetEnemy;
        this.damage = damage;
        this.pendingEffects = pendingEffects;
        this.lastKnownTargetX = targetEnemy.getWorldCenter().x;
        this.lastKnownTargetY = targetEnemy.getWorldCenter().y;
    }

    @Override
    public void update(float delta) {
        if (!alive) {
            return;
        }

        // Track the enemy if still alive
        if (targetEnemy.isAlive()) {
            lastKnownTargetX = targetEnemy.getWorldCenter().x;
            lastKnownTargetY = targetEnemy.getWorldCenter().y;
        }

        float dx = lastKnownTargetX - currentX;
        float dy = lastKnownTargetY - currentY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        float step = SPEED * delta;
        if (dist <= step) {
            // Arrived
            currentX = lastKnownTargetX;
            currentY = lastKnownTargetY;
            if (targetEnemy.isAlive()) {
                targetEnemy.takeDamage(damage);
            }
            pendingEffects.add(new ImpactEffect(currentX, currentY));
            alive = false;
        } else {
            float ratio = step / dist;
            currentX += dx * ratio;
            currentY += dy * ratio;
        }
    }

    @Override
    public void drawShapes(ShapeRenderer shapeRenderer) {
        shapeRenderer.setColor(0.5f, 1f, 0.3f, 0.9f);
        shapeRenderer.circle(currentX, currentY, RADIUS);
        // Glow
        shapeRenderer.setColor(0.8f, 1f, 0.2f, 0.4f);
        shapeRenderer.circle(currentX, currentY, RADIUS * 1.5f);
    }
}
