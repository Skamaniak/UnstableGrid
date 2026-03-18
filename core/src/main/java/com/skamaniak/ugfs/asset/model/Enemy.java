package com.skamaniak.ugfs.asset.model;

public class Enemy extends GameAsset {
    private int health;
    private float speed;
    private int scrap;
    private boolean flying;
    private String shape;
    private float[] color;
    private int radius;

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

    public String getShape() {
        return shape;
    }

    public float[] getColor() {
        return color;
    }

    public int getRadius() {
        return radius;
    }
}
