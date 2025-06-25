package me.kteq.hiddenarmor.listener.packet;

import com.destroystokyo.paper.MaterialTags;
import com.mojang.datafixers.util.Pair;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import me.kteq.hiddenarmor.HiddenArmor;
import me.kteq.hiddenarmor.item.ArmorPieceItem;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.inventory.InventoryMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PacketHandler extends ChannelDuplexHandler {

    public static void sendContainerSetSlotPacket(Player player, EquipmentSlot equipmentSlot) {
        int packetIndex = switch (equipmentSlot) {
            case HEAD -> 5;
            case CHEST -> 6;
            case LEGS -> 7;
            case FEET -> 8;
            case HAND, OFF_HAND, BODY, SADDLE -> throw new IllegalArgumentException();
        };

        var serverPlayer = MinecraftServer.getServer().getPlayerList().getPlayer(player.getUniqueId());

        if (serverPlayer == null) { // wtf
            return;
        }

        serverPlayer.connection.send(
                new ClientboundContainerSetSlotPacket(
                        serverPlayer.inventoryMenu.containerId,
                        serverPlayer.inventoryMenu.incrementStateId(),
                        packetIndex,
                        serverPlayer.getInventory().equipment.get(CraftEquipmentSlot.getNMS(equipmentSlot))
                )
        );
    }

    private final HiddenArmor plugin;
    private final UUID uuid;

    PacketHandler(HiddenArmor plugin, UUID uuid) {
        this.plugin = plugin;
        this.uuid = uuid;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Object newPacket = null;

        if (msg instanceof ClientboundSetEquipmentPacket packet) {
            newPacket = handlePacket(packet);
        } else if (msg instanceof ClientboundContainerSetContentPacket packet) {
            newPacket = handlePacket(packet);
        } else if (msg instanceof ClientboundContainerSetSlotPacket packet) {
            newPacket = handlePacket(packet);
        } else if (msg instanceof ClientboundBundlePacket packet) { // See ServerEntity#addPairing
            List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
            packet.subPackets().forEach(subPacket -> {
                if (subPacket instanceof ClientboundSetEquipmentPacket setEquipmentPacket) {
                    var newSubPacket = handlePacket(setEquipmentPacket);

                    if (newSubPacket != null) {
                        packets.add(newSubPacket);
                        return;
                    }
                }
                packets.add(subPacket);
            });
            newPacket = new ClientboundBundlePacket(packets);
        }

        super.write(ctx, newPacket != null ? newPacket : msg, promise);
    }

    private ClientboundSetEquipmentPacket handlePacket(ClientboundSetEquipmentPacket packet) {
        var targetPlayerUuid = plugin.getUUIDFromEntityId(packet.getEntity());

        if (targetPlayerUuid == null) {
            return null;
        }

        var target = Bukkit.getPlayer(targetPlayerUuid);

        if (target == null) {
            return null;
        }

        if (plugin.getHiddenArmorManager().isArmorHidden(target)) {
            return new ClientboundSetEquipmentPacket(
                    packet.getEntity(),
                    packet.getSlots().stream().map(pair -> {
                        if (!pair.getFirst().isArmor()) {
                            return pair;
                        }

                        var bukkitItem = pair.getSecond().getBukkitStack();

                        if (bukkitItem.getType() == Material.ELYTRA
                                && (plugin.getHiddenArmorConfig().ignoreElytra() || target.isGliding())
                                && !target.isInvisible()) {
                            return pair;
                        }

                        if (!ignore(bukkitItem)) {
                            return new Pair<>(pair.getFirst(), net.minecraft.world.item.ItemStack.EMPTY);
                        }

                        return pair;
                    }).collect(Collectors.toCollection(ArrayList::new))
            );
        } else {
            return null;
        }
    }

    private ClientboundContainerSetContentPacket handlePacket(ClientboundContainerSetContentPacket packet) {
        var player = MinecraftServer.getServer().getPlayerList().getPlayer(uuid);

        if (player == null ||
                !plugin.getHiddenArmorManager().isArmorHidden(player.getBukkitEntity()) ||
                packet.containerId() != InventoryMenu.CONTAINER_ID) {
            return null;
        }

        var copied = NonNullList.<net.minecraft.world.item.ItemStack>createWithCapacity(packet.items().size());

        for (int i = 0; i < packet.items().size(); i++) {
            net.minecraft.world.item.ItemStack item = packet.items().get(i);

            if (InventoryMenu.ARMOR_SLOT_START <= i && i < InventoryMenu.ARMOR_SLOT_END) {
                ItemStack bukkitItem = item.asBukkitMirror();

                if (ignore(bukkitItem)) {
                    copied.add(i, item);
                    continue;
                }

                ItemStack armorPieceItem = ArmorPieceItem.create(bukkitItem);
                copied.add(i, CraftItemStack.asNMSCopy(armorPieceItem));
            } else {
                copied.add(i, item);
            }
        }

        return new ClientboundContainerSetContentPacket(
                InventoryMenu.CONTAINER_ID,
                packet.stateId(),
                copied,
                packet.carriedItem()
        );
    }

    private ClientboundContainerSetSlotPacket handlePacket(ClientboundContainerSetSlotPacket packet) {
        var player = MinecraftServer.getServer().getPlayerList().getPlayer(uuid);

        if (player == null ||
                !plugin.getHiddenArmorManager().isArmorHidden(player.getBukkitEntity()) ||
                packet.getContainerId() != InventoryMenu.CONTAINER_ID) {
            return null;
        }

        if (!(InventoryMenu.ARMOR_SLOT_START <= packet.getSlot() && packet.getSlot() < InventoryMenu.ARMOR_SLOT_END)) {
            return null;
        }

        ItemStack bukkitItem = packet.getItem().asBukkitMirror();
        if (ignore(bukkitItem)) {
            return null;
        }

        return new ClientboundContainerSetSlotPacket(
                packet.getContainerId(),
                packet.getStateId(),
                packet.getSlot(),
                CraftItemStack.asNMSCopy(ArmorPieceItem.create(bukkitItem))
        );
    }

    private boolean ignore(ItemStack itemStack) {
        return (plugin.getHiddenArmorConfig().ignoreLeatherArmor() && itemStack.getType().toString().startsWith("LEATHER")) ||
                (plugin.getHiddenArmorConfig().ignoreTurtleHelmet() && itemStack.getType().equals(Material.TURTLE_HELMET)) ||
                !(MaterialTags.ARMOR.isTagged(itemStack.getType()) || itemStack.getType().equals(Material.ELYTRA)) ||
                (itemStack.getType().equals(Material.ELYTRA) && plugin.getHiddenArmorConfig().ignoreElytra());
    }
}
