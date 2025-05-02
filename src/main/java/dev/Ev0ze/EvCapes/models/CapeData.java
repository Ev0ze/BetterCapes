package dev.Ev0ze.EvCapes.models;

/**
 * Represents data for a Minecraft cape
 */
public class CapeData {
    private String uuid;
    private String alias;
    private String url;
    private boolean supported;
    private String playerWithCape; // Player name who has this cape

    public CapeData(String uuid, String alias, String url, boolean supported) {
        this.uuid = uuid;
        this.alias = alias;
        this.url = url;
        this.supported = supported;
    }

    public String getUuid() {
        return uuid;
    }

    public String getAlias() {
        return alias;
    }

    public String getUrl() {
        return url;
    }

    public boolean isSupported() {
        return supported;
    }

    public String getPlayerWithCape() {
        return playerWithCape;
    }

    public void setPlayerWithCape(String playerWithCape) {
        this.playerWithCape = playerWithCape;
    }

    @Override
    public String toString() {
        return "CapeData{" +
                "alias='" + alias + '\'' +
                ", playerWithCape='" + playerWithCape + '\'' +
                '}';
    }
}
