package me.kteq.hiddenarmor.listener;

import me.kteq.hiddenarmor.HiddenArmor;
import me.kteq.hiddenarmor.handler.ArmorUpdater;
import me.kteq.hiddenarmor.manager.HiddenArmorManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class InventoryShiftClickListener implements Listener {

    private final HiddenArmor plugin;
    private final HiddenArmorManager hiddenArmorManager;

    public InventoryShiftClickListener(HiddenArmor plugin) {
        this.plugin = plugin;
        this.hiddenArmorManager = plugin.getHiddenArmorManager();
        plugin.registerListener(this);
    }

    @EventHandler
    public void onShiftClickArmor(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) ||
                !hiddenArmorManager.isArmorHidden(player) ||
                !(event.getClickedInventory() instanceof PlayerInventory) ||
                !event.isShiftClick()
        ) {
            return;
        }

        var inv = player.getInventory();
        ItemStack armor = event.getCurrentItem();

        if (armor == null) return;

        if ((armor.getType().toString().endsWith("_HELMET") && inv.getHelmet() == null) ||
                ((armor.getType().toString().endsWith("_CHESTPLATE") || armor.getType().equals(Material.ELYTRA)) && inv.getChestplate() == null) ||
                (armor.getType().toString().endsWith("_LEGGINGS") && inv.getLeggings() == null) ||
                (armor.getType().toString().endsWith("_BOOTS") && inv.getBoots() == null)) {
            plugin.runPlayerTask(player, ArmorUpdater::updateSelf);
        }
    }
}
