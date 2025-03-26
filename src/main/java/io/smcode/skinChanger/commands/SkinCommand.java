package io.smcode.skinChanger.commands;

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
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SkinCommand implements CommandExecutor {
    private static final String PROFILE_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String SKIN_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false";
    private static final Map<String, Collection<ProfileProperty>> cache = new HashMap();

    public SkinCommand() {
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            if (args.length == 0) {
                player.sendMessage("§cUsage: /skin <player>");
                return true;
            } else {
                String targetSkin = args[0];
                PlayerProfile playerProfile = player.getPlayerProfile();
                Collection<ProfileProperty> properties = this.getTextureProperty(targetSkin);
                if (properties.isEmpty()) {
                    player.sendMessage("§cPlayer " + targetSkin + " does not exist.");
                    return true;
                } else {
                    playerProfile.setProperties(properties);
                    player.setPlayerProfile(playerProfile);
                    player.sendMessage("§aYour skin has been changed!");
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

            String var5;
            try {
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
                HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
                var5 = (String)response.body();
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

            return var5;
        } catch (InterruptedException | IOException var8) {
            throw new RuntimeException(var8);
        }
    }
}