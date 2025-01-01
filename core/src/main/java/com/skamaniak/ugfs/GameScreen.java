package com.skamaniak.ugfs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.skamaniak.ugfs.action.PlayerAction;
import com.skamaniak.ugfs.action.PlayerActionFactory;
import com.skamaniak.ugfs.asset.GameAssetManager;
import com.skamaniak.ugfs.asset.model.*;
import com.skamaniak.ugfs.game.GameState;
import com.skamaniak.ugfs.game.entity.*;
import com.skamaniak.ugfs.input.KeyboardControls;
import com.skamaniak.ugfs.ui.BuildMenu;
import com.skamaniak.ugfs.ui.DetailsMenu;
import com.skamaniak.ugfs.ui.WiringMenu;
import com.skamaniak.ugfs.view.SceneCamera;

public class GameScreen implements Screen {
    public static final int WORLD_WIDTH = 1024;
    public static final int WORLD_HEIGHT = 1024;

    private final UnstableGrid game;
    private final SceneCamera sceneCamera;
    private final FitViewport viewport;
    private final GameEntityFactory gameEntityFactory;

    private final Vector2 leftClickPosition = new Vector2();
    private final Vector2 mousePosition = new Vector2();

    // UI
    private final BuildMenu buildMenu = new BuildMenu();
    private final DetailsMenu detailsMenu = new DetailsMenu();
    private final WiringMenu wiringMenu = new WiringMenu();

    // Player Actions
    private final PlayerActionFactory playerActionFactory;
    private PlayerAction pendingPlayerAction;


    private GameState gameState;

    public GameScreen(UnstableGrid unstableGrid) {
        this.game = unstableGrid;
        this.gameState = new GameState(game, game.level);
        this.sceneCamera = new SceneCamera(this.game.level.getLevelWidth() * GameAssetManager.TILE_SIZE_PX / 2,
            this.game.level.getLevelHeight() * GameAssetManager.TILE_SIZE_PX / 2);
        this.viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, sceneCamera);
        this.gameEntityFactory = new GameEntityFactory();

        this.playerActionFactory = new PlayerActionFactory(gameState, this::showGameObjectDetails, this::buildGameObject);
        this.pendingPlayerAction = playerActionFactory.detailsSelection();

        populateGameStateWithDummyData();
    }

    private void populateGameStateWithDummyData() {
        Generator solarGenerator = GameAssetManager.INSTANCE.getGenerator("generator.solar-panel");
        GeneratorEntity generatorSolarPanel1 = new GeneratorEntity(new Vector2(4, 10), solarGenerator);
        gameState.registerGenerator(generatorSolarPanel1);

        GeneratorEntity generatorSolarPanel2 = new GeneratorEntity(new Vector2(4, 8), solarGenerator);
        gameState.registerGenerator(generatorSolarPanel2);

        GeneratorEntity generatorSolarPanel3 = new GeneratorEntity(new Vector2(4, 6), solarGenerator);
        gameState.registerGenerator(generatorSolarPanel3);

        GeneratorEntity generatorSolarPanel4 = new GeneratorEntity(new Vector2(4, 4), solarGenerator);
        gameState.registerGenerator(generatorSolarPanel4);

        Generator waterGenerator = GameAssetManager.INSTANCE.getGenerator("generator.water");
        GeneratorEntity waterGenerator1 = new GeneratorEntity(new Vector2(4, 2), waterGenerator);
        gameState.registerGenerator(waterGenerator1);

        PowerStorage storageCapacitor = GameAssetManager.INSTANCE.getPowerStorage("power-storage.capacitor");
        PowerStorageEntity storageEntity = new PowerStorageEntity(new Vector2(7, 6), storageCapacitor);
        gameState.registerPowerStorage(storageEntity);

        PowerStorage storageBattery = GameAssetManager.INSTANCE.getPowerStorage("power-storage.battery");
        PowerStorageEntity storageEntity2 = new PowerStorageEntity(new Vector2(7, 4), storageBattery);
        gameState.registerPowerStorage(storageEntity2);

        Tower towerTesla = GameAssetManager.INSTANCE.getTower("tower.tesla");
        TowerEntity towerEntity = new TowerEntity(new Vector2(10, 6), towerTesla);
        gameState.registerTower(towerEntity);

        Tower towerLaser = GameAssetManager.INSTANCE.getTower("tower.laser");
        TowerEntity towerEntity2 = new TowerEntity(new Vector2(10, 10), towerLaser);
        gameState.registerTower(towerEntity2);

        Conduit conduitCopper = GameAssetManager.INSTANCE.getConduit("conduit.copper-wire");
        Conduit conduitAcsr = GameAssetManager.INSTANCE.getConduit("conduit.acsr");
        gameState.registerLink(conduitCopper, generatorSolarPanel1, towerEntity2);
        gameState.registerLink(conduitCopper, generatorSolarPanel2, storageEntity);
        gameState.registerLink(conduitCopper, generatorSolarPanel3, storageEntity);
        gameState.registerLink(conduitCopper, generatorSolarPanel4, storageEntity);
        gameState.registerLink(conduitCopper, generatorSolarPanel4, storageEntity2);
        gameState.registerLink(conduitAcsr, waterGenerator1, storageEntity2);
        gameState.registerLink(conduitCopper, storageEntity2, towerEntity);
        gameState.registerLink(conduitCopper, storageEntity, towerEntity2);
    }

    @Override
    public void render(float delta) {
        readInputs();
        updateCamera();
        gameState.simulate(delta);
        selectPlayerAction();
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
        if (game.input.isPressed(KeyboardControls.DEBUG_SHOW_FPS)) {
            System.out.println(Gdx.graphics.getFramesPerSecond());
        }

        if (game.input.justClicked(Input.Buttons.LEFT)) {
            leftClickPosition.set(Gdx.input.getX(), Gdx.input.getY());
            viewport.unproject(leftClickPosition);
            pendingPlayerAction.handleClick(leftClickPosition);
        }

        gameState.readInputs();
        buildMenu.handleInput();
        detailsMenu.handleInput();
        wiringMenu.handleInput();

        mousePosition.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(mousePosition);
        pendingPlayerAction.handleMouseMove(mousePosition);
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
        pendingPlayerAction.render(game.batch);
        game.batch.end();

        buildMenu.draw();
        detailsMenu.draw();
        wiringMenu.draw();
    }

    private void selectPlayerAction() {
        GameAsset buildAsset = buildMenu.getSelectedAsset();
        Conduit wiringConduit = wiringMenu.getSelectedConduit();
        if (buildAsset != null) {
            pendingPlayerAction = playerActionFactory.building(buildAsset);
//        } else if (wiringConduit != null) { //TODO
//            pendingPlayerAction = playerActionFactory.wiring();
        } else {
            pendingPlayerAction = playerActionFactory.detailsSelection();
        }
    }

    private void showGameObjectDetails(String details) {
        detailsMenu.showDetails(details);
    }

    private void buildGameObject(Vector2 position, GameAsset asset) {
        buildMenu.resetSelection();
        Vector2 entityPosition = NavigationUtils.meshVectorFromWorldVector(position);
        GameEntity newEntity = gameEntityFactory.createEntity(entityPosition, asset);
        gameState.registerEntity(newEntity);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        buildMenu.resize(width, height);
        detailsMenu.resize(width, height);
        wiringMenu.resize(width, height);
    }

    @Override
    public void show() {
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(game.input);
        multiplexer.addProcessor(buildMenu.getStage());
        multiplexer.addProcessor(detailsMenu.getStage());
        multiplexer.addProcessor(wiringMenu.getStage());
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
        wiringMenu.dispose();
    }
}
