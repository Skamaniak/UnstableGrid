package com.skamaniak.ugfs.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.GameConstants;
import com.skamaniak.ugfs.UnstableGrid;
import com.skamaniak.ugfs.asset.GameAssetManager;
import com.skamaniak.ugfs.asset.model.Conduit;
import com.skamaniak.ugfs.asset.model.Enemy;
import com.skamaniak.ugfs.asset.model.Level;
import com.skamaniak.ugfs.asset.model.Terrain;
import com.skamaniak.ugfs.asset.model.TerrainType;
import com.skamaniak.ugfs.game.entity.*;
import com.skamaniak.ugfs.simulation.PowerConsumer;
import com.skamaniak.ugfs.simulation.PowerGrid;
import com.skamaniak.ugfs.simulation.PowerSource;

import com.skamaniak.ugfs.game.effect.*;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class GameState {
    public final UnstableGrid game;
    private final Level level;
    private final PowerGrid grid;

    private final Set<GeneratorEntity> generators = new HashSet<>();
    private final Set<PowerStorageEntity> storages = new HashSet<>();
    private final Set<TowerEntity> towers = new HashSet<>();
    private final Set<ConduitEntity> conduits = new HashSet<>();

    private final Map<String, GameEntity> entityByPosition = new HashMap<>();
    private final Map<String, Level.Tile> tileByPosition = new HashMap<>();

    private final List<EnemyInstance> enemies = new ArrayList<>();
    private final WaveManager waveManager;
    private final TilePathfinder pathfinder;
    private boolean gameOver;
    private boolean victory;

    private final Color healthBarColor = new Color();
    private final Map<GameEntity, int[]> wireCountCache = new HashMap<>();
    private final List<VisualEffect> effects = new ArrayList<>();
    private final List<VisualEffect> pendingEffects = new ArrayList<>();
    private static final Random soundRandom = new Random();
    private static final Color EMP_COLOR = new Color(0.3f, 0.5f, 1f, 1f);

    private int scrap;

    public GameState(UnstableGrid game, Level level) {
        this.game = game;
        this.level = level;

        this.scrap = level.getScrap();
        this.grid = new PowerGrid();

        for (Level.Tile tile : level.getMap()) {
            tileByPosition.put(tileKey(tile.getX(), tile.getY()), tile);
        }

        // Build terrain grid for pathfinding
        int w = level.getLevelWidth();
        int h = level.getLevelHeight();
        TerrainType[][] terrainGrid = new TerrainType[w][h];
        for (TerrainType[] col : terrainGrid) Arrays.fill(col, TerrainType.WATER);
        for (Level.Tile tile : level.getMap()) {
            Terrain terrain = GameAssetManager.INSTANCE.getTerrain(tile.getTerrainId());
            terrainGrid[tile.getX()][tile.getY()] = terrain.getTerrainType();
        }
        this.pathfinder = new TilePathfinder(terrainGrid, w, h, entityByPosition);

        // Initialize wave manager
        Level.Position base = level.getBase();
        if (base != null && level.getWaves() != null) {
            final int baseX = base.getX();
            final int baseY = base.getY();
            this.waveManager = new WaveManager(
                level.getWaves(),
                level.getSpawnLocations(),
                new WaveManager.EnemyLookup() {
                    @Override
                    public com.skamaniak.ugfs.asset.model.Enemy getEnemy(String id) {
                        return GameAssetManager.INSTANCE.getEnemy(id);
                    }
                },
                new WaveManager.PathComputer() {
                    @Override
                    public List<Vector2> computePath(Vector2 spawnWorldPos, boolean flying) {
                        if (flying) {
                            List<Vector2> flyPath = new ArrayList<>();
                            flyPath.add(new Vector2(spawnWorldPos));
                            flyPath.add(new Vector2(
                                (baseX + 0.5f) * GameConstants.TILE_SIZE_PX,
                                (baseY + 0.5f) * GameConstants.TILE_SIZE_PX));
                            return flyPath;
                        }
                        int sx = (int) (spawnWorldPos.x / GameConstants.TILE_SIZE_PX);
                        int sy = (int) (spawnWorldPos.y / GameConstants.TILE_SIZE_PX);
                        return pathfinder.findPath(sx, sy, baseX, baseY);
                    }
                }
            );
        } else {
            this.waveManager = null;
        }

    }

    public void registerEntity(GameEntity entity) {
        if (entity instanceof TowerEntity) {
            registerTower((TowerEntity) entity);
        } else if (entity instanceof PowerStorageEntity) {
            registerPowerStorage((PowerStorageEntity) entity);
        } else if (entity instanceof GeneratorEntity) {
            registerGenerator((GeneratorEntity) entity);
        } else {
            throw new RuntimeException("Unknown entity " + entity);
        }
    }

    public void registerTower(TowerEntity tower) {
        towers.add(tower);
        entityByPosition.put(positionKey(tower), tower);
        grid.addSink(tower);
    }

    public void registerGenerator(GeneratorEntity generator) {
        generators.add(generator);
        entityByPosition.put(positionKey(generator), generator);
        grid.addSource(generator);
    }

    public void registerPowerStorage(PowerStorageEntity storage) {
        storages.add(storage);
        entityByPosition.put(positionKey(storage), storage);
        grid.addStorage(storage);
    }

    public void registerLinkFree(Conduit conduit, PowerSource source, PowerConsumer destination) {
        ConduitEntity existing = findConduit(source, destination);
        if (existing != null) {
            if (existing.conduit.equals(conduit)) {
                return;
            }
            conduits.remove(existing);
            grid.removeConduit(existing);
        }
        ConduitEntity conduitEntity = new ConduitEntity(conduit, source, destination);
        conduits.add(conduitEntity);
        grid.addConduit(conduitEntity);
    }

    public void registerLink(Conduit conduit, PowerSource source, PowerConsumer destination) {
        // Replace existing wire between same endpoints (refund old, charge difference)
        ConduitEntity existing = findConduit(source, destination);
        if (existing != null) {
            if (existing.conduit.equals(conduit)) {
                return; // exact same wire type, nothing to do
            }
            sellConduit(existing);
        }

        if (!spendScrap(conduit.getScrapCost())) {
            return;
        }
        ConduitEntity conduitEntity = new ConduitEntity(conduit, source, destination);
        conduits.add(conduitEntity);
        grid.addConduit(conduitEntity);
    }

    public int getScrap() {
        return scrap;
    }

    public void addScrap(int amount) {
        scrap += amount;
    }

    public boolean spendScrap(int amount) {
        if (scrap < amount) {
            return false;
        }
        scrap -= amount;
        return true;
    }

    public void sellConduit(ConduitEntity conduit) {
        conduits.remove(conduit);
        grid.removeConduit(conduit);
        addScrap(conduit.conduit.getScrapCost());
    }

    public void sellGenerator(GeneratorEntity generator) {
        List<ConduitEntity> connected = findConnectedConduits(generator);
        for (ConduitEntity conduit : connected) {
            sellConduit(conduit);
        }
        generators.remove(generator);
        entityByPosition.remove(positionKey(generator));
        grid.removeSource(generator);
        addScrap(generator.getTotalScrapInvested());
    }

    public void sellStorage(PowerStorageEntity storage) {
        List<ConduitEntity> connected = findConnectedConduits(storage);
        for (ConduitEntity conduit : connected) {
            sellConduit(conduit);
        }
        storages.remove(storage);
        entityByPosition.remove(positionKey(storage));
        grid.removeStorage(storage);
        addScrap(storage.getTotalScrapInvested());
    }

    public void sellTower(TowerEntity tower) {
        List<ConduitEntity> connected = findConnectedConduits(tower);
        for (ConduitEntity conduit : connected) {
            sellConduit(conduit);
        }
        towers.remove(tower);
        entityByPosition.remove(positionKey(tower));
        grid.removeSink(tower);
        addScrap(tower.getTotalScrapInvested());
    }

    public void sellEntity(GameEntity entity) {
        if (entity instanceof TowerEntity) {
            sellTower((TowerEntity) entity);
        } else if (entity instanceof PowerStorageEntity) {
            sellStorage((PowerStorageEntity) entity);
        } else if (entity instanceof GeneratorEntity) {
            sellGenerator((GeneratorEntity) entity);
        } else {
            throw new RuntimeException("Unknown entity " + entity);
        }
        repathAliveEnemies();
    }

    private void repathAliveEnemies() {
        Level.Position base = level.getBase();
        if (base == null) {
            return;
        }
        int baseX = base.getX();
        int baseY = base.getY();
        for (int i = 0, n = enemies.size(); i < n; i++) {
            EnemyInstance enemy = enemies.get(i);
            if (!enemy.isAlive() || enemy.isFlying()) {
                continue;
            }
            Vector2 pos = enemy.getWorldPosition();
            int sx = (int) (pos.x / GameConstants.TILE_SIZE_PX);
            int sy = (int) (pos.y / GameConstants.TILE_SIZE_PX);
            List<Vector2> newPath = pathfinder.findPath(sx, sy, baseX, baseY);
            enemy.repath(newPath);
        }
    }

    public boolean upgradeEntity(GameEntity entity) {
        if (!entity.canUpgrade()) {
            return false;
        }
        if (!spendScrap(entity.getUpgradeCost())) {
            return false;
        }
        entity.applyUpgrade();
        return true;
    }

    public ConduitEntity findConduit(PowerSource from, PowerConsumer to) {
        for (ConduitEntity conduit : conduits) {
            if (conduit.from == from && conduit.to == to) {
                return conduit;
            }
        }
        return null;
    }

    public boolean hasOutgoingConduits(PowerSource source) {
        for (ConduitEntity conduit : conduits) {
            if (conduit.from == source) {
                return true;
            }
        }
        return false;
    }

    public List<ConduitEntity> findConnectedConduits(Object entity) {
        List<ConduitEntity> result = new ArrayList<>();
        for (ConduitEntity conduit : conduits) {
            if (conduit.from == entity || conduit.to == entity) {
                result.add(conduit);
            }
        }
        return result;
    }

    public int computeSellValue(GameEntity entity) {
        int total = entity.getTotalScrapInvested();
        for (ConduitEntity conduit : findConnectedConduits(entity)) {
            total += conduit.conduit.getScrapCost();
        }
        return total;
    }

    public Set<GeneratorEntity> getGenerators() {
        return generators;
    }

    public Set<PowerStorageEntity> getStorages() {
        return storages;
    }

    public Set<TowerEntity> getTowers() {
        return towers;
    }

    public Set<ConduitEntity> getConduits() {
        return conduits;
    }

    public void readInputs() {
        // TODO read inputs
    }

    public Level.Tile getTerrainTile(Vector2 coordinates) {
        return getTerrainTile((int) coordinates.x, (int) coordinates.y);
    }

    public Level.Tile getTerrainTile(int x, int y) {
        return tileByPosition.get(tileKey(x / GameConstants.TILE_SIZE_PX, y / GameConstants.TILE_SIZE_PX));
    }

    public GameEntity getEntityAt(Vector2 coordinates) {
        return getEntityAt((int) coordinates.x, (int) coordinates.y);
    }

    public GameEntity getEntityAt(int x, int y) {
        return entityByPosition.get(tileKey(x / GameConstants.TILE_SIZE_PX, y / GameConstants.TILE_SIZE_PX));
    }

    private static String positionKey(GameEntity entity) {
        return tileKey((int) entity.getPosition().x, (int) entity.getPosition().y);
    }

    private static String tileKey(int x, int y) {
        return x + "," + y;
    }

    public void simulate(float delta) {
        grid.simulatePropagation(delta);
        simulateShooting(delta);
        simulateEnemies(delta);
        updateEffects(delta);
    }

    private void simulateShooting(float delta) {
        for (TowerEntity tower : towers) {
            if (tower.attemptShot(delta, enemies)) {
                playTowerSound(tower);
                spawnShotEffect(tower);
            }
        }
    }

    private void playTowerSound(TowerEntity tower) {
        float pitch = 0.85f + soundRandom.nextFloat() * 0.30f;
        float volume = 0.90f + soundRandom.nextFloat() * 0.20f;
        GameAssetManager.INSTANCE.loadSound(tower.tower.getShotSound()).play(volume, pitch, 0f);
    }

    private void spawnShotEffect(TowerEntity tower) {
        ShotResult sr = tower.getShotResult();
        if (!sr.fired) {
            return;
        }

        String towerId = tower.tower.getId();
        if ("tower.laser".equals(towerId)) {
            effects.add(new LaserBeamEffect(sr.towerX, sr.towerY, sr.targetX, sr.targetY));
            effects.add(new ImpactEffect(sr.targetX, sr.targetY));
        } else if ("tower.tesla".equals(towerId)) {
            effects.add(new LightningArcEffect(sr.towerX, sr.towerY, sr.targetX, sr.targetY, soundRandom));
            effects.add(new ImpactEffect(sr.targetX, sr.targetY));
        } else if ("tower.plasma".equals(towerId)) {
            effects.add(new PlasmaProjectileEffect(sr.towerX, sr.towerY, sr.targetEnemy, sr.damage, pendingEffects));
            // Impact spawned on arrival by PlasmaProjectileEffect into pendingEffects
        } else if ("tower.microwave".equals(towerId)) {
            effects.add(new AoePulseEffect(sr.towerX, sr.towerY, sr.rangePx, Color.ORANGE));
            spawnAoeImpacts(sr);
        } else if ("tower.emp".equals(towerId)) {
            effects.add(new AoePulseEffect(sr.towerX, sr.towerY, sr.rangePx, EMP_COLOR));
            spawnAoeImpacts(sr);
        }
    }

    private void spawnAoeImpacts(ShotResult sr) {
        for (int i = 0; i < sr.aoeTargetCount; i++) {
            EnemyInstance enemy = sr.aoeTargets[i];
            effects.add(new ImpactEffect(enemy.getWorldCenter().x, enemy.getWorldCenter().y));
        }
    }

    private void updateEffects(float delta) {
        Iterator<VisualEffect> it = effects.iterator();
        while (it.hasNext()) {
            VisualEffect effect = it.next();
            effect.update(delta);
            if (!effect.isAlive()) {
                it.remove();
            }
        }
        effects.addAll(pendingEffects);
        pendingEffects.clear();
    }

    private void simulateEnemies(float delta) {
        if (waveManager != null) {
            int aliveCount = 0;
            for (int i = 0, n = enemies.size(); i < n; i++) {
                if (enemies.get(i).isAlive()) {
                    aliveCount++;
                }
            }
            List<EnemyInstance> spawned = waveManager.update(delta, aliveCount);
            enemies.addAll(spawned);
        }

        for (int i = 0, n = enemies.size(); i < n; i++) {
            enemies.get(i).move(delta);
        }

        Iterator<EnemyInstance> it = enemies.iterator();
        while (it.hasNext()) {
            EnemyInstance enemy = it.next();
            if (enemy.hasReachedBase()) {
                gameOver = true;
                it.remove();
            } else if (!enemy.isAlive()) {
                addScrap(enemy.getEnemy().getScrap());
                it.remove();
            }
        }

        if (waveManager != null) {
            WaveManager.WaveStatus status = waveManager.getWaveStatus();
            if (!gameOver && status.isAllWavesExhausted() && enemies.isEmpty()) {
                victory = true;
            }
        }
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isVictory() {
        return victory;
    }

    public WaveManager.WaveStatus getWaveStatus() {
        if (waveManager == null) {
            return null;
        }
        return waveManager.getWaveStatus();
    }

    public boolean isBuildingAllowed() {
        if (waveManager == null) {
            return true;
        }
        return !waveManager.getWaveStatus().isWaveActive();
    }

    public int getAliveEnemyCount() {
        int count = 0;
        for (int i = 0, n = enemies.size(); i < n; i++) {
            if (enemies.get(i).isAlive()) {
                count++;
            }
        }
        return count;
    }

    public void drawTextures(float delta) {
        drawTerrain();
        drawGameEntities();

        for (int i = 0, n = effects.size(); i < n; i++) {
            effects.get(i).drawTextures(game.batch);
        }
    }

    public void drawShapes(ShapeRenderer shapeRenderer) {
        for (GeneratorEntity generator : generators) {
            generator.drawChevrons(shapeRenderer);
        }
        for (PowerStorageEntity storage : storages) {
            storage.drawChevrons(shapeRenderer);
        }
        for (TowerEntity tower : towers) {
            tower.drawChevrons(shapeRenderer);
        }

        // Draw spawn points
        if (level.getSpawnLocations() != null) {
            shapeRenderer.setColor(Color.RED);
            for (Level.SpawnLocation spawn : level.getSpawnLocations()) {
                float cx = (spawn.getX() + 0.5f) * GameConstants.TILE_SIZE_PX;
                float cy = (spawn.getY() + 0.5f) * GameConstants.TILE_SIZE_PX;
                shapeRenderer.circle(cx, cy, 20);
            }
        }

        // Draw base
        Level.Position base = level.getBase();
        if (base != null) {
            shapeRenderer.setColor(Color.GREEN);
            float bx = (base.getX() + 0.5f) * GameConstants.TILE_SIZE_PX;
            float by = (base.getY() + 0.5f) * GameConstants.TILE_SIZE_PX;
            shapeRenderer.circle(bx, by, 20);
        }

        // Draw enemies
        for (int i = 0, n = enemies.size(); i < n; i++) {
            EnemyInstance enemy = enemies.get(i);
            if (!enemy.isAlive()) {
                continue;
            }
            float ex = enemy.getWorldPosition().x;
            float ey = enemy.getWorldPosition().y;
            Enemy enemyAsset = enemy.getEnemy();
            float[] color = enemyAsset.getColor();
            int radius = enemyAsset.getRadius();

            // Body
            shapeRenderer.setColor(color[0], color[1], color[2], 1f);
            if ("triangle".equals(enemyAsset.getShape())) {
                float halfBase = radius * 0.866f;
                float topY = ey + radius;
                float bottomY = ey - radius * 0.5f;
                shapeRenderer.triangle(
                    ex - halfBase, bottomY,
                    ex + halfBase, bottomY,
                    ex, topY);
            } else {
                shapeRenderer.circle(ex, ey, radius);
            }

            // Health bar background
            float barOffset = radius + 4;
            shapeRenderer.setColor(Color.DARK_GRAY);
            shapeRenderer.rect(ex - 12, ey - barOffset - 4, 24, 4);

            // Health bar foreground
            float hf = enemy.getHealthFraction();
            healthBarColor.set(Color.RED).lerp(Color.GREEN, hf);
            shapeRenderer.setColor(healthBarColor);
            shapeRenderer.rect(ex - 12, ey - barOffset - 4, 24 * hf, 4);
        }

        // Draw visual effects
        for (int i = 0, n = effects.size(); i < n; i++) {
            effects.get(i).drawShapes(shapeRenderer);
        }
    }

    private void drawTerrain() {
        level.getMap().forEach(tile -> {
            TextureRegion texture = GameAssetManager.INSTANCE.loadTerrainTileTexture(tile);
            game.batch.draw(texture, tile.getX() * GameConstants.TILE_SIZE_PX,
                tile.getY() * GameConstants.TILE_SIZE_PX);
        });
    }

    private void drawGameEntities() {
        for (ConduitEntity conduit : conduits) {
            conduit.draw(game.batch);
        }

        for (GeneratorEntity generator : generators) {
            generator.draw(game.batch);
        }

        for (PowerStorageEntity storage : storages) {
            storage.draw(game.batch);
        }

        for (TowerEntity tower : towers) {
            tower.draw(game.batch);
        }

        if (GameConstants.wireOverlayDetailed) {
            drawWireCountBadges();
        }
    }

    private void drawWireCountBadges() {
        // Compute wire counts in a single pass over conduits
        for (int[] count : wireCountCache.values()) {
            count[0] = 0;
        }
        for (ConduitEntity conduit : conduits) {
            if (conduit.from instanceof GameEntity) {
                incrementWireCount((GameEntity) conduit.from);
            }
            if (conduit.to instanceof GameEntity) {
                incrementWireCount((GameEntity) conduit.to);
            }
        }
        // Draw badges
        com.badlogic.gdx.graphics.g2d.BitmapFont font = GameAssetManager.INSTANCE.getFont();
        font.setColor(com.badlogic.gdx.graphics.Color.YELLOW);
        for (Map.Entry<GameEntity, int[]> entry : wireCountCache.entrySet()) {
            int count = entry.getValue()[0];
            if (count > 0) {
                GameEntity entity = entry.getKey();
                float x = entity.getPosition().x * GameConstants.TILE_SIZE_PX + 2;
                float y = (entity.getPosition().y + 1) * GameConstants.TILE_SIZE_PX - 2;
                font.draw(game.batch, Integer.toString(count), x, y);
            }
        }
        font.setColor(com.badlogic.gdx.graphics.Color.WHITE);
    }

    private void incrementWireCount(GameEntity entity) {
        int[] count = wireCountCache.get(entity);
        if (count == null) {
            count = new int[]{0};
            wireCountCache.put(entity, count);
        }
        count[0]++;
    }

}
