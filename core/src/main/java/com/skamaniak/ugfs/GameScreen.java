package com.skamaniak.ugfs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.skamaniak.ugfs.asset.GameAssetManager;
import com.skamaniak.ugfs.input.KeyboardControls;
import com.skamaniak.ugfs.view.SceneCamera;

public class GameScreen implements Screen {
    private static final int WORLD_WIDTH = 1024;
    private static final int WORLD_HEIGHT = 768;

    private final UnstableGrid game;
    private final SceneCamera sceneCamera;
    private final FitViewport viewport;

    private final Vector2 clickPosition = new Vector2();

    public GameScreen(UnstableGrid unstableGrid) {
        this.game = unstableGrid;
        this.sceneCamera = new SceneCamera(this.game.level.getSceneWidth() * GameAssetManager.TILE_SIZE_PX / 2,
            this.game.level.getSceneHeight() * GameAssetManager.TILE_SIZE_PX / 2);
        this.viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, sceneCamera);
    }

    @Override
    public void render(float delta) {
        readInputs();
        updateCamera();
        draw();
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
    }

    private void updateCamera() {
        sceneCamera.updatePosition();
        sceneCamera.updateViewBounds();
    }

    private void draw() {
        viewport.apply();

        game.batch.setProjectionMatrix(viewport.getCamera().combined);
        game.batch.begin();
        game.level.getMap().forEach(tile -> {
            TextureRegion texture = game.gameAssetManager.loadTerrainTileTexture(tile);
            game.batch.draw(texture, tile.getX() * GameAssetManager.TILE_SIZE_PX,
                tile.getY() * GameAssetManager.TILE_SIZE_PX);
        });

        game.batch.draw(game.gameAssetManager.loadTexture("assets/select-reticle.png"),
            clickPosition.x - clickPosition.x % GameAssetManager.TILE_SIZE_PX,
            clickPosition.y - clickPosition.y % GameAssetManager.TILE_SIZE_PX);
        game.batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void show() {

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

    }
}
