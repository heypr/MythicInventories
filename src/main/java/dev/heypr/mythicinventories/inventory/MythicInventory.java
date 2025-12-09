package dev.heypr.mythicinventories.inventory;

import dev.heypr.mythicinventories.MythicInventories;
import net.kyori.adventure.text.Component;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MythicInventory implements InventoryHolder {

    private final HashMap<Integer, HashMap<ClickType, List<String>>> clickSkills = new HashMap<>();
    private final HashMap<Integer, ItemStack> interactableItems = new HashMap<>();
    private final Set<Integer> trinketSlots = new HashSet<>();
    private final Set<Integer> savedItems = new HashSet<>();
    private final Inventory inventory;
    private String internalName;

    /**
     * Configuration data for a specialized trinket slot.
     * @param skill The MythicMobs skill name to execute.
     * @param interval The execution interval in ticks, due to inconsistencies on mythic's side of things, this is handled incorrectly when called via the getInt method.
     * @param uses The maximum number of skill uses (-1 for infinite).
     * @param initialItemDisappears True if the configured placeholder item should be removed when a trinket is placed.
     */
    public record TrinketConfig(String skill, String interval, String uses, boolean initialItemDisappears) { }

    /**
     * Constructor for creating a new inventory.
     *
     * @param plugin Instance of the MythicInventories plugin.
     * @param size   The size of the inventory.
     * @param title  The title of the inventory.
     */
    public MythicInventory(MythicInventories plugin, int size, Component title) {
        this.inventory = plugin.getServer().createInventory(this, size, title);
    }

    /**
     * Constructor for getting an existing inventory.
     * <p></p>
     * Do not use this constructor for creating a new inventory.
     *
     * @param plugin       Instance of the MythicInventories plugin.
     * @param internalName The internal name of the inventory.
     */
    public MythicInventory(MythicInventories plugin, String internalName) {
        this.inventory = plugin.getInventoryManager().getInventories().get(internalName).getInventory();
    }

    /**
     * Checks if a specific slot is configured as a trinket slot.
     *
     * @param slot The slot index to check.
     * @return True if the slot is a trinket slot.
     */
    public boolean isTrinketSlot(int slot) {
        return trinketSlots.contains(slot);
    }

    /**
     * Registers a slot as a trinket slot with its specific configuration.
     *
     * @param slot The slot index.
     */
    public void addTrinketSlot(int slot) {
        trinketSlots.add(slot);
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    /**
     * Get the internal name of the inventory.
     *
     * @return The internal name of the inventory.
     */
    public String getInternalName() {
        return internalName;
    }

    /**
     * Set the internal name of the inventory.
     *
     * @param internalName The internal name of the inventory.
     */
    public void setInternalName(String internalName) {
        this.internalName = internalName;
    }

    /**
     * Get the item in the specified slot.
     *
     * @param slot The slot to get the item from.
     * @param item The item in the specified slot.
     */
    public void setItem(int slot, ItemStack item) {
        this.inventory.setItem(slot, item);
    }

    /**
     * Gets all the unsaved items in the inventory.
     * <p>
     * Map contains the item as the key and the slot as the value.
     *
     * @return A map of all the unsaved items in the inventory.
     */
    public Set<Integer> getSavedItems() {
        return savedItems;
    }

    /**
     * Add an unsaved item to the inventory.
     *
     * @param slot The slot to add the item to.
     */
    public void addSavedItem(int slot) {
        savedItems.add(slot);
    }

    /**
     * Remove an unsaved item from the inventory.
     *
     * @param slot The slot to remove the item from.
     */
    public void removeSavedItem(int slot) {
        savedItems.remove(slot);
    }

    /**
     * Gets all the interactable items in the inventory.
     * <p>
     * Map contains the item as the key and the slot as the value.
     *
     * @return A map of all the interactable items in the inventory.
     */
    public HashMap<Integer, ItemStack> getInteractableItems() {
        return interactableItems;
    }

    /**
     * Add an interactable item to the inventory.
     *
     * @param item The item to add.
     * @param slot The slot to add the item to.
     */
    public void addInteractableItem(int slot, ItemStack item) {
        interactableItems.put(slot, item);
    }

    /**
     * Remove an interactable item from the inventory.
     *
     * @param item The item to remove.
     */
    public void removeInteractableItem(ItemStack item) {
        interactableItems.entrySet().removeIf(entry -> entry.getValue().equals(item));
    }

    /**
     * Remove an interactable item from the inventory.
     *
     * @param slot The slot to remove the item from.
     */
    public void removeInteractableItem(int slot) {
        interactableItems.remove(slot);
    }

    /**
     * Get a click skill from the inventory.
     *
     * @param slot The slot to get the click skill(s) for.
     * @return The click skill for the inventory.
     */
    @Nullable
    public HashMap<ClickType, List<String>> getClickTypes(int slot) {
        return clickSkills.get(slot);
    }

    /**
     * Get a click skill from the inventory.
     *
     * @param slot The slot to get the click skill(s) for.
     * @param clickType The click type to get the skill for.
     * @return The click skill for the inventory.
     */
    @Nullable
    public List<String> getClickSkills(int slot, ClickType clickType) {
        return clickSkills.get(slot).get(clickType);
    }

    /**
     * Add multiple click skills to the inventory.
     *
     * @param slot   The slot to add the skills to.
     * @param skills The skills to add to the click type.
     */
    public void addClickSkills(int slot, HashMap<ClickType, List<String>> skills) {
        clickSkills.put(slot, skills);
    }

    /**
     * Add a click requirement to the inventory.
     *
     * @param slot      The slot to listen for.
     * @param clickType The click type to listen to.
     */
    public void addClick(int slot, ClickType clickType) {
        HashMap<ClickType, List<String>> empty = new HashMap<>();
        empty.put(clickType, null);
        clickSkills.put(slot, empty);
    }

    /**
     * Add a click skill to the inventory.
     *
     * @param slot      The slot to add the skill to.
     * @param clickType The click type to add the skill to.
     * @param skills    The skills to add to the click type.
     */
    public void addClickSkills(int slot, ClickType clickType, List<String> skills) {
        HashMap<ClickType, List<String>> click = new HashMap<>();
        click.put(clickType, skills);
        clickSkills.put(slot, click);
    }

    /**
     * Add a click skill to the inventory.
     *
     * @param slot      The slot to add the skill to.
     * @param clickType The click type to add the skill to.
     * @param skill     The skill to add to the click type.
     */
    public void addClickSkill(int slot, ClickType clickType, String skill) {
        HashMap<ClickType, List<String>> clickMap = clickSkills.get(slot);
        if (clickMap == null) {
            clickMap = new HashMap<>();
        }
        List<String> skills = clickMap.get(clickType);
        if (skills == null) {
            skills = new ArrayList<>();
        }
        skills.add(skill);
        clickMap.put(clickType, skills);
        clickSkills.put(slot, clickMap);
    }
}
