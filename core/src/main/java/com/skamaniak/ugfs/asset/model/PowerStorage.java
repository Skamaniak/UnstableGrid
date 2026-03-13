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

    @Override
    public int getBuildCost() {
        return levels.get(0).getScrapCost();
    }

    public static class Level {
        private int level;
        private int powerStorage;
        private int powerLossStandby;
        private int scrapCost;

        public int getLevel() {
            return level;
        }

        public int getPowerStorage() {
            return powerStorage;
        }

        public int getPowerLossStandby() {
            return powerLossStandby;
        }

        public int getScrapCost() {
            return scrapCost;
        }
    }
}
