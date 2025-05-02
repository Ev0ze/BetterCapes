package dev.Ev0ze.EvCapes;

import dev.Ev0ze.EvCapes.commands.CapesCommand;
import dev.Ev0ze.EvCapes.commands.SkinCommand;
import dev.Ev0ze.EvCapes.listeners.InventoryClickListener;
import dev.Ev0ze.EvCapes.utils.ConfigCapeManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    private ConfigCapeManager capeManager;

    @Override
    public void onEnable() {
        // Save default configs
        saveDefaultConfig();
        saveResource("capes.yml", false);

        // Initialize the cape manager
        this.capeManager = new ConfigCapeManager(this);

        // Register the /identity command (formerly /nick)
        this.getCommand("identity").setExecutor(new SkinCommand());

        // Register the /capes command
        CapesCommand capesCommand = new CapesCommand(this);
        this.getCommand("capes").setExecutor(capesCommand);

        // Register the inventory click listener for the capes GUI
        getServer().getPluginManager().registerEvents(new InventoryClickListener(capesCommand), this);

        getLogger().info("EvCapes plugin enabled! Loaded capes from config.");
    }

    @Override
    public void onDisable() {
        getLogger().info("EvCapes plugin disabled!");
    }

    /**
     * Gets the cape manager
     * @return The cape manager
     */
    public ConfigCapeManager getCapeManager() {
        return capeManager;
    }
}