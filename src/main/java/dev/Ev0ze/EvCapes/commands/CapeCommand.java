package dev.Ev0ze.EvCapes.commands;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class CapeCommand implements CommandExecutor, TabCompleter {
    private static final String PROFILE_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String SKIN_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false";
    private static final Map<String, Collection<ProfileProperty>> cache = new HashMap<>();
    private final Plugin plugin;

    public CapeCommand(Plugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin instance cannot be null");
        }
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can execute this command!");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage("§cUsage: /stealcape <playerName>");
            return true;
        }

        // Get the target player name
        String targetCapePlayer = args[0];

        // Check if the target player exists and has a cape
        if (!playerHasCape(targetCapePlayer)) {
            player.sendMessage("§cPlayer " + targetCapePlayer + " does not exist or does not have a cape.");
            return true;
        }

        // Retrieve the player's profile and get the cape texture from the target
        PlayerProfile playerProfile = player.getPlayerProfile();
        Collection<ProfileProperty> properties = getCapeTextureProperty(targetCapePlayer);
        if (properties.isEmpty()) {
            player.sendMessage("§cPlayer " + targetCapePlayer + " does not exist.");
            return true;
        }

        String skinValue = null;
        Iterator<ProfileProperty> iterator = playerProfile.getProperties().iterator();
        while (iterator.hasNext()) {
            ProfileProperty prop = iterator.next();
            if (prop.getName().equals("textures")) {
                skinValue = prop.getValue();
                break;
            }
        }
        if (skinValue == null) {
            player.sendMessage("§cCould not get your current skin data.");
            return true;
        }

        byte[] decodedBytes = Base64.decodeBase64(skinValue);
        String decodedString = new String(decodedBytes);
        JsonObject textureJson = JsonParser.parseString(decodedString).getAsJsonObject();
        JsonObject textures = textureJson.getAsJsonObject("textures");
        String capeValue = properties.iterator().next().getValue();
        byte[] decodedCapeBytes = Base64.decodeBase64(capeValue);
        String decodedCapeString = new String(decodedCapeBytes);
        JsonObject capeJson = JsonParser.parseString(decodedCapeString).getAsJsonObject();
        JsonObject capeTextures = capeJson.getAsJsonObject("textures");

        if (capeTextures.has("CAPE")) {
            textures.add("CAPE", capeTextures.getAsJsonObject("CAPE"));
            String newTextureJson = textureJson.toString();
            String encodedString = Base64.encodeBase64String(newTextureJson.getBytes());
            // Replace the textures property with the new value
            playerProfile.setProperty(new ProfileProperty("textures", encodedString,
                    playerProfile.getProperties().iterator().next().getSignature()));
            player.setPlayerProfile(playerProfile);
            player.sendMessage("§aYou have successfully stolen " + targetCapePlayer + "'s cape!");
            updateSkin(player);
        } else {
            player.sendMessage("§cFailed to steal cape: Player " + targetCapePlayer + " does not have a cape.");
        }
        return true;
    }

    private Collection<ProfileProperty> getCapeTextureProperty(String targetCapePlayer) {
        if (cache.containsKey(targetCapePlayer)) {
            return cache.get(targetCapePlayer);
        }
        String profileResponse = makeRequest(PROFILE_URL + targetCapePlayer);
        JsonObject profileObject;
        try {
            profileObject = JsonParser.parseString(profileResponse).getAsJsonObject();
        } catch (Exception e) {
            return List.of();
        }
        String uuid = profileObject.get("id").getAsString();
        String skinResponse = makeRequest(String.format(SKIN_URL, uuid));
        JsonObject skinObject = JsonParser.parseString(skinResponse)
                .getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();
        String value = skinObject.get("value").getAsString();
        String signature = skinObject.get("signature").getAsString();
        ProfileProperty profileProperty = new ProfileProperty("textures", value, signature);
        cache.put(targetCapePlayer, List.of(profileProperty));
        return List.of(profileProperty);
    }

    /**
     * Checks if a player has a cape
     *
     * @param playerName The name of the player to check
     * @return true if the player has a cape, false otherwise
     */
    private boolean playerHasCape(String playerName) {
        Collection<ProfileProperty> properties = getCapeTextureProperty(playerName);
        if (properties.isEmpty()) {
            return false;
        }

        String capeValue = properties.iterator().next().getValue();
        byte[] decodedCapeBytes = Base64.decodeBase64(capeValue);
        String decodedCapeString = new String(decodedCapeBytes);
        JsonObject capeJson = JsonParser.parseString(decodedCapeString).getAsJsonObject();
        JsonObject capeTextures = capeJson.getAsJsonObject("textures");

        return capeTextures.has("CAPE");
    }

    private String makeRequest(String url) {
        try {
            HttpClient client = HttpClient.newBuilder().build();
            String responseBody;
            try {
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
                HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
                responseBody = response.body();
            } catch (Throwable t) {
                if (client != null) {
                    try {
                        client.close();
                    } catch (Throwable suppressed) {
                        t.addSuppressed(suppressed);
                    }
                }
                throw t;
            }
            if (client != null) {
                client.close();
            }
            return responseBody;
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateSkin(Player player) {
        // Defensive check for plugin instance
        if (plugin == null) {
            Bukkit.getLogger().severe("Plugin instance is null in CapeCommand.updateSkin; skipping hide/show operations.");
            return;
        }
        Bukkit.getOnlinePlayers().forEach(otherPlayer -> {
            if (!otherPlayer.equals(player)) {
                otherPlayer.hidePlayer(plugin, player);
                otherPlayer.showPlayer(plugin, player);
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // No tab completions for player names
        return new ArrayList<>();
    }
}