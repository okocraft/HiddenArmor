package me.kteq.hiddenarmor.listener;

import me.kteq.hiddenarmor.HiddenArmor;
import me.kteq.hiddenarmor.handler.ArmorUpdater;
import me.kteq.hiddenarmor.manager.HiddenArmorManager;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

public class GameModeListener implements Listener {

    private final HiddenArmor plugin;
    private final HiddenArmorManager hiddenArmorManager;

    public GameModeListener(HiddenArmor plugin) {
        this.plugin = plugin;
        this.hiddenArmorManager = plugin.getHiddenArmorManager();
        plugin.registerListener(this);
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (!hiddenArmorManager.isEnabled(event.getPlayer())) {
            return;
        }

        if (event.getNewGameMode().equals(GameMode.CREATIVE)) {
            hiddenArmorManager.disablePlayer(event.getPlayer(), false);
            ArmorUpdater.updatePlayer(event.getPlayer());
        }

        plugin.runPlayerTask(event.getPlayer(), player -> {
            if (event.getNewGameMode().equals(GameMode.CREATIVE)) {
                hiddenArmorManager.enablePlayer(player, false);
            } else {
                ArmorUpdater.updatePlayer(player);
            }
        });
    }
}
