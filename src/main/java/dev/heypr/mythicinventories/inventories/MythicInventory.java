package dev.heypr.mythicinventories.inventories;

import dev.heypr.mythicinventories.MythicInventories;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class MythicInventory implements InventoryHolder {

    private final Inventory inventory;
    private String internalName;
    private final Set<Integer> savedItems = new HashSet<>();
    private final HashMap<Integer, ItemStack> interactableItems = new HashMap<>();


    public MythicInventory(MythicInventories plugin, int size, Component title) {
        this.inventory = plugin.getServer().createInventory(this, size, title);
    }

    /**
     * Constructor for getting an existing inventory.
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
}
