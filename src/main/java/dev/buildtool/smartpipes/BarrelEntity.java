package dev.buildtool.smartpipes;

import dev.buildtool.satako.BaseBlockEntity;
import dev.buildtool.satako.DefaultInventory;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.Collections;

public class BarrelEntity extends BaseBlockEntity implements Inventory {
    //TODO figure out proper synchronization to client
    DefaultInventory inventory = new DefaultInventory(64);
    public int itemCountIndicator;
    public ItemStack contentIndicator = ItemStack.EMPTY;

    public BarrelEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public int size() {
        return inventory.size();
    }

    @Override
    public boolean isEmpty() {
        return inventory.isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        return inventory.getStack(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        markDirty();
        return inventory.removeStack(slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        markDirty();
        return inventory.removeStack(slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        inventory.setStack(slot, stack);
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        inventory.clear();
        markDirty();
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return isEmpty() || containsAny(Collections.singleton(stack.getItem()));
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.put("Items", inventory.writeToTag());
        if (!world.getPlayers().isEmpty())
            markDirty();
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        inventory = inventory.readFromTag(nbt.getCompound("Items"));
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
        //dirty trick to synchronize displayed item
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            syncToClients(world);
        });
        thread.start();
    }

    private void syncToClients(World world) {
        if (!world.isClient) {
            Collection<ServerPlayerEntity> list = PlayerLookup.tracking(this);
            if (!list.isEmpty()) {
                PacketByteBuf packetByteBuf = new PacketByteBuf(Unpooled.buffer());
                packetByteBuf.writeBlockPos(pos);
                int itemcount = 0;
                ItemStack content = ItemStack.EMPTY;
                for (int i = 0; i < inventory.size(); i++) {
                    ItemStack itemStack = inventory.getStack(i);
                    if (!itemStack.isEmpty()) {
                        itemcount += itemStack.getCount();
                        if (content == ItemStack.EMPTY)
                            content = itemStack;
                    }
                }
                packetByteBuf.writeInt(itemcount);
                packetByteBuf.writeItemStack(content);
                list.forEach(serverPlayerEntity -> ServerPlayNetworking.send(serverPlayerEntity, SmartPipes.barrelItemCount, packetByteBuf));
            }
        }
    }

    @Override
    public void markDirty() {
        super.markDirty();
        world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        syncToClients(world);
    }
}
