package com.skamaniak.ugfs.game.entity;

import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.GameConstants;
import com.skamaniak.ugfs.asset.model.Enemy;

import java.util.List;

public class EnemyInstance {
    private final Enemy enemy;
    private final float maxHealth;
    private float currentHealth;
    private final Vector2 worldPosition;
    private List<Vector2> path;
    private int pathIndex;
    private boolean alive;
    private boolean reachedBase;

    public EnemyInstance(Enemy enemy, Vector2 spawnWorldPosition, List<Vector2> path) {
        this.enemy = enemy;
        this.maxHealth = enemy.getHealth();
        this.currentHealth = maxHealth;
        this.worldPosition = new Vector2(spawnWorldPosition);
        this.path = path;
        this.pathIndex = 0;
        this.alive = true;
        this.reachedBase = false;
    }

    public void move(float delta) {
        if (!alive || reachedBase || path == null || path.isEmpty()) {
            return;
        }

        float speed = enemy.getSpeed() * GameConstants.TILE_SIZE_PX;
        float remaining = speed * delta;

        while (remaining > 0 && pathIndex < path.size()) {
            Vector2 target = path.get(pathIndex);
            float dx = target.x - worldPosition.x;
            float dy = target.y - worldPosition.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (dist <= remaining) {
                worldPosition.set(target);
                remaining -= dist;
                pathIndex++;
            } else {
                float ratio = remaining / dist;
                worldPosition.x += dx * ratio;
                worldPosition.y += dy * ratio;
                remaining = 0;
            }
        }

        if (pathIndex >= path.size()) {
            reachedBase = true;
        }
    }

    public void takeDamage(int amount) {
        currentHealth -= amount;
        if (currentHealth <= 0) {
            currentHealth = 0;
            alive = false;
        }
    }

    public boolean hasReachedBase() {
        return reachedBase;
    }

    public boolean isAlive() {
        return alive;
    }

    public Vector2 getWorldCenter() {
        return worldPosition;
    }

    public float getHealthFraction() {
        return currentHealth / maxHealth;
    }

    public Enemy getEnemy() {
        return enemy;
    }

    public Vector2 getWorldPosition() {
        return worldPosition;
    }

    public void repath(List<Vector2> newPath) {
        if (newPath == null) {
            return;
        }
        this.path = newPath;
        this.pathIndex = 0;
        if (!newPath.isEmpty()) {
            newPath.set(0, new Vector2(worldPosition));
        }
    }

    public boolean isFlying() {
        return enemy.isFlying();
    }
}
