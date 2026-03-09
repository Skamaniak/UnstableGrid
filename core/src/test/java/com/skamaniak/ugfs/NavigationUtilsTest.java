package com.skamaniak.ugfs;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NavigationUtilsTest {

    @Test
    void alignCoordinateWithMesh_snapsToGrid() {
        assertEquals(64f, NavigationUtils.alignCoordinateWithMesh(100f), 0.001f);
    }

    @Test
    void alignCoordinateWithMesh_withOffset() {
        // 100 → snaps to 64, then adds offset 32 → 96
        assertEquals(96f, NavigationUtils.alignCoordinateWithMesh(100f, 32f), 0.001f);
    }

    @Test
    void alignCoordinateWithMesh_exactBoundary() {
        assertEquals(128f, NavigationUtils.alignCoordinateWithMesh(128f), 0.001f);
    }

    @Test
    void alignCoordinateWithMesh_zero() {
        assertEquals(0f, NavigationUtils.alignCoordinateWithMesh(0f), 0.001f);
    }

    @Test
    void meshVectorFromWorldVector() {
        Vector2 result = NavigationUtils.meshVectorFromWorldVector(new Vector2(130f, 200f));
        assertEquals(2f, result.x, 0.001f);
        assertEquals(3f, result.y, 0.001f);
    }

    @Test
    void worldCoordinateIntoMeshCoordinate() {
        assertEquals(0, NavigationUtils.worldCoordinateIntoMeshCoordinate(0f));
        assertEquals(0, NavigationUtils.worldCoordinateIntoMeshCoordinate(63f));
        assertEquals(1, NavigationUtils.worldCoordinateIntoMeshCoordinate(64f));
        assertEquals(1, NavigationUtils.worldCoordinateIntoMeshCoordinate(127f));
        assertEquals(2, NavigationUtils.worldCoordinateIntoMeshCoordinate(128f));
    }
}
