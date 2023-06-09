package me.kteq.hiddenarmor.manager;

import me.kteq.hiddenarmor.HiddenArmor;
import me.kteq.hiddenarmor.handler.ArmorUpdater;
import me.kteq.hiddenarmor.handler.MessageHandler;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;


public class HiddenArmorManager {

    private final HiddenArmor plugin;
    private final Set<UUID> enabledPlayers = Collections.synchronizedSet(new HashSet<>());

    public HiddenArmorManager(HiddenArmor plugin) {
        this.plugin = plugin;
        loadEnabledPlayers();
    }

    public void togglePlayer(Player player, boolean inform) {
        if (isEnabled(player)) {
            disablePlayer(player, inform);
        } else {
            enablePlayer(player, inform);
        }
    }

    public void enablePlayer(Player player, boolean inform) {
        if (isEnabled(player)) return;
        if (inform) {
            Map<String, String> placeholderMap = new HashMap<>();
            placeholderMap.put("visibility", "%visibility-hidden%");
            MessageHandler.getInstance().actionBar(player, "%armor-visibility%", false, placeholderMap);
        }

        this.enabledPlayers.add(player.getUniqueId());
        ArmorUpdater.updatePlayer(player);
    }

    public void disablePlayer(Player player, boolean inform) {
        if (!isEnabled(player)) return;
        if (inform) {
            Map<String, String> placeholderMap = new HashMap<>();
            placeholderMap.put("visibility", "%visibility-shown%");
            MessageHandler.getInstance().actionBar(player, "%armor-visibility%", false, placeholderMap);
        }

        enabledPlayers.remove(player.getUniqueId());
        ArmorUpdater.updatePlayer(player);
    }

    public boolean isEnabled(Player player) {
        return this.enabledPlayers.contains(player.getUniqueId());
    }

    public boolean isArmorHidden(Player player) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return false;
        }

        if (player.isInvisible()) {
            return plugin.getHiddenArmorConfig().alwaysHideGearWhenInvisible();
        }

        return isEnabled(player);
    }

    public void saveCurrentEnabledPlayers() {
        var enabledPlayersConfig = new YamlConfiguration();
        enabledPlayersConfig.set("enabled-players", this.enabledPlayers.stream().map(UUID::toString).toList());

        try {
            enabledPlayersConfig.save(enabledPlayersFile().toFile());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save enabled players to enabled-players.yml", e);
        }
    }

    private void loadEnabledPlayers() {
        var path = enabledPlayersFile();

        if (Files.notExists(path)) {
            return;
        }

        var enabledPlayersConfig = YamlConfiguration.loadConfiguration(path.toFile());
        this.enabledPlayers.clear();
        this.enabledPlayers.addAll(enabledPlayersConfig.getStringList("enabled-players").stream().map(this::parseToUUID).filter(Objects::nonNull).toList());
    }

    private UUID parseToUUID(String strUuid) {
        try {
            return UUID.fromString(strUuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Path enabledPlayersFile() {
        return plugin.getDataFolder().toPath().resolve("enabled-players.yml");
    }
}
