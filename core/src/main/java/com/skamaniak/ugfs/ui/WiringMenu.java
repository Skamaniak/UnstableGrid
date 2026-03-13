package com.skamaniak.ugfs.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
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
import com.skamaniak.ugfs.game.GameState;

import java.util.HashMap;
import java.util.Map;

public class WiringMenu {
    private final Stage stage;
    private final Viewport viewport;
    private final GameState gameState;
    private final Map<TextButton, Conduit> buttonConduits = new HashMap<>();
    private Table menu;
    private Conduit selectedConduit;

    public WiringMenu(SpriteBatch batch, GameState gameState) {
        this.gameState = gameState;
        this.viewport = new ScreenViewport();
        this.stage = new Stage(viewport, batch);
        menu = createMenu();
        stage.addActor(menu);
        setupDismissListener();
    }

    private void setupDismissListener() {
        stage.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (menu.isVisible() && menu.hit(x - menu.getX(), y - menu.getY(), true) == null) {
                    hide();
                }
                return false;
            }
        });
    }

    private Table createMenu() {
        Table menuTable = new Table();
        menuTable.setVisible(false);
        menuTable.defaults().minWidth(180).fillX();

        for (Conduit conduit : GameAssetManager.INSTANCE.getConduits()) {
            TextButton button = new TextButton(conduit.getName() + " - " + conduit.getBuildCost(), GameAssetManager.INSTANCE.getSkin());
            button.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    if (button == Input.Buttons.LEFT) {
                        if (gameState.getScrap() < conduit.getBuildCost()) {
                            return false;
                        }
                        selectedConduit = conduit;
                        menuTable.setVisible(false);
                        return true;
                    }
                    return false;
                }
            });
            menuTable.add(button).row();
            buttonConduits.put(button, conduit);
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
        for (Map.Entry<TextButton, Conduit> entry : buttonConduits.entrySet()) {
            if (gameState.getScrap() < entry.getValue().getBuildCost()) {
                entry.getKey().setColor(0.8f, 0.3f, 0.3f, 1f);
            } else {
                entry.getKey().setColor(Color.WHITE);
            }
        }
    }

    public void draw() {
        viewport.apply();
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

    public void hide() {
        menu.setVisible(false);
        selectedConduit = null;
    }

    public boolean isVisible() {
        return menu.isVisible();
    }

    public void resetSelection() {
        selectedConduit = null;
    }

    public Conduit getSelectedConduit() {
        return selectedConduit;
    }
}
