package com.skamaniak.ugfs.game.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.asset.GameAssetManager;
import com.skamaniak.ugfs.asset.model.Tower;
import com.skamaniak.ugfs.simulation.PowerConsumer;

public class TowerEntity extends GameEntity implements PowerConsumer {
    public final Tower tower;
    private int level = 1;

    private float powerTakenIn = 0f;
    private float powerBank = 0f;

    private float cumulativeDelta = 0;

    public TowerEntity(Vector2 position, Tower tower) {
        super(position);
        this.tower = tower;
    }

    @Override
    public float consume(float power, float delta) {
        Tower.Level towerLevel = towerLevel();
        float usablePower = powerBank + Math.min(power, towerLevel.getPowerIntakeRate() * delta - powerTakenIn);

        float newPowerBankState = Math.min(usablePower, towerLevel.getPowerStorage());
        float powerStored = newPowerBankState - powerBank;
        powerBank = newPowerBankState;
        powerTakenIn += powerStored;

        return power - powerStored;
    }

    private Tower.Level towerLevel() {
        return tower.getLevels().get(level - 1);
    }

    @Override
    public void resetPropagation() {
        powerTakenIn = 0f;
    }

    public boolean attemptShot(float delta) { //TODO what if delta is some huge number?
        cumulativeDelta += delta;

        Tower.Level level = towerLevel();
        float timeBetweenShots = 1f / level.getFireRate();
        if (cumulativeDelta >= timeBetweenShots) {

            if (powerBank >= level.getPowerCostShot()) {
                powerBank -= level.getPowerCostShot();
                cumulativeDelta -= timeBetweenShots;
                shoot();
                return true;
            } else {
                // Not enough power to shoot. Do not cumulate delta further but be ready to shoot next time if power permits.
                cumulativeDelta = timeBetweenShots;
            }
        }
        return false;
    }

    private void shoot() {
        // TODO target enemy and cause damage
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
            ", powerTakenIn=" + powerTakenIn +
            ", powerBank=" + powerBank +
            ", cumulativeDelta=" + cumulativeDelta +
            '}';
    }

    @Override
    public String getDetails() {
        int maxPower = towerLevel().getPowerStorage();
        return tower.getName()
            + "\nLevel: " + level
            + "\nPosition: [" + (int) position.x + "," + (int) position.y + "]"
            + "\nMax capacity: " + maxPower
            + "\nStored: " + Math.round(powerBank * 100 / maxPower) + "% \t (" + Math.round(powerBank) + "/" + maxPower + ")";
    }
}
