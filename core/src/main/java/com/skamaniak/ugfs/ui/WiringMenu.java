package com.skamaniak.ugfs.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.skamaniak.ugfs.asset.GameAssetManager;
import com.skamaniak.ugfs.asset.model.Conduit;

public class WiringMenu {
    private final Stage stage;
    private final Viewport viewport;
    private Conduit selectedConduit;

    public WiringMenu() {
        this.viewport = new ScreenViewport();
        this.stage = new Stage(viewport);
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
                if (button == Input.Buttons.RIGHT) {
                    menuTable.setPosition(x, y);
                    menuTable.setVisible(true);
                    return true;
                } else if (button == Input.Buttons.LEFT && menuTable.isVisible()) {
                    menuTable.setVisible(false);
                    resetSelection();
                    return true;
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
