package me.kteq.hiddenarmor.listener;

import me.kteq.hiddenarmor.HiddenArmor;
import me.kteq.hiddenarmor.handler.ArmorUpdater;
import me.kteq.hiddenarmor.manager.HiddenArmorManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;

public class EntityToggleGlideListener implements Listener {

    private final HiddenArmor plugin;
    private final HiddenArmorManager hiddenArmorManager;

    public EntityToggleGlideListener(HiddenArmor plugin) {
        this.plugin = plugin;
        this.hiddenArmorManager = plugin.getHiddenArmorManager();
        plugin.registerListener(this);
    }

    @EventHandler
    public void onPlayerToggleGlide(EntityToggleGlideEvent e) {
        if (!(e.getEntity() instanceof Player player) || !hiddenArmorManager.isArmorHidden(player)) {
            return;
        }

        plugin.runPlayerTask(player, ArmorUpdater::updatePlayer);
    }
}
