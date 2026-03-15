package com.skamaniak.ugfs.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.skamaniak.ugfs.asset.GameAssetManager;
import com.skamaniak.ugfs.game.GameState;
import com.skamaniak.ugfs.game.WaveManager;

public class WaveHud {
    private final Stage stage;
    private final Viewport viewport;
    private final Label waveInfoLabel;
    private final Label flashLabel;
    private final GameState gameState;
    private boolean previousWaveActive;

    public WaveHud(SpriteBatch batch, GameState gameState) {
        this.gameState = gameState;
        this.viewport = new ScreenViewport();
        this.stage = new Stage(this.viewport, batch);
        Skin skin = GameAssetManager.INSTANCE.getSkin();

        waveInfoLabel = new Label("", skin);
        waveInfoLabel.setAlignment(Align.center);
        stage.addActor(waveInfoLabel);

        flashLabel = new Label("", skin);
        flashLabel.setAlignment(Align.center);
        flashLabel.getColor().a = 0f;
        stage.addActor(flashLabel);

        this.previousWaveActive = false;
    }

    public void handleInput() {
        float delta = com.badlogic.gdx.Gdx.graphics.getDeltaTime();
        stage.act(delta);

        WaveManager.WaveStatus status = gameState.getWaveStatus();
        if (status == null) {
            waveInfoLabel.setText("");
            return;
        }

        int aliveCount = gameState.getAliveEnemyCount();

        boolean currentWaveActive = status.isWaveActive();
        if (currentWaveActive && !previousWaveActive) {
            showFlash("Enemies approaching!\nBuilding is disabled", Color.RED);
        } else if (!currentWaveActive && previousWaveActive) {
            showFlash("Wave cleared!\nBuilding is enabled", Color.GREEN);
        }
        previousWaveActive = currentWaveActive;

        if (status.isAllWavesExhausted() && aliveCount == 0) {
            waveInfoLabel.setText("All waves cleared!");
        } else if (status.isAllWavesExhausted()) {
            waveInfoLabel.setText("Final wave -- ACTIVE\nEnemies: " + aliveCount + " alive");
        } else if (status.isWaveActive()) {
            waveInfoLabel.setText("Wave " + status.getCurrentWaveNumber() + " of " + status.getTotalWaves() + " -- ACTIVE\n"
                + "Enemies: " + aliveCount + " alive, " + status.getPendingSpawnCount() + " spawning");
        } else {
            int displayWave = status.getCurrentWaveNumber() + 1;
            waveInfoLabel.setText("Next wave in: " + (int) Math.ceil(status.getCountdown()) + "s\n"
                + "Wave " + displayWave + " of " + status.getTotalWaves() + "\n"
                + "Enemies: " + aliveCount);
        }
        waveInfoLabel.pack();

        positionLabels();
    }

    private void showFlash(String text, Color color) {
        flashLabel.clearActions();
        flashLabel.setText(text);
        flashLabel.pack();
        flashLabel.setColor(color);
        flashLabel.getColor().a = 0f;
        flashLabel.addAction(Actions.sequence(
            Actions.fadeIn(0.3f),
            Actions.delay(1.5f),
            Actions.fadeOut(0.8f)
        ));
    }

    private void positionLabels() {
        float centerX = viewport.getWorldWidth() / 2f;
        float topY = viewport.getWorldHeight() - 10;

        waveInfoLabel.setPosition(centerX - waveInfoLabel.getWidth() / 2f,
            topY - waveInfoLabel.getHeight());

        flashLabel.setPosition(centerX - flashLabel.getWidth() / 2f,
            topY - waveInfoLabel.getHeight() - flashLabel.getHeight() - 5);
    }

    public void draw() {
        viewport.apply();
        stage.draw();
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
        positionLabels();
    }

    public void dispose() {
        stage.dispose();
    }

    public Stage getStage() {
        return stage;
    }
}
