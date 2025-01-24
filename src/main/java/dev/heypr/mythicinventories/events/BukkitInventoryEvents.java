package dev.heypr.mythicinventories.events;

import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.inventories.MythicInventory;
import dev.heypr.mythicinventories.misc.MIClickType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

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
            checkClickType(event, inventory, event.getRawSlot());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof MythicInventory inventory)) return;
        if (event.getCursor() == null) return;

        for (int slot : event.getRawSlots()) {
            if (isInteractable(inventory, slot)) continue;
            event.setCancelled(true);
            checkDragType(event, inventory, slot);
            }
        }

    @EventHandler
    private void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof MythicInventory inventory)) return;

        plugin.getInventorySerializer().saveInventory(inventory, (Player) event.getPlayer());
    }

    private void castSkill(InventoryInteractEvent event, String skill) {
        if (plugin.isMythicMobsEnabled()) {
            plugin.getMythicInst().getAPIHelper().castSkill(event.getWhoClicked(), skill);
        }
        else {
            plugin.getLogger().warning("MythicMobs is not enabled! Cannot cast skill: " + skill);
        }
    }

    private void checkClickType(InventoryClickEvent event, MythicInventory inventory, int slot) {
        HashMap<MIClickType, List<String>> clickTypes = inventory.getClickTypes(slot);
        if (clickTypes == null) return;
        Set<MIClickType> clickTypesSet = clickTypes.keySet();
        for (MIClickType clickType : clickTypesSet) {
            List<String> skills = inventory.getClickSkills(slot, clickType);
            if (skills == null) continue;
            for (String skill : skills) {
                performTypeChecks(clickType.name(), event, skill, event.getAction());
            }
        }
    }

    private void performTypeChecks(String clickType, InventoryClickEvent event, String skill, InventoryAction action) {
        boolean isDrop = (action == DROP_ALL_SLOT || action == DROP_ONE_SLOT || action == DROP_ALL_CURSOR || action == DROP_ONE_CURSOR);
        boolean isMiddleClick = (action == CLONE_STACK);
        boolean isHotbarSwap = (action == HOTBAR_SWAP);
        switch (clickType) {
            case "LEFT_CLICK":
                if (event.isLeftClick() && !event.isShiftClick()) {
                    castSkill(event, skill);
                }
                break;
            case "RIGHT_CLICK":
                if (event.isRightClick() && !event.isShiftClick()) {
                    castSkill(event, skill);
                }
                break;
            case "SHIFT_LEFT_CLICK":
                if (event.isShiftClick() && event.isLeftClick()) {
                    castSkill(event, skill);
                }
                break;
            case "SHIFT_RIGHT_CLICK":
                if (event.isShiftClick() && event.isRightClick()) {
                    castSkill(event, skill);
                }
                break;
            case "MIDDLE_CLICK":
                if (isMiddleClick) {
                    castSkill(event, skill);
                }
                break;
            case "SHIFT_MIDDLE_CLICK":
                if (event.isShiftClick() && isMiddleClick) {
                    castSkill(event, skill);
                }
                break;
            case "DROP":
                if (isDrop) {
                    castSkill(event, skill);
                }
                break;
            case "SHIFT_DROP":
                if (event.isShiftClick() && isDrop) {
                    castSkill(event, skill);
                }
                break;
            case "HOTBAR_SWAP":
                if (isHotbarSwap) {
                    castSkill(event, skill);
                }
                break;
        }
    }

    private void checkDragType(InventoryDragEvent event, MythicInventory inventory, int slot) {
        Set<MIClickType> clickTypes = inventory.getClickTypes(slot).keySet();
        for (MIClickType clickType : clickTypes) {
            List<String> skills = inventory.getClickSkills(slot, clickType);
            if (skills == null) continue;
            for (String skill : skills) {
                switch (clickType.name()) {
                    case "LEFT_CLICK":
                        if (event.getType().equals(DragType.SINGLE)) {
                            castSkill(event, skill);
                        }
                        break;
                    case "RIGHT_CLICK":
                        if (event.getType().equals(DragType.EVEN)) {
                            castSkill(event, skill);
                        }
                        break;
                }
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
