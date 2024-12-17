package com.skamaniak.ugfs.asset.model;

import java.util.List;

public class Generator extends GameAsset {
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
        private int energyStorage;
        private int energyGenerationRate;
        private int connectRange;
        private int scrapCost;

        public int getLevel() {
            return level;
        }

        public int getEnergyStorage() {
            return energyStorage;
        }

        public int getEnergyGenerationRate() {
            return energyGenerationRate;
        }

        public int getConnectRange() {
            return connectRange;
        }

        public int getScrapCost() {
            return scrapCost;
        }
    }

}
