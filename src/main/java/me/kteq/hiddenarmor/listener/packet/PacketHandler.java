package me.kteq.hiddenarmor.listener.packet;

import com.destroystokyo.paper.MaterialTags;
import com.mojang.datafixers.util.Pair;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import me.kteq.hiddenarmor.HiddenArmor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
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
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class PacketHandler extends ChannelDuplexHandler {

    private static final Method AS_NMS_COPY;

    static {
        try {
            var craftItemStackClass = Class.forName(Bukkit.getServer().getClass().getPackageName() + ".inventory.CraftItemStack");
            AS_NMS_COPY = craftItemStackClass.getDeclaredMethod("asNMSCopy", ItemStack.class);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void sendContainerSetSlotPacket(Player player, EquipmentSlot equipmentSlot) {
        // See CraftInventoryPlayer#getHelmet ~ #getBoots
        int index = player.getInventory().getSize() - switch (equipmentSlot) {
            case HEAD -> 2;
            case CHEST -> 3;
            case LEGS -> 4;
            case FEET -> 5;
            case HAND, OFF_HAND -> throw new IllegalArgumentException();
        };

        int packetIndex = switch (equipmentSlot) {
            case HEAD -> 5;
            case CHEST -> 6;
            case LEGS -> 7;
            case FEET -> 8;
            case HAND, OFF_HAND -> throw new IllegalArgumentException();
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
                        serverPlayer.getInventory().getItem(index)
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
        } else if (msg instanceof ClientboundBundlePacket packet && packet.subPackets() instanceof List<Packet<ClientGamePacketListener>> subPackets) { // See ServerEntity#addPairing
            int index = 0;

            for (var sub : packet.subPackets()) {
                if (sub instanceof ClientboundSetEquipmentPacket subPacket) {
                    var newSubPacket = handlePacket(subPacket);

                    if (newSubPacket != null) {
                        subPackets.set(index, newSubPacket);
                    }
                }
                index++;
            }
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
                packet.getContainerId() != InventoryMenu.CONTAINER_ID) {
            return null;
        }

        var copied = NonNullList.<net.minecraft.world.item.ItemStack>createWithCapacity(packet.getItems().size());

        for (int i = 0; i < packet.getItems().size(); i++) {
            var item = packet.getItems().get(i);

            if (InventoryMenu.ARMOR_SLOT_START <= i && i < InventoryMenu.ARMOR_SLOT_END) {
                try {
                    copied.add(i, (net.minecraft.world.item.ItemStack) AS_NMS_COPY.invoke(null, getHiddenArmorPiece(item.asBukkitCopy())));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                copied.add(i, item);
            }
        }

        return new ClientboundContainerSetContentPacket(
                packet.getContainerId(),
                packet.getStateId(),
                copied,
                packet.getCarriedItem()
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

        try {
            return new ClientboundContainerSetSlotPacket(
                    packet.getContainerId(),
                    packet.getStateId(),
                    packet.getSlot(),
                    (net.minecraft.world.item.ItemStack) AS_NMS_COPY.invoke(null, getHiddenArmorPiece(packet.getItem().asBukkitCopy()))
            );
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean ignore(ItemStack itemStack) {
        return (plugin.getHiddenArmorConfig().ignoreLeatherArmor() && itemStack.getType().toString().startsWith("LEATHER")) ||
                (plugin.getHiddenArmorConfig().ignoreTurtleHelmet() && itemStack.getType().equals(Material.TURTLE_HELMET)) ||
                !(MaterialTags.ARMOR.isTagged(itemStack.getType()) || itemStack.getType().equals(Material.ELYTRA)) ||
                (itemStack.getType().equals(Material.ELYTRA) && plugin.getHiddenArmorConfig().ignoreElytra());
    }

    public ItemStack getHiddenArmorPiece(ItemStack itemStack) {
        if (itemStack.getType().equals(Material.AIR)) {
            return itemStack;
        }

        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemStack.getType().equals(Material.ELYTRA)) {
            var result = new ItemStack(Material.ELYTRA);

            result.editMeta(Damageable.class, meta -> {
                meta.displayName(itemMeta.displayName());
                meta.lore(itemMeta.lore());
                meta.setDamage(((Damageable) itemMeta).getDamage());
                itemMeta.getEnchants().forEach((enchantment, level) -> meta.addEnchant(enchantment, level, true));
                meta.setAttributeModifiers(itemMeta.getAttributeModifiers());
            });

            return result;
        }

        var lore = new ArrayList<>(Objects.requireNonNullElse(itemMeta.lore(), Collections.emptyList()));

        var durability = getPieceDurability(itemStack);

        if (durability != null) {
            lore.add(durability);
        }

        Material button = getArmorButtonMaterial(itemStack);

        if (button != null) {
            var newItem = new ItemStack(button);

            newItem.editMeta(meta -> {
                meta.displayName(
                        Objects.requireNonNullElseGet(
                                itemStack.displayName(),
                                () -> Component.translatable()
                                        .key(itemStack.getType().translationKey())
                                        .style(Style.style(itemStack.getRarity().getColor(), TextDecoration.ITALIC.withState(false)))
                                        .build()
                        )
                );
                meta.lore(lore);
                itemMeta.getEnchants().forEach((enchantment, level) -> meta.addEnchant(enchantment, level, true));
            });

            return newItem;
        } else {
            var result = itemStack.clone();
            result.editMeta(meta -> meta.lore(lore));
            return result;
        }
    }

    private Material getArmorButtonMaterial(ItemStack armor) {
        if (!MaterialTags.ARMOR.isTagged(armor.getType())) {
            return null;
        }

        String m = armor.getType().toString();
        if (m.startsWith("NETHERITE_"))
            return Material.POLISHED_BLACKSTONE_BUTTON;
        if (m.startsWith("DIAMOND_"))
            return Material.WARPED_BUTTON;
        if (m.startsWith("GOLDEN_"))
            return Material.BIRCH_BUTTON;
        if (m.startsWith("IRON_"))
            return Material.STONE_BUTTON;
        if (m.startsWith("LEATHER_") && !plugin.getHiddenArmorConfig().ignoreLeatherArmor())
            return Material.ACACIA_BUTTON;
        if (m.startsWith("CHAINMAIL_"))
            return Material.JUNGLE_BUTTON;
        if (m.startsWith("TURTLE_") && !plugin.getHiddenArmorConfig().ignoreTurtleHelmet())
            return Material.CRIMSON_BUTTON;
        return null;
    }

    private Component getPieceDurability(ItemStack itemStack) {
        if (!(itemStack.getItemMeta() instanceof Damageable meta)) {
            return null;
        }

        int maxDurability = itemStack.getType().getMaxDurability();

        if (maxDurability == 0) {
            return null;
        }

        return Component.translatable()
                .key("item.durability")
                .args(
                        Component.text(maxDurability - meta.getDamage()),
                        Component.text(maxDurability)
                ).style(Style.style(NamedTextColor.WHITE, TextDecoration.ITALIC.withState(false)))
                .build();
    }
}
