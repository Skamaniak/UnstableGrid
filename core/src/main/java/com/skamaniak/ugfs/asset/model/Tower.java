package com.skamaniak.ugfs.asset.model;

import java.util.List;

public class Tower extends GameAsset {
    private String menuIcon;
    private String texture;
    private String shotSound;
    private String damageType;
    private String targeting;
    private List<Level> levels;

    public String getMenuIcon() {
        return menuIcon;
    }

    public String getTexture() {
        return texture;
    }

    public String getShotSound() {
        return shotSound;
    }

    public String getDamageType() {
        return damageType;
    }

    public String getTargeting() {
        return targeting;
    }

    public List<Level> getLevels() {
        return levels;
    }

    public static class Level {
        private int level;
        private int energyStorage;
        private int energyIntakeRate;
        private int energyCostShot;
        private int energyCostStandby;
        private float towerRange;
        private float fireRate;
        private int damage;
        private int scrapCost;

        public int getLevel() {
            return level;
        }

        public int getEnergyStorage() {
            return energyStorage;
        }

        public int getEnergyIntakeRate() {
            return energyIntakeRate;
        }

        public int getEnergyCostShot() {
            return energyCostShot;
        }

        public int getEnergyCostStandby() {
            return energyCostStandby;
        }

        public float getTowerRange() {
            return towerRange;
        }

        public float getFireRate() {
            return fireRate;
        }

        public int getDamage() {
            return damage;
        }

        public int getScrapCost() {
            return scrapCost;
        }

    }
}
