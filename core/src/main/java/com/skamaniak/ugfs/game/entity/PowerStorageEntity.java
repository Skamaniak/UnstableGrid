package com.skamaniak.ugfs.game.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.asset.GameAssetManager;
import com.skamaniak.ugfs.asset.model.PowerStorage;
import com.skamaniak.ugfs.simulation.PowerConsumer;
import com.skamaniak.ugfs.simulation.PowerProducer;

import java.util.HashSet;
import java.util.Set;

public class PowerStorageEntity extends GameEntity implements PowerConsumer, PowerProducer {

    private final PowerStorage storage;
    private int level = 1;

    private float powerBank = 0f;
    private boolean propagated = false;

    private final Set<PowerConsumer> to = new HashSet<>();

    public PowerStorageEntity(Vector2 position, PowerStorage storage) {
        super(position);
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

        PowerStorage.Level storageLevel = powerStorageLevel();
        if (!propagated) {
            // Simulate power loss (just once per propagation as the storage's consume method might be called from multiple conduits.
            powerBank = Math.max(powerBank - storageLevel.getPowerLossStandby() * delta, 0);
        }
        float usablePower = power + powerBank;
        float remainingPower = usablePower;
        for (PowerConsumer input : to) {
            remainingPower = input.consume(remainingPower, delta);
        }

        float powerSent = usablePower - remainingPower;
        float newPowerBankState = Math.min(remainingPower, storageLevel.getPowerStorage());
        float powerStored = newPowerBankState - powerBank;

        powerBank = Math.max(newPowerBankState, 0); // rounding errors may send this to negative numbers
        propagated = true;

        return power - (powerStored + powerSent);
    }

    public PowerStorage.Level powerStorageLevel() {
        return storage.getLevels().get(level - 1);
    }

    @Override
    public void resetPropagation() {
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

    @Override
    public void draw(SpriteBatch batch) {
        Texture texture = GameAssetManager.INSTANCE.loadTexture(storage.getTexture());
        batch.draw(texture,
            position.x * GameAssetManager.TILE_SIZE_PX,
            position.y * GameAssetManager.TILE_SIZE_PX,
            GameAssetManager.TILE_SIZE_PX,
            GameAssetManager.TILE_SIZE_PX);

        drawEnergyLevel(batch, powerBank, powerStorageLevel().getPowerStorage());
    }

    @Override
    public String toString() {
        return "PowerStorageEntity{" +
            "storage=" + storage +
            ", level=" + level +
            ", powerBank=" + powerBank +
            ", propagated=" + propagated +
            ", to=" + to +
            '}';
    }

    @Override
    public String getDetails() {
        int maxPower = powerStorageLevel().getPowerStorage();
        return storage.getName()
            + "\nLevel: " + level
            + "\nPosition: [" + (int) position.x + "," + (int) position.y + "]"
            + "\nMax capacity: " + maxPower
            + "\nStored: " + Math.round(powerBank * 100 / maxPower) + "% \t (" + Math.round(powerBank) + "/" + maxPower + ")";
    }
}

