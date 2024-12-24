package com.skamaniak.ugfs.game.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.UnstableGrid;
import com.skamaniak.ugfs.asset.GameAssetManager;
import com.skamaniak.ugfs.asset.model.Generator;
import com.skamaniak.ugfs.simulation.PowerConsumer;
import com.skamaniak.ugfs.simulation.PowerProducer;

import java.util.HashSet;
import java.util.Set;

public class GeneratorEntity extends GameEntity implements PowerProducer {

    private Generator generator;
    private int level = 1;
    private float powerBank = 0f;

    private final Set<PowerConsumer> to = new HashSet<>();

    public GeneratorEntity(Vector2 position, Generator generator) {
        super(position);
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
        powerBank = Math.max(powerBank, 0); // rounding errors may send this to negative numbers
    }

    private void updatePowerStorage(float delta) {
        Generator.Level generatorLevel = generatorLevel();
        float newPower = powerBank + (generatorLevel.getPowerGenerationRate() * delta);
        this.powerBank = Math.min(newPower, generatorLevel.getPowerStorage());
    }

    private Generator.Level generatorLevel() {
        return generator.getLevels().get(level - 1);
    }

    @Override
    public void draw(SpriteBatch batch) {
        Texture texture = GameAssetManager.INSTANCE.loadTexture(generator.getTexture());
        batch.draw(texture,
                position.x * GameAssetManager.TILE_SIZE_PX,
                position.y * GameAssetManager.TILE_SIZE_PX,
                GameAssetManager.TILE_SIZE_PX,
                GameAssetManager.TILE_SIZE_PX);

        drawEnergyLevel(batch, powerBank, generatorLevel().getPowerStorage());
    }

    @Override
    public String toString() {
        return "GeneratorEntity{" +
                "generator=" + generator +
                ", level=" + level +
                ", powerBank=" + powerBank +
                ", to=" + to +
                '}';
    }
}
