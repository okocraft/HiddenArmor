package me.kteq.hiddenarmor.listener;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import me.kteq.hiddenarmor.HiddenArmor;
import me.kteq.hiddenarmor.handler.ArmorUpdater;
import me.kteq.hiddenarmor.manager.HiddenArmorManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onShiftClickArmor(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) ||
            !hiddenArmorManager.isArmorHidden(player) ||
            !(event.getClickedInventory() instanceof PlayerInventory) ||
            !event.isShiftClick()
        ) {
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null || item.isEmpty()) {
            return;
        }

        Equippable equippable = item.getData(DataComponentTypes.EQUIPPABLE);
        if (equippable == null) {
            return;
        }

        if (player.getInventory().getItem(equippable.slot()).isEmpty()) {
            this.plugin.runPlayerTask(player, ArmorUpdater::updateSelf);
        }
    }
}
