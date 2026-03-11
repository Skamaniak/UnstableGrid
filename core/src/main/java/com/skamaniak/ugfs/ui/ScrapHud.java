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

public class ScrapHud {
    private final Stage stage;
    private final Viewport viewport;
    private final Label scrapLabel;
    private final GameState gameState;
    private int lastDisplayedScrap;

    public ScrapHud(SpriteBatch batch, GameState gameState) {
        this.gameState = gameState;
        this.lastDisplayedScrap = gameState.getScrap();
        this.viewport = new ScreenViewport();
        this.stage = new Stage(this.viewport, batch);
        Skin skin = GameAssetManager.INSTANCE.getSkin();

        scrapLabel = new Label("Scrap: " + lastDisplayedScrap, skin);
        scrapLabel.setAlignment(Align.topLeft);
        scrapLabel.setPosition(10, viewport.getWorldHeight() - 30);
        stage.addActor(scrapLabel);
    }

    public void handleInput() {
        stage.act();
        int currentScrap = gameState.getScrap();
        if (currentScrap != lastDisplayedScrap) {
            scrapLabel.setText("Scrap: " + currentScrap);
            scrapLabel.clearActions();
            if (currentScrap > lastDisplayedScrap) {
                scrapLabel.addAction(Actions.sequence(
                    Actions.color(Color.GREEN),
                    Actions.delay(0.1f),
                    Actions.color(Color.WHITE, 0.5f)
                ));
            } else {
                scrapLabel.addAction(Actions.sequence(
                    Actions.color(Color.RED),
                    Actions.delay(0.1f),
                    Actions.color(Color.WHITE, 0.5f)
                ));
            }
            lastDisplayedScrap = currentScrap;
        }
    }

    public void draw() {
        viewport.apply();
        stage.draw();
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
        scrapLabel.setPosition(10, viewport.getWorldHeight() - 30);
    }

    public void dispose() {
        stage.dispose();
    }

    public Stage getStage() {
        return stage;
    }
}
