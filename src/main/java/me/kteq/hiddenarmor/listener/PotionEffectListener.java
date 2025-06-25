package me.kteq.hiddenarmor.listener;

import me.kteq.hiddenarmor.HiddenArmor;
import me.kteq.hiddenarmor.handler.ArmorUpdater;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffectType;

public class PotionEffectListener implements Listener {

    private final HiddenArmor plugin;

    public PotionEffectListener(HiddenArmor plugin) {
        this.plugin = plugin;
        plugin.registerListener(this);
    }

    @EventHandler
    public void onPlayerInvisibleEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getModifiedType() != PotionEffectType.INVISIBILITY) {
            return;
        }

        plugin.runPlayerTask(player, ArmorUpdater::updatePlayer);
    }
}
