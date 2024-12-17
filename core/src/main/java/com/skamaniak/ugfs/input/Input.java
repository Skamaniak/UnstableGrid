package com.skamaniak.ugfs.input;

import com.badlogic.gdx.InputAdapter;

import java.util.HashSet;
import java.util.Set;


public class Input extends InputAdapter {
    private final Set<Integer> pressed = new HashSet<>();

    public boolean isPressed(KeyboardControls control) {
        for (int key : control.getKeys()) {
            if (pressed.contains(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyDown(int keycode) {
        return pressed.add(keycode);
    }

    @Override
    public boolean keyUp(int keycode) {
        return pressed.remove(keycode);
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }


    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }
}
