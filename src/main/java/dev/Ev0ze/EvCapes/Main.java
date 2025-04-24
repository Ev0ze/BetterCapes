package dev.Ev0ze.EvCapes;

import dev.Ev0ze.EvCapes.commands.CapeCommand;
import dev.Ev0ze.EvCapes.commands.SkinCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        // Register the /identity command (formerly /nick)
        this.getCommand("identity").setExecutor(new SkinCommand());
        // Register the /stealcape command (formerly /cape)
        CapeCommand capeCmd = new CapeCommand(this);
        this.getCommand("stealcape").setExecutor(capeCmd);
        this.getCommand("stealcape").setTabCompleter(capeCmd);
        // Save the default config (if not present)
        saveDefaultConfig();
    }
}