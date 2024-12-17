package com.skamaniak.ugfs.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class SceneCamera extends OrthographicCamera {
    public static final float ZOOM_MIN = 0.5f;
    public static final float ZOOM_MAX = 2f;
    public static final float ZOOM_STEP = 1f;
    public static final float MOVE_STEP = 512f;

    public final Rectangle viewBounds = new Rectangle();
    private final Vector2 focusCoordinates;


    public SceneCamera(int x, int y) {
        this.focusCoordinates = new Vector2(x, y);
    }

    public void updatePosition() {
        boolean updateRequired = position.x != focusCoordinates.x || position.y != focusCoordinates.y;
        if (updateRequired) {
            this.position.x = focusCoordinates.x;
            this.position.y = focusCoordinates.y;
            update();
        }
    }

    public void zoomOut() {
        float currentZoom = zoom;
        float nextZoom = zoom + ZOOM_STEP * Gdx.graphics.getDeltaTime();
        zoom = Math.min(nextZoom, ZOOM_MAX);
        if (currentZoom != zoom) {
            update();
        }
    }

    public void zoomIn() {
        float currentZoom = zoom;
        float nextZoom = zoom - ZOOM_STEP * Gdx.graphics.getDeltaTime();
        zoom = Math.max(nextZoom, ZOOM_MIN);
        if (currentZoom != zoom) {
            update();
        }
    }

    public void moveLeft() {
        focusCoordinates.x -= MOVE_STEP * Gdx.graphics.getDeltaTime();
    }

    public void moveRight() {
        focusCoordinates.x += MOVE_STEP * Gdx.graphics.getDeltaTime();
    }

    public void moveUp() {
        focusCoordinates.y += MOVE_STEP * Gdx.graphics.getDeltaTime();
    }

    public void moveDown() {
        focusCoordinates.y -= MOVE_STEP * Gdx.graphics.getDeltaTime();
    }

    public void updateViewBounds() {
        float width = viewportWidth * zoom;
        float height = viewportHeight * zoom;
        float w = width * Math.abs(up.y) + height * Math.abs(up.x);
        float h = height * Math.abs(up.y) + width * Math.abs(up.x);
        viewBounds.set(position.x - w / 2, position.y - h / 2, w, h);
    }
}
