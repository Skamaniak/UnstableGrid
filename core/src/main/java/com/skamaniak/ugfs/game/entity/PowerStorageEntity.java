package com.skamaniak.ugfs.game.entity;

import com.skamaniak.ugfs.asset.model.PowerStorage;
import com.skamaniak.ugfs.simulation.PowerConsumer;
import com.skamaniak.ugfs.simulation.PowerProducer;

import java.util.HashSet;
import java.util.Set;

public class PowerStorageEntity implements PowerConsumer, PowerProducer {

    private final PowerStorage storage;
    private int level = 1;

    private float powerTakenIn = 0f;
    private float powerSentOut = 0f;
    private float powerBank = 0f;
    private boolean propagated = false;

    private final Set<PowerConsumer> to = new HashSet<>();

    public PowerStorageEntity(PowerStorage storage) {
        this.storage = storage;
    }

    public void levelUp() {

    }

    @Override
    public boolean addTo(PowerConsumer consumer) {
        return to.add(consumer);
    }

    @Override
    public boolean removeTo(PowerConsumer consumer) {
        return to.remove(consumer);
    }

    @Override
    public float consume(float power, float delta) {
        propagated = true;
        PowerStorage.Level storageLevel = powerStorageLevel();
        float usablePower = powerBank + Math.min(power, storageLevel.getPowerIntakeRate() * delta - powerTakenIn);
        float transferablePower = Math.min(usablePower, storageLevel.getPowerOutputRate() * delta - powerSentOut);

        float powerToSend = transferablePower;
        for (PowerConsumer input : to) {
            if (powerToSend != 0) {
                powerToSend = input.consume(powerToSend, delta);
            }
        }

        float powerSent = transferablePower - powerToSend;
        powerSentOut += powerSent;

        usablePower -= powerSent;

        float newPowerBankState = Math.min(usablePower, storageLevel.getPowerStorage());
        float powerStored = newPowerBankState - powerBank;
        powerBank = newPowerBankState;
        powerTakenIn += powerStored + powerSent;

        return power - (powerStored + powerSent);
    }

    public PowerStorage.Level powerStorageLevel() {
        return storage.getLevels().get(level - 1);
    }

    @Override
    public void resetPropagation() {
        powerSentOut = 0f;
        powerTakenIn = 0f;
        propagated = false;

        for (PowerConsumer powerConsumer : to) {
            powerConsumer.resetPropagation();
        }
    }

    @Override
    public void produce(float delta) {
        if (!propagated) {
            consume(0, delta);
        }
    }
}

