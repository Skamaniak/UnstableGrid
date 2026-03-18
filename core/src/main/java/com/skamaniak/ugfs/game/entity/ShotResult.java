package com.skamaniak.ugfs.game.entity;

public class ShotResult {
    private static final int MAX_AOE_TARGETS = 64;

    public boolean fired;
    public float towerX;
    public float towerY;
    public float targetX;
    public float targetY;
    public float rangePx;
    public String targeting;
    public int damage;
    public EnemyInstance targetEnemy;
    public final EnemyInstance[] aoeTargets = new EnemyInstance[MAX_AOE_TARGETS];
    public int aoeTargetCount;

    public void reset() {
        fired = false;
        towerX = 0;
        towerY = 0;
        targetX = 0;
        targetY = 0;
        rangePx = 0;
        targeting = "single";
        damage = 0;
        targetEnemy = null;
        aoeTargetCount = 0;
    }
}
