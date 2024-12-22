package com.skamaniak.ugfs.game.entity;

import com.skamaniak.ugfs.asset.model.Tower;
import com.skamaniak.ugfs.simulation.PowerConsumer;

public class TowerEntity implements PowerConsumer {
    public final Tower tower;
    private int level = 1;

    private float powerTakenIn = 0f;
    private float powerBank = 0f;

    private float cumulativeDelta = 0;

    public TowerEntity(Tower tower) {
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
}
