package com.skamaniak.ugfs.game.entity;

import com.skamaniak.ugfs.asset.model.Conduit;
import com.skamaniak.ugfs.simulation.PowerConsumer;
import com.skamaniak.ugfs.simulation.PowerSource;

import java.util.HashSet;
import java.util.Set;

public class ConduitEntity implements PowerConsumer, PowerSource {

    private final Conduit conduit;
    private final Set<PowerConsumer> to = new HashSet<>();

    private float propagatedPower = 0f;

    public ConduitEntity(Conduit conduit) {
        this.conduit = conduit;
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
        float transferablePower = Math.min(power, conduit.getPowerTransferRate() * delta - propagatedPower);
        float powerToSend = transferablePower;
        for (PowerConsumer input : to) {
            if (powerToSend != 0) {
                powerToSend = input.consume(powerToSend, delta);
            }
        }
        float powerSent = transferablePower - powerToSend;
        propagatedPower += powerSent;

        return power - powerSent;
    }

    @Override
    public void resetPropagation() {
        propagatedPower = 0f;

        for (PowerConsumer powerConsumer : to) {
            powerConsumer.resetPropagation();
        }
    }
}

