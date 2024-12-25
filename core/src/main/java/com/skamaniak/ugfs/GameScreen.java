package com.skamaniak.ugfs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.skamaniak.ugfs.asset.GameAssetManager;
import com.skamaniak.ugfs.asset.model.*;
import com.skamaniak.ugfs.game.GameState;
import com.skamaniak.ugfs.game.entity.GameEntity;
import com.skamaniak.ugfs.game.entity.GeneratorEntity;
import com.skamaniak.ugfs.game.entity.PowerStorageEntity;
import com.skamaniak.ugfs.game.entity.TowerEntity;
import com.skamaniak.ugfs.input.KeyboardControls;
import com.skamaniak.ugfs.ui.BuildMenu;
import com.skamaniak.ugfs.ui.DetailsMenu;
import com.skamaniak.ugfs.view.SceneCamera;

public class GameScreen implements Screen {
    public static final int WORLD_WIDTH = 1024;
    public static final int WORLD_HEIGHT = 1024;

    private final UnstableGrid game;
    private final SceneCamera sceneCamera;
    private final FitViewport viewport;

    private final Vector2 clickPosition = new Vector2();
    private final Vector2 mousePosition = new Vector2();

    // UI
    private final BuildMenu buildMenu = new BuildMenu();
    private final DetailsMenu detailsMenu = new DetailsMenu();

    private GameState gameState;

    public GameScreen(UnstableGrid unstableGrid) {
        this.game = unstableGrid;
        this.gameState = new GameState(game, game.level);
        this.sceneCamera = new SceneCamera(this.game.level.getLevelWidth() * GameAssetManager.TILE_SIZE_PX / 2,
            this.game.level.getLevelHeight() * GameAssetManager.TILE_SIZE_PX / 2);
        this.viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, sceneCamera);

        populateGameStateWithDummyData();


    }

    private void populateGameStateWithDummyData() {
        Generator generator = GameAssetManager.INSTANCE.getGenerator("generator.solar-panel");
        GeneratorEntity generatorSolarPanel1 = new GeneratorEntity(new Vector2(4, 10), generator);
        gameState.registerGenerator(generatorSolarPanel1);

        GeneratorEntity generatorSolarPanel2 = new GeneratorEntity(new Vector2(4, 8), generator);
        gameState.registerGenerator(generatorSolarPanel2);

        GeneratorEntity generatorSolarPanel3 = new GeneratorEntity(new Vector2(4, 6), generator);
        gameState.registerGenerator(generatorSolarPanel3);

        GeneratorEntity generatorSolarPanel4 = new GeneratorEntity(new Vector2(4, 4), generator);
        gameState.registerGenerator(generatorSolarPanel4);

        PowerStorage storageCapacitor = GameAssetManager.INSTANCE.getPowerStorage("power-storage.capacitor");
        PowerStorageEntity storageEntity = new PowerStorageEntity(new Vector2(7, 6), storageCapacitor);
        gameState.registerPowerStorage(storageEntity);

        PowerStorage storageBattery = GameAssetManager.INSTANCE.getPowerStorage("power-storage.battery");
        PowerStorageEntity storageEntity2 = new PowerStorageEntity(new Vector2(7, 4), storageBattery);
        gameState.registerPowerStorage(storageEntity2);

        Tower towerTesla = GameAssetManager.INSTANCE.getTower("tower.tesla");
        TowerEntity towerEntity = new TowerEntity(new Vector2(10, 5), towerTesla);
        gameState.registerTower(towerEntity);

        Tower towerLaser = GameAssetManager.INSTANCE.getTower("tower.laser");
        TowerEntity towerEntity2 = new TowerEntity(new Vector2(10, 10), towerLaser);
        gameState.registerTower(towerEntity2);

        Conduit conduit = GameAssetManager.INSTANCE.getConduit("conduit.copper-wire");
        gameState.registerLink(conduit, generatorSolarPanel1, towerEntity2);
        gameState.registerLink(conduit, generatorSolarPanel2, storageEntity);
        gameState.registerLink(conduit, generatorSolarPanel3, storageEntity);
        gameState.registerLink(conduit, generatorSolarPanel4, storageEntity);
        gameState.registerLink(conduit, generatorSolarPanel4, storageEntity2);
        gameState.registerLink(conduit, storageEntity, towerEntity);
        gameState.registerLink(conduit, storageEntity, towerEntity2);
    }

    @Override
    public void render(float delta) {
        readInputs();
        updateCamera();
        gameState.simulate(delta);
        draw(delta);
    }

    private void readInputs() {

        if (game.input.isPressed(KeyboardControls.CAMERA_ZOOM_IN)) {
            sceneCamera.zoomIn();
        }
        if (game.input.isPressed(KeyboardControls.CAMERA_ZOOM_OUT)) {
            sceneCamera.zoomOut();
        }
        if (game.input.isPressed(KeyboardControls.CAMERA_MOVE_UP)) {
            sceneCamera.moveUp();
        }
        if (game.input.isPressed(KeyboardControls.CAMERA_MOVE_DOWN)) {
            sceneCamera.moveDown();
        }
        if (game.input.isPressed(KeyboardControls.CAMERA_MOVE_RIGHT)) {
            sceneCamera.moveRight();
        }
        if (game.input.isPressed(KeyboardControls.CAMERA_MOVE_LEFT)) {
            sceneCamera.moveLeft();
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            clickPosition.set(Gdx.input.getX(), Gdx.input.getY());
            viewport.unproject(clickPosition);
        }

        gameState.readInputs();
        buildMenu.handleInput();
        detailsMenu.handleInput();
        mousePosition.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(mousePosition);
    }

    private void updateCamera() {
        sceneCamera.updatePosition();
        sceneCamera.updateViewBounds();
    }

    private void draw(float delta) {
        viewport.apply();
        game.batch.setProjectionMatrix(viewport.getCamera().combined);
        game.batch.begin();

        gameState.draw(delta);

        drawCursor();
        game.batch.end();

        buildMenu.draw();
        detailsMenu.draw();
    }

    private void drawCursor() { //TODO temporal, also needs to take into account if there is anything already built on the tile
        GameAsset selectedAsset = buildMenu.getSelectedAsset();

        if (selectedAsset != null) {
            Level.Tile tile = gameState.getTerrainTile((int) mousePosition.x, (int) mousePosition.y);
            GameEntity gameEntity = gameState.getEntityAt((int) mousePosition.x, (int) mousePosition.y);
            if (tile != null) {
                String texture;
                Terrain terrain = GameAssetManager.INSTANCE.getTerrain(tile.getTerrainId());
                if (gameEntity == null && selectedAsset.getBuildableOn().contains(terrain.getTerrainType())) {
                    texture = "assets/visual/valid-selection.png";
                } else {
                    texture = "assets/visual/invalid-selection.png";
                }
                game.batch.draw(GameAssetManager.INSTANCE.loadTexture(texture),
                    mousePosition.x - mousePosition.x % GameAssetManager.TILE_SIZE_PX,
                    mousePosition.y - mousePosition.y % GameAssetManager.TILE_SIZE_PX);
            }
        } else {
            game.batch.draw(GameAssetManager.INSTANCE.loadTexture("assets/visual/select-reticle.png"),
                clickPosition.x - clickPosition.x % GameAssetManager.TILE_SIZE_PX,
                clickPosition.y - clickPosition.y % GameAssetManager.TILE_SIZE_PX);
            GameEntity entity = gameState.getEntityAt((int) clickPosition.x, (int) clickPosition.y);
            if (entity != null) {
                detailsMenu.showDetails(entity.getDetails());
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        buildMenu.resize(width, height);
        detailsMenu.resize(width, height);
    }

    @Override
    public void show() {
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(game.input);
        multiplexer.addProcessor(buildMenu.getStage());
        multiplexer.addProcessor(detailsMenu.getStage());
        Gdx.input.setInputProcessor(multiplexer);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        buildMenu.dispose();
        detailsMenu.dispose();
    }
}
