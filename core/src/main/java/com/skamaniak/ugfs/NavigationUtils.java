package com.skamaniak.ugfs;

import com.badlogic.gdx.math.Vector2;

public class NavigationUtils {


    public static float alignCoordinateWithMesh(float coordinate) {
        return alignCoordinateWithMesh(coordinate, 0f);
    }

    public static float alignCoordinateWithMesh(float coordinate, float offset) {
        return (coordinate - coordinate % GameConstants.TILE_SIZE_PX) + offset;
    }

    public static Vector2 meshVectorFromWorldVector(Vector2 worldVector) {
        return new Vector2(worldCoordinateIntoMeshCoordinate(worldVector.x),
            worldCoordinateIntoMeshCoordinate(worldVector.y));
    }

    public static int worldCoordinateIntoMeshCoordinate(float coordinate) {
        return (int) coordinate / GameConstants.TILE_SIZE_PX;
    }
}
