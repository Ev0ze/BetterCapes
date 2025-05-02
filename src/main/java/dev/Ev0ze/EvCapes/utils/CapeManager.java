package dev.Ev0ze.EvCapes.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.Ev0ze.EvCapes.api.MineSkinAPI;
import dev.Ev0ze.EvCapes.models.CapeData;
import dev.Ev0ze.EvCapes.models.CapeTextureData;
import dev.Ev0ze.EvCapes.models.JsonCapeData;
import org.apache.commons.codec.binary.Base64;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class CapeManager {
    private static final Map<UUID, String> originalCapes = new HashMap<>();

    // Cache for cape textures - Map<PlayerName, CapeTextureData>
    private static final Map<String, CapeTextureData> capeTextureCache = new HashMap<>();

    // Cache expiration time in milliseconds (1 hour)
    private static final long CACHE_EXPIRATION_TIME = 60 * 60 * 1000;

    private final Plugin plugin;
    private List<CapeData> availableCapes;
    private CapeScanner capeScanner;

    public CapeManager(Plugin plugin) {
        this.plugin = plugin;
        // Initialize the available capes list
        this.availableCapes = new ArrayList<>();

        // Initialize and start the cape scanner
        this.capeScanner = new CapeScanner(plugin);
        this.capeScanner.startScanning();

        // Load capes asynchronously
        loadCapes();
    }

    /**
     * Loads the available capes from the MineSkin API
     */
    private void loadCapes() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<CapeData> capes = MineSkinAPI.getKnownCapes();
                // Find players with these capes using the cape scanner
                capes = MineSkinAPI.findPlayersWithCapes(capes, capeScanner);
                // Update the available capes list
                this.availableCapes = capes;
                Bukkit.getLogger().info("[EvCapes] Loaded " + capes.size() + " capes from MineSkin API");
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.SEVERE, "[EvCapes] Error loading capes from MineSkin API", e);
            }
        });
    }

    /**
     * Gets the list of available capes
     * @return The list of available capes
     */
    public List<CapeData> getAvailableCapes() {
        return new ArrayList<>(availableCapes);
    }

    /**
     * Stops the cape scanner
     * Call this method when the plugin is disabled
     */
    public void shutdown() {
        if (capeScanner != null) {
            capeScanner.stopScanning();
        }
    }

    /**
     * Applies a cape to a player using the cape URL directly
     * @param player The player to apply the cape to
     * @param capeUrl The URL of the cape texture
     * @return true if successful, false otherwise
     */
    public boolean applyCapeByUrl(Player player, String capeUrl) {
        try {
            // Get the player's current skin texture
            PlayerProfile playerProfile = player.getPlayerProfile();
            String skinValue = null;
            String skinSignature = null;

            for (ProfileProperty prop : playerProfile.getProperties()) {
                if (prop.getName().equals("textures")) {
                    skinValue = prop.getValue();
                    skinSignature = prop.getSignature();
                    break;
                }
            }

            if (skinValue == null) {
                Bukkit.getLogger().warning("Could not get current skin data for player " + player.getName());
                return false;
            }

            // Decode the player's current skin texture
            byte[] decodedSkinBytes = Base64.decodeBase64(skinValue);
            String decodedSkinString = new String(decodedSkinBytes);
            JsonObject skinTextureJson = JsonParser.parseString(decodedSkinString).getAsJsonObject();
            JsonObject skinTextures = skinTextureJson.getAsJsonObject("textures");

            // Create a cape object with the URL
            JsonObject capeObject = new JsonObject();
            capeObject.addProperty("url", capeUrl);

            // Add the cape texture to the player's skin texture
            skinTextures.add("CAPE", capeObject);

            // Encode the updated skin texture
            String newTextureJson = skinTextureJson.toString();
            String encodedString = Base64.encodeBase64String(newTextureJson.getBytes());

            // Apply the updated skin texture to the player
            playerProfile.setProperty(new ProfileProperty("textures", encodedString, skinSignature));
            player.setPlayerProfile(playerProfile);

            // Update the player's appearance
            updatePlayerAppearance(player);

            return true;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Error applying cape by URL", e);
            return false;
        }
    }

    /**
     * Applies a cape to a player using the player name who has the cape
     * @param player The player to apply the cape to
     * @param capePlayerName The name of the player who has the cape
     * @return true if successful, false otherwise
     */
    public boolean applyCape(Player player, String capePlayerName) {
        // This method will use the existing stealcape functionality
        // but with the player name from our cape data
        if (capePlayerName == null || capePlayerName.isEmpty()) {
            return false;
        }

        try {
            // Check if the player has permission for this cape
            CapeData capeData = findCapeByPlayerName(capePlayerName);
            if (capeData != null) {
                String permissionNode = "evcapes.cape." + capeData.getAlias().toLowerCase().replace(" ", "");
                if (!player.hasPermission(permissionNode) && !player.hasPermission("evcapes.cape.*")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to use the " +
                                      ChatColor.GOLD + capeData.getAlias() + ChatColor.RED + " cape!");
                    return false;
                }
            }

            // Check if the cape texture is in the cache
            CapeTextureData capeTextureData = getCapeTextureFromCache(capePlayerName);

            if (capeTextureData == null) {
                // Cape texture not in cache, fetch it
                capeTextureData = fetchCapeTexture(capePlayerName);

                if (capeTextureData == null) {
                    // Failed to fetch cape texture
                    return false;
                }

                // Add to cache
                capeTextureCache.put(capePlayerName, capeTextureData);
            }

            // Check if the player has a cape
            if (!capeTextureData.getCapeData().hasCape()) {
                Bukkit.getLogger().warning("Player " + capePlayerName + " does not have a cape.");
                return false;
            }

            // Get the player's current skin texture
            PlayerProfile playerProfile = player.getPlayerProfile();
            String skinValue = null;
            String skinSignature = null;

            for (ProfileProperty prop : playerProfile.getProperties()) {
                if (prop.getName().equals("textures")) {
                    skinValue = prop.getValue();
                    skinSignature = prop.getSignature();
                    break;
                }
            }

            if (skinValue == null) {
                Bukkit.getLogger().warning("Could not get current skin data for player " + player.getName());
                return false;
            }

            // Decode the player's current skin texture
            byte[] decodedSkinBytes = Base64.decodeBase64(skinValue);
            String decodedSkinString = new String(decodedSkinBytes);
            JsonObject skinTextureJson = JsonParser.parseString(decodedSkinString).getAsJsonObject();
            JsonObject skinTextures = skinTextureJson.getAsJsonObject("textures");

            // Add the cape texture to the player's skin texture
            JsonObject capeObject = capeTextureData.getCapeData().getCapeObject();

            // Log the cape URL for debugging
            if (capeObject.has("url")) {
                String capeUrl = capeObject.get("url").getAsString();
                Bukkit.getLogger().info("Applying cape with URL: " + capeUrl);

                // Verify this is the correct cape by checking the URL against the CapeData
                CapeData expectedCape = findCapeByPlayerName(capePlayerName);
                if (expectedCape != null && !capeUrl.equals(expectedCape.getUrl())) {
                    Bukkit.getLogger().warning("Cape URL mismatch! Expected: " + expectedCape.getUrl() + ", Got: " + capeUrl);
                    // Try to use the expected URL instead
                    JsonObject fixedCapeObject = new JsonObject();
                    fixedCapeObject.addProperty("url", expectedCape.getUrl());
                    capeObject = fixedCapeObject;
                }
            }

            skinTextures.add("CAPE", capeObject);

            // Encode the updated skin texture
            String newTextureJson = skinTextureJson.toString();
            String encodedString = Base64.encodeBase64String(newTextureJson.getBytes());

            // Apply the updated skin texture to the player
            playerProfile.setProperty(new ProfileProperty("textures", encodedString, skinSignature));
            player.setPlayerProfile(playerProfile);

            // Update the player's appearance
            updatePlayerAppearance(player);

            return true;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Error applying cape", e);
            return false;
        }
    }

    /**
     * Makes an HTTP request to the specified URL
     * @param url The URL to request
     * @return The response body as a string
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the operation is interrupted
     */
    private String makeRequest(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    /**
     * Gets a cape texture from the cache
     * @param playerName The name of the player who has the cape
     * @return The cape texture data, or null if not in cache or expired
     */
    private CapeTextureData getCapeTextureFromCache(String playerName) {
        // Check if the cape texture is in the cache
        if (capeTextureCache.containsKey(playerName)) {
            CapeTextureData capeTextureData = capeTextureCache.get(playerName);

            // Check if the cache entry is expired
            if (capeTextureData.isExpired(CACHE_EXPIRATION_TIME)) {
                // Remove expired entry
                capeTextureCache.remove(playerName);
                return null;
            }

            return capeTextureData;
        }

        return null;
    }

    /**
     * Fetches a cape texture from the Mojang API
     * @param playerName The name of the player who has the cape
     * @return The cape texture data, or null if failed
     */
    private CapeTextureData fetchCapeTexture(String playerName) {
        try {
            // Get the cape texture from the player
            String profileResponse = makeRequest("https://api.mojang.com/users/profiles/minecraft/" + playerName);
            JsonObject profileObject;
            try {
                profileObject = JsonParser.parseString(profileResponse).getAsJsonObject();
            } catch (Exception e) {
                Bukkit.getLogger().warning("Player " + playerName + " does not exist.");
                return null;
            }

            String uuid = profileObject.get("id").getAsString();
            String skinResponse = makeRequest("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");

            JsonObject skinObject = JsonParser.parseString(skinResponse)
                    .getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();
            String value = skinObject.get("value").getAsString();
            String signature = skinObject.get("signature").getAsString();

            // Extract cape data from the texture
            byte[] decodedBytes = Base64.decodeBase64(value);
            String decodedString = new String(decodedBytes);
            JsonObject textureJson = JsonParser.parseString(decodedString).getAsJsonObject();
            JsonObject textures = textureJson.getAsJsonObject("textures");

            boolean hasCape = textures.has("CAPE");
            JsonObject capeObject = hasCape ? textures.getAsJsonObject("CAPE") : new JsonObject();
            JsonCapeData jsonCapeData = new JsonCapeData(capeObject, hasCape);

            return new CapeTextureData(value, signature, jsonCapeData);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Error fetching cape texture", e);
            return null;
        }
    }

    /**
     * Finds a cape by the player name who has it
     * @param playerName The name of the player who has the cape
     * @return The cape data, or null if not found
     */
    private CapeData findCapeByPlayerName(String playerName) {
        for (CapeData cape : availableCapes) {
            if (playerName.equalsIgnoreCase(cape.getPlayerWithCape())) {
                return cape;
            }
        }
        return null;
    }

    /**
     * Checks if the player's original cape is stored
     * @param player The player to check
     * @return true if the player's original cape is stored, false otherwise
     */
    public boolean hasOriginalCape(Player player) {
        return originalCapes.containsKey(player.getUniqueId());
    }

    /**
     * Stores the player's original cape texture
     * @param player The player whose cape to store
     */
    public void storeOriginalCape(Player player) {
        PlayerProfile profile = player.getPlayerProfile();
        for (ProfileProperty property : profile.getProperties()) {
            if (property.getName().equals("textures")) {
                originalCapes.put(player.getUniqueId(), property.getValue());
                break;
            }
        }
    }

    /**
     * Resets a player's cape to their original cape
     * @param player The player whose cape to reset
     * @return true if successful, false otherwise
     */
    public boolean resetCape(Player player) {
        UUID playerId = player.getUniqueId();
        if (!originalCapes.containsKey(playerId)) {
            return false;
        }

        String originalValue = originalCapes.get(playerId);
        PlayerProfile profile = player.getPlayerProfile();

        // Find the current textures property
        ProfileProperty currentTexture = null;
        for (ProfileProperty property : profile.getProperties()) {
            if (property.getName().equals("textures")) {
                currentTexture = property;
                break;
            }
        }

        if (currentTexture == null) {
            return false;
        }

        // Set the original texture back
        profile.setProperty(new ProfileProperty("textures", originalValue, currentTexture.getSignature()));
        player.setPlayerProfile(profile);
        updatePlayerAppearance(player);

        return true;
    }

    /**
     * Updates the player's appearance for all other players
     * @param player The player to update
     */
    public void updatePlayerAppearance(Player player) {
        if (plugin == null) {
            Bukkit.getLogger().severe("Plugin instance is null in CapeManager.updatePlayerAppearance");
            return;
        }

        Bukkit.getOnlinePlayers().forEach(otherPlayer -> {
            if (!otherPlayer.equals(player)) {
                otherPlayer.hidePlayer(plugin, player);
                otherPlayer.showPlayer(plugin, player);
            }
        });
    }

    /**
     * Checks if a player has a cape stored in their texture property
     * @param player The player to check
     * @return true if the player has a cape, false otherwise
     */
    public boolean playerHasCape(Player player) {
        PlayerProfile profile = player.getPlayerProfile();
        for (ProfileProperty property : profile.getProperties()) {
            if (property.getName().equals("textures")) {
                String value = property.getValue();
                byte[] decodedBytes = Base64.decodeBase64(value);
                String decodedString = new String(decodedBytes);
                JsonObject textureJson = JsonParser.parseString(decodedString).getAsJsonObject();
                JsonObject textures = textureJson.getAsJsonObject("textures");
                return textures.has("CAPE");
            }
        }
        return false;
    }
}
