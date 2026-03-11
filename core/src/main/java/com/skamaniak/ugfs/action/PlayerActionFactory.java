package com.skamaniak.ugfs.action;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.GameConstants;
import com.skamaniak.ugfs.NavigationUtils;
import com.skamaniak.ugfs.asset.GameAssetManager;
import com.skamaniak.ugfs.asset.model.Conduit;
import com.skamaniak.ugfs.asset.model.GameAsset;
import com.skamaniak.ugfs.asset.model.Level;
import com.skamaniak.ugfs.asset.model.Terrain;
import com.skamaniak.ugfs.game.GameState;
import com.skamaniak.ugfs.game.entity.ConduitEntity;
import com.skamaniak.ugfs.game.entity.GameEntity;
import com.skamaniak.ugfs.input.PlayerInput;
import com.skamaniak.ugfs.simulation.PowerConsumer;
import com.skamaniak.ugfs.simulation.PowerSource;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class PlayerActionFactory {
    private final Wiring wiring;
    private final Building building;
    private final DetailsSelection detailsSelection;
    private final GameState gameState;
    private final PlayerInput input;

    public PlayerActionFactory(GameState gameState, PlayerInput input, Consumer<String> detailsAction, BiConsumer<Vector2, GameAsset> buildAction, Runnable onWireCreated) {
        this.gameState = gameState;
        this.input = input;
        wiring = new Wiring(gameState, input, onWireCreated);
        building = new Building(gameState, buildAction);
        detailsSelection = new DetailsSelection(gameState, detailsAction);
    }

    public Wiring wiring(GameEntity source, Conduit conduit) {
        wiring.source = source;
        wiring.conduit = conduit;
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

    public WireRemoval wireRemoval(GameEntity source, Runnable onWireRemoved) {
        return new WireRemoval(gameState, input, source, onWireRemoved);
    }

    private static class Wiring implements PlayerAction {
        private static final Color CIRCLE_COLOR = new Color(0f, 1f, 0f, 0.25f);
        private final GameState gameState;
        private final PlayerInput input;
        private final Runnable onWireCreated;
        private GameEntity source;
        private Conduit conduit;

        public Wiring(GameState gameState, PlayerInput input, Runnable onWireCreated) {
            this.gameState = gameState;
            this.input = input;
            this.onWireCreated = onWireCreated;
        }

        //TODO do not expose handle click or mouse move, register callbacks in PlayerInput instead
        @Override
        public void handleClick(Vector2 mousePosition) {
            GameEntity target = gameState.getEntityAt(mousePosition);
            if (target == null || target == source || !(target instanceof PowerConsumer)) {
                return;
            }
            if (isWithinRange(target)) {
                gameState.registerLink(conduit, (PowerSource) source, (PowerConsumer) target);
                onWireCreated.run();
            }
        }

        private boolean isWithinRange(GameEntity target) {
            int offset = GameAssetManager.TILE_SIZE_PX / 2;
            float sourceX = source.getPosition().x * GameAssetManager.TILE_SIZE_PX + offset;
            float sourceY = source.getPosition().y * GameAssetManager.TILE_SIZE_PX + offset;
            float targetX = target.getPosition().x * GameAssetManager.TILE_SIZE_PX + offset;
            float targetY = target.getPosition().y * GameAssetManager.TILE_SIZE_PX + offset;
            float distance = (float) Math.sqrt(Math.pow(targetX - sourceX, 2) + Math.pow(targetY - sourceY, 2));
            return distance <= conduit.getConnectRange() * GameAssetManager.TILE_SIZE_PX;
        }

        @Override
        public void handleMouseMove(Vector2 mousePosition) {

        }

        @Override
        public void drawTextures(SpriteBatch batch) {

        }

        @Override
        public void drawShapes(ShapeRenderer shapeRenderer) {
            int radius = (conduit.getConnectRange() * GameAssetManager.TILE_SIZE_PX);
            int offset = GameAssetManager.TILE_SIZE_PX / 2;
            int x = ((int) source.getPosition().x * GameAssetManager.TILE_SIZE_PX) + offset;
            int y = ((int) source.getPosition().y * GameAssetManager.TILE_SIZE_PX) + offset;
            shapeRenderer.setColor(CIRCLE_COLOR);
            shapeRenderer.circle(x, y, radius);

            Vector2 mousePosition = input.getMousePosition();
            float distance = mousePosition.dst(x, y);
            if (distance > radius) {
                shapeRenderer.setColor(Color.RED);
            } else {
                shapeRenderer.setColor(Color.GREEN);
            }
            shapeRenderer.line(x, y, mousePosition.x, mousePosition.y);
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
            Level.Tile tile = gameState.getTerrainTile(mousePosition);

            if (tile != null) {
                GameEntity gameEntity = gameState.getEntityAt(mousePosition);
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
        public void drawTextures(SpriteBatch batch) {
            if (mousePosition != null && texture != null) {
                batch.draw(GameAssetManager.INSTANCE.loadTexture(texture),
                    NavigationUtils.alignCoordinateWithMesh(mousePosition.x),
                    NavigationUtils.alignCoordinateWithMesh(mousePosition.y));
            }
        }

        @Override
        public void drawShapes(ShapeRenderer shapeRenderer) {

        }
    }

    private static class DetailsSelection implements PlayerAction {
        private final GameState gameState;
        private final Consumer<String> showDetailsHandler;

        private Vector2 clickPosition;
        private GameEntity selectedEntity;

        public DetailsSelection(GameState gameState, Consumer<String> showDetailsHandler) {
            this.gameState = gameState;
            this.showDetailsHandler = showDetailsHandler;
        }

        @Override
        public void handleClick(Vector2 mousePosition) {
            clickPosition = mousePosition;
            selectedEntity = gameState.getEntityAt(clickPosition);
            showDetailsHandler.accept(selectedEntity != null ? selectedEntity.getDetails() : "");
        }

        @Override
        public void handleMouseMove(Vector2 mousePosition) {
            if (selectedEntity != null) {
                showDetailsHandler.accept(selectedEntity.getDetails());
            }
        }

        @Override
        public void drawTextures(SpriteBatch batch) {
            if (clickPosition != null) {
                batch.draw(GameAssetManager.INSTANCE.loadTexture("assets/visual/select-reticle.png"),
                    NavigationUtils.alignCoordinateWithMesh(clickPosition.x),
                    NavigationUtils.alignCoordinateWithMesh(clickPosition.y));
            }
        }

        @Override
        public void drawShapes(ShapeRenderer shapeRenderer) {

        }
    }

    private static class WireRemoval implements PlayerAction {
        private static final Color HIGHLIGHT_COLOR = new Color(1f, 0.5f, 0f, 0.5f);
        private static final Color PREVIEW_COLOR = new Color(1f, 0f, 0f, 0.75f);

        private final GameState gameState;
        private final PlayerInput input;
        private final GameEntity source;
        private final Runnable onWireRemoved;
        private final List<ConduitEntity> connectedConduits;
        private GameEntity hoveredEntity;

        public WireRemoval(GameState gameState, PlayerInput input, GameEntity source, Runnable onWireRemoved) {
            this.gameState = gameState;
            this.input = input;
            this.source = source;
            this.onWireRemoved = onWireRemoved;
            this.connectedConduits = gameState.findConnectedConduits(source);
        }

        @Override
        public void handleClick(Vector2 mousePosition) {
            GameEntity target = gameState.getEntityAt(mousePosition);
            if (target == null || !(target instanceof PowerConsumer)) {
                return;
            }
            ConduitEntity conduit = gameState.findConduit((PowerSource) source, (PowerConsumer) target);
            if (conduit != null) {
                gameState.sellConduit(conduit);
                onWireRemoved.run();
            }
        }

        @Override
        public void handleMouseMove(Vector2 mousePosition) {
            hoveredEntity = gameState.getEntityAt(mousePosition);
        }

        @Override
        public void drawTextures(SpriteBatch batch) {
        }

        @Override
        public void drawShapes(ShapeRenderer shapeRenderer) {
            int offset = GameConstants.TILE_SIZE_PX / 2;
            for (ConduitEntity conduit : connectedConduits) {
                if (conduit.from != source) {
                    continue;
                }
                if (!(conduit.to instanceof GameEntity)) {
                    continue;
                }
                GameEntity consumer = (GameEntity) conduit.to;
                float cx = consumer.getPosition().x * GameConstants.TILE_SIZE_PX + offset;
                float cy = consumer.getPosition().y * GameConstants.TILE_SIZE_PX + offset;
                shapeRenderer.setColor(HIGHLIGHT_COLOR);
                shapeRenderer.circle(cx, cy, offset);
            }

            if (hoveredEntity != null && hoveredEntity != source && hoveredEntity instanceof PowerConsumer) {
                ConduitEntity hovered = gameState.findConduit((PowerSource) source, (PowerConsumer) hoveredEntity);
                if (hovered != null && hovered.to instanceof GameEntity) {
                    float sx = source.getPosition().x * GameConstants.TILE_SIZE_PX + offset;
                    float sy = source.getPosition().y * GameConstants.TILE_SIZE_PX + offset;
                    float tx = ((GameEntity) hovered.to).getPosition().x * GameConstants.TILE_SIZE_PX + offset;
                    float ty = ((GameEntity) hovered.to).getPosition().y * GameConstants.TILE_SIZE_PX + offset;
                    shapeRenderer.setColor(PREVIEW_COLOR);
                    shapeRenderer.rectLine(sx, sy, tx, ty, 4f);
                }
            }
        }
    }
}

