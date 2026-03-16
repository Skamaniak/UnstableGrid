package com.skamaniak.ugfs.ui;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.skamaniak.ugfs.asset.GameAssetManager;

public class WireOverlayButton {
    private final Stage stage;
    private final Viewport viewport;
    private final TextButton button;
    private boolean userToggleOn;

    public WireOverlayButton(SpriteBatch batch) {
        this.viewport = new ScreenViewport();
        this.stage = new Stage(this.viewport, batch);
        Skin skin = GameAssetManager.INSTANCE.getSkin();

        button = new TextButton("Wires: OFF", skin);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                userToggleOn = !userToggleOn;
                button.setText(userToggleOn ? "Wires: ON" : "Wires: OFF");
            }
        });
        button.pack();
        stage.addActor(button);
    }

    public void handleInput() {
        stage.act();
    }

    public void draw() {
        viewport.apply();
        stage.draw();
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
        button.setPosition(viewport.getWorldWidth() - button.getWidth() - 10,
            viewport.getWorldHeight() - button.getHeight() - 10);
    }

    public void dispose() {
        stage.dispose();
    }

    public Stage getStage() {
        return stage;
    }

    public boolean isUserToggleOn() {
        return userToggleOn;
    }
}
