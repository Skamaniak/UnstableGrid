package com.skamaniak.ugfs.game.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.GameConstants;
import com.skamaniak.ugfs.asset.GameAssetManager;
import com.skamaniak.ugfs.asset.model.Tower;
import com.skamaniak.ugfs.simulation.PowerConsumer;

import java.util.List;

public class TowerEntity extends GameEntity implements PowerConsumer {
    public final Tower tower;
    private int level = 1;

    private float powerBank = 0f;
    private float cumulativeDelta = 0;
    private boolean propagated = false;
    private final ShotResult shotResult = new ShotResult();

    public TowerEntity(Vector2 position, Tower tower) {
        super(position);
        this.tower = tower;
        this.totalScrapInvested = towerLevel().getScrapCost();
    }

    @Override
    public float consume(float power, float delta) {
        Tower.Level towerLevel = towerLevel();

        if (!propagated) {
            // Simulate power loss (just once per propagation as the storage's consume method might be called from multiple conduits.
            powerBank = Math.max(powerBank - towerLevel.getPowerLossStandby() * delta, 0);
        }

        float newPowerBankState = Math.min(powerBank + power, towerLevel.getPowerStorage());
        float powerStored = newPowerBankState - powerBank;
        powerBank = newPowerBankState;

        propagated = true;
        return power - powerStored;
    }

    public float getPowerBank() {
        return powerBank;
    }

    private Tower.Level towerLevel() {
        return tower.getLevels().get(level - 1);
    }

    @Override
    public void resetPropagation() {
        propagated = false;
    }

    public ShotResult getShotResult() {
        return shotResult;
    }

    public boolean attemptShot(float delta, List<EnemyInstance> enemies) {
        cumulativeDelta += delta;

        Tower.Level level = towerLevel();
        if (level.getFireRate() <= 0) {
            return false;
        }
        float timeBetweenShots = 1f / level.getFireRate();
        if (cumulativeDelta >= timeBetweenShots) {
            if (powerBank >= level.getPowerCostShot() && shoot(enemies, level)) {
                powerBank -= level.getPowerCostShot();
                cumulativeDelta -= timeBetweenShots;
                return true;
            } else {
                // Not enough power or no target — cap delta to avoid burst firing when a target appears.
                cumulativeDelta = timeBetweenShots;
            }
        }
        return false;
    }

    private boolean shoot(List<EnemyInstance> enemies, Tower.Level level) {
        float rangePx = level.getTowerRange() * GameConstants.TILE_SIZE_PX;
        float towerCenterX = (position.x + 0.5f) * GameConstants.TILE_SIZE_PX;
        float towerCenterY = (position.y + 0.5f) * GameConstants.TILE_SIZE_PX;

        shotResult.reset();
        shotResult.towerX = towerCenterX;
        shotResult.towerY = towerCenterY;
        shotResult.rangePx = rangePx;
        shotResult.damage = level.getDamage();

        String targeting = tower.getTargeting();
        shotResult.targeting = targeting != null ? targeting : "single";

        if ("aoe".equals(targeting)) {
            return shootAoe(enemies, level, rangePx, towerCenterX, towerCenterY);
        } else {
            return shootSingle(enemies, level, rangePx, towerCenterX, towerCenterY);
        }
    }

    private boolean shootSingle(List<EnemyInstance> enemies, Tower.Level level,
                                float rangePx, float towerCenterX, float towerCenterY) {
        EnemyInstance closest = null;
        float closestDistSq = rangePx * rangePx;
        float closestX = 0, closestY = 0;

        boolean canTargetFlying = tower.isCanTargetFlying();
        for (int i = 0, n = enemies.size(); i < n; i++) {
            EnemyInstance enemy = enemies.get(i);
            if (!enemy.isAlive()) {
                continue;
            }
            if (!canTargetFlying && enemy.isFlying()) {
                continue;
            }
            Vector2 enemyPos = enemy.getWorldCenter();
            float dx = enemyPos.x - towerCenterX;
            float dy = enemyPos.y - towerCenterY;
            float distSq = dx * dx + dy * dy;
            if (distSq <= closestDistSq) {
                closestDistSq = distSq;
                closest = enemy;
                closestX = enemyPos.x;
                closestY = enemyPos.y;
            }
        }

        if (closest != null) {
            shotResult.fired = true;
            shotResult.targetEnemy = closest;
            shotResult.targetX = closestX;
            shotResult.targetY = closestY;
            if (!tower.isDeferDamage()) {
                closest.takeDamage(level.getDamage());
            }
            return true;
        }
        return false;
    }

    private boolean shootAoe(List<EnemyInstance> enemies, Tower.Level level,
                             float rangePx, float towerCenterX, float towerCenterY) {
        float rangeSq = rangePx * rangePx;
        int hitCount = 0;
        boolean canTargetFlying = tower.isCanTargetFlying();

        for (int i = 0, n = enemies.size(); i < n; i++) {
            EnemyInstance enemy = enemies.get(i);
            if (!enemy.isAlive()) {
                continue;
            }
            if (!canTargetFlying && enemy.isFlying()) {
                continue;
            }
            Vector2 enemyPos = enemy.getWorldCenter();
            float dx = enemyPos.x - towerCenterX;
            float dy = enemyPos.y - towerCenterY;
            float distSq = dx * dx + dy * dy;
            if (distSq <= rangeSq) {
                enemy.takeDamage(level.getDamage());
                if (shotResult.aoeTargetCount < shotResult.aoeTargets.length) {
                    shotResult.aoeTargets[shotResult.aoeTargetCount++] = enemy;
                }
                hitCount++;
            }
        }

        if (hitCount > 0) {
            shotResult.fired = true;
            shotResult.targetX = towerCenterX;
            shotResult.targetY = towerCenterY;
            return true;
        }
        return false;
    }

    @Override
    public void draw(SpriteBatch batch) {
        Texture texture = GameAssetManager.INSTANCE.loadTexture(tower.getTexture());
        batch.draw(texture,
            position.x * GameAssetManager.TILE_SIZE_PX,
            position.y * GameAssetManager.TILE_SIZE_PX,
            GameAssetManager.TILE_SIZE_PX,
            GameAssetManager.TILE_SIZE_PX);

        drawEnergyLevel(batch, powerBank, towerLevel().getPowerStorage());
    }

    @Override
    public String toString() {
        return "TowerEntity{" +
            "tower=" + tower +
            ", level=" + level +
            ", powerBank=" + powerBank +
            ", cumulativeDelta=" + cumulativeDelta +
            '}';
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public int getMaxLevel() {
        return tower.getLevels().size();
    }

    @Override
    public boolean canUpgrade() {
        return level < getMaxLevel();
    }

    @Override
    public int getUpgradeCost() {
        return canUpgrade() ? tower.getLevels().get(level).getScrapCost() : -1;
    }

    @Override
    public void applyUpgrade() {
        int cost = getUpgradeCost();
        level++;
        totalScrapInvested += cost;
    }

    @Override
    public int getScrapCost() {
        return towerLevel().getScrapCost();
    }

    @Override
    public String getDetails() {
        Tower.Level level = towerLevel();
        int maxPower = level.getPowerStorage();
        return tower.getName()
            + "\nLevel: " + this.level
            + "\nPosition: [" + (int) position.x + "," + (int) position.y + "]"
            + "\nMax capacity: " + maxPower
            + "\nStored: " + Math.round(powerBank * 100 / maxPower) + "% \t (" + Math.round(powerBank) + "/" + maxPower + ")"
            + "\nDamage: " + level.getDamage()
            + "\nFire rate: " + level.getFireRate()
            + "\nDPS: " + level.getDamage() * level.getFireRate();
    }
}
