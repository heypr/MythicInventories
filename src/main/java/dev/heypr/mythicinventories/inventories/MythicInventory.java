package dev.heypr.mythicinventories.inventories;

import dev.heypr.mythicinventories.MythicInventories;
import dev.heypr.mythicinventories.misc.MIClickType;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MythicInventory implements InventoryHolder {

    private final Inventory inventory;
    private String internalName;
    private Set<Integer> savedItems = new HashSet<>();
    private HashMap<Integer, ItemStack> interactableItems = new HashMap<>();
    private HashMap<Integer, HashMap<MIClickType, List<String>>> clickSkills = new HashMap<>();

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
        this.inventory = plugin.getInventories().get(internalName).getInventory();
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
    public HashMap<MIClickType, List<String>> getClickTypes(int slot) {
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
    public List<String> getClickSkills(int slot, MIClickType clickType) {
        return clickSkills.get(slot).get(clickType);
    }

    /**
     * Add multiple click skills to the inventory.
     *
     * @param slot   The slot to add the skills to.
     * @param skills The skills to add to the click type.
     */
    public void addClickSkills(int slot, HashMap<MIClickType, List<String>> skills) {
        clickSkills.put(slot, skills);
    }

    /**
     * Add a click requirement to the inventory.
     *
     * @param slot      The slot to listen for.
     * @param clickType The click type to listen to.
     */
    public void addClick(int slot, MIClickType clickType) {
        HashMap<MIClickType, List<String>> empty = new HashMap<>();
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
    public void addClickSkills(int slot, MIClickType clickType, List<String> skills) {
        HashMap<MIClickType, List<String>> click = new HashMap<>();
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
    public void addClickSkill(int slot, MIClickType clickType, String skill) {
        HashMap<MIClickType, List<String>> clickMap = clickSkills.get(slot);
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
