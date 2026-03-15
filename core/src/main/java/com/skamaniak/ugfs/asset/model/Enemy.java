package com.skamaniak.ugfs.asset.model;

public class Enemy extends GameAsset {
    private int health;
    private float speed;
    private int scrap;
    private boolean flying;

    public int getHealth() {
        return health;
    }

    public float getSpeed() {
        return speed;
    }

    public int getScrap() {
        return scrap;
    }

    public boolean isFlying() {
        return flying;
    }
}
