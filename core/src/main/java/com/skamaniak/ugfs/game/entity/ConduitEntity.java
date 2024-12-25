package com.skamaniak.ugfs.game.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.asset.GameAssetManager;
import com.skamaniak.ugfs.asset.model.Conduit;
import com.skamaniak.ugfs.simulation.PowerConsumer;
import com.skamaniak.ugfs.simulation.PowerSource;

import java.util.Objects;

public class ConduitEntity implements PowerConsumer, Drawable {
    public Conduit conduit;
    public PowerSource from;
    public PowerConsumer to;

    private float powerTransferred = 0;

    public ConduitEntity(Conduit conduit, PowerSource from, PowerConsumer to) {
        this.conduit = conduit;
        this.from = from;
        this.to = to;
    }

    public void register() {
        this.from.addTo(this);
    }

    public void unregister() {
        this.from.removeTo(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConduitEntity that = (ConduitEntity) o;
        return Objects.equals(conduit, that.conduit) && Objects.equals(from, that.from) && Objects.equals(to, that.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conduit, from, to);
    }

    @Override
    public void resetPropagation() {
        powerTransferred = 0f;
        to.resetPropagation();
    }

    @Override
    public float consume(float power, float delta) {
        power = Math.max(power - conduit.getPowerTransferLoss() * delta, 0);

        float transferablePower = Math.min(power, conduit.getPowerTransferRate() * delta - powerTransferred);
        float powerLeft = to.consume(transferablePower, delta);
        return power - (transferablePower - powerLeft);
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (from instanceof GameEntity && to instanceof GameEntity) {
            Vector2 fromPosition = ((GameEntity) from).position;
            Vector2 toPosition = ((GameEntity) to).position;
            drawLine(batch,
                fromPosition.x * GameAssetManager.TILE_SIZE_PX + GameAssetManager.TILE_SIZE_PX / 2,
                fromPosition.y * GameAssetManager.TILE_SIZE_PX + GameAssetManager.TILE_SIZE_PX / 2,
                toPosition.x * GameAssetManager.TILE_SIZE_PX + GameAssetManager.TILE_SIZE_PX / 2,
                toPosition.y * GameAssetManager.TILE_SIZE_PX + GameAssetManager.TILE_SIZE_PX / 2);
        }
    }

    public void drawLine(SpriteBatch batch, float x1, float y1, float x2, float y2) { //FIXME this is just ugly, nasty. no excuse
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy); // Calculate the line's length
        float angle = (float) Math.atan2(dy, dx) * (180 / (float) Math.PI); // Calculate the angle in degrees
        float thickness = conduit.getLineThickness();

        // Adjust the texture region's width in UV space to repeat the texture
        TextureRegion textureRegion = GameAssetManager.INSTANCE.loadRepeatingTexture(conduit.getTexture());
        Texture texture = textureRegion.getTexture();
        textureRegion.setRegion(0, 0, (int) (length / texture.getWidth()), texture.getHeight());

        batch.draw(textureRegion,
            x1, y1,                     // Starting position
            0, thickness / 2,           // Origin for rotation (middle-left of the texture)
            length, thickness,          // Width and Height of the line
            1, 1,                       // Scale
            angle                       // Rotation angle
        );
    }
}

