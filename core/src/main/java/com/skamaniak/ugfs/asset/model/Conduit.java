package com.skamaniak.ugfs.asset.model;

public class Conduit extends GameAsset {
    private int energyTransferRate;
    private int energyTransferLoss;
    private int connectRange;
    private int scrapCost;

    public int getEnergyTransferRate() {
        return energyTransferRate;
    }

    public int getEnergyTransferLoss() {
        return energyTransferLoss;
    }

    public int getConnectRange() {
        return connectRange;
    }

    public int getScrapCost() {
        return scrapCost;
    }
}
