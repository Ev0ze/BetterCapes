package dev.Ev0ze.EvCapes.models;

import com.google.gson.JsonObject;

/**
 * Represents cape data extracted from a texture JSON
 */
public class JsonCapeData {
    private final JsonObject capeObject;
    private final boolean hasCape;

    /**
     * Creates a new JsonCapeData
     * @param capeObject The cape JSON object
     * @param hasCape Whether the texture has a cape
     */
    public JsonCapeData(JsonObject capeObject, boolean hasCape) {
        this.capeObject = capeObject;
        this.hasCape = hasCape;
    }

    /**
     * Gets the cape JSON object
     * @return The cape JSON object
     */
    public JsonObject getCapeObject() {
        return capeObject;
    }

    /**
     * Checks if the texture has a cape
     * @return true if the texture has a cape, false otherwise
     */
    public boolean hasCape() {
        return hasCape;
    }
}
