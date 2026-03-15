package com.skamaniak.ugfs.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.skamaniak.ugfs.MainMenuScreen;
import com.skamaniak.ugfs.UnstableGrid;
import com.skamaniak.ugfs.asset.GameAssetManager;

public class GameOverOverlay {
    private final Stage stage;
    private final Viewport viewport;
    private final UnstableGrid game;
    private final Runnable onDispose;
    private boolean visible;
    private Texture bgTexture;

    public GameOverOverlay(SpriteBatch batch, UnstableGrid game, Runnable onDispose) {
        this.game = game;
        this.onDispose = onDispose;
        this.viewport = new ScreenViewport();
        this.stage = new Stage(this.viewport, batch);
        this.visible = false;
    }

    public void show(boolean victory) {
        if (visible) {
            return;
        }
        visible = true;

        Skin skin = GameAssetManager.INSTANCE.getSkin();

        Table root = new Table();
        root.setFillParent(true);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        bgTexture = new Texture(pixmap);
        pixmap.dispose();
        TextureRegionDrawable bg = new TextureRegionDrawable(bgTexture);
        bg.setMinWidth(0);
        bg.setMinHeight(0);
        root.setBackground(bg.tint(new Color(0, 0, 0, 0.7f)));

        String titleText = victory ? "Victory" : "Defeated";
        Color titleColor = victory ? Color.GREEN : Color.RED;

        Label titleLabel = new Label(titleText, skin);
        titleLabel.setColor(titleColor);
        titleLabel.setFontScale(3f);
        root.add(titleLabel).padBottom(40);
        root.row();

        TextButton mainMenuButton = new TextButton("Main Menu", skin);
        mainMenuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.postRunnable(() -> {
                    onDispose.run();
                    game.setScreen(new MainMenuScreen(game));
                });
            }
        });
        root.add(mainMenuButton).width(200).height(50);

        stage.addActor(root);
    }

    public boolean isVisible() {
        return visible;
    }

    public void handleInput() {
        stage.act();
    }

    public void draw() {
        if (!visible) {
            return;
        }
        viewport.apply();
        stage.draw();
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    public void dispose() {
        stage.dispose();
        if (bgTexture != null) {
            bgTexture.dispose();
        }
    }

    public Stage getStage() {
        return stage;
    }
}
