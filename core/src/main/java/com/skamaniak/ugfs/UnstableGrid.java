package com.skamaniak.ugfs;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.skamaniak.ugfs.asset.GameAssetManager;
import com.skamaniak.ugfs.asset.model.Level;
import com.skamaniak.ugfs.input.Input;

public class UnstableGrid extends Game {
    public Input input;

    public Level level;
    public SpriteBatch batch;
    public BitmapFont font;

    public void create() {
        batch = new SpriteBatch();

        input = new Input();

        level = GameAssetManager.INSTANCE.getLevel("level.tutorial"); //Tutorial

        // use libGDX's default font
        font = new BitmapFont();


        this.setScreen(new MainMenuScreen(this));
    }

    public void render() {
        ScreenUtils.clear(Color.BLACK);
        super.render();
    }

    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
