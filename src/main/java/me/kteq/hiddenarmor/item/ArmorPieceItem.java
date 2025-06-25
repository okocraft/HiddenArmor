package me.kteq.hiddenarmor.item;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.bukkit.inventory.ItemType.ACACIA_BUTTON;
import static org.bukkit.inventory.ItemType.BIRCH_BUTTON;
import static org.bukkit.inventory.ItemType.CHAINMAIL_BOOTS;
import static org.bukkit.inventory.ItemType.CHAINMAIL_CHESTPLATE;
import static org.bukkit.inventory.ItemType.CHAINMAIL_HELMET;
import static org.bukkit.inventory.ItemType.CHAINMAIL_LEGGINGS;
import static org.bukkit.inventory.ItemType.CRIMSON_BUTTON;
import static org.bukkit.inventory.ItemType.DIAMOND_BOOTS;
import static org.bukkit.inventory.ItemType.DIAMOND_CHESTPLATE;
import static org.bukkit.inventory.ItemType.DIAMOND_HELMET;
import static org.bukkit.inventory.ItemType.DIAMOND_LEGGINGS;
import static org.bukkit.inventory.ItemType.GOLDEN_BOOTS;
import static org.bukkit.inventory.ItemType.GOLDEN_CHESTPLATE;
import static org.bukkit.inventory.ItemType.GOLDEN_HELMET;
import static org.bukkit.inventory.ItemType.GOLDEN_LEGGINGS;
import static org.bukkit.inventory.ItemType.IRON_BOOTS;
import static org.bukkit.inventory.ItemType.IRON_CHESTPLATE;
import static org.bukkit.inventory.ItemType.IRON_HELMET;
import static org.bukkit.inventory.ItemType.IRON_LEGGINGS;
import static org.bukkit.inventory.ItemType.JUNGLE_BUTTON;
import static org.bukkit.inventory.ItemType.LEATHER_BOOTS;
import static org.bukkit.inventory.ItemType.LEATHER_CHESTPLATE;
import static org.bukkit.inventory.ItemType.LEATHER_HELMET;
import static org.bukkit.inventory.ItemType.LEATHER_LEGGINGS;
import static org.bukkit.inventory.ItemType.NETHERITE_BOOTS;
import static org.bukkit.inventory.ItemType.NETHERITE_CHESTPLATE;
import static org.bukkit.inventory.ItemType.NETHERITE_HELMET;
import static org.bukkit.inventory.ItemType.NETHERITE_LEGGINGS;
import static org.bukkit.inventory.ItemType.POLISHED_BLACKSTONE_BUTTON;
import static org.bukkit.inventory.ItemType.STONE_BUTTON;
import static org.bukkit.inventory.ItemType.TURTLE_HELMET;
import static org.bukkit.inventory.ItemType.WARPED_BUTTON;

public final class ArmorPieceItem {

    private static final Map<ItemType, ItemType> ARMOR_TO_BUTTON_TYPE_MAP;

    static {
        Map<ItemType, ItemType> building = new HashMap<>();

        Stream.of(NETHERITE_HELMET, NETHERITE_CHESTPLATE, NETHERITE_LEGGINGS, NETHERITE_BOOTS).forEach(type -> building.put(type, POLISHED_BLACKSTONE_BUTTON));
        Stream.of(DIAMOND_HELMET, DIAMOND_CHESTPLATE, DIAMOND_LEGGINGS, DIAMOND_BOOTS).forEach(type -> building.put(type, WARPED_BUTTON));
        Stream.of(GOLDEN_HELMET, GOLDEN_CHESTPLATE, GOLDEN_LEGGINGS, GOLDEN_BOOTS).forEach(type -> building.put(type, BIRCH_BUTTON));
        Stream.of(IRON_HELMET, IRON_CHESTPLATE, IRON_LEGGINGS, IRON_BOOTS).forEach(type -> building.put(type, STONE_BUTTON));
        Stream.of(CHAINMAIL_HELMET, CHAINMAIL_CHESTPLATE, CHAINMAIL_LEGGINGS, CHAINMAIL_BOOTS).forEach(type -> building.put(type, ACACIA_BUTTON));
        Stream.of(LEATHER_HELMET, LEATHER_CHESTPLATE, LEATHER_LEGGINGS, LEATHER_BOOTS).forEach(type -> building.put(type, JUNGLE_BUTTON));
        Stream.of(TURTLE_HELMET).forEach(type -> building.put(type, CRIMSON_BUTTON));

        ARMOR_TO_BUTTON_TYPE_MAP = Collections.unmodifiableMap(building);
    }

    public static ItemStack create(ItemStack item) {
        if (item.isEmpty() || item.getType() == Material.ELYTRA) {
            return item;
        }

        ItemType buttonType = ARMOR_TO_BUTTON_TYPE_MAP.get(item.getType().asItemType());
        if (buttonType == null) {
            return item;
        }

        ItemStack button = buttonType.createItemStack();

        int maxDamage = item.getDataOrDefault(DataComponentTypes.MAX_DAMAGE, 0);
        if (maxDamage != 0) {
            button.setData(DataComponentTypes.MAX_DAMAGE, maxDamage);

            int damage = item.getDataOrDefault(DataComponentTypes.DAMAGE, 0);
            button.setData(DataComponentTypes.DAMAGE, damage);
        }

        Component displayName = item.getData(DataComponentTypes.CUSTOM_NAME);
        ItemRarity rarity = item.getDataOrDefault(DataComponentTypes.RARITY, ItemRarity.COMMON);
        button.setData(
                DataComponentTypes.CUSTOM_NAME,
                Objects.requireNonNullElseGet(
                        displayName,
                        () -> Component.translatable()
                                .key(item.getType())
                                .style(Style.style(rarity.color(), TextDecoration.ITALIC.withState(false)))
                                .build()
                )
        );

        ItemEnchantments enchantments = item.getData(DataComponentTypes.ENCHANTMENTS);
        if (enchantments != null && !enchantments.enchantments().isEmpty()) {
            button.setData(DataComponentTypes.ENCHANTMENTS, enchantments);
        }

        return button;
    }


    private ArmorPieceItem() {
        throw new UnsupportedOperationException();
    }
}
