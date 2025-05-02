package dev.Ev0ze.EvCapes.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.Ev0ze.EvCapes.models.CapeData;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * API client for the MineSkin API
 */
public class MineSkinAPI {
    private static final String CAPES_ENDPOINT = "https://api.mineskin.org/v2/capes";
    private static final String USER_AGENT = "EvCapes/1.0";

    /**
     * Fetches the list of known capes from the MineSkin API
     * @return A list of CapeData objects
     */
    public static List<CapeData> getKnownCapes() {
        List<CapeData> capes = new ArrayList<>();

        try {
            HttpClient client = HttpClient.newBuilder().build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CAPES_ENDPOINT))
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray capesArray = jsonResponse.getAsJsonArray("capes");

                for (JsonElement element : capesArray) {
                    JsonObject capeObject = element.getAsJsonObject();
                    String uuid = capeObject.get("uuid").getAsString();
                    String alias = capeObject.get("alias").getAsString();
                    String url = capeObject.get("url").getAsString();
                    boolean supported = capeObject.has("supported") && capeObject.get("supported").getAsBoolean();

                    capes.add(new CapeData(uuid, alias, url, supported));
                }
            } else {
                Bukkit.getLogger().warning("Failed to fetch capes from MineSkin API. Status code: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Error fetching capes from MineSkin API", e);
        }

        return capes;
    }

    /**
     * Asynchronously fetches the list of known capes from the MineSkin API
     * @return A CompletableFuture that will be completed with the list of CapeData objects
     */
    public static CompletableFuture<List<CapeData>> getKnownCapesAsync() {
        return CompletableFuture.supplyAsync(MineSkinAPI::getKnownCapes);
    }

    /**
     * Finds players who have specific capes
     * This method uses the CapeScanner to find real players with the capes
     * @param capes The list of capes to find players for
     * @param capeScanner The cape scanner to use
     * @return The same list of capes with playerWithCape field populated
     */
    public static List<CapeData> findPlayersWithCapes(List<CapeData> capes, dev.Ev0ze.EvCapes.utils.CapeScanner capeScanner) {
        if (capeScanner != null) {
            // Use the cape scanner to assign players to capes
            return capeScanner.assignPlayersToCapes(capes);
        } else {
            // Fallback to default assignments if scanner is not available
            Bukkit.getLogger().warning("[EvCapes] CapeScanner not available. Using default player assignments.");

            // Default fallback players
            String[] fallbackPlayers = {
                "Notch", "jeb_", "Dinnerbone", "Grumm", "Marc_IRL", "Searge",
                "TheMogMiner", "EvilSeph", "Mojang", "MojangStudios"
            };

            int fallbackIndex = 0;

            // Assign default players to capes
            for (CapeData cape : capes) {
                String player = fallbackPlayers[fallbackIndex % fallbackPlayers.length];
                fallbackIndex++;

                cape.setPlayerWithCape(player);
                Bukkit.getLogger().info("[EvCapes] Assigned default player " + player + " to cape " + cape.getAlias());
            }

            return capes;
        }
    }
}
