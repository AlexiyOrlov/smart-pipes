package dev.buildtool.smartpipes;

import dev.buildtool.satako.DefaultInventory;
import dev.buildtool.satako.Functions;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/**
 * Created on 8/23/19.
 */
public class FilterInventory extends DefaultInventory {
    public boolean whitelist;
    public boolean holdItems = true;

    public FilterInventory() {
        super(9);
    }

    boolean isItemWhitelisted(PipedItem pipedItem) {
        if (whitelist) {
            ItemStack item = pipedItem.getContent();

            for (int i = 0; i < size(); i++) {
                ItemStack itemStack = getStack(i);
                if (Functions.areItemTypesEqual(item, itemStack))
                    return true;
            }
        }
        return false;
    }

    boolean isItemBlacklisted(PipedItem pipedItem) {
        if (!whitelist) {
            ItemStack item = pipedItem.getContent();
            for (int i = 0; i < size(); i++) {
                ItemStack itemStack = getStack(i);
                if (Functions.areItemTypesEqual(item, itemStack))
                    return true;
            }
        }
        return false;
    }

    @Override
    public NbtCompound writeToTag() {
        NbtCompound tag = super.writeToTag();
        tag.putBoolean("Whitelist", whitelist);
        tag.putBoolean("Item_holding", holdItems);
        return tag;
    }

    @Override
    public void readFromTag(NbtCompound nbtCompound) {
        whitelist = nbtCompound.getBoolean("Whitelist");
        if (nbtCompound.contains("Item_holding"))
            holdItems = nbtCompound.getBoolean("Item_holding");
        super.readFromTag(nbtCompound);
    }
}
