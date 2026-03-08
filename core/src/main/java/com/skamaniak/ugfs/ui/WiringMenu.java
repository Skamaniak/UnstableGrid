package com.skamaniak.ugfs.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.skamaniak.ugfs.asset.GameAssetManager;
import com.skamaniak.ugfs.asset.model.Conduit;

import java.util.function.Supplier;

public class WiringMenu {
    private final Stage stage;
    private final Viewport viewport;
    private final Supplier<Boolean> openMenuTest;
    private Conduit selectedConduit;

    public WiringMenu(SpriteBatch batch, Supplier<Boolean> openMenuTest) {
        this.viewport = new ScreenViewport();
        this.stage = new Stage(viewport, batch);
        this.openMenuTest = openMenuTest;
        Table menu = createMenu();
        setupInputListener(menu);
        stage.addActor(menu);
    }

    private Table createMenu() {
        Table menuTable = new Table();
        menuTable.setVisible(false);

        for (Conduit conduit : GameAssetManager.INSTANCE.getConduits()) {
            TextButton button = new TextButton(conduit.getName(), GameAssetManager.INSTANCE.getSkin());
            button.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    if (button == Input.Buttons.LEFT) {
                        selectedConduit = conduit;
                        menuTable.setVisible(false);
                        return true;
                    }
                    return false;
                }
            });
            menuTable.add(button).row();
        }
        return menuTable;
    }

    private void setupInputListener(Table menuTable) {
        stage.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (button == Input.Buttons.RIGHT && openMenuTest.get()) {
                    menuTable.setPosition(x, y);
                    menuTable.setVisible(true);
                    resetSelection();
                } else if (menuTable.isVisible()) {
                    menuTable.setVisible(false);
                    resetSelection();
                }
                return false;
            }
        });
    }

    public void handleInput() {
        stage.act();
    }

    public void draw() {
        stage.draw();
    }

    public void dispose() {
        stage.dispose();
    }

    public Stage getStage() {
        return stage;
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    public void resetSelection() {
        selectedConduit = null;
    }

    public Conduit getSelectedConduit() {
        return selectedConduit;
    }
}
