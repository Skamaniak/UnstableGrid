package com.skamaniak.ugfs.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.skamaniak.ugfs.asset.GameAssetManager;
import com.skamaniak.ugfs.asset.model.GameAsset;

import java.util.Collection;
import java.util.HashSet;

public class BuildMenu {
    private final Stage stage;
    private final Viewport viewport;
    private final Collection<Button> buildButtons = new HashSet<>();
    private final Label description;

    private GameAsset selectedAsset;

    public BuildMenu() {
        this.viewport = new ScreenViewport();
        this.stage = new Stage(this.viewport);
        Skin skin = GameAssetManager.INSTANCE.getSkin();

        // Create and configure the menu table
        Table buildMenu = new Table();
        buildMenu.setFillParent(false);


        // Create the tab buttons
        HorizontalGroup group = new HorizontalGroup();
        Button generatorTab = new TextButton("Generator", skin, "toggle");
        Button storageTab = new TextButton("Storage", skin, "toggle");
        Button towerTab = new TextButton("Tower", skin, "toggle");

        group.addActor(generatorTab);
        group.addActor(storageTab);
        group.addActor(towerTab);

        buildMenu.add(group);
        buildMenu.row();

        // Create the tab content. Just using images here for simplicity.
        Stack content = new Stack();
        Actor generatorMenu = createBuildSubmenu(GameAssetManager.INSTANCE.getGenerators());
        Actor storageMenu = createBuildSubmenu(GameAssetManager.INSTANCE.getPowerStorages());
        Actor towerMenu = createBuildSubmenu(GameAssetManager.INSTANCE.getTowers());

        content.addActor(generatorMenu);
        content.addActor(storageMenu);
        content.addActor(towerMenu);

        buildMenu.add(content).expandX().fill();
        buildMenu.row();

        description = new Label("", skin);
        description.setWrap(true);
        Container<Label> labelContainer = new Container<>(description);
        labelContainer.fillX().minHeight(175);

        buildMenu.add(labelContainer).expandX().fillX().top();

        // Listen to changes in the tab button checked states
        // Set visibility of the tab content to match the checked state
        ChangeListener tab_listener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                resetSelection();
                generatorMenu.setVisible(generatorTab.isChecked());
                storageMenu.setVisible(storageTab.isChecked());
                towerMenu.setVisible(towerTab.isChecked());
            }
        };
        generatorTab.addListener(tab_listener);
        storageTab.addListener(tab_listener);
        towerTab.addListener(tab_listener);

        // Let only one tab button be checked at a time
        ButtonGroup<Button> tabs = new ButtonGroup<>();
        tabs.setMinCheckCount(1);
        tabs.setMaxCheckCount(1);

        tabs.add(generatorTab);
        tabs.add(storageTab);
        tabs.add(towerTab);

        buildMenu.row();

        Window window = new Window("Build Menu", skin);
        window.add(buildMenu);
        window.align(Align.topLeft);
        window.setWidth(220);
        window.setHeight(400);
        stage.addActor(window);
    }

    private <T extends GameAsset> Actor createBuildSubmenu(Collection<T> gameAssets) {
        Table menu = new Table().top().left();

        for (T gameAsset : gameAssets) {
            Button button = new TextButton(gameAsset.getName(), GameAssetManager.INSTANCE.getSkin(), "toggle");
            button.addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    resetSelection();
                    button.setChecked(true);
                    description.setText(gameAsset.getDescription());
                    selectedAsset = gameAsset;
                }
            });
            menu.add(button).expandX().fillX().top();
            menu.row();
            buildButtons.add(button);
        }
        return menu;
    }

    public void resetSelection() {
        for (Button button : buildButtons) {
            button.setChecked(false);
        }
        description.setText("");
        selectedAsset = null;
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

    public void handleInput() {
        stage.act();
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    public GameAsset getSelectedAsset() {
        return selectedAsset;
    }
}
