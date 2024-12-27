package com.skamaniak.ugfs.game.entity;

import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.asset.model.GameAsset;
import com.skamaniak.ugfs.asset.model.Generator;
import com.skamaniak.ugfs.asset.model.PowerStorage;
import com.skamaniak.ugfs.asset.model.Tower;

public class GameEntityFactory {

    public GameEntity createEntity(Vector2 position, GameAsset asset) {
        if (asset instanceof Generator) {
            return new GeneratorEntity(position, (Generator) asset);
        } else if (asset instanceof PowerStorage) {
            return new PowerStorageEntity(position, (PowerStorage) asset);
        } else if (asset instanceof Tower) {
            return new TowerEntity(position, (Tower) asset);
        } else {
            throw new RuntimeException("Unknown asset " + asset);
        }
    }
}
