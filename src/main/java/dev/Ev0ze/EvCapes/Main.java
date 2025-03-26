package dev.Ev0ze.EvCapes;

import dev.Ev0ze.EvCapes.commands.CapeCommand;
import dev.Ev0ze.EvCapes.commands.SkinCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        this.getCommand("nick").setExecutor(new SkinCommand());
        this.getCommand("cape").setExecutor(new CapeCommand(this));
    }
}
