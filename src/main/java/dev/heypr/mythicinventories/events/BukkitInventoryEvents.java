package dev.heypr.mythicinventories.events;

import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.inventories.MythicInventory;
import dev.heypr.mythicinventories.misc.MIClickType;
import io.lumine.mythic.api.mobs.GenericCaster;
import io.lumine.mythic.api.skills.Skill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.skills.SkillTriggers;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;

import java.util.Collections;
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

    private void castSkill(InventoryInteractEvent event, String inputSkill) {
        if (plugin.isMythicMobsEnabled()) {
            Skill skill = plugin.getMythicInst().getSkillManager().getSkill(null, Collections.singleton(inputSkill)).get();
            SkillMetadata meta = plugin.getMythicInst().getSkillManager().getEventBus().buildSkillMetadata(SkillTriggers.API, new GenericCaster(BukkitAdapter.adapt(event.getWhoClicked())), BukkitAdapter.adapt(event.getWhoClicked()), BukkitAdapter.adapt(event.getWhoClicked().getLocation()), true);
            if (skill.isUsable(meta)) skill.execute(meta);
        }
        else {
            plugin.getLogger().warning("MythicMobs was not found! Cannot cast skill: " + inputSkill);
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
}
