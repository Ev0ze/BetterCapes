package dev.Ev0ze.EvCapes;

import dev.Ev0ze.EvCapes.commands.CapeCommand;
import dev.Ev0ze.EvCapes.commands.CapesCommand;
import dev.Ev0ze.EvCapes.commands.SkinCommand;
import dev.Ev0ze.EvCapes.listeners.InventoryClickListener;
import dev.Ev0ze.EvCapes.utils.CapeManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    private CapeManager capeManager;

    @Override
    public void onEnable() {
        // Initialize the cape manager
        this.capeManager = new CapeManager(this);

        // Register the /identity command (formerly /nick)
        this.getCommand("identity").setExecutor(new SkinCommand());

        // Register the /stealcape command (formerly /cape)
        CapeCommand capeCmd = new CapeCommand(this);
        this.getCommand("stealcape").setExecutor(capeCmd);
        this.getCommand("stealcape").setTabCompleter(capeCmd);

        // Register the new /capes command
        CapesCommand capesCommand = new CapesCommand(this);
        this.getCommand("capes").setExecutor(capesCommand);

        // Register the inventory click listener for the capes GUI
        getServer().getPluginManager().registerEvents(new InventoryClickListener(capesCommand), this);

        // Save the default config (if not present)
        saveDefaultConfig();

        getLogger().info("EvCapes plugin enabled! Scanning for players with capes...");
    }

    @Override
    public void onDisable() {
        // Shut down the cape manager
        if (capeManager != null) {
            capeManager.shutdown();
        }

        getLogger().info("EvCapes plugin disabled!");
    }

    /**
     * Gets the cape manager
     * @return The cape manager
     */
    public CapeManager getCapeManager() {
        return capeManager;
    }
}