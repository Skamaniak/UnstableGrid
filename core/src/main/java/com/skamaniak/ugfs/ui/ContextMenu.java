package com.skamaniak.ugfs.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.skamaniak.ugfs.asset.GameAssetManager;
import com.skamaniak.ugfs.game.GameState;
import com.skamaniak.ugfs.game.entity.GameEntity;
import com.skamaniak.ugfs.input.PlayerInput;
import com.skamaniak.ugfs.simulation.PowerProducer;

public class ContextMenu {
    private final Stage stage;
    private final Viewport viewport;
    private final GameState gameState;
    private final PlayerInput playerInput;
    private final Table menuTable;
    private final WiringMenu wiringMenu;
    private final BuildMenu buildMenu;

    private GameEntity targetEntity;
    private boolean wiringRequested;
    private boolean wireRemovalRequested;
    private boolean sellArmed;
    private boolean sellConfirmed;

    private TextButton sellButton;

    public ContextMenu(SpriteBatch batch, GameState gameState, PlayerInput playerInput, WiringMenu wiringMenu, BuildMenu buildMenu) {
        this.gameState = gameState;
        this.playerInput = playerInput;
        this.wiringMenu = wiringMenu;
        this.buildMenu = buildMenu;
        this.viewport = new ScreenViewport();
        this.stage = new Stage(viewport, batch);

        menuTable = new Table();
        menuTable.setVisible(false);
        stage.addActor(menuTable);

        setupInputListener();
    }

    private void setupInputListener() {
        stage.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (button == Input.Buttons.RIGHT) {
                    GameEntity entity = gameState.getEntityAt(playerInput.getRightClickPosition());
                    if (entity != null) {
                        openMenu(entity, x, y);
                        return true;
                    } else {
                        hide();
                    }
                } else if (button == Input.Buttons.LEFT) {
                    if (menuTable.isVisible() && menuTable.hit(x - menuTable.getX(), y - menuTable.getY(), true) == null) {
                        hide();
                    }
                }
                return false;
            }
        });
    }

    private void openMenu(GameEntity entity, float x, float y) {
        targetEntity = entity;
        wiringRequested = false;
        wireRemovalRequested = false;
        sellArmed = false;
        sellConfirmed = false;
        wiringMenu.hide();
        buildMenu.resetSelection();

        menuTable.clear();
        menuTable.defaults().minWidth(180).fillX();
        menuTable.setVisible(true);

        Skin skin = GameAssetManager.INSTANCE.getSkin();

        if (entity instanceof PowerProducer) {
            TextButton wireButton = new TextButton("Wire", skin);
            wireButton.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    if (button == Input.Buttons.LEFT) {
                        wiringRequested = true;
                        wiringMenu.show(menuTable.getX(), menuTable.getY() + menuTable.getHeight());
                        menuTable.setVisible(false);
                        return true;
                    }
                    return false;
                }
            });
            menuTable.add(wireButton).row();

            boolean hasOutgoingWires = gameState.hasOutgoingConduits((PowerProducer) entity);
            if (hasOutgoingWires) {
                TextButton removeWireButton = new TextButton("Remove Wire", skin);
                removeWireButton.addListener(new InputListener() {
                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                        if (button == Input.Buttons.LEFT) {
                            hide();
                            wireRemovalRequested = true;
                            return true;
                        }
                        return false;
                    }
                });
                menuTable.add(removeWireButton).row();
            }
        }

        int sellValue = gameState.computeSellValue(entity);
        sellButton = new TextButton("Sell (+" + sellValue + " scrap)", skin);
        sellButton.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (button == Input.Buttons.LEFT) {
                    if (!sellArmed) {
                        sellArmed = true;
                        int sv = gameState.computeSellValue(targetEntity);
                        sellButton.setText("Confirm Sell (+" + sv + " scrap)");
                    } else {
                        hide();
                        sellConfirmed = true;
                    }
                    return true;
                }
                return false;
            }
        });
        menuTable.add(sellButton).row();

        menuTable.pack();
        menuTable.setPosition(x, y - menuTable.getHeight());
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
    }

    public void dispose() {
        stage.dispose();
    }

    public Stage getStage() {
        return stage;
    }

    public void hide() {
        menuTable.setVisible(false);
        sellArmed = false;
        sellConfirmed = false;
        wiringRequested = false;
        wireRemovalRequested = false;
    }

    public GameEntity getTargetEntity() {
        return targetEntity;
    }

    public boolean isWiringRequested() {
        return wiringRequested;
    }

    public void resetWiringRequested() {
        wiringRequested = false;
    }

    public boolean isWireRemovalRequested() {
        return wireRemovalRequested;
    }

    public void resetWireRemovalRequested() {
        wireRemovalRequested = false;
    }

    public boolean isSellConfirmed() {
        return sellConfirmed;
    }

    public void resetSellConfirmed() {
        sellConfirmed = false;
    }
}
