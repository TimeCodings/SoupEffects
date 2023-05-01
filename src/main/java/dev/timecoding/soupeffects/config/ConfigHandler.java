package dev.timecoding.soupeffects.config;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import dev.timecoding.soupeffects.SoupEffects;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ConfigHandler {

    public YamlConfiguration cfg = null;
    private SoupEffects plugin;
    private File f = null;
    private boolean retry = false;
    private String newconfigversion = "1.1.0";

    public ConfigHandler(SoupEffects plugin) {
        this.plugin = plugin;
    }

    public void init() {
        f = new File(plugin.getDataFolder(), "config.yml");
        if (!f.exists()) {
            plugin.saveDefaultConfig();
        }
        cfg = YamlConfiguration.loadConfiguration(f);
        cfg.options().copyDefaults(true);
        checkForConfigUpdate();
    }

    public String getPluginVersion() {
        return plugin.getDescription().getVersion();
    }

    public void save() {
        try {
            cfg.save(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNewestConfigVersion() {
        return this.newconfigversion;
    }

    public boolean configUpdateAvailable() {
        if (!getNewestConfigVersion().equalsIgnoreCase(getString("config-version")))
            return true;
        return false;
    }

    public void reload() {
        f = new File(plugin.getDataFolder(), "config.yml");
        cfg = YamlConfiguration.loadConfiguration(f);
    }

    public ItemStack readItem(String key, Player player, PotionEffectType potionEffectType){
        key = "Item."+key+".";
        ItemStack itemStack = new ItemStack(Material.AIR, 1);
        if(!getString(key+"Material").equalsIgnoreCase(key+"Material")){
            itemStack = new ItemStack(getMaterialByString(getString(key+"Material")), 1);
        }
        if(getString(key+"Texture") != "" && !getString(key+"Texture").equalsIgnoreCase(key+"Texture")){
            itemStack = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta sm = (SkullMeta) itemStack.getItemMeta();
            GameProfile profile = new GameProfile(UUID.randomUUID(), "");
            profile.getProperties().put("textures", new Property("texture", getString(key+"Texture")));
            try {
                Field profileField = sm.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(sm, profile);
            } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
                e.printStackTrace();
            }
            itemStack.setItemMeta(sm);
        }
        if(getInteger(key+"Amount") > 1) {
            itemStack.setAmount(getInteger(key+"Amount"));
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        String displayName = getString(key+"DisplayName");
        if(displayName.equalsIgnoreCase("") || displayName.equalsIgnoreCase(key+"DisplayName")){
            displayName = cfg.getString(key+"DisplayName");
        }
        if(player != null){
            displayName = displayName.replace("%player%", player.getName());
        }
        itemMeta.setDisplayName(displayName);
        List<String> enchants = getStringList(key+"Enchantments", player);
        if(enchants != null) {
            for (String ench : enchants) {
                String[] split = ench.split(":");
                if (split.length > 1) {
                    boolean ignoreLevelRestriction = true;
                    if (split.length >= 3) {
                        ignoreLevelRestriction = Boolean.valueOf(split[2]);
                    }
                    itemMeta.addEnchant(getEnchantmentByString(split[0]), Integer.valueOf(split[1]), ignoreLevelRestriction);
                } else {
                    itemMeta.addEnchant(getEnchantmentByString(ench), 1, true);
                }
            }
        }
        List<String> lore = getStringList(key+"Lore", player);
        lore.replaceAll(s -> s.replace("%effect%", potionEffectType.getName()));
        itemMeta.setLore(lore);
        itemMeta.setUnbreakable(getBoolean(key+"Unbreakable"));
        itemStack.setDurability(Short.valueOf(String.valueOf(getInteger(key+"Durability"))));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public Integer getItemSlot(String key) {
        return getInteger("Item." + key + ".Slot");
    }

    public void checkForConfigUpdate() {
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "Checking for config updates...");
        if (configUpdateAvailable()) {
            final Map<String, Object> quicksave = getConfig().getValues(true);
            File file = new File("plugins//TikTokLive", "config.yml");
            if (file.exists()) {
                try {
                    Files.delete(file.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Config Update found! (" + getNewestConfigVersion() + ") Updating config...");
                Bukkit.getScheduler().runTaskLaterAsynchronously(this.plugin, new Runnable() {

                    public void run() {
                        plugin.saveResource("config.yml", true);
                        reload();
                        for (String save : quicksave.keySet()) {
                            if (keyExists(save) && quicksave.get(save) != null && !save.equalsIgnoreCase("config-version")) {
                                getConfig().set(save, quicksave.get(save));
                            }
                        }
                        save();
                        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Config got updated!");
                    }
                }, 50);
            } else {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "No Config found! Creating a new one...");
                this.plugin.saveResource("config.yml", false);
            }
        } else {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "No config update found!");
        }
    }


    private List<String> getStringList(String key, Player player){
        if(keyExists(key)){
            List<String> list = getConfig().getStringList(key);
            list.replaceAll(msg -> msg.replace("&", "§"));
            if(player != null){
                list.replaceAll(msg -> msg.replace("%player%", player.getName()));
            }
            return list;
        }
        return new ArrayList<>();
    }

    public Material getMaterialByString(String material) {
        for (Material mats : Material.values()) {
            if (mats.name().toLowerCase().equalsIgnoreCase(material.toLowerCase())) {
                return Material.valueOf(material);
            }
        }
        return Material.GRASS;
    }

    public Enchantment getEnchantmentByString(String enchant) {
        for (Enchantment ench : Enchantment.values()) {
            if (ench.toString().toLowerCase().equalsIgnoreCase(enchant.toLowerCase())) {
                return Enchantment.getByName(enchant);
            }
        }
        return Enchantment.ARROW_DAMAGE;
    }

    public YamlConfiguration getConfig() {
        return cfg;
    }

    public void setString(String key, String value) {
        cfg.set(key, value);
        save();
    }

    public Integer getInteger(String key) {
        if (keyExists(key)) {
            return cfg.getInt(key);
        }
        return 1;
    }

    public String getString(String key) {
        if (keyExists(key)) {
            return ChatColor.translateAlternateColorCodes('&', cfg.getString(key));
        }
        return "";
    }

    public Boolean getBoolean(String key) {
        if (keyExists(key)) {
            return cfg.getBoolean(key);
        }
        return false;
    }

    public boolean keyExists(String key) {
        if (cfg.get(key) != null) {
            return true;
        }
        return false;
    }
}
