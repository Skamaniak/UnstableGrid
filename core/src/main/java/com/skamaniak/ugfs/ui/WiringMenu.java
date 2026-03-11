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

public class WiringMenu {
    private final Stage stage;
    private final Viewport viewport;
    private Table menu;
    private Conduit selectedConduit;

    public WiringMenu(SpriteBatch batch) {
        this.viewport = new ScreenViewport();
        this.stage = new Stage(viewport, batch);
        menu = createMenu();
        stage.addActor(menu);
    }

    private Table createMenu() {
        Table menuTable = new Table();
        menuTable.setVisible(false);
        menuTable.defaults().minWidth(180).fillX();

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
        menuTable.pack();
        return menuTable;
    }

    public void show(float x, float y) {
        resetSelection();
        menu.setPosition(x, y - menu.getHeight());
        menu.setVisible(true);
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
