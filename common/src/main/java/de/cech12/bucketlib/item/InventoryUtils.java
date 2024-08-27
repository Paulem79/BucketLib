package de.cech12.bucketlib.item;

import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class InventoryUtils {
    public static List<ItemStack> getItemsInInventory(@Nonnull CraftingContainer inv) {
        List<ItemStack> invItems = new ArrayList<>();

        for (int i = 0; i < inv.getContainerSize(); i++) {
            invItems.add(inv.getItem(i));
        }

        return invItems;
    }
}
