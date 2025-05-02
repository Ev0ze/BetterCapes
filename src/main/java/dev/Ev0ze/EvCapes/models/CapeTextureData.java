package dev.Ev0ze.EvCapes.models;

import com.destroystokyo.paper.profile.ProfileProperty;

/**
 * Represents cached cape texture data
 */
public class CapeTextureData {
    private final String value;
    private final String signature;
    private final long timestamp;
    private final JsonCapeData capeData;

    /**
     * Creates a new CapeTextureData
     * @param value The texture value
     * @param signature The texture signature
     * @param capeData The cape data extracted from the texture
     */
    public CapeTextureData(String value, String signature, JsonCapeData capeData) {
        this.value = value;
        this.signature = signature;
        this.timestamp = System.currentTimeMillis();
        this.capeData = capeData;
    }

    /**
     * Gets the texture value
     * @return The texture value
     */
    public String getValue() {
        return value;
    }

    /**
     * Gets the texture signature
     * @return The texture signature
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Gets the timestamp when this data was created
     * @return The timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the cape data extracted from the texture
     * @return The cape data
     */
    public JsonCapeData getCapeData() {
        return capeData;
    }

    /**
     * Checks if this data is expired
     * @param expirationTime The expiration time in milliseconds
     * @return true if expired, false otherwise
     */
    public boolean isExpired(long expirationTime) {
        return System.currentTimeMillis() - timestamp > expirationTime;
    }

    /**
     * Creates a ProfileProperty from this data
     * @return A new ProfileProperty
     */
    public ProfileProperty toProfileProperty() {
        return new ProfileProperty("textures", value, signature);
    }
}
