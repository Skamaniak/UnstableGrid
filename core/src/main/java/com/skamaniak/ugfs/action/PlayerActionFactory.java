package com.skamaniak.ugfs.action;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.NavigationUtils;
import com.skamaniak.ugfs.asset.GameAssetManager;
import com.skamaniak.ugfs.asset.model.GameAsset;
import com.skamaniak.ugfs.asset.model.Level;
import com.skamaniak.ugfs.asset.model.Terrain;
import com.skamaniak.ugfs.game.GameState;
import com.skamaniak.ugfs.game.entity.GameEntity;

import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class PlayerActionFactory {
    private final Wiring wiring;
    private final Building building;
    private final DetailsSelection detailsSelection;

    public PlayerActionFactory(GameState gameState, Consumer<String> detailsAction, BiConsumer<Vector2, GameAsset> buildAction) {
        wiring = new Wiring();
        building = new Building(gameState, buildAction);
        detailsSelection = new DetailsSelection(gameState, detailsAction);
    }

    public Wiring wiring() {
        // TODO init wiring
        return wiring;
    }

    public Building building(GameAsset assetToBuild) {
        building.assetToBuild = assetToBuild;
        return building;
    }

    public DetailsSelection detailsSelection() {
        // TODO init details selection
        return detailsSelection;
    }


    private static class Wiring implements PlayerAction {

        @Override
        public void handleClick(Vector2 mousePosition) {

        }

        @Override
        public void handleMouseMove(Vector2 mousePosition) {

        }

        @Override
        public void render(SpriteBatch batch) {

        }
    }

    private static class Building implements PlayerAction {
        private final GameState gameState;
        private final BiConsumer<Vector2, GameAsset> build;

        private Vector2 mousePosition;

        private GameAsset assetToBuild;
        private String texture;

        public Building(GameState gameState, BiConsumer<Vector2, GameAsset> build) {
            this.gameState = gameState;
            this.build = build;
        }

        @Override
        public void handleClick(Vector2 mousePosition) {
            if (isBuildable(mousePosition)) {
                build.accept(mousePosition, assetToBuild);
            }
        }

        private boolean isBuildable(Vector2 mousePosition) {
            Level.Tile tile = gameState.getTerrainTile((int) mousePosition.x, (int) mousePosition.y);
            GameEntity gameEntity = gameState.getEntityAt((int) mousePosition.x, (int) mousePosition.y);

            if (tile != null) {
                Terrain terrain = GameAssetManager.INSTANCE.getTerrain(tile.getTerrainId());
                return gameEntity == null
                    && assetToBuild.getBuildableOn().contains(terrain.getTerrainType());
            }
            return false;
        }

        @Override
        public void handleMouseMove(Vector2 mousePosition) {
            this.mousePosition = mousePosition;

            if (isBuildable(mousePosition)) {
                texture = "assets/visual/valid-selection.png";
            } else {
                texture = "assets/visual/invalid-selection.png";
            }
        }

        @Override
        public void render(SpriteBatch batch) {
            if (mousePosition != null && texture != null) {
                batch.draw(GameAssetManager.INSTANCE.loadTexture(texture),
                    NavigationUtils.alignCoordinateWithMesh(mousePosition.x),
                    NavigationUtils.alignCoordinateWithMesh(mousePosition.y));
            }
        }
    }

    private static class DetailsSelection implements PlayerAction {
        private final GameState gameState;
        private final Consumer<String> showDetailsHandler;

        private Vector2 clickPosition;

        public DetailsSelection(GameState gameState, Consumer<String> showDetailsHandler) {
            this.gameState = gameState;
            this.showDetailsHandler = showDetailsHandler;
        }

        @Override
        public void handleClick(Vector2 mousePosition) {
            clickPosition = mousePosition;

            GameEntity entity = gameState.getEntityAt((int) clickPosition.x, (int) clickPosition.y);
            if (entity != null) {
                showDetailsHandler.accept(entity.getDetails());
            }
        }

        @Override
        public void handleMouseMove(Vector2 mousePosition) {
            // do nothing
        }

        @Override
        public void render(SpriteBatch batch) {
            if (clickPosition != null) {
                batch.draw(GameAssetManager.INSTANCE.loadTexture("assets/visual/select-reticle.png"),
                    NavigationUtils.alignCoordinateWithMesh(clickPosition.x),
                    NavigationUtils.alignCoordinateWithMesh(clickPosition.y));
            }
        }
    }
}

