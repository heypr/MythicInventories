package me.hyper.mythicinventories.inventories;

import me.hyper.mythicinventories.MythicInventories;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MythicInventory implements org.bukkit.inventory.InventoryHolder {

    private final Inventory inventory;

    public MythicInventory(MythicInventories plugin, int size, Component title) {
        this.inventory = plugin.getServer().createInventory(this, size, title);
    }

    public MythicInventory(MythicInventory inventory) {
        this.inventory = inventory.getInventory();
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public void setItem(int slot, ItemStack item) {
        this.inventory.setItem(slot - 1, item);
    }
}
