package me.kteq.hiddenarmor.handler;

import me.kteq.hiddenarmor.listener.packet.PacketHandler;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;

public final class ArmorUpdater {

    public static void updatePlayer(Player player) {
        updateSelf(player);
        updateOthers(player);
    }

    public static void updateSelf(Player player) {
        for (var slot : EquipmentSlot.values()) {
            if (slot.isArmor()) {
                PacketHandler.sendContainerSetSlotPacket(player, slot);
            }
        }
    }

    public static void updateOthers(Player player) {
        var enumMap = new EnumMap<EquipmentSlot, ItemStack>(EquipmentSlot.class);

        for (var slot : EquipmentSlot.values()) {
            enumMap.put(slot, player.getInventory().getItem(slot));
        }

        for (var viewer : player.getWorld().getPlayers()) {
            if (!viewer.equals(player) && player.getTrackedPlayers().contains(viewer)) {
                viewer.sendEquipmentChange(player, enumMap);
            }
        }
    }

    private ArmorUpdater() {
        throw new UnsupportedOperationException();
    }
}
