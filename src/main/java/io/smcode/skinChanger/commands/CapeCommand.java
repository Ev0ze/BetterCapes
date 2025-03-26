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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class CapeCommand implements CommandExecutor {
    private static final String PROFILE_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String SKIN_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false";
    private static final Map<String, Collection<ProfileProperty>> cache = new HashMap();

    public CapeCommand() {
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can execute this command!");
            return true;
        } else if (args.length == 0) {
            player.sendMessage("§cUsage: /cape <player>");
            return true;
        } else {
            String targetCapePlayer = args[0];
            PlayerProfile playerProfile = player.getPlayerProfile();
            Collection<ProfileProperty> properties = this.getCapeTextureProperty(targetCapePlayer);
            if (properties.isEmpty()) {
                player.sendMessage("§cPlayer " + targetCapePlayer + " does not exist.");
                return true;
            } else {
                String skinValue = null;
                Iterator var10 = playerProfile.getProperties().iterator();

                while(var10.hasNext()) {
                    ProfileProperty prop = (ProfileProperty)var10.next();
                    if (prop.getName().equals("textures")) {
                        skinValue = prop.getValue();
                        break;
                    }
                }

                if (skinValue == null) {
                    player.sendMessage("§cCould not get your current skin data.");
                    return true;
                } else {
                    byte[] decodedBytes = Base64.decodeBase64(skinValue);
                    String decodedString = new String(decodedBytes);
                    JsonObject textureJson = JsonParser.parseString(decodedString).getAsJsonObject();
                    JsonObject textures = textureJson.getAsJsonObject("textures");
                    String capeValue = ((ProfileProperty)properties.iterator().next()).getValue();
                    byte[] decodedCapeBytes = Base64.decodeBase64(capeValue);
                    String decodedCapeString = new String(decodedCapeBytes);
                    JsonObject capeJson = JsonParser.parseString(decodedCapeString).getAsJsonObject();
                    JsonObject capeTextures = capeJson.getAsJsonObject("textures");
                    if (capeTextures.has("CAPE")) {
                        textures.add("CAPE", capeTextures.getAsJsonObject("CAPE"));
                        String var19 = textureJson.toString();
                        String encodedString = Base64.encodeBase64String(var19.getBytes());
                        playerProfile.setProperty(new ProfileProperty("textures", encodedString, ((ProfileProperty)playerProfile.getProperties().iterator().next()).getSignature()));
                        player.setPlayerProfile(playerProfile);
                        player.setPlayerProfile(playerProfile);
                        player.sendMessage("§aYour cape has been changed!");
                        this.updateSkin(player);
                        return true;
                    } else {
                        player.sendMessage("§cPlayer " + targetCapePlayer + " does not have a cape.");
                        return true;
                    }
                }
            }
        }
    }

    private Collection<ProfileProperty> getCapeTextureProperty(String targetCapePlayer) {
        if (cache.containsKey(targetCapePlayer)) {
            return (Collection)cache.get(targetCapePlayer);
        } else {
            String profileResponse = this.makeRequest("https://api.mojang.com/users/profiles/minecraft/" + targetCapePlayer);

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
            cache.put(targetCapePlayer, List.of(profileProperty));
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

    private void updateSkin(Player player) {
        Bukkit.getOnlinePlayers().forEach((otherPlayer) -> {
            if (!otherPlayer.equals(player)) {
                otherPlayer.hidePlayer((Plugin)null, player);
                otherPlayer.showPlayer((Plugin)null, player);
            }

        });
    }
}