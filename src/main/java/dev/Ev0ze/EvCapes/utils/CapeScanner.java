package dev.Ev0ze.EvCapes.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.Ev0ze.EvCapes.models.CapeData;
import org.apache.commons.codec.binary.Base64;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Scans for players with specific capes
 */
public class CapeScanner {
    private static final String PROFILE_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String SKIN_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false";

    // List of popular Minecraft players to scan for capes
    private static final String[] PLAYERS_TO_SCAN = {
        // Mojang team members
        "Notch", "jeb_", "Dinnerbone", "Grumm", "_Grum", "Marc_IRL", "Searge",
        "TheMogMiner", "EvilSeph", "Mojang", "MojangStudios", "KrisJelbring",
        "xlson", "jonkagstrom", "MidnightEnforcer", "Bopogamel", "JahKob",
        "MinecraftChick", "Vu1kan", "Eldrone", "MojangTeam", "Mojang_Team",

        // Popular content creators
        "Dream", "GeorgeNotFound", "Sapnap", "TommyInnit", "Technoblade", "Ph1LzA",
        "Skeppy", "BadBoyHalo", "CaptainSparklez", "DanTDM", "Stampy", "LDShadowLady",
        "iHasCupquake", "PopularMMOs", "PrestonPlayz", "JeromeASF", "BajanCanadian",
        "SkyDoesMinecraft", "AntVenom", "Vikkstar123", "Syndicate", "NettyPlays",
        "Quackity", "KarlJacobs", "Tubbo", "Ranboo", "WilburSoot", "Nihachu", "JackManifold",
        "Punz", "Purpled", "Foolish_Gamers", "HBomb94", "CaptainPuffy", "awesamdude", "Ponk",
        "ConnorEatsPants", "Slimecicle", "Fundy", "Eret", "TheEret", "hannahxxrose", "Antfrost",

        // Minecraft Championship players
        "Smajor", "Smajor1995", "Seapeekay", "KryticZeuz", "Fruitberries", "Illumina",
        "PeteZahHutt", "Quig", "TapL", "Krtzyy", "KingBurren", "Michaelmcchill", "KaraCorvus",
        "FalseSymmetry", "Rendog", "GrianMC", "Grian", "MumboJumbo", "Smallishbeans", "InTheLittleWood",
        "SolidarityGaming", "Shubble", "vGumiho", "GeminiTay", "PearlescentMoon", "GoodTimesWithScar",
        "Cubfan135", "Ryguyrocky", "JoeyGraceffa", "LDShadowLady", "Strawburry17", "Yammy_xox",

        // Hermitcraft members
        "Xisuma", "Xisumavoid", "ZombieCleo", "ImpulseSV", "Tango", "TangoTek", "Zedaph",
        "Docm77", "EthosLab", "VintageBeef", "Keralis", "BdoubleO100", "Iskall85", "StressMonster101",
        "JoeHills", "ZedaphPlays", "iJevin", "Hypnotizd", "xBCrafted", "Welsknight",

        // Other notable players
        "MrBeast", "PewDiePie", "Ninja", "Shroud", "TimTheTatman", "DrLupo",
        "Pokimane", "Valkyrae", "LazarBeam", "Muselk", "Loserfruit", "Vikkstar123",
        "CouRage", "DrDisrespect", "Tfue", "Myth", "SypherPK", "NickEh30",
        "Jschlatt", "Philza", "TheRealKsi", "Ninja", "Corpse_Husband", "Sykkuno",
        "Ludwig", "Disguised_Toast", "Valkyrae", "Pokimane", "Jacksepticeye", "Markiplier",
        "Pewdiepie", "MrBeast6000", "Technothepig", "Skeppy", "a6d", "F1NN5TER", "VelvetIsCake",
        "TapL", "Spifey", "TommyInnit", "Deo", "TimeDeo", "Bitzel", "Wisp", "SB737", "Sneegsnag",
        "Zyph", "GamerBoy80", "Purpled", "ItsAlyssa", "hannahxxrose", "Wallibear", "Sammy_Green",
        "Wispexe", "Refraction", "Swavy", "Furryeboy", "Zyphon_", "Cxlvxn", "Calvin", "Nestor",
        "Fruitberries", "Illumina", "Krinios", "Mefs", "Boffy", "Danteh", "Punz", "Astelic",
        "Timedeo", "Defone", "Manhal_iq", "Huahwi", "Stimpy", "Marcel", "Grapeapplesauce", "Kiingtong"
    };

    // Cache of cape URLs to player names
    private final Map<String, List<String>> capeUrlToPlayers = new ConcurrentHashMap<>();

    // Cache of cape textures
    private final Map<String, String> playerToCapeUrl = new ConcurrentHashMap<>();

    private final Plugin plugin;
    private BukkitTask scanTask;

    // Scan interval in ticks (20 ticks = 1 second)
    // 12000 ticks = 10 minutes
    private static final long SCAN_INTERVAL = 12000L;

    // Rate limit: delay between API requests in milliseconds
    private static final long API_REQUEST_DELAY = 1100L; // Just over 1 second to stay under Mojang's rate limit

    public CapeScanner(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts the cape scanning process
     */
    public void startScanning() {
        // Initial scan
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::scanForCapes);

        // Schedule periodic scans
        scanTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::scanForCapes, SCAN_INTERVAL, SCAN_INTERVAL);
    }

    /**
     * Stops the cape scanning process
     */
    public void stopScanning() {
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
    }

    /**
     * Scans for players with capes
     */
    private void scanForCapes() {
        Bukkit.getLogger().info("[EvCapes] Starting cape scan...");

        // Clear the current cache
        capeUrlToPlayers.clear();
        playerToCapeUrl.clear();

        // Scan each player
        for (String playerName : PLAYERS_TO_SCAN) {
            try {
                // Add delay to avoid rate limiting
                Thread.sleep(API_REQUEST_DELAY);

                // Check if the player has a cape
                String capeUrl = getCapeUrl(playerName);
                if (capeUrl != null) {
                    // Add to player to cape URL map
                    playerToCapeUrl.put(playerName, capeUrl);

                    // Add to cape URL to players map
                    capeUrlToPlayers.computeIfAbsent(capeUrl, k -> new ArrayList<>()).add(playerName);

                    Bukkit.getLogger().info("[EvCapes] Found cape for player " + playerName + ": " + capeUrl);
                }
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "[EvCapes] Error scanning player " + playerName, e);
            }
        }

        Bukkit.getLogger().info("[EvCapes] Cape scan complete. Found " + playerToCapeUrl.size() + " players with capes.");
    }

    /**
     * Gets the cape URL for a player
     * @param playerName The player name
     * @return The cape URL, or null if the player doesn't have a cape
     */
    private String getCapeUrl(String playerName) {
        try {
            // Get the player's UUID
            String profileResponse = makeRequest(PROFILE_URL + playerName);
            JsonObject profileObject;
            try {
                profileObject = JsonParser.parseString(profileResponse).getAsJsonObject();
            } catch (Exception e) {
                Bukkit.getLogger().fine("[EvCapes] Player " + playerName + " does not exist.");
                return null;
            }

            String uuid = profileObject.get("id").getAsString();

            // Get the player's skin data
            String skinResponse = makeRequest(String.format(SKIN_URL, uuid));
            JsonObject skinObject = JsonParser.parseString(skinResponse)
                    .getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();
            String value = skinObject.get("value").getAsString();

            // Decode the texture data
            byte[] decodedBytes = Base64.decodeBase64(value);
            String decodedString = new String(decodedBytes);
            JsonObject textureJson = JsonParser.parseString(decodedString).getAsJsonObject();
            JsonObject textures = textureJson.getAsJsonObject("textures");

            // Check if the player has a cape
            if (textures.has("CAPE")) {
                JsonObject capeObject = textures.getAsJsonObject("CAPE");
                return capeObject.get("url").getAsString();
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[EvCapes] Error getting cape for player " + playerName, e);
        }

        return null;
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
     * Finds a player with a specific cape
     * @param capeUrl The cape URL
     * @return The player name, or null if no player with the cape was found
     */
    public String findPlayerWithCape(String capeUrl) {
        List<String> players = capeUrlToPlayers.get(capeUrl);
        if (players != null && !players.isEmpty()) {
            return players.get(0);
        }
        return null;
    }

    /**
     * Gets the cape URL for a player
     * @param playerName The player name
     * @return The cape URL, or null if the player doesn't have a cape
     */
    public String getPlayerCapeUrl(String playerName) {
        return playerToCapeUrl.get(playerName);
    }

    /**
     * Assigns players to capes based on the scan results
     * @param capes The list of capes to assign players to
     * @return The same list of capes with playerWithCape field populated
     */
    public List<CapeData> assignPlayersToCapes(List<CapeData> capes) {
        for (CapeData cape : capes) {
            // Try to find a player with this cape URL
            String player = findPlayerWithCape(cape.getUrl());

            if (player != null) {
                cape.setPlayerWithCape(player);
                Bukkit.getLogger().info("[EvCapes] Assigned player " + player + " to cape " + cape.getAlias());
            } else {
                // If no player found, use a default player
                cape.setPlayerWithCape("Notch");
                Bukkit.getLogger().warning("[EvCapes] No player found with cape " + cape.getAlias() + " (URL: " + cape.getUrl() + "). Using default player.");
            }
        }

        return capes;
    }
}
