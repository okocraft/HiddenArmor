package me.kteq.hiddenarmor.listener.packet;

import me.kteq.hiddenarmor.HiddenArmor;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerListener implements Listener {

    private static final String IDENTIFIER = "HiddenArmor";

    private final HiddenArmor plugin;
    private final AtomicBoolean closing = new AtomicBoolean();

    public PlayerListener(HiddenArmor plugin) {
        this.plugin = plugin;
        plugin.registerListener(this);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!closing.get()) {
            hook(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!closing.get()) {
            unhook(event.getPlayer());
        }
    }

    public void hookAll() {
        if (!closing.get()) {
            Bukkit.getOnlinePlayers().forEach(this::hook);
        }
    }

    public void unhookAll() {
        if (!closing.getAndSet(true)) {
            Bukkit.getOnlinePlayers().forEach(this::unhook);
        }
    }

    private void hook(Player player) {
        plugin.cacheEntityId(player);

        var serverPlayer = MinecraftServer.getServer().getPlayerList().getPlayer(player.getUniqueId());

        if (serverPlayer == null) {
            return;
        }

        var pipeline = serverPlayer.connection.connection.channel.pipeline();

        if (pipeline.get(IDENTIFIER) == null) {
            pipeline.addBefore("packet_handler", IDENTIFIER, new PacketHandler(plugin, player.getUniqueId()));
        }
    }

    private void unhook(Player player) {
        plugin.removeCache(player);

        var serverPlayer = MinecraftServer.getServer().getPlayerList().getPlayer(player.getUniqueId());

        if (serverPlayer == null) {
            return;
        }

        var pipeline = serverPlayer.connection.connection.channel.pipeline();

        if (pipeline.get(IDENTIFIER) != null) {
            pipeline.remove(IDENTIFIER);
        }
    }
}
