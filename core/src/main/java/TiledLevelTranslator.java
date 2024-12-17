public class TiledLevelTranslator {
    public static int[] tiles = new int[] {
        32, 26, 26, 13, 26, 26, 36, 5, 11, 24, 26, 13, 26, 13, 26, 20,
        29, 10, 12, 12, 12, 11, 12, 12, 12, 12, 12, 11, 12, 12, 12, 14,
        16, 12, 5, 12, 12, 12, 12, 12, 5, 12, 12, 12, 11, 5, 12, 27,
        29, 12, 12, 11, 12, 12, 10, 12, 12, 5, 11, 12, 12, 11, 12, 14,
        29, 12, 12, 12, 12, 11, 12, 11, 12, 12, 12, 4, 12, 12, 12, 14,
        16, 12, 12, 5, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 5, 14,
        16, 12, 12, 11, 12, 12, 12, 12, 12, 5, 5, 12, 11, 12, 12, 27,
        29, 11, 12, 12, 12, 12, 5, 12, 12, 12, 12, 12, 12, 12, 12, 27,
        29, 12, 12, 12, 4, 12, 12, 12, 11, 12, 12, 12, 12, 12, 11, 14,
        29, 11, 12, 5, 5, 12, 5, 12, 11, 5, 5, 12, 25, 22, 12, 14,
        16, 12, 12, 12, 5, 12, 12, 12, 12, 12, 25, 28, 21, 29, 5, 14,
        16, 12, 12, 11, 12, 12, 12, 11, 12, 12, 27, 32, 20, 31, 28, 21,
        29, 12, 12, 38, 22, 12, 25, 15, 15, 28, 34, 29, 37, 13, 26, 33,
        16, 12, 25, 34, 31, 28, 34, 32, 26, 26, 26, 23, 5, 12, 12, 27,
        29, 12, 37, 26, 26, 33, 19, 36, 11, 12, 11, 5, 12, 11, 12, 14,
        18, 28, 15, 15, 28, 34, 31, 15, 28, 28, 15, 28, 28, 15, 15, 34
    };


    public static void main(String[] args) {
        int firstClayWaterId = 13; // taken from the json
        int tilesInClayWaterRow = 13; // taken from the tileset
        int tilesInClayRow = 6; // taken from the tileset
        int worldWidth = 16;
        int worldHeight = 16;

        int index = 0;
        firstClayWaterId--;
        for (int tile: tiles) {
            tile --;
            String terrainId;
            int tileNumber;
            int variant;

            if (tile >= firstClayWaterId) {
                tile -= firstClayWaterId;

                terrainId = "terrain.water-clay";
                variant = tile / tilesInClayWaterRow;
                tileNumber = tile % tilesInClayWaterRow;
            } else {
                terrainId = "terrain.clay";
                variant = tile / tilesInClayRow;
                tileNumber = tile % tilesInClayRow;
            }
            System.out.println("{\"x\": " + (index % worldWidth)
                + ", \"y\": " + (worldHeight - ((index / worldWidth) + 1))
                + ", \"terrainId\": " + "\"" + terrainId + "\""
                + ", \"tileNumber\": " + tileNumber
                + ", \"variant\": " + variant
                + "},"
            );
            index++;
        }

    }
}
