package dev.Ev0ze.EvCapes.commands;

import dev.Ev0ze.EvCapes.Main;
import dev.Ev0ze.EvCapes.models.CapeData;
import dev.Ev0ze.EvCapes.utils.ConfigCapeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CapesCommand implements CommandExecutor {
    private final Plugin plugin;
    private final ConfigCapeManager capeManager;
    private final Map<UUID, CapeData> selectedCapes = new HashMap<>();
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    // Number of capes per page (2 rows of 9)
    private static final int CAPES_PER_PAGE = 18;

    public CapesCommand(Plugin plugin) {
        this.plugin = plugin;
        // Get the cape manager from the Main class
        this.capeManager = ((Main) plugin).getCapeManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can execute this command!");
            return true;
        }

        // Store the player's original cape when they first open the GUI
        capeManager.storeOriginalCape(player);

        // Open the cape selection GUI
        openCapesGUI(player);
        return true;
    }

    /**
     * Opens the cape selection GUI for a player
     * @param player The player to open the GUI for
     */
    private void openCapesGUI(Player player) {
        openCapesGUI(player, 0);
    }

    /**
     * Opens the cape selection GUI for a player at a specific page
     * @param player The player to open the GUI for
     * @param page The page number (0-based)
     */
    private void openCapesGUI(Player player, int page) {
        // Store the player's current page
        playerPages.put(player.getUniqueId(), page);

        // Create a new inventory with 27 slots (3 rows)
        Inventory gui = Bukkit.createInventory(player, 27, ChatColor.DARK_PURPLE + "Cape Selection - Page " + (page + 1));

        // Get the available capes from the CapeManager
        List<CapeData> availableCapes = capeManager.getAvailableCapes();

        // Calculate the total number of pages
        int totalPages = (int) Math.ceil((double) availableCapes.size() / CAPES_PER_PAGE);

        // Calculate the start and end indices for the current page
        int startIndex = page * CAPES_PER_PAGE;
        int endIndex = Math.min(startIndex + CAPES_PER_PAGE, availableCapes.size());

        // Fill the top 2 rows with white banners for each cape on the current page
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            CapeData cape = availableCapes.get(i);
            ItemStack banner = createBanner(cape, player);
            gui.setItem(slot, banner);
            slot++;
        }

        // Add navigation buttons in the bottom row

        // Previous page button (red candle) - only if not on the first page
        if (page > 0) {
            ItemStack prevButton = new ItemStack(Material.RED_CANDLE);
            ItemMeta prevMeta = prevButton.getItemMeta();
            prevMeta.setDisplayName(ChatColor.RED + "Previous Page");
            prevMeta.setLore(Arrays.asList(ChatColor.GRAY + "Click to go to page " + page));
            prevButton.setItemMeta(prevMeta);
            gui.setItem(18, prevButton);
        }

        // Reset button at the bottom middle slot (slot 22)
        ItemStack resetButton = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta resetMeta = resetButton.getItemMeta();
        resetMeta.setDisplayName(ChatColor.RED + "Reset Cape");
        resetMeta.setLore(Arrays.asList(ChatColor.GRAY + "Click to reset to your original cape"));
        resetButton.setItemMeta(resetMeta);
        gui.setItem(22, resetButton);

        // Next page button (green candle) - only if not on the last page
        if (page < totalPages - 1) {
            ItemStack nextButton = new ItemStack(Material.GREEN_CANDLE);
            ItemMeta nextMeta = nextButton.getItemMeta();
            nextMeta.setDisplayName(ChatColor.GREEN + "Next Page");
            nextMeta.setLore(Arrays.asList(ChatColor.GRAY + "Click to go to page " + (page + 2)));
            nextButton.setItemMeta(nextMeta);
            gui.setItem(26, nextButton);
        }

        // Open the GUI for the player
        player.openInventory(gui);
    }

    /**
     * Creates a banner item for a cape
     * @param cape The cape data
     * @param player The player viewing the GUI
     * @return The banner item
     */
    private ItemStack createBanner(CapeData cape, Player player) {
        ItemStack banner = new ItemStack(Material.WHITE_BANNER);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + cape.getAlias() + " Cape");

        List<String> lore = new ArrayList<>();
        // Add the cape URL to the lore
        lore.add(ChatColor.GRAY + "URL: " + ChatColor.YELLOW + cape.getUrl());

        // Check if the player has permission for this cape
        String permissionNode = "evcapes.cape." + cape.getAlias().toLowerCase().replace(" ", "");
        boolean hasPermission = player.hasPermission(permissionNode) || player.hasPermission("evcapes.cape.*");

        if (hasPermission) {
            lore.add(ChatColor.GREEN + "You have permission to use this cape");
        } else {
            lore.add(ChatColor.RED + "You don't have permission to use this cape");
            lore.add(ChatColor.RED + "Required permission: " + permissionNode);
        }

        // Check if this cape is selected by the player
        if (selectedCapes.containsKey(player.getUniqueId()) &&
            selectedCapes.get(player.getUniqueId()).equals(cape)) {
            // Add enchantment glow to show it's selected
            // Using a basic enchantment that should be available in all versions
            // Using NamespacedKey to get the enchantment instead of constants
            meta.addEnchant(Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("unbreaking")), 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            lore.add(ChatColor.GREEN + "Selected");
        } else if (hasPermission) {
            lore.add(ChatColor.GRAY + "Click to select");
        }

        meta.setLore(lore);
        banner.setItemMeta(meta);
        return banner;
    }

    /**
     * Handles inventory click events for the cape selection GUI
     * @param player The player who clicked
     * @param slot The slot that was clicked
     */
    public void handleInventoryClick(Player player, int slot) {
        // Get the current page for this player
        int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);

        // Get the available capes from the CapeManager
        List<CapeData> availableCapes = capeManager.getAvailableCapes();

        // Calculate the total number of pages
        int totalPages = (int) Math.ceil((double) availableCapes.size() / CAPES_PER_PAGE);

        // Check if the click was on the previous page button (slot 18)
        if (slot == 18 && currentPage > 0) {
            // Go to the previous page
            openCapesGUI(player, currentPage - 1);
            return;
        }

        // Check if the click was on the next page button (slot 26)
        if (slot == 26 && currentPage < totalPages - 1) {
            // Go to the next page
            openCapesGUI(player, currentPage + 1);
            return;
        }

        // Check if the click was on a cape banner (slots 0-17)
        if (slot >= 0 && slot < 18) {
            // Calculate the actual index in the capes list
            int capeIndex = currentPage * CAPES_PER_PAGE + slot;

            // Make sure the index is valid
            if (capeIndex < availableCapes.size()) {
                CapeData selectedCape = availableCapes.get(capeIndex);
                String playerWithCape = selectedCape.getPlayerWithCape();

                // Check if the player has permission for this cape
                String permissionNode = "evcapes.cape." + selectedCape.getAlias().toLowerCase().replace(" ", "");
                if (!player.hasPermission(permissionNode) && !player.hasPermission("evcapes.cape.*")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to use the " +
                                      ChatColor.GOLD + selectedCape.getAlias() + ChatColor.RED + " cape!");
                    player.sendMessage(ChatColor.RED + "Required permission: " + permissionNode);

                    // Reopen the GUI on the same page
                    openCapesGUI(player, currentPage);
                    return;
                }

                // Store the selected cape
                selectedCapes.put(player.getUniqueId(), selectedCape);

                // Try to apply the cape directly by URL first
                boolean success = false;
                if (selectedCape.getUrl() != null && !selectedCape.getUrl().isEmpty()) {
                    success = capeManager.applyCapeByUrl(player, selectedCape.getUrl());
                    if (success) {
                        player.sendMessage(ChatColor.GREEN + "Successfully applied the " + ChatColor.GOLD + selectedCape.getAlias() +
                                          ChatColor.GREEN + " cape using direct URL!");
                    }
                }

                // If direct URL method failed, show error message
                if (!success) {
                    player.sendMessage(ChatColor.RED + "Failed to apply the " + ChatColor.GOLD + selectedCape.getAlias() +
                                      ChatColor.RED + " cape. Please contact an administrator.");
                }

                // Reopen the GUI to show the selection on the same page
                openCapesGUI(player, currentPage);
            }
        }

        // Check if the click was on the reset button (slot 22)
        if (slot == 22) {
            if (capeManager.resetCape(player)) {
                player.sendMessage(ChatColor.GREEN + "Your cape has been reset to the original state!");
                selectedCapes.remove(player.getUniqueId());
            } else {
                player.sendMessage(ChatColor.RED + "Failed to reset your cape.");
            }
            player.closeInventory();
        }
    }
}
