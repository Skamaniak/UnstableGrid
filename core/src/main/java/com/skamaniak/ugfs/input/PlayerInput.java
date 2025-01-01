package com.skamaniak.ugfs.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;


public class PlayerInput extends InputAdapter {
    private final Set<Integer> pressed = new HashSet<>();
    private final Vector2 leftClickPosition = new Vector2();
    private final Vector2 rightClickPosition = new Vector2();
    private final Vector2 mousePosition = new Vector2();
    private final Function<Vector2, Vector2> unproject;

    public PlayerInput(Function<Vector2, Vector2> unproject) {
        this.unproject = unproject;
    }

    public boolean isPressed(KeyboardControls control) {
        for (int key : control.getKeys()) {
            if (pressed.contains(key)) {
                return true;
            }
        }
        return false;
    }

    public Vector2 getLeftClickPosition() {
        return leftClickPosition;
    }

    public Vector2 getRightClickPosition() {
        return rightClickPosition;
    }

    public Vector2 getMousePosition() {
        return mousePosition;
    }

    public boolean justClicked(int mouseButton) {
        return Gdx.input.isButtonJustPressed(mouseButton);
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
        if (button == Input.Buttons.LEFT) {
            leftClickPosition.set(screenX, screenY);
            unproject.apply(leftClickPosition);
            return true;
        } else if (button == Input.Buttons.RIGHT) {
            rightClickPosition.set(screenX, screenY);
            unproject.apply(rightClickPosition);
            return true;
        }
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
        mousePosition.set(screenX, screenY);
        unproject.apply(mousePosition);
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }

}
