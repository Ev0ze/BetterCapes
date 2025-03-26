package dev.Ev0ze.EvCapes;

import dev.Ev0ze.EvCapes.commands.CapeCommand;
import dev.Ev0ze.EvCapes.commands.SkinCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        // Register the /nick command as before.
        this.getCommand("nick").setExecutor(new SkinCommand());
        // Register the updated /cape command.
        CapeCommand capeCmd = new CapeCommand(this);
        this.getCommand("cape").setExecutor(capeCmd);
        this.getCommand("cape").setTabCompleter(capeCmd);
        // Save the default config (if not present)
        saveDefaultConfig();
    }
}