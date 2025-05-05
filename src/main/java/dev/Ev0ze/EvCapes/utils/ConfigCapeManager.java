package dev.Ev0ze.EvCapes.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.Ev0ze.EvCapes.models.CapeData;
import org.apache.commons.codec.binary.Base64;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages capes using a configuration file
 */
public class ConfigCapeManager {
    private static final Map<UUID, String> originalCapes = new HashMap<>();

    private final Plugin plugin;
    private List<CapeData> availableCapes;
    private FileConfiguration capesConfig;

    public ConfigCapeManager(Plugin plugin) {
        this.plugin = plugin;
        this.availableCapes = new ArrayList<>();

        // Load capes from config
        loadCapes();
    }

    /**
     * Loads capes from the capes.yml config file
     */
    private void loadCapes() {
        // Create the capes.yml file if it doesn't exist
        File capesFile = new File(plugin.getDataFolder(), "capes.yml");
        if (!capesFile.exists()) {
            plugin.saveResource("capes.yml", false);
        }

        // Load the capes config
        capesConfig = YamlConfiguration.loadConfiguration(capesFile);

        // Clear the current capes list
        availableCapes.clear();

        // Load capes from the config
        ConfigurationSection capesSection = capesConfig.getConfigurationSection("capes");
        if (capesSection != null) {
            for (String id : capesSection.getKeys(false)) {
                ConfigurationSection capeSection = capesSection.getConfigurationSection(id);
                if (capeSection != null) {
                    String name = capeSection.getString("name", id);
                    String url = capeSection.getString("url");
                    boolean enabled = capeSection.getBoolean("enabled", true);

                    if (url != null && enabled) {
                        CapeData cape = new CapeData(id, name, url, true);
                        availableCapes.add(cape);
                    }
                }
            }
        }

        Bukkit.getLogger().info("[EvCapes] Loaded " + availableCapes.size() + " capes from config");
    }

    /**
     * Gets the list of available capes
     * @return The list of available capes
     */
    public List<CapeData> getAvailableCapes() {
        return new ArrayList<>(availableCapes);
    }

    /**
     * Applies a cape to a player using the cape ID
     * @param player The player to apply the cape to
     * @param capeId The ID of the cape to apply
     * @return true if successful, false otherwise
     */
    public boolean applyCape(Player player, String capeId) {
        // Find the cape by ID
        CapeData cape = null;
        for (CapeData c : availableCapes) {
            if (c.getUuid().equals(capeId)) {
                cape = c;
                break;
            }
        }

        if (cape == null) {
            Bukkit.getLogger().warning("[EvCapes] Cape with ID " + capeId + " not found");
            return false;
        }

        // Apply the cape using its URL
        return applyCapeByUrl(player, cape.getUrl());
    }

    /**
     * Applies a cape to a player using the cape URL directly
     * @param player The player to apply the cape to
     * @param capeUrl The URL of the cape texture
     * @return true if successful, false otherwise
     */
    public boolean applyCapeByUrl(Player player, String capeUrl) {
        try {
            // Store the player's original cape if not already stored
            if (!originalCapes.containsKey(player.getUniqueId())) {
                storeOriginalCape(player);
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
                Bukkit.getLogger().warning("[EvCapes] Could not get current skin data for player " + player.getName());
                return false;
            }

            // Log the original skin value for debugging
            Bukkit.getLogger().info("[EvCapes] Original skin value for " + player.getName() + ": " + skinValue);

            // Decode the player's current skin texture
            byte[] decodedSkinBytes = Base64.decodeBase64(skinValue);
            String decodedSkinString = new String(decodedSkinBytes);
            JsonObject skinTextureJson = JsonParser.parseString(decodedSkinString).getAsJsonObject();
            JsonObject skinTextures = skinTextureJson.getAsJsonObject("textures");

            // Create a cape object with the URL
            JsonObject capeObject = new JsonObject();
            capeObject.addProperty("url", capeUrl);

            // Add the cape texture to the player's skin texture
            // First, check if there's already a CAPE property and remove it
            if (skinTextures.has("CAPE")) {
                skinTextures.remove("CAPE");
            }

            // Now add the new cape
            skinTextures.add("CAPE", capeObject);

            // Encode the updated skin texture
            String newTextureJson = skinTextureJson.toString();
            String encodedString = Base64.encodeBase64String(newTextureJson.getBytes());

            // Log the new skin value for debugging
            Bukkit.getLogger().info("[EvCapes] New skin value for " + player.getName() + ": " + encodedString);

            // Create a new profile property with the updated texture
            ProfileProperty newProperty = new ProfileProperty("textures", encodedString, skinSignature);

            // Clear existing texture properties and add the new one
            playerProfile.getProperties().removeIf(prop -> prop.getName().equals("textures"));
            playerProfile.setProperty(newProperty);

            // Apply the updated profile to the player
            player.setPlayerProfile(playerProfile);

            // Force update the player's appearance for all other players with multiple attempts
            // This is crucial for other players to see the changes
            forceUpdatePlayerAppearance(player, capeUrl);

            return true;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "[EvCapes] Error applying cape by URL", e);
            return false;
        }
    }

    /**
     * Forces an update of the player's appearance with multiple attempts
     * @param player The player to update
     * @param capeUrl The cape URL for logging
     */
    private void forceUpdatePlayerAppearance(Player player, String capeUrl) {
        // First immediate update
        updatePlayerAppearance(player);

        // Then schedule several updates with increasing delays to ensure it takes effect
        int[] delays = {5, 10, 20, 40};
        for (int delay : delays) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                updatePlayerAppearance(player);
                Bukkit.getLogger().info("[EvCapes] Update attempt at " + delay + " ticks for player " +
                                       player.getName() + " with cape URL: " + capeUrl);
            }, delay);
        }
    }

    /**
     * Removes a player's cape and restores their original skin
     * @param player The player to remove the cape from
     * @return true if successful, false otherwise
     */
    public boolean removeCape(Player player) {
        try {
            // Check if the player has an original cape stored
            if (!originalCapes.containsKey(player.getUniqueId())) {
                // Player doesn't have a stored original cape, just remove any cape
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
                    return false;
                }

                // Decode the player's current skin texture
                byte[] decodedSkinBytes = Base64.decodeBase64(skinValue);
                String decodedSkinString = new String(decodedSkinBytes);
                JsonObject skinTextureJson = JsonParser.parseString(decodedSkinString).getAsJsonObject();
                JsonObject skinTextures = skinTextureJson.getAsJsonObject("textures");

                // Remove the cape texture from the player's skin texture
                if (skinTextures.has("CAPE")) {
                    skinTextures.remove("CAPE");

                    // Encode the updated skin texture
                    String newTextureJson = skinTextureJson.toString();
                    String encodedString = Base64.encodeBase64String(newTextureJson.getBytes());

                    // Apply the updated skin texture to the player
                    playerProfile.setProperty(new ProfileProperty("textures", encodedString, skinSignature));
                    player.setPlayerProfile(playerProfile);

                    // Update the player's appearance
                    updatePlayerAppearance(player);
                }

                return true;
            }

            // Restore the player's original cape
            String originalValue = originalCapes.get(player.getUniqueId());
            PlayerProfile playerProfile = player.getPlayerProfile();

            // Get the current signature
            String signature = null;
            for (ProfileProperty prop : playerProfile.getProperties()) {
                if (prop.getName().equals("textures")) {
                    signature = prop.getSignature();
                    break;
                }
            }

            if (signature == null) {
                return false;
            }

            // Apply the original texture value
            playerProfile.setProperty(new ProfileProperty("textures", originalValue, signature));
            player.setPlayerProfile(playerProfile);

            // Remove the stored original cape
            originalCapes.remove(player.getUniqueId());

            // Update the player's appearance
            updatePlayerAppearance(player);

            return true;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "[EvCapes] Error removing cape", e);
            return false;
        }
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

    /**
     * Resets a player's cape to their original cape
     * @param player The player whose cape to reset
     * @return true if successful, false otherwise
     */
    public boolean resetCape(Player player) {
        UUID playerId = player.getUniqueId();
        if (!originalCapes.containsKey(playerId)) {
            Bukkit.getLogger().warning("[EvCapes] No original cape stored for player " + player.getName());
            return false;
        }

        try {
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
                Bukkit.getLogger().warning("[EvCapes] Could not find textures property for player " + player.getName());
                return false;
            }

            // Log the original value for debugging
            Bukkit.getLogger().info("[EvCapes] Resetting to original skin value for " + player.getName());

            // Create a new profile property with the original texture
            ProfileProperty newProperty = new ProfileProperty("textures", originalValue, currentTexture.getSignature());

            // Clear existing texture properties and add the new one
            profile.getProperties().removeIf(prop -> prop.getName().equals("textures"));
            profile.setProperty(newProperty);

            // Apply the updated profile to the player
            player.setPlayerProfile(profile);

            // Force update the player's appearance with multiple attempts
            forceUpdatePlayerAppearance(player, "RESET");

            // Don't remove the original cape from storage in case they want to reset again

            return true;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "[EvCapes] Error resetting cape for player " + player.getName(), e);
            return false;
        }
    }

    /**
     * Updates a player's appearance for all players
     * @param player The player to update
     */
    private void updatePlayerAppearance(Player player) {
        if (plugin == null) {
            Bukkit.getLogger().severe("[EvCapes] Plugin instance is null in ConfigCapeManager.updatePlayerAppearance");
            return;
        }

        // Log the update attempt
        Bukkit.getLogger().info("[EvCapes] Updating appearance for player " + player.getName());

        // Hide and show the player to update their appearance for all other players
        for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
            if (!otherPlayer.equals(player) && otherPlayer.canSee(player)) {
                try {
                    // Hide the player first
                    otherPlayer.hidePlayer(plugin, player);

                    // Add a small delay before showing the player again
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        try {
                            otherPlayer.showPlayer(plugin, player);
                            Bukkit.getLogger().info("[EvCapes] Player " + player.getName() + " is now visible to " + otherPlayer.getName());
                        } catch (Exception e) {
                            Bukkit.getLogger().severe("[EvCapes] Error showing player " + player.getName() + " to " + otherPlayer.getName() + ": " + e.getMessage());
                        }
                    }, 2L);
                } catch (Exception e) {
                    Bukkit.getLogger().severe("[EvCapes] Error hiding player " + player.getName() + " from " + otherPlayer.getName() + ": " + e.getMessage());
                }
            }
        }
    }
}
