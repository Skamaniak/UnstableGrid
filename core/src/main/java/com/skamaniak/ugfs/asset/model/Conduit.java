package com.skamaniak.ugfs.asset.model;

public class Conduit extends GameAsset {
    private int powerTransferRate;
    private int powerTransferLoss;
    private int connectRange;
    private int scrapCost;

    public int getPowerTransferRate() {
        return powerTransferRate;
    }

    public int getPowerTransferLoss() {
        return powerTransferLoss;
    }

    public int getConnectRange() {
        return connectRange;
    }

    public int getScrapCost() {
        return scrapCost;
    }
}
