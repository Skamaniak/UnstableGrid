package com.skamaniak.ugfs.input;

import java.util.Set;

import static com.badlogic.gdx.Input.Keys.*;

public enum KeyboardControls {
    CAMERA_MOVE_LEFT(A, LEFT),
    CAMERA_MOVE_RIGHT(D, RIGHT),
    CAMERA_MOVE_UP(W, UP),
    CAMERA_MOVE_DOWN(S, DOWN),
    DEBUG_SHOW_FPS(Z),

    CAMERA_ZOOM_IN(E),
    CAMERA_ZOOM_OUT(Q);

    private final Set<Integer> keys;

    KeyboardControls(Integer... keys) {
        this.keys = Set.of(keys);
    }

    public Set<Integer> getKeys() {
        return keys;
    }
}
