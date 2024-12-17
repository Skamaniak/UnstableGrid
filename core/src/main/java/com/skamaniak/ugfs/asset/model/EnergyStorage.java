package com.skamaniak.ugfs.asset.model;

import java.util.List;

public class EnergyStorage extends GameAsset {
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
        private int energyIntakeRate;
        private int energyOutputRate;
        private int energyCostStandby;
        private int connectRange;
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

        public int getEnergyOutputRate() {
            return energyOutputRate;
        }

        public int getEnergyCostStandby() {
            return energyCostStandby;
        }

        public int getConnectRange() {
            return connectRange;
        }

        public int getScrapCost() {
            return scrapCost;
        }
    }
}
