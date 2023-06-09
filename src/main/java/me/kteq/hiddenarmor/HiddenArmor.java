package me.kteq.hiddenarmor;

import me.kteq.hiddenarmor.command.HiddenArmorCommand;
import me.kteq.hiddenarmor.command.ToggleArmorCommand;
import me.kteq.hiddenarmor.handler.MessageHandler;
import me.kteq.hiddenarmor.listener.EntityToggleGlideListener;
import me.kteq.hiddenarmor.listener.GameModeListener;
import me.kteq.hiddenarmor.listener.InventoryShiftClickListener;
import me.kteq.hiddenarmor.listener.PotionEffectListener;
import me.kteq.hiddenarmor.listener.packet.PlayerListener;
import me.kteq.hiddenarmor.manager.HiddenArmorManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class HiddenArmor extends JavaPlugin {

    private static final boolean FOLIA;

    static {
        boolean folia = false;

        try {
            Bukkit.class.getDeclaredMethod("getAsyncScheduler");
            folia = true;
        } catch (NoSuchMethodException ignored) {
        }

        FOLIA = folia;
    }

    private final AtomicReference<Config> configReference = new AtomicReference<>(Config.createDefault());
    private final Map<Integer, UUID> entityUUIDMap = new ConcurrentHashMap<>();

    private HiddenArmorManager hiddenArmorManager;
    private PlayerListener playerListener;

    @Override
    public void onEnable() {
        // Default config file
        this.saveDefaultConfig();
        this.reloadConfig();
        this.reloadHiddenArmorConfig();

        // Instantiate managers
        this.hiddenArmorManager = new HiddenArmorManager(this);

        MessageHandler.getInstance().setup(this, "&c[&fHiddenArmor&c] &f");

        // Enable commands
        new ToggleArmorCommand(this);
        new HiddenArmorCommand(this);

        this.playerListener = new PlayerListener(this);
        this.playerListener.hookAll();

        // Register event listeners
        new InventoryShiftClickListener(this);
        new GameModeListener(this);
        new PotionEffectListener(this);
        new EntityToggleGlideListener(this);
    }

    @Override
    public void onDisable() {
        playerListener.unhookAll();
        hiddenArmorManager.saveCurrentEnabledPlayers();
    }

    public Config getHiddenArmorConfig() {
        return configReference.get();
    }

    public void reloadHiddenArmorConfig() {
        configReference.set(Config.readConfig(getConfig()));
    }

    public HiddenArmorManager getHiddenArmorManager() {
        return hiddenArmorManager;
    }

    public void cacheEntityId(Player player) {
        entityUUIDMap.put(player.getEntityId(), player.getUniqueId());
    }

    public void removeCache(Player player) {
        entityUUIDMap.remove(player.getEntityId());
    }

    public UUID getUUIDFromEntityId(int id) {
        return entityUUIDMap.get(id);
    }

    public void registerListener(Listener listener) {
        Bukkit.getServer().getPluginManager().registerEvents(listener, this);
    }

    public void runPlayerTask(Player player, Consumer<Player> task) {
        if (FOLIA) {
            //player.getScheduler().runDelayed(this, $ -> task.accept(player), null, 1L);
            throw new UnsupportedOperationException(); // FIXME: folia
        } else {
            getServer().getScheduler().runTaskLater(this, () -> task.accept(player), 1L);
        }
    }

    public record Config(boolean alwaysHideGearWhenInvisible,
                         boolean ignoreLeatherArmor, boolean ignoreTurtleHelmet, boolean ignoreElytra,
                         boolean toggleDefaultPermission, boolean toggleOtherDefaultPermission,
                         String defaultLocale) {

        private static Config createDefault() {
            return new Config(
                    false,
                    true, false, false,
                    true, false,
                    "en_us");
        }

        private static Config readConfig(ConfigurationSection source) {
            return new Config(
                    source.getBoolean("invisibility-potion.always-hide-gear"),
                    source.getBoolean("ignore.leather-armor", true),
                    source.getBoolean("ignore.turtle-helmet", false),
                    source.getBoolean("ignore.elytra", false),
                    source.getBoolean("default-permissions.toggle", true),
                    source.getBoolean("default-permissions.toggle-other", false),
                    source.getString("locale.default-locale", "en_us")
            );
        }
    }
}
