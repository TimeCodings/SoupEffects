package dev.timecoding.soupeffects.listener;

import dev.timecoding.soupeffects.SoupEffects;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class SoupListener implements Listener {

    private SoupEffects plugin;

    private HashMap<Player, List<PotionEffect>> potionEffectList = new HashMap<>();
    public SoupListener(SoupEffects plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event){
        Player player = event.getPlayer();
        if(!player.hasMetadata("no_soup")) {
            if (this.plugin.getConfigHandler().getBoolean("ToggleReceiver") && event.getPlayer().getKiller() != null) {
                player = event.getPlayer().getKiller();
            }
            Player finalPlayer = player;
            Bukkit.getScheduler().runTaskLater(this.plugin, new Runnable() {
                @Override
                public void run() {
                    ItemStack randomSoup = getRandomSoup(finalPlayer);
                    if (potionEffectList.containsKey(finalPlayer)) {
                        if (!plugin.getConfigHandler().getBoolean("RemoveEffectsOnDeath")) {
                            for (PotionEffect potionEffect : potionEffectList.get(finalPlayer)) {
                                finalPlayer.addPotionEffect(potionEffect);
                            }
                        } else {
                            potionEffectList.remove(finalPlayer);
                        }
                    }
                    if (randomSoup != null) {
                        finalPlayer.getInventory().addItem(randomSoup);
                        finalPlayer.playSound(finalPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2, 2);
                    }
                }
            }, 10);
        }else{
            player.removeMetadata("no_soup", this.plugin);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event){
        if(event.getEntityType() == EntityType.PLAYER){
            if(event.getDamager().getType() == EntityType.ARROW || event.getDamager().getType() == EntityType.SPECTRAL_ARROW){
                Arrow arrow = (Arrow) event.getDamager();
                if(arrow.getShooter() instanceof Player){
                    Player player = (Player) arrow.getShooter();
                    if(player.getUniqueId().toString().equalsIgnoreCase(event.getEntity().getUniqueId().toString())) {
                        if(player.getHealth() <= event.getFinalDamage()) {
                            player.setMetadata("no_soup", new FixedMetadataValue(this.plugin, true));
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event){
        Player player = event.getPlayer();
            if(event.getItem() != null && event.getItem().getType() != Material.AIR){
                ItemStack itemStack = event.getItem();
                if(itemStack.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(this.plugin, "potion_effect_type"), PersistentDataType.STRING)){
                    event.setCancelled(false);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.getByName(itemStack.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(this.plugin, "potion_effect_type"), PersistentDataType.STRING)), 999999999, itemStack.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(this.plugin, "potion_effect_amplifier"), PersistentDataType.INTEGER), true, true));
                    player.getItemInHand().setType(Material.AIR);
                    player.updateInventory();
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 2, 2);
                    List<PotionEffect> list = new ArrayList<>();
                    if(potionEffectList.containsKey(player)){
                        list = potionEffectList.get(player);
                    }
                    list.add(getRandomPotionEffects().get(itemStack.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "potion_effect_picked"), PersistentDataType.INTEGER)));
                    potionEffectList.remove(player);
                    potionEffectList.put(player, list);
                }
            }
    }

    private ItemStack getRandomSoup(Player player){
        if(this.getRandomPotionEffects().size() > 0){
            Random random = new Random();
            Integer picked = random.nextInt(this.getRandomPotionEffects().size());
            ItemStack itemStack = this.plugin.getConfigHandler().readItem("Soup", player, this.getRandomPotionEffects().get(picked).getType());
            ItemMeta itemMeta = itemStack.getItemMeta();
            PersistentDataContainer persistentDataContainer = itemMeta.getPersistentDataContainer();
            persistentDataContainer.set(new NamespacedKey(this.plugin, "potion_effect_type"), PersistentDataType.STRING, this.getRandomPotionEffects().get(picked).getType().getName().toString());
            persistentDataContainer.set(new NamespacedKey(this.plugin, "potion_effect_amplifier"), PersistentDataType.INTEGER, this.getRandomPotionEffects().get(picked).getAmplifier());
            persistentDataContainer.set(new NamespacedKey(this.plugin, "potion_effect_picked"), PersistentDataType.INTEGER, picked);
            itemStack.setItemMeta(itemMeta);
            return itemStack;
        }
        return null;
    }

    private List<PotionEffect> getRandomPotionEffects(){
        List<PotionEffect> potionEffectTypes = new ArrayList<>();
        if(this.plugin.getConfigHandler().keyExists("Effects") && this.plugin.getConfigHandler().getConfig().getStringList("Effects") != null){
            for(String string : this.plugin.getConfigHandler().getConfig().getStringList("Effects")){
                String type = string;
                Integer amplifier = 1;
                if(type.split(":").length > 1){
                    type = type.split(":")[0];
                    if(isInteger(type.split(":")[1])){
                        amplifier = Integer.parseInt(type.split(":")[1]);
                    }
                }
                if(PotionEffectType.getByName(type) != null){
                    potionEffectTypes.add(new PotionEffect(PotionEffectType.getByName(type), 999999999, amplifier, true, true));
                }
            }
        }
        return potionEffectTypes;
    }

    private boolean isInteger(String string){
        try{
            Integer.parseInt(string);
            return true;
        }catch (NumberFormatException exception){
            return false;
        }
    }

}
