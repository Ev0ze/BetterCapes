package io.smcode.skinChanger;

import io.smcode.skinChanger.commands.CapeCommand;
import io.smcode.skinChanger.commands.SkinCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SkinChangerPlugin extends JavaPlugin {
    public SkinChangerPlugin() {
    }

    public void onEnable() {
        this.getCommand("nick").setExecutor(new SkinCommand());
        this.getCommand("cape").setExecutor(new CapeCommand());
    }
}