package com.skamaniak.ugfs.asset.model;

import java.util.List;

public class PowerStorage extends GameAsset {
    private String menuIcon;
    private String texture;
    private List<Level> levels;

    public String getMenuIcon() {
        return menuIcon;
    }

    public String getTexture() {
        return texture;
    }

    public List<Level> getLevels() {
        return levels;
    }

    public static class Level {
        private int level;
        private int powerStorage;
        private int powerIntakeRate;
        private int powerOutputRate;
        private int powerCostStandby;
        private int scrapCost;

        public int getLevel() {
            return level;
        }

        public int getPowerStorage() {
            return powerStorage;
        }

        public int getPowerIntakeRate() {
            return powerIntakeRate;
        }

        public int getPowerOutputRate() {
            return powerOutputRate;
        }

        public int getPowerCostStandby() {
            return powerCostStandby;
        }

        public int getScrapCost() {
            return scrapCost;
        }
    }
}
