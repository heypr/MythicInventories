package dev.heypr.mythicinventories.events;

import io.lumine.mythic.bukkit.MythicBukkit;
import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.inventories.MythicInventory;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.DragType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import static org.bukkit.event.inventory.InventoryAction.*;

public class InventoryEvents implements Listener {

    private final MythicInventories plugin;

    public InventoryEvents(MythicInventories plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MythicInventory || event.getClickedInventory() instanceof MythicInventory)) return;

        if (event.getCurrentItem() instanceof ItemStack item) {
            event.setCancelled(true);
            checkClickType(event, item);
            runCommands(item);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof MythicInventory)) return;

        if (event.getCursor() instanceof ItemStack item) {
            event.setCancelled(true);
            checkDragType(event, item);
            runCommands(item);
        }

        if (event.getOldCursor() instanceof ItemStack item) {
            event.setCancelled(true);
            checkDragType(event, item);
            runCommands(item);
        }
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
        MythicBukkit.inst().getAPIHelper().castSkill(event.getWhoClicked(), skillName);
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
        MythicBukkit.inst().getAPIHelper().castSkill(event.getWhoClicked(), skillName);
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
                if (action == InventoryAction.CLONE_STACK) {
                    castSkill(event, item);
                }
                break;
            case "SHIFT_MIDDLE_CLICK":
                if (event.isShiftClick() && action == InventoryAction.CLONE_STACK){
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
                if (action == InventoryAction.HOTBAR_SWAP) {
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

    private void runCommands(ItemStack item) {
        NamespacedKey key = new NamespacedKey(plugin, "commands");
        if (!item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            return;
        }

        String commands = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (commands == null) {
            return;
        }

        String[] commandList = commands.split(";");
        for (String command : commandList) {
            try {
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
            }
            catch (Exception e) {
                plugin.getLogger().warning("Error running command: " + command);
            }
        }
    }

    private boolean isInteractable(ItemStack item) {
        NamespacedKey key = new NamespacedKey(plugin, "interactable");
        if (!item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BOOLEAN)) {
            return false;
        }
        if (item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.BOOLEAN) == null) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.BOOLEAN);
    }
}
