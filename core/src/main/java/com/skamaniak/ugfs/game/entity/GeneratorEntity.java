package com.skamaniak.ugfs.game.entity;

import com.skamaniak.ugfs.asset.model.Generator;
import com.skamaniak.ugfs.simulation.PowerConsumer;
import com.skamaniak.ugfs.simulation.PowerProducer;

import java.util.HashSet;
import java.util.Set;

public class GeneratorEntity implements PowerProducer {

    private Generator generator;
    private int level = 1;
    private float powerBank = 0f;

    private final Set<PowerConsumer> to = new HashSet<>();

    public GeneratorEntity(Generator generator) {
        this.generator = generator;
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
    public void resetPropagation() {
        for (PowerConsumer powerConsumer : to) {
            powerConsumer.resetPropagation();
        }
    }

    @Override
    public void produce(float delta) {
        updatePowerStorage(delta);

        for (PowerConsumer powerConsumer : to) {
            if (powerBank > 0) {
                powerBank = powerConsumer.consume(powerBank, delta);
            }
        }
    }

    private void updatePowerStorage(float delta) {
        Generator.Level generatorLevel = generatorLevel();
        float newPower = powerBank + (generatorLevel.getPowerGenerationRate() * delta);
        this.powerBank = Math.min(newPower, generatorLevel.getPowerStorage());
    }

    private Generator.Level generatorLevel() {
        return generator.getLevels().get(level - 1);
    }
}
