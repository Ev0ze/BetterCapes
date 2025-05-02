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
     * Fetches the list of known capes from the MineSkin API and adds custom capes
     * @return A list of CapeData objects
     */
    public static List<CapeData> getKnownCapes() {
        List<CapeData> capes = new ArrayList<>();

        // Add custom capes first
        addCustomCapes(capes);

        // Then fetch from MineSkin API
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

                    // Check if we already have this cape (by URL) to avoid duplicates
                    boolean isDuplicate = false;
                    for (CapeData existingCape : capes) {
                        if (existingCape.getUrl().equals(url)) {
                            isDuplicate = true;
                            break;
                        }
                    }

                    if (!isDuplicate) {
                        capes.add(new CapeData(uuid, alias, url, supported));
                    }
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
     * Adds custom capes to the list
     * @param capes The list to add capes to
     */
    private static void addCustomCapes(List<CapeData> capes) {
        // Minecraft Capes
        capes.add(new CapeData("minecon_2011", "Minecon 2011", "http://textures.minecraft.net/texture/953cac8b779fe41383e675ee2b86071a71658f2180f56fbce8aa315ea70e2ed6", true));
        capes.add(new CapeData("minecon_2012", "Minecon 2012", "http://textures.minecraft.net/texture/a2e8d97ec79100e90a75d369d1b3ba81273c4f82bc1b737e934eed4a854be1b6", true));
        capes.add(new CapeData("minecon_2013", "Minecon 2013", "http://textures.minecraft.net/texture/153b1a0dfcbae953cdeb6f2c2bf6bf79943239b1372780da44bcbb29273131da", true));
        capes.add(new CapeData("minecon_2015", "Minecon 2015", "http://textures.minecraft.net/texture/b0cc08840700447322d953a02b965f1d65a13a603bf64b17c803c21446fe1635", true));
        capes.add(new CapeData("minecon_2016", "Minecon 2016", "http://textures.minecraft.net/texture/e7dfea16dc83c97df01a12fabbd1216359c0cd0ea42f9999b6e97c584963e980", true));
        capes.add(new CapeData("mojang_classic", "Mojang Classic", "http://textures.minecraft.net/texture/8f120319222a9f4a11620b3529a5fd4d32b3fb8c0b15b9132c869f9dd106a403", true));
        capes.add(new CapeData("mojang_studios", "Mojang Studios", "http://textures.minecraft.net/texture/5786fe99be377dfb6858859f926c4dbc995751e91cee373468c5fbf4865e7151", true));
        capes.add(new CapeData("translator", "Translator", "http://textures.minecraft.net/texture/1bf91499701404e21bd46b0191d63239a4ef76ebde88d27e4d430ac211df681e", true));
        capes.add(new CapeData("migrator", "Migrator", "http://textures.minecraft.net/texture/2340c0e03dd24a11b15a8b33c2a7e9e32abb2051b2481d0d0e2e00a3b0b4e", true));
        capes.add(new CapeData("cobalt", "Cobalt", "http://textures.minecraft.net/texture/ca35c56efe71ed290385f4ab5346a1826b546a54d519e6a3ff01efa01acce81", true));
        capes.add(new CapeData("scrolls", "Scrolls", "http://textures.minecraft.net/texture/3efadf6510961830f9fcc077f19b4daf286d502b5f5aafbd807c7bbffcaca245", true));
        capes.add(new CapeData("mojira_mod", "Mojira Moderator", "http://textures.minecraft.net/texture/ae677f7d98ac70a533713518416df4452fe5700365c09cf45d0d156ea9396551", true));
        capes.add(new CapeData("realms_mapmaker", "Realms Mapmaker", "http://textures.minecraft.net/texture/17912790ff164b93196f08ba71d0e62129304776d0f347334f8a6eae509f8a56", true));
        capes.add(new CapeData("mojang_10th", "Mojang 10th Anniversary", "http://textures.minecraft.net/texture/9e507afc56359978a3eb3e32367042b853cddd0995d17d0da995662913fb00f7", true));
        capes.add(new CapeData("birthday", "Minecraft Birthday", "http://textures.minecraft.net/texture/2056f2eebd759cce93460907186ef44e9192954ae12b227d817eb4b55627a7fc", true));
        capes.add(new CapeData("millionth", "Millionth Customer", "http://textures.minecraft.net/texture/70f9971c0a7c28b5b212bc8537884c67e2bd9a7e9af8a46c434bc8174f5d1d0", true));
        capes.add(new CapeData("prismarine", "Prismarine", "http://textures.minecraft.net/texture/d8f8d13a1adf9636a16c7f990eb26dabb1c4155e5b39446a68aea67e8ac0eb", true));
        capes.add(new CapeData("turtle", "Turtle", "http://textures.minecraft.net/texture/5048ea61566353397247d2b7d946034de926b997d5e66c86483dfb1e031aee95", true));
        capes.add(new CapeData("pancape", "Pan Cape", "http://textures.minecraft.net/texture/cfdaf5c5ec4b2ad9f3b0977e0b5d14a9f046aef3a4c3251b3f3b127c7de5fad", true));
        capes.add(new CapeData("mff", "Minecraft Festival", "http://textures.minecraft.net/texture/9cb58e7d1c5cb0a4c7673d8bc89d7c0747271d69c926ab1d4d35942c0a3cdcb5", true));

        // Optifine Capes
        capes.add(new CapeData("of_developer", "OptiFine Developer", "http://s.optifine.net/capes/sp614x.png", true));
        capes.add(new CapeData("of_donator", "OptiFine Donator", "http://s.optifine.net/capes/Notch.png", true));
        capes.add(new CapeData("of_helper", "OptiFine Helper", "http://s.optifine.net/capes/Dinnerbone.png", true));

        // Minecraft Dungeons Capes
        capes.add(new CapeData("md_creeper", "Creeper Cloak", "http://textures.minecraft.net/texture/1981bea5f6fdc7db4d851278f21c3607b7e542cc3c4ceed1f02e6e0ac6c1f", true));
        capes.add(new CapeData("md_enderman", "Enderman Cloak", "http://textures.minecraft.net/texture/5c7c3e305f5f35c1b5c584b1922dae87a5b3c25a2c310be36707a307b317fa", true));
        capes.add(new CapeData("md_hero", "Hero Cloak", "http://textures.minecraft.net/texture/1717667f5483b0771e740d691344dc3d4f86a83a3f13b2c3d4b6c3a71e8a4", true));

        // Minecraft Legends Capes
        capes.add(new CapeData("ml_founder", "Legends Founder Cape", "http://textures.minecraft.net/texture/3b2e5b0c8114b1e45fcb0c8f8cc5d7d0bf93e7f7d5b36e7c84d3e1c0c4a3d", true));

        // Minecraft Live Capes
        capes.add(new CapeData("ml_2023", "Minecraft Live 2023", "http://textures.minecraft.net/texture/2c3f5a1d8f1f6fafaef37cb49b9c012d2d4d2efde2b48c719c2cb5c337bc", true));

        // Minecraft Marketplace Capes
        capes.add(new CapeData("marketplace_creator", "Marketplace Creator Cape", "http://textures.minecraft.net/texture/c3af7fb821254664558f28361158ca73303c9d2eaac95f49a533295323a969ce", true));
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
