package dev.heypr.mythicinventories.listeners;

import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.inventory.MythicInventory;
import dev.heypr.mythicinventories.inventory.MythicInventory.TrinketConfig;
import dev.heypr.mythicinventories.trinket.AttributeManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.bukkit.event.inventory.InventoryAction.*;

public class BukkitInventoryEvents implements Listener {

    private final MythicInventories plugin;
    private final AttributeManager attributeManager;

    public BukkitInventoryEvents(MythicInventories plugin) {
        this.plugin = plugin;
        this.attributeManager = plugin.getAttributeManager();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        if (!(event.getClickedInventory().getHolder() instanceof MythicInventory inventory)) return;

        final int slot = event.getRawSlot();
        final Player player = (Player) event.getWhoClicked();
        ItemStack cursorItem = event.getCursor();
        ItemStack clickedItem = event.getCurrentItem();

        plugin.getLogger().info("[Debug] InventoryClickEvent: Action=" + event.getAction() + ", Slot=" + slot + ", Player=" + player.getName());

        if (event.getAction() == MOVE_TO_OTHER_INVENTORY) {
            if (event.getClickedInventory().equals(player.getInventory())) {
                ItemStack itemToMove = event.getCurrentItem();
                if (itemToMove == null || itemToMove.getType().isAir()) return;

                plugin.getLogger().info("[Debug] Shift-click detected from player inventory.");
                TrinketConfig itemConfig = plugin.getTrinketManager().getTrinketConfigFromItem(itemToMove);

                if (itemConfig != null) {
                    int targetSlot = -1;

                    for (int i = 0; i < inventory.getInventory().getSize(); i++) {
                        if (inventory.isTrinketSlot(i)) {
                            ItemStack existingItem = inventory.getInventory().getItem(i);
                            if (existingItem == null || existingItem.getType().isAir() || (inventory.getInteractableItems().containsKey(i) && inventory.getInteractableItems().get(i).isSimilar(existingItem))) {
                                targetSlot = i;
                                break;
                            }
                        }
                    }

                    event.setCancelled(true);
                    if (targetSlot != -1) {
                        plugin.getLogger().info("[Debug] Shift-click: Found target trinket slot " + targetSlot);
                        inventory.getInventory().setItem(targetSlot, itemToMove.clone());
                        event.setCurrentItem(new ItemStack(Material.AIR));

                        handleTrinketPlacement(event, inventory, itemConfig, player, targetSlot);
                    } else {
                        plugin.getLogger().info("[Debug] Shift-click: No empty trinket slots available.");
                    }
                }
                else {
                    plugin.getLogger().info("[Debug] Shift-click: Item is not a valid trinket.");
                    event.setCancelled(true);
                    player.sendMessage("This item cannot be used as a trinket!");
                }
                return;
            }
            else if (event.getClickedInventory().equals(inventory.getInventory())) {
                if (!inventory.isTrinketSlot(slot) && !isInteractable(inventory, slot)) {
                    plugin.getLogger().info("[Debug] Shift-click: Cancelled (Not a trinket or interactable slot).");
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (inventory.isTrinketSlot(slot)) {
            plugin.getLogger().info("[Debug] Interacting with Trinket Slot: " + slot);
            if (cursorItem.getType() != Material.AIR && event.getClickedInventory().equals(inventory.getInventory())) {
                TrinketConfig itemConfig = plugin.getTrinketManager().getTrinketConfigFromItem(cursorItem);

                if (itemConfig != null) {
                    plugin.getLogger().info("[Debug] Placing trinket from cursor into slot " + slot);
                    handleTrinketPlacement(event, inventory, itemConfig, player, slot);
                }
                else {
                    plugin.getLogger().info("[Debug] Denied: Item on cursor is not a valid trinket.");
                    event.setCancelled(true);
                    player.sendMessage("This item cannot be used as a trinket!");
                }
                return;
            }
            else if (clickedItem != null && clickedItem.getType() != Material.AIR && event.getClickedInventory().equals(inventory.getInventory())) {
                if (inventory.getInteractableItems().containsKey(slot) && inventory.getInteractableItems().get(slot).isSimilar(clickedItem)) {
                    plugin.getLogger().info("[Debug] Interacting with placeholder item in slot " + slot);
                    event.setCancelled(true);
                    checkClickType(event, inventory, slot);
                    return;
                }

                TrinketConfig itemConfig = plugin.getTrinketManager().getTrinketConfigFromItem(clickedItem);
                if (itemConfig != null) {
                    plugin.getLogger().info("[Debug] Removing trinket from slot " + slot);
                    handleTrinketRemoval(event, inventory, itemConfig, player, slot);
                    return;
                }
            }
        }

        if (event.getCurrentItem() != null) {
            if (!hasInteractable(inventory) || !isInteractable(inventory, event.getRawSlot())) {
                plugin.getLogger().info("[Debug] Non-trinket slot interaction. Cancelling and checking skills.");
                event.setCancelled(true);
                checkClickType(event, inventory, event.getRawSlot());
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        plugin.getLogger().info("[Debug] InventoryDragEvent detected.");
        if (!(event.getInventory().getHolder() instanceof MythicInventory inventory)) return;
        if (event.getCursor() == null) return;

        for (int slot : event.getRawSlots()) {
            if (isInteractable(inventory, slot)) {
                plugin.getLogger().info("[Debug] Drag in interactable slot " + slot + " allowed.");
                continue;
            }
            plugin.getLogger().info("[Debug] Drag in restricted slot " + slot + " cancelled.");
            event.setCancelled(true);
            checkDragType(event, inventory, slot);
        }
    }

    @EventHandler
    private void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof MythicInventory inventory)) return;
        plugin.getLogger().info("[Debug] Inventory closed by " + event.getPlayer().getName() + ". Saving inventory.");
        plugin.getInventorySerializer().saveInventory(inventory, (Player) event.getPlayer());
    }

    private void handleTrinketPlacement(InventoryClickEvent event, MythicInventory inventory, TrinketConfig config, Player player, int slot) {
        plugin.getLogger().info("[Debug] handleTrinketPlacement: Slot=" + slot + ", Skill=" + config.skill());
        ItemStack trinketToPlace = event.getAction() == MOVE_TO_OTHER_INVENTORY ? inventory.getInventory().getItem(slot) : event.getCursor().clone();

        if (event.getAction() != MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
        }

        if (event.getClickedInventory() == null) return;

        if (event.getAction() != MOVE_TO_OTHER_INVENTORY) {
            event.getClickedInventory().setItem(slot, trinketToPlace);
            event.setCursor(new ItemStack(Material.AIR));
        }

        if (trinketToPlace == null || trinketToPlace.getType().isAir()) {
            plugin.getLogger().warning("[Debug] Placement aborted: trinketToPlace is null or air.");
            return;
        }

        UUID configKey = UUID.nameUUIDFromBytes(config.skill().getBytes(StandardCharsets.UTF_8));
        ItemMeta meta = trinketToPlace.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(plugin.getTrinketCacheKey(), PersistentDataType.STRING, configKey.toString());
            trinketToPlace.setItemMeta(meta);
            plugin.getLogger().info("[Debug] Set PDC key on trinket: " + configKey);
        }

        inventory.getInventory().setItem(slot, trinketToPlace);

        plugin.getLogger().info("[Debug] Requesting Attribute Application for slot " + slot);
        attributeManager.applyAttributes(player, slot, trinketToPlace);

        if (!config.skill().equals("NO_SKILL")) {
            plugin.getLogger().info("[Debug] Skill recognized. Caching and starting tasks.");
            plugin.getCacheManager().cacheTrinketSkill(configKey, config.skill());
            plugin.getTrinketScheduler().startTrinketSkillTask(player, slot, config, inventory);
            plugin.getTrinketScheduler().startTrinketAttributeTask(player, slot, config, inventory);
        }
    }

    private void handleTrinketRemoval(InventoryClickEvent event, MythicInventory inventory, TrinketConfig config, Player player, int slot) {
        plugin.getLogger().info("[Debug] handleTrinketRemoval: Slot=" + slot);
        event.setCancelled(true);
        if (event.getClickedInventory() == null) return;
        ItemStack trinketToRemove = event.getCurrentItem();

        plugin.getLogger().info("[Debug] Stopping tasks and removing attributes for slot " + slot);
        plugin.getTrinketScheduler().stopTrinketSkillTask(player, slot);
        plugin.getTrinketScheduler().stopTrinketAttributeTask(player, slot);
        attributeManager.removeAttributes(player, slot);

        ItemMeta meta = trinketToRemove.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().remove(plugin.getTrinketCacheKey());
            trinketToRemove.setItemMeta(meta);
            plugin.getLogger().info("[Debug] Removed PDC key from trinket.");
        }

        ItemStack placeholder = inventory.getInteractableItems().get(slot);

        if (config.initialItemDisappears() && placeholder != null) {
            plugin.getLogger().info("[Debug] Replacing trinket with placeholder item.");
            event.getClickedInventory().setItem(slot, placeholder);
        }
        else {
            event.getClickedInventory().setItem(slot, new ItemStack(Material.AIR));
        }

        if (event.isShiftClick() || event.getAction() == MOVE_TO_OTHER_INVENTORY) {
            plugin.getLogger().info("[Debug] Shift-click removal: Returning to inventory.");
            HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(trinketToRemove);
            if (!remaining.isEmpty()) {
                player.getWorld().dropItem(player.getLocation(), trinketToRemove);
                player.sendMessage("Your inventory was full, the trinket was dropped on the ground!");
                plugin.getLogger().info("[Debug] Inventory full: Dropped trinket on ground.");
            }
        }
        else {
            plugin.getLogger().info("[Debug] Manual removal: Placing trinket on cursor.");
            event.setCursor(trinketToRemove);
        }

        player.updateInventory();
    }

    private void castSkill(InventoryInteractEvent event, String inputSkill) {
        if (!plugin.getMythicManager().isMythicMobsEnabled()) {
            plugin.getLogger().warning("MythicMobs was not found! Cannot cast skill: " + inputSkill);
            return;
        }
        Player player = (Player) event.getWhoClicked();
        plugin.getLogger().info("[Debug] Executing Mythic Skill: " + inputSkill + " for " + player.getName());
        plugin.getMythicManager().executeMythicSkill(player, inputSkill);
    }

    private void checkClickType(InventoryClickEvent event, MythicInventory inventory, int slot) {
        HashMap<ClickType, List<String>> clickTypes = inventory.getClickTypes(slot);
        if (clickTypes == null) return;

        plugin.getLogger().info("[Debug] Checking click skills for slot " + slot + ". Found " + clickTypes.size() + " types.");

        Set<ClickType> clickTypesSet = clickTypes.keySet();
        for (ClickType clickType : clickTypesSet) {
            List<String> skills = inventory.getClickSkills(slot, clickType);
            if (skills == null) continue;

            for (String skill : skills) {
                performTypeChecks(clickType, event, skill, event.getAction());
            }
        }
    }

    private void performTypeChecks(ClickType clickType, InventoryClickEvent event, String skill, InventoryAction action) {
        boolean isDrop = (action == DROP_ALL_SLOT || action == DROP_ONE_SLOT || action == DROP_ALL_CURSOR || action == DROP_ONE_CURSOR);
        boolean isMiddleClick = (action == CLONE_STACK);
        boolean isHotbarSwap = (action == HOTBAR_SWAP);
        switch (clickType) {
            case LEFT:
                if (event.isLeftClick() && !event.isShiftClick()) {
                    castSkill(event, skill);
                }
                break;
            case RIGHT:
                if (event.isRightClick() && !event.isShiftClick()) {
                    castSkill(event, skill);
                }
                break;
            case SHIFT_LEFT:
                if (event.isShiftClick() && event.isLeftClick()) {
                    castSkill(event, skill);
                }
                break;
            case SHIFT_RIGHT:
                if (event.isShiftClick() && event.isRightClick()) {
                    castSkill(event, skill);
                }
                break;
            case MIDDLE:
                if (isMiddleClick) {
                    if (event.isShiftClick()) {
                        castSkill(event, skill);
                        break;
                    }
                    castSkill(event, skill);
                }
                break;
            case DROP:
                if (isDrop) {
                    castSkill(event, skill);
                }
                break;
            case NUMBER_KEY:
                if (isHotbarSwap) {
                    castSkill(event, skill);
                }
                break;
        }
    }

    private void checkDragType(InventoryDragEvent event, MythicInventory inventory, int slot) {
        HashMap<ClickType, List<String>> clickTypesMap = inventory.getClickTypes(slot);
        if (clickTypesMap == null) return;

        Set<ClickType> clickTypes = clickTypesMap.keySet();
        for (ClickType clickType : clickTypes) {
            List<String> skills = inventory.getClickSkills(slot, clickType);
            if (skills == null) continue;
            for (String skill : skills) {
                switch (clickType) {
                    case LEFT:
                        if (event.getType().equals(DragType.SINGLE)) {
                            castSkill(event, skill);
                        }
                        break;
                    case RIGHT:
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
}
