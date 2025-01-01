package com.skamaniak.ugfs.action;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

public interface PlayerAction {
    void handleClick(Vector2 mousePosition);

    void handleMouseMove(Vector2 mousePosition);

    void render(SpriteBatch batch);
}
