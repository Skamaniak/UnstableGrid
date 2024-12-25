package com.skamaniak.ugfs.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.skamaniak.ugfs.asset.GameAssetManager;

public class DetailsMenu {
    public final int WIDTH = 220;
    public final int HEIGHT = 220;
    private final Stage stage;
    private final Viewport viewport;
    private final Label details;

    public DetailsMenu() {
        this.viewport = new ScreenViewport();
        this.stage = new Stage(this.viewport);
        Skin skin = GameAssetManager.INSTANCE.getSkin();

        details = new Label("", skin);
        details.setWrap(true);
        details.setAlignment(Align.topLeft);

        Window window = new Window("Details", skin);
        window.add(details).expand().fill();
        window.setSize(WIDTH, HEIGHT);
        window.setPosition(
            (Gdx.graphics.getWidth() - window.getWidth()),
            (Gdx.graphics.getHeight() - window.getHeight())
        );
        stage.addActor(window);
    }

    public void draw() {
        viewport.apply();
        stage.draw();
    }

    public void showDetails(String description) {
        details.setText(description);
    }

    public void dispose() {
        stage.dispose();
    }

    public Stage getStage() {
        return stage;
    }

    public void handleInput() {
        stage.act();
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }
}
