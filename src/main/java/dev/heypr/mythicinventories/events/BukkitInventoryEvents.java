package dev.heypr.mythicinventories.events;

import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.inventories.MythicInventory;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import static org.bukkit.event.inventory.InventoryAction.*;

public class BukkitInventoryEvents implements Listener {

    private final MythicInventories plugin;

    public BukkitInventoryEvents(MythicInventories plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        if (!(event.getClickedInventory().getHolder() instanceof MythicInventory inventory)) return;
        if (event.getCurrentItem() == null) return;

        if (!hasInteractable(inventory) || !isInteractable(inventory, event.getRawSlot())) {
            event.setCancelled(true);
            checkClickType(event, event.getCurrentItem());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof MythicInventory inventory)) return;
        if (event.getCursor() == null) return;

        for (int slot : event.getRawSlots()) {
            if (isInteractable(inventory, slot)) continue;
            event.setCancelled(true);
            checkDragType(event, event.getCursor());
//          runCommands(item);
            }
        }

    @EventHandler
    private void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof MythicInventory inventory)) return;

        plugin.getInventorySerializer().saveInventory(inventory, (Player) event.getPlayer());
    }

    private void castSkill(InventoryDragEvent event, ItemStack item) {
        NamespacedKey key = new NamespacedKey(plugin, "skill");
        if (!item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            return;
        }
        String skillName = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (skillName == null) {
            return;
        }
        if (plugin.isMythicMobsEnabled()) {
            plugin.getMythicInst().getAPIHelper().castSkill(event.getWhoClicked(), skillName);
        }
        else {
            plugin.getLogger().warning("MythicMobs is not enabled! Cannot cast skill: " + skillName);
        }
    }

    private void castSkill(InventoryClickEvent event, ItemStack item) {
        NamespacedKey key = new NamespacedKey(plugin, "skill");
        if (!item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            return;
        }
        String skillName = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (skillName == null) {
            return;
        }
        if (plugin.isMythicMobsEnabled()) {
            plugin.getMythicInst().getAPIHelper().castSkill(event.getWhoClicked(), skillName);
        }
        else {
            plugin.getLogger().warning("MythicMobs is not enabled! Cannot cast skill: " + skillName);
        }
    }

    private void checkClickType(InventoryClickEvent event, ItemStack item) {
        NamespacedKey key = new NamespacedKey(plugin, "click_type");
        if (!item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            return;
        }
        String clickType = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        InventoryAction action = event.getAction();
        if (clickType == null) {
            castSkill(event, item);
            return;
        }
        boolean isDrop = (action == DROP_ALL_SLOT || action == DROP_ONE_SLOT || action == DROP_ALL_CURSOR || action == DROP_ONE_CURSOR);
        boolean isMiddleClick = (action == CLONE_STACK);
        boolean isHotbarSwap = (action == HOTBAR_SWAP);
        switch (clickType) {
            case "LEFT_CLICK":
                if (event.isLeftClick() && !event.isShiftClick()) {
                    castSkill(event, item);
                }
                break;
            case "RIGHT_CLICK":
                if (event.isRightClick() && !event.isShiftClick()) {
                    castSkill(event, item);
                }
                break;
            case "SHIFT_LEFT_CLICK":
                if (event.isShiftClick() && event.isLeftClick()) {
                    castSkill(event, item);
                }
                break;
            case "SHIFT_RIGHT_CLICK":
                if (event.isShiftClick() && event.isRightClick()) {
                    castSkill(event, item);
                }
                break;
            case "MIDDLE_CLICK":
                if (isMiddleClick) {
                    castSkill(event, item);
                }
                break;
            case "SHIFT_MIDDLE_CLICK":
                if (event.isShiftClick() && isMiddleClick) {
                    castSkill(event, item);
                }
                break;
            case "DROP":
                if (isDrop) {
                    castSkill(event, item);
                }
                break;
            case "SHIFT_DROP":
                if (event.isShiftClick() && isDrop) {
                    castSkill(event, item);
                }
                break;
            case "HOTBAR_SWAP":
                if (isHotbarSwap) {
                    castSkill(event, item);
                }
                break;
        }
    }

    private void checkDragType(InventoryDragEvent event, ItemStack item) {
        NamespacedKey key = new NamespacedKey(plugin, "click_type");
        if (item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            String clickType = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
            if (clickType == null) {
                castSkill(event, item);
                return;
            }
            switch (clickType) {
                case "LEFT_CLICK":
                    if (event.getType().equals(DragType.SINGLE)) {
                        castSkill(event, item);
                    }
                    break;
                case "RIGHT_CLICK":
                    if (event.getType().equals(DragType.EVEN)) {
                        castSkill(event, item);
                    }
                    break;
            }
        }
    }

    private boolean isInteractable(MythicInventory inventory, int slot) {
        return inventory.getInteractableItems().containsKey(slot);
    }

    private boolean hasInteractable(MythicInventory inventory) {
        return !inventory.getInteractableItems().isEmpty();
    }

// TODO: fix this shit
//    private void runCommands(ItemStack item) {
//        NamespacedKey key = new NamespacedKey(plugin, "commands");
//        if (!item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
//            return;
//        }
//
//        String commands = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
//
//        if (commands == null) {
//            return;
//        }
//
//        String[] commandList = commands.split(";");
//        for (String command : commandList) {
//            try {
//                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
//            }
//            catch (Exception e) {
//                plugin.getLogger().warning("Error running command: " + command);
//            }
//        }
//    }
}
