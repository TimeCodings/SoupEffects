package dev.timecoding.soupeffects;

import dev.timecoding.soupeffects.config.ConfigHandler;
import dev.timecoding.soupeffects.listener.SoupListener;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class SoupEffects extends JavaPlugin {

    private ConfigHandler configHandler;

    @Override
    public void onEnable() {
        ConsoleCommandSender consoleCommandSender = this.getServer().getConsoleSender();
        this.configHandler = new ConfigHandler(this);
        consoleCommandSender.sendMessage(ChatColor.GREEN+"Loading ConfigHandler...");
        this.configHandler.init();
        consoleCommandSender.sendMessage(ChatColor.GREEN+"Successfully loaded the ConfigHandler...");
        consoleCommandSender.sendMessage(ChatColor.YELLOW+"SoupEffects "+ChatColor.GREEN+"by TimeCode got enabled!");
        PluginManager pluginManager = this.getServer().getPluginManager();
        pluginManager.registerEvents(new SoupListener(this), this);
    }

    @Override
    public void onDisable() {
        ConsoleCommandSender consoleCommandSender = this.getServer().getConsoleSender();
        consoleCommandSender.sendMessage(ChatColor.YELLOW+"SoupEffects "+ChatColor.GREEN+"by TimeCode got "+ChatColor.RED+"disabled!");
    }

    public ConfigHandler getConfigHandler() {
        return configHandler;
    }
}
