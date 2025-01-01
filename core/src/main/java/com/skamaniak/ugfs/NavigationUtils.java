package com.skamaniak.ugfs;

import com.badlogic.gdx.math.Vector2;
import com.skamaniak.ugfs.asset.GameAssetManager;

public class NavigationUtils {
    public static float alignCoordinateWithMesh(float coordinate) {
        return coordinate - coordinate % GameAssetManager.TILE_SIZE_PX;
    }

    public static Vector2 meshVectorFromWorldVector(Vector2 worldVector) {
        return new Vector2(worldCoordinateIntoMeshCoordinate(worldVector.x),
            worldCoordinateIntoMeshCoordinate(worldVector.y));
    }

    public static int worldCoordinateIntoMeshCoordinate(float coordinate) {
        return (int) coordinate / GameAssetManager.TILE_SIZE_PX;
    }
}
