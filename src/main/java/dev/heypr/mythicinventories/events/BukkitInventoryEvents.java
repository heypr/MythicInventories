package dev.heypr.mythicinventories.events;

import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.inventories.MythicInventory;
import dev.heypr.mythicinventories.inventories.MythicInventory.TrinketConfig;
import dev.heypr.mythicinventories.util.MIClickType;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.utils.jnbt.CompoundTag;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.InventoryInteractEvent;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

        final int slot = event.getRawSlot();
        final Player player = (Player) event.getWhoClicked();

        if (inventory.isTrinketSlot(slot)) {
            TrinketConfig slotConfig = inventory.getTrinketSlotConfig(slot);
            ItemStack cursorItem = event.getCursor();
            ItemStack clickedItem = event.getCurrentItem();

            if (cursorItem.getType() != Material.AIR && event.getClickedInventory().equals(inventory.getInventory())) {
                TrinketConfig itemConfig = getTrinketConfigFromItem(cursorItem);

                if (itemConfig != null) {
                    handleTrinketPlacement(event, inventory, itemConfig, player, slot);
                }
                else {
                    event.setCancelled(true);
                    player.sendMessage("This item cannot be used as a trinket!");
                }
                return;
            }

            else if (clickedItem != null && clickedItem.getType() != Material.AIR && event.getClickedInventory().equals(inventory.getInventory())) {
                if (inventory.getInteractableItems().containsKey(slot) && inventory.getInteractableItems().get(slot).isSimilar(clickedItem)) {
                    event.setCancelled(true);
                    checkClickType(event, inventory, slot);
                    return;
                }

                TrinketConfig itemConfig = getTrinketConfigFromItem(clickedItem);
                if (itemConfig != null) {
                    handleTrinketRemoval(event, inventory, slotConfig, player, slot);
                    return;
                }
            }
        }

        if (event.getCurrentItem() != null) {
            if (!hasInteractable(inventory) || !isInteractable(inventory, event.getRawSlot())) {
                event.setCancelled(true);
                checkClickType(event, inventory, event.getRawSlot());
            }
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

    private TrinketConfig getTrinketConfigFromItem(ItemStack item) {
        CompoundTag data = BukkitAdapter.adapt(item).getCustomData();

        if (data.getString("is_trinket") == null) {
            return null;
        }

        if (!data.getString("is_trinket").equals("true")) {
            return null;
        }

        String skill = data.getString("trinket_skill");
        String interval = data.getString("trinket_interval");
        String uses = data.getString("trinket_uses");

        if (skill == null || skill.isEmpty() || Integer.parseInt(interval) <= 0) {
            return null;
        }
        return new TrinketConfig(skill, interval, uses, false);
    }

    private void handleTrinketPlacement(InventoryClickEvent event, MythicInventory inventory, TrinketConfig itemConfig, Player player, int slot) {
        event.setCancelled(true);

        ItemStack trinketToPlace = event.getCursor().clone();

        if (event.getClickedInventory() == null) return;

        event.getClickedInventory().setItem(slot, trinketToPlace);
        event.setCursor(new ItemStack(Material.AIR));

        UUID key = UUID.nameUUIDFromBytes(itemConfig.skill().getBytes(StandardCharsets.UTF_8));
        plugin.addToTrinketSkillCache(key, itemConfig.skill());
        plugin.getTrinketScheduler().startTrinketSkillTask(player, slot, itemConfig, trinketToPlace, inventory);
    }

    private void handleTrinketRemoval(InventoryClickEvent event, MythicInventory inventory, TrinketConfig config, Player player, int slot) {
        event.setCancelled(true);
        if (event.getClickedInventory() == null) return;
        ItemStack trinketToRemove = event.getCurrentItem().clone();

        plugin.getTrinketScheduler().stopTrinketSkillTask(player, slot);

        ItemStack placeholder = inventory.getInteractableItems().get(slot);

        if (config.initialItemDisappears() && placeholder != null) {
            event.getClickedInventory().setItem(slot, placeholder);
        }
        else {
            event.getClickedInventory().setItem(slot, new ItemStack(Material.AIR));
        }

        HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(trinketToRemove);
        if (!remaining.isEmpty()) {
            player.getWorld().dropItem(player.getLocation(), trinketToRemove);
            player.sendMessage("Your inventory was full, the trinket was dropped on the ground!");
        }

        player.updateInventory();
    }

    private void castSkill(InventoryInteractEvent event, String inputSkill) {
        if (!plugin.isMythicMobsEnabled()) {
            plugin.getLogger().warning("MythicMobs was not found! Cannot cast skill: " + inputSkill);
            return;
        }
        Player player = (Player) event.getWhoClicked();
        plugin.executeMythicSkill(player, inputSkill);
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
        HashMap<MIClickType, List<String>> clickTypesMap = inventory.getClickTypes(slot);
        if (clickTypesMap == null) return;

        Set<MIClickType> clickTypes = clickTypesMap.keySet();
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
}
