package com.skamaniak.ugfs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.skamaniak.ugfs.action.PlayerAction;
import com.skamaniak.ugfs.action.PlayerActionFactory;
import com.skamaniak.ugfs.asset.GameAssetManager;
import com.skamaniak.ugfs.asset.model.*;
import com.skamaniak.ugfs.game.GameState;
import com.skamaniak.ugfs.game.entity.*;
import com.skamaniak.ugfs.input.KeyboardControls;
import com.skamaniak.ugfs.input.PlayerInput;
import com.skamaniak.ugfs.simulation.PowerProducer;
import com.skamaniak.ugfs.ui.BuildMenu;
import com.skamaniak.ugfs.ui.ContextMenu;
import com.skamaniak.ugfs.ui.DetailsMenu;
import com.skamaniak.ugfs.ui.ScrapHud;
import com.skamaniak.ugfs.ui.WiringMenu;
import com.skamaniak.ugfs.view.SceneCamera;

public class GameScreen implements Screen {
    public static final int WORLD_WIDTH = 1024;
    public static final int WORLD_HEIGHT = 1024;

    private final UnstableGrid game;
    private final SceneCamera sceneCamera;
    private final FitViewport viewport;
    private final GameEntityFactory gameEntityFactory;
    private final PlayerInput playerInput;

    // UI
    private final BuildMenu buildMenu;
    private final DetailsMenu detailsMenu;
    private final WiringMenu wiringMenu;
    private final ContextMenu contextMenu;
    private final ScrapHud scrapHud;

    // Player Actions
    private final PlayerActionFactory playerActionFactory;
    private PlayerAction pendingPlayerAction;
    private boolean inWireRemovalMode;

    private GameState gameState;

    public GameScreen(UnstableGrid unstableGrid) {
        this.game = unstableGrid;
        this.gameState = new GameState(game, game.level);
        GameAssetManager.INSTANCE.loadSound(game.level.getMusic()).loop(0.15f); //TODO take volume from settings
        this.sceneCamera = new SceneCamera(this.game.level.getLevelWidth() * GameAssetManager.TILE_SIZE_PX / 2,
            this.game.level.getLevelHeight() * GameAssetManager.TILE_SIZE_PX / 2);
        this.viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, sceneCamera);
        this.gameEntityFactory = new GameEntityFactory();

        this.playerInput = new PlayerInput(viewport::unproject);

        this.buildMenu = new BuildMenu(unstableGrid.batch);
        this.detailsMenu = new DetailsMenu(unstableGrid.batch);
        this.wiringMenu = new WiringMenu(unstableGrid.batch);
        this.contextMenu = new ContextMenu(unstableGrid.batch, gameState, playerInput, wiringMenu);
        this.scrapHud = new ScrapHud(unstableGrid.batch, gameState);

        this.playerActionFactory = new PlayerActionFactory(gameState, playerInput, this::showGameObjectDetails, this::buildGameObject, this::onWireCreated);
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
        if (playerInput.isPressed(KeyboardControls.CAMERA_ZOOM_IN)) {
            sceneCamera.zoomIn();
        }
        if (playerInput.isPressed(KeyboardControls.CAMERA_ZOOM_OUT)) {
            sceneCamera.zoomOut();
        }
        if (playerInput.isPressed(KeyboardControls.CAMERA_MOVE_UP)) {
            sceneCamera.moveUp();
        }
        if (playerInput.isPressed(KeyboardControls.CAMERA_MOVE_DOWN)) {
            sceneCamera.moveDown();
        }
        if (playerInput.isPressed(KeyboardControls.CAMERA_MOVE_RIGHT)) {
            sceneCamera.moveRight();
        }
        if (playerInput.isPressed(KeyboardControls.CAMERA_MOVE_LEFT)) {
            sceneCamera.moveLeft();
        }
        if (playerInput.isPressed(KeyboardControls.DEBUG_SHOW_FPS)) {
            System.out.println(Gdx.graphics.getFramesPerSecond());
        }

        if (playerInput.justClicked(Input.Buttons.LEFT)) {
            pendingPlayerAction.handleClick(playerInput.getLeftClickPosition());
        }

        gameState.readInputs();
        buildMenu.handleInput();
        detailsMenu.handleInput();
        wiringMenu.handleInput();
        contextMenu.handleInput();
        scrapHud.handleInput();

        pendingPlayerAction.handleMouseMove(playerInput.getMousePosition());
    }

    private void updateCamera() {
        sceneCamera.updatePosition();
        sceneCamera.updateViewBounds();
    }

    private void draw(float delta) {
        viewport.apply();

        game.batch.setProjectionMatrix(viewport.getCamera().combined);
        game.batch.begin();
        gameState.drawTextures(delta);
        pendingPlayerAction.drawTextures(game.batch);
        game.batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        game.shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        gameState.drawShapes(game.shapeRenderer);
        pendingPlayerAction.drawShapes(game.shapeRenderer);
        game.shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        buildMenu.draw();
        detailsMenu.draw();
        wiringMenu.draw();
        contextMenu.draw();
        scrapHud.draw();
    }

    private void selectPlayerAction() {
        // One-shot: sell confirmed
        if (contextMenu.isSellConfirmed()) {
            GameEntity entity = contextMenu.getTargetEntity();
            contextMenu.resetSellConfirmed();
            if (entity != null) {
                gameState.sellEntity(entity);
            }
            inWireRemovalMode = false;
            pendingPlayerAction = playerActionFactory.detailsSelection();
            return;
        }

        // One-shot: wire removal requested
        if (contextMenu.isWireRemovalRequested()) {
            GameEntity entity = contextMenu.getTargetEntity();
            contextMenu.resetWireRemovalRequested();
            if (entity != null) {
                inWireRemovalMode = true;
                pendingPlayerAction = playerActionFactory.wireRemoval(entity, this::onWireRemoved);
            }
            return;
        }

        // One-shot: wiring conduit selected via context menu
        if (contextMenu.isWiringRequested()) {
            Conduit selectedConduit = wiringMenu.getSelectedConduit();
            if (selectedConduit != null) {
                GameEntity entity = contextMenu.getTargetEntity();
                if (entity instanceof PowerProducer) {
                    contextMenu.resetWiringRequested();
                    inWireRemovalMode = false;
                    pendingPlayerAction = playerActionFactory.wiring(entity, selectedConduit);
                    return;
                }
            }
            // Conduit not yet selected in WiringMenu — keep waiting
            return;
        }

        // Persist wire removal mode until right-click exits it
        if (inWireRemovalMode) {
            if (playerInput.justClicked(Input.Buttons.RIGHT)) {
                inWireRemovalMode = false;
            } else {
                return;
            }
        }

        // Build mode takes priority over persistent wiring
        GameAsset buildAsset = buildMenu.getSelectedAsset();
        if (buildAsset != null) {
            wiringMenu.resetSelection();
            pendingPlayerAction = playerActionFactory.building(buildAsset);
            return;
        }

        // Persistent wiring: conduit still selected from WiringMenu
        Conduit wiringConduit = wiringMenu.getSelectedConduit();
        if (wiringConduit != null) {
            GameEntity wiringSource = contextMenu.getTargetEntity();
            if (wiringSource instanceof PowerProducer) {
                pendingPlayerAction = playerActionFactory.wiring(wiringSource, wiringConduit);
                return;
            }
        }

        // Default
        pendingPlayerAction = playerActionFactory.detailsSelection();
    }

    private void showGameObjectDetails(String details) {
        detailsMenu.showDetails(details);
    }

    private void onWireCreated() {
        wiringMenu.resetSelection();
        contextMenu.resetWiringRequested();
    }

    private void onWireRemoved() {
        inWireRemovalMode = false;
        pendingPlayerAction = playerActionFactory.detailsSelection();
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
        contextMenu.resize(width, height);
        scrapHud.resize(width, height);
    }

    @Override
    public void show() {
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(playerInput);
        multiplexer.addProcessor(contextMenu.getStage());
        multiplexer.addProcessor(buildMenu.getStage());
        multiplexer.addProcessor(detailsMenu.getStage());
        multiplexer.addProcessor(wiringMenu.getStage());
        multiplexer.addProcessor(scrapHud.getStage());
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
        contextMenu.dispose();
        scrapHud.dispose();
    }
}
