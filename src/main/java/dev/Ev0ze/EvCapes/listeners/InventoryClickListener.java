package dev.Ev0ze.EvCapes.listeners;

import dev.Ev0ze.EvCapes.commands.CapesCommand;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class InventoryClickListener implements Listener {
    private final CapesCommand capesCommand;

    public InventoryClickListener(CapesCommand capesCommand) {
        this.capesCommand = capesCommand;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Check if the inventory is our cape selection GUI
        if (event.getView().getTitle().startsWith(ChatColor.DARK_PURPLE + "Cape Selection")) {
            event.setCancelled(true); // Prevent taking items from the GUI

            if (event.getCurrentItem() == null) {
                return;
            }

            // Handle the click in the CapesCommand class
            capesCommand.handleInventoryClick(player, event.getRawSlot());
        }
    }
}
