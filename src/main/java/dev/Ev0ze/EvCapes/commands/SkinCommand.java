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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class SkinCommand implements CommandExecutor {
    private static final String PROFILE_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String SKIN_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false";
    private static final Map<String, Collection<ProfileProperty>> cache = new HashMap<>();
    private Plugin plugin;

    public SkinCommand() {
        this.plugin = Bukkit.getPluginManager().getPlugin("EvCapes");
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            if (args.length == 0) {
                player.sendMessage("§cUsage: /identity <player>");
                return true;
            } else {
                String targetSkin = args[0];
                PlayerProfile playerProfile = player.getPlayerProfile();
                Collection<ProfileProperty> properties = this.getTextureProperty(targetSkin);
                if (properties.isEmpty()) {
                    player.sendMessage("§cPlayer " + targetSkin + " does not exist.");
                    return true;
                } else {
                    // Set the player's skin
                    playerProfile.setProperties(properties);
                    player.setPlayerProfile(playerProfile);

                    // Set the player's display name to match the target player
                    player.setDisplayName(targetSkin);
                    player.setPlayerListName(targetSkin);

                    // Update the player's appearance for all other players
                    updatePlayerAppearance(player);

                    player.sendMessage("§aYour identity has been changed to " + targetSkin + "!");
                    return true;
                }
            }
        } else {
            sender.sendMessage("§cOnly players can execute this command!");
            return true;
        }
    }

    private Collection<ProfileProperty> getTextureProperty(String targetSkin) {
        if (cache.containsKey(targetSkin)) {
            return (Collection)cache.get(targetSkin);
        } else {
            String profileResponse = this.makeRequest("https://api.mojang.com/users/profiles/minecraft/" + targetSkin);

            JsonObject profileObject;
            try {
                profileObject = JsonParser.parseString(profileResponse).getAsJsonObject();
            } catch (Exception var10) {
                return List.of();
            }

            String uuid = profileObject.get("id").getAsString();
            String skinResponse = this.makeRequest("https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false".formatted(uuid));
            JsonObject skinObject = JsonParser.parseString(skinResponse).getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();
            String value = skinObject.get("value").getAsString();
            String signature = skinObject.get("signature").getAsString();
            ProfileProperty profileProperty = new ProfileProperty("textures", value, signature);
            cache.put(targetSkin, List.of(profileProperty));
            return List.of(profileProperty);
        }
    }

    private String makeRequest(String url) {
        try {
            HttpClient client = HttpClient.newBuilder().build();

            String responseBody;
            try {
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
                HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
                responseBody = response.body();
            } catch (Throwable var7) {
                if (client != null) {
                    try {
                        client.close();
                    } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                    }
                }

                throw var7;
            }

            if (client != null) {
                client.close();
            }

            return responseBody;
        } catch (InterruptedException | IOException var8) {
            throw new RuntimeException(var8);
        }
    }

    private void updatePlayerAppearance(Player player) {
        // Defensive check for plugin instance
        if (plugin == null) {
            Bukkit.getLogger().severe("Plugin instance is null in SkinCommand.updatePlayerAppearance; skipping hide/show operations.");
            return;
        }

        // Hide and show the player to update their appearance for all other players
        Bukkit.getOnlinePlayers().forEach(otherPlayer -> {
            if (!otherPlayer.equals(player)) {
                otherPlayer.hidePlayer(plugin, player);
                otherPlayer.showPlayer(plugin, player);
            }
        });
    }
}