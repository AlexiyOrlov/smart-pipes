package dev.buildtool.smartpipes;

import com.google.common.collect.Sets;
import dev.buildtool.satako.BaseBlockEntity;
import dev.buildtool.satako.DefaultInventory;
import dev.buildtool.satako.Functions;
import dev.buildtool.satako.UniqueList;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;

public class ItemPipeEntity extends BaseBlockEntity implements SidedInventory, ExtendedScreenHandlerFactory {

    private static final String ITEM_FLOW = "Flow";
    public UniqueList<PipedItem> pipedItems = new UniqueList<>();
    private final HashMap<Integer, Direction> slots;
    private DefaultInventory inventory;
    private int nextPipeId;
    /**
     * Customizable
     */
    public HashMap<Direction, Direction> fromTo;
    public HashMap<Direction, FilterInventory> filters;
    private FilterInventory northFilter = new FilterInventory();
    private FilterInventory southFilter = new FilterInventory();
    private FilterInventory westFilter = new FilterInventory();
    private FilterInventory eastFilter = new FilterInventory();
    private FilterInventory topFilter = new FilterInventory();
    private FilterInventory bottomFilter = new FilterInventory();

    /**
     * Items that were created while block entity wasn't being tracked
     */
    private final HashSet<PipedItem> ghostItems = new HashSet<>();

    public ItemPipeEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        inventory = new DefaultInventory(6);
        fromTo = new HashMap<>(6, 1);
        fromTo.put(Direction.UP, Direction.DOWN);
        fromTo.put(Direction.DOWN, Direction.UP);
        fromTo.put(Direction.EAST, Direction.WEST);
        fromTo.put(Direction.WEST, Direction.EAST);
        fromTo.put(Direction.SOUTH, Direction.NORTH);
        fromTo.put(Direction.NORTH, Direction.SOUTH);
        slots = new HashMap<>(6, 1);
        slots.put(Direction.NORTH.ordinal(), Direction.SOUTH);
        slots.put(Direction.SOUTH.ordinal(), Direction.NORTH);
        slots.put(Direction.WEST.ordinal(), Direction.EAST);
        slots.put(Direction.EAST.ordinal(), Direction.WEST);
        slots.put(Direction.UP.ordinal(), Direction.DOWN);
        slots.put(Direction.DOWN.ordinal(), Direction.UP);
        filters = new HashMap<>(6, 1);
        filters.put(Direction.NORTH, northFilter);
        filters.put(Direction.SOUTH, southFilter);
        filters.put(Direction.WEST, westFilter);
        filters.put(Direction.EAST, eastFilter);
        filters.put(Direction.UP, topFilter);
        filters.put(Direction.DOWN, bottomFilter);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[]{side.getOpposite().ordinal()};

    }

    @Override
    public boolean canInsert(int slot, ItemStack itemStack, Direction dir) {
        ItemStack stack = inventory.getStack(slot);
        if (stack.isEmpty())
            return true;
        return Functions.areItemTypesEqual(itemStack, stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slots.get(slot) == dir;
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
        return inventory.removeStack(slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return inventory.removeStack(slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        inventory.setStack(slot, stack);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        inventory.clear();
    }

    private static boolean isOutOfPipeBounds(PipedItem pipedItem) {
        return pipedItem.x < 0 || pipedItem.x > 1 || pipedItem.z < 0 || pipedItem.z > 1 || pipedItem.y < 0 || pipedItem.y > 1;
    }

    @SuppressWarnings("unchecked")
        //TODO
    void tick() {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack next = inventory.getStack(i);
            if (!next.isEmpty()) {
                Direction from = slots.get(i);
                Direction to = fromTo.get(from);
                PipedItem pipedItem = new PipedItem(next.copy(), from.getOffsetX() / 2f + 0.5f, from.getOffsetY() / 2f + 0.5f, from.getOffsetZ() / 2f + 0.5f, nextPipeId++, (short) 0, to);
                pipedItem.from = from;
                inventory.removeStack(i);
                pipedItems.add(pipedItem);

                createItemOnClient(pipedItem);
            }
        }
        float speed = 0.08f;
        float centerBound0 = 0.45f, centerBound1 = 0.55f;
        HashSet<PipedItem> removeables = new HashSet<>();
        ArrayList<PipedItem> otherItems = (ArrayList<PipedItem>) pipedItems.clone();
        for (PipedItem pipedItem : pipedItems) {

            final Direction goesFrom = pipedItem.from;
            //move item to pipe's center
            if (!pipedItem.aligned) {
                boolean ax = false, ay = false, az = false;
                if (!isBetween(centerBound0, centerBound1, pipedItem.x)) {
                    pipedItem.x += pipedItem.from.getOpposite().getOffsetX() * speed;
                } else {
                    ax = true;
                }
                if (!isBetween(centerBound0, centerBound1, pipedItem.y)) {
                    pipedItem.y += pipedItem.from.getOpposite().getOffsetY() * speed;
                } else {
                    ay = true;
                }
                if (!isBetween(centerBound0, centerBound1, pipedItem.z)) {
                    pipedItem.z += pipedItem.from.getOpposite().getOffsetZ() * speed;
                } else az = true;
                if (ax && az && ay) {
                    pipedItem.aligned = true;
                }
            } else {
                final BlockEntity ahead = world.getBlockEntity(pos.offset(pipedItem.to));
                EnumSet<Direction> allowedDestinations = getAvailableDirections(pipedItem);

                // Prefer moving straight:
                if (ahead instanceof Inventory && allowedDestinations.contains(pipedItem.to)) {
                    boolean canTransfer = isOutOfPipeBounds(pipedItem);
                    if (ahead instanceof SidedInventory sidedInventory) {
                        if (canInsertInto(sidedInventory, pipedItem)) {
                            if (canTransfer) {
                                // TODO smooth transfer from pipe to pipe
                                if (insertInto(sidedInventory, pipedItem)) {
                                    removeables.add(pipedItem);
                                }
                            } else
                                move(pipedItem, speed);
                        } else {
                            //TODO to method; check Inventories?
                            ArrayList<Direction> sidedirs = Functions.getSideDirections(pipedItem.to);
//                                System.out.println(pipedItem.to);
                            boolean foundSide = false;
                            for (Direction sidedir : sidedirs) {
                                BlockEntity blockEntity = world.getBlockEntity(pos.offset(sidedir));
                                if (blockEntity instanceof SidedInventory sided2) {
                                    if (canInsertInto(sided2, pipedItem)) {

                                        pipedItem.to = sidedir;
                                        foundSide = true;
                                        break;
                                    }
                                }
                            }
                            if (!foundSide) {
                                pipedItem.to = pipedItem.to.getOpposite();
                            }
                        }
                    } else {
                        Inventory inv = (Inventory) ahead;
                        if (canInsertInto(inv, pipedItem)) {
                            if (canTransfer) {

                                if (insertInto(inv, pipedItem)) {
                                    removeables.add(pipedItem);
                                }
                            } else
                                move(pipedItem, speed);
                        } else {
                            //TODO push item in random side direction
                            pipedItem.to = allowedDestinations.toArray(new Direction[0])[world.random.nextInt(allowedDestinations.size())];
                        }
                    }
                } else {
                    // Otherwise, check all directions except where it came from
                    boolean foundDestination = false;
                    EnumSet<Direction> allowedDirections =/*getAvailableDirections(pipedItem);*/EnumSet.of(pipedItem.from);
                    Direction[] directions = ArrayUtils.removeElements(Direction.values(), pipedItem.from);
                    for (Direction direction : directions) {
                        FilterInventory filterInventory = filters.get(direction);
                        if (filterInventory.isItemWhitelisted(pipedItem)) {
                            allowedDirections.clear();
                            allowedDirections.add(direction);
                            break;
                        } else if (!filterInventory.whitelist) {
                            if (!filterInventory.isItemBlacklisted(pipedItem)) {
                                allowedDirections.add(direction);
                                allowedDirections.remove(pipedItem.from);
                            }
                        }
                    }

                    for (Direction direction : allowedDirections) {

                        BlockPos sidepos = pos.offset(direction);
                        BlockEntity sideEntity = world.getBlockEntity(sidepos);
                        if (sideEntity != null) {
                            boolean canTransfer = isOutOfPipeBounds(pipedItem);
                            pipedItem.to = direction;
                            if (sideEntity instanceof SidedInventory sidedInventory) {
                                if (canInsertInto(sidedInventory, pipedItem)) {
                                    if (canTransfer) {
                                        if (insertInto(sidedInventory, pipedItem))
                                            removeables.add(pipedItem);
                                    } else
                                        move(pipedItem, speed);
                                    foundDestination = true;
                                    break;
                                }
                            } else if (sideEntity instanceof Inventory invent2) {

                                {
                                    if (canInsertInto(invent2, pipedItem)) {
                                        pipedItem.to = direction;
                                        if (canTransfer) {
                                            if (insertInto(invent2, pipedItem))
                                                removeables.add(pipedItem);
                                        } else
                                            move(pipedItem, speed);
                                        foundDestination = true;
                                        break;
                                    } else {
                                        pipedItem.to = allowedDestinations.toArray(new Direction[0])[world.random.nextInt(allowedDestinations.size())];
                                    }
                                }

                            }
                        }
                    }
                    // otherwise, go back from dead end.
                    if (!foundDestination) {
                        boolean goesToSide = false;
                        ArrayList<Direction> sideDirs = Functions.getSideDirections(pipedItem.to);
                        //TODO check for Inventories?
                        for (Direction sideDir : sideDirs) {
                            BlockEntity sideentity = world.getBlockEntity(pos.offset(sideDir));
                            if (sideentity instanceof SidedInventory sidedInventory) {
                                pipedItem.to = sideDir;
                                if (canInsertInto(sidedInventory, pipedItem)) {
                                    goesToSide = true;
                                    break;
                                }
                            }
                        }
                        if (!goesToSide)
                            pipedItem.to = goesFrom;
                    }
                }
            }

            updateItemOnClient(pipedItem);
            otherItems.remove(pipedItem);
            final ItemStack content = pipedItem.getContent();
            if (content.getCount() < content.getMaxCount()) {
                HashSet<PipedItem> merged = new HashSet<>(otherItems.size());
                for (PipedItem otherItem : otherItems) {
                    ItemStack otherstack = otherItem.getContent();
                    if (Functions.areItemTypesEqual(content, otherstack)) {
                        if (otherstack.getCount() <= content.getCount() && otherstack.getCount() + content.getCount() < content.getMaxCount()) {
                            if (otherItem.to == pipedItem.to) {
                                content.increment(otherstack.getCount());
//                                    System.out.println("Merge "+content+" with "+otherstack);
                                otherstack.decrement(otherstack.getCount());
                                removeables.add(otherItem);
                                merged.add(otherItem);
                                break;
                            }
                        }
                    }
                }
                otherItems.removeAll(merged);
            }
        }
        pipedItems.removeAll(removeables);

        destroyItemsOnClient(removeables);
    }

    private static boolean canInsertInto(SidedInventory inventory, PipedItem item) {
        ItemStack stack = item.getContent();
        Direction to = item.to;
        int[] slots = inventory.getAvailableSlots(to.getOpposite());
        for (int slot : slots) {
            if (inventory.canInsert(slot, stack, to.getOpposite())) {
                ItemStack next = inventory.getStack(slot);
                int count = next.getCount();
                int insCount = stack.getCount();
                if (Functions.areItemTypesEqual(stack, next) && count < next.getMaxCount() && count + insCount <= next.getMaxCount())
                    return true;
            }
        }
        for (int slot : slots) {
            if (inventory.canInsert(slot, stack, to.getOpposite())) {
                if (inventory.getStack(slot).isEmpty())
                    return true;
            }
        }
        return false;
    }

    private static boolean canInsertInto(Inventory inventory, PipedItem item) {
        if (inventory instanceof ChestBlockEntity chestBlockEntity) {
            inventory = ChestBlock.getInventory((ChestBlock) chestBlockEntity.getCachedState().getBlock(), chestBlockEntity.getCachedState(), chestBlockEntity.getWorld(), chestBlockEntity.getPos(), true);
        }
        if (inventory != null) {
            ItemStack stack = item.getContent();
            for (int i = 0; i < inventory.size(); i++) {
                if (inventory.isValid(i, stack)) {
                    ItemStack next = inventory.getStack(i);
                    int count = next.getCount();
                    if (Functions.areItemTypesEqual(stack, next) && count < next.getMaxCount() && count + stack.getCount() <= next.getMaxCount()) {
                        return true;
                    }
                }
            }
            for (int i = 0; i < inventory.size(); i++) {
                if (inventory.isValid(i, stack) && inventory.getStack(i).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return true on success
     */
    private static boolean insertInto(Inventory inventory, PipedItem item) {
        if (inventory instanceof ChestBlockEntity chestBlockEntity) {
            inventory = ChestBlock.getInventory((ChestBlock) chestBlockEntity.getCachedState().getBlock(), chestBlockEntity.getCachedState(), chestBlockEntity.getWorld(), chestBlockEntity.getPos(), true);
        }
        if (inventory != null) {
            ItemStack stack = item.getContent();
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack next = inventory.getStack(i);
                int count = stack.getCount();
                int nextCount = next.getCount();
                if (Functions.areItemTypesEqual(stack, next) && nextCount < next.getMaxCount() && nextCount + count <= next.getMaxCount()) {

                    next.increment(count);
                    stack.decrement(count);
                    inventory.markDirty();
                    return true;
                }
            }
            for (int i = 0; i < inventory.size(); i++) {
                if (inventory.getStack(i).isEmpty()) {
                    inventory.setStack(i, stack);
                    inventory.markDirty();
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean insertInto(SidedInventory inventory, PipedItem item) {
        ItemStack stack = item.getContent();
        final Direction to = item.to;
        int[] slots = inventory.getAvailableSlots(to.getOpposite());
        for (int slot : slots) {
            if (inventory.canInsert(slot, stack, item.to.getOpposite())) {
                ItemStack next = inventory.getStack(slot);
                int count = next.getCount();
                int insCount = stack.getCount();
                if (Functions.areItemTypesEqual(stack, next) && count < next.getMaxCount() && count + insCount <= next.getMaxCount()) {
                    next.increment(insCount);
                    stack.decrement(insCount);
                    inventory.markDirty();
                    return true;
                }
            }
        }

        for (int slot : slots) {
            if (inventory.canInsert(slot, stack, item.to.getOpposite())) {
                if (inventory.getStack(slot).isEmpty()) {
                    inventory.setStack(slot, stack);
                    inventory.markDirty();
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks filters for available directions
     */
    private EnumSet<Direction> getAvailableDirections(PipedItem pipedItem) {
        EnumSet<Direction> allowedDestinations = EnumSet.of(pipedItem.from);
        Direction[] directionCheckForWhitelist = ArrayUtils.removeElement(Direction.values(), pipedItem.from);
        for (Direction direction : directionCheckForWhitelist) {
            FilterInventory filterInventory = filters.get(direction);
            if (filterInventory.isItemWhitelisted(pipedItem)) {
                if (filterInventory.holdItems) {
                    allowedDestinations.clear();
                    allowedDestinations.add(direction);
                } else {
                    BlockEntity behindFilter = world.getBlockEntity(pos.offset(direction));
                    boolean cantGo = false;
                    if (behindFilter instanceof SidedInventory sidedInventory) {
                        if (!canInsertInto(sidedInventory, pipedItem)) {
                            cantGo = true;
                        }
                    } else if (behindFilter instanceof Inventory inventory) {
                        if (!canInsertInto(inventory, pipedItem)) {
                            cantGo = true;
                        }
                    }
                    if (cantGo) {
                        allowedDestinations.add(pipedItem.from.getOpposite());

                        Set<Direction> set = Sets.difference(EnumSet.allOf(Direction.class), allowedDestinations);
                        allowedDestinations.addAll(set);
                        allowedDestinations.remove(pipedItem.from);
                    } else {
                        pipedItem.to = direction;
                    }
                }
                break;
            } else if (!filterInventory.whitelist) {
                if (!filterInventory.isItemBlacklisted(pipedItem)) {
                    allowedDestinations.add(direction);
                }
            }
        }

        return allowedDestinations;
    }


    private static void move(PipedItem item, float speed) {
        item.x += item.to.getOffsetX() * speed;
        item.y += item.to.getOffsetY() * speed;
        item.z += item.to.getOffsetZ() * speed;
    }

    private static boolean isBetween(float lesser, float bigger, float value) {
        return value >= lesser && value <= bigger;
    }

    private void createItemOnClient(PipedItem pipedItem) {

        Collection<ServerPlayerEntity> list = PlayerLookup.tracking(this);
        if (!list.isEmpty()) {
            PacketByteBuf byteBuf = new PacketByteBuf(Unpooled.buffer());
            byteBuf.writeBlockPos(pos);
            byteBuf.writeItemStack(pipedItem.getContent());
            byteBuf.writeFloat(pipedItem.x);
            byteBuf.writeFloat(pipedItem.y);
            byteBuf.writeFloat(pipedItem.z);
            byteBuf.writeShort(pipedItem.rotation);
            byteBuf.writeInt(pipedItem.id);
            byteBuf.writeEnumConstant(pipedItem.to);
            list.forEach(serverPlayerEntity -> ServerPlayNetworking.send(serverPlayerEntity, SmartPipes.pipeItemCreation, byteBuf));
        }
    }

    private void updateItemOnClient(PipedItem pipedItem) {
        Collection<ServerPlayerEntity> list = PlayerLookup.tracking(this);
        if (!list.isEmpty()) {
            PacketByteBuf packetByteBuf = new PacketByteBuf(Unpooled.buffer());
            packetByteBuf.writeBlockPos(pos);
            packetByteBuf.writeInt(pipedItem.id);
            packetByteBuf.writeFloat(pipedItem.x);
            packetByteBuf.writeFloat(pipedItem.y);
            packetByteBuf.writeFloat(pipedItem.z);
            packetByteBuf.writeShort(pipedItem.rotation);
            list.forEach(serverPlayerEntity -> ServerPlayNetworking.send(serverPlayerEntity, SmartPipes.pipeItemUpdate, packetByteBuf));
        }
    }

    void destroyItemsOnClient(Set<PipedItem> removeables) {
        Collection<ServerPlayerEntity> list = PlayerLookup.tracking(this);
        if (!list.isEmpty()) {
            removeables.addAll(ghostItems);
            for (PipedItem removeable : removeables) {
                PacketByteBuf packetByteBuf = new PacketByteBuf(Unpooled.buffer());
                packetByteBuf.writeBlockPos(pos);
                packetByteBuf.writeInt(removeable.id);
                list.forEach(serverPlayerEntity -> ServerPlayNetworking.send(serverPlayerEntity, SmartPipes.pipeItemRemoval, packetByteBuf));
            }
            ghostItems.clear();
        } else {
            if (removeables.size() > 0) {
                ghostItems.addAll(removeables);
            }
        }
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        PacketByteBuf packetByteBuf = new PacketByteBuf(Unpooled.buffer());
        packetByteBuf.writeBlockPos(pos);
        return new PipeScreenHandler(syncId, inv, packetByteBuf);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Override
    public Text getDisplayName() {
        return new LiteralText("Smart pipe");
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        NbtCompound tag = inventory.writeToTag();
        nbt.put("Inventory", tag);
        for (int i = 0; i < pipedItems.size(); i++) {
            nbt.put("Item_" + i, pipedItems.get(i).writeToTag());
        }
        nbt.putInt("Piped_items", pipedItems.size());
        int c = 0;
        for (Map.Entry<Direction, Direction> entry : fromTo.entrySet()) {
            nbt.putByte("From_" + c, (byte) entry.getKey().ordinal());
            nbt.putByte("To_" + c, (byte) entry.getValue().ordinal());
            c++;
        }
        if (c > 0)
            nbt.putByte(ITEM_FLOW, (byte) c);
        nbt.put("North_filter", northFilter.writeToTag());
        nbt.put("South_filter", southFilter.writeToTag());
        nbt.put("East_filter", eastFilter.writeToTag());
        nbt.put("West_filter", westFilter.writeToTag());
        nbt.put("Top_filter", topFilter.writeToTag());
        nbt.put("Bottom_filter", bottomFilter.writeToTag());
        nbt.putInt("Next_item_id", nextPipeId);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        inventory.readFromTag(nbt.getCompound("Inventory"));
        nextPipeId = nbt.getInt("Next_item_id");
        int pipedItemCount = nbt.getInt("Piped_items");
        if (world != null && world.isClient)
            pipedItems.clear();
        for (int i = 0; i < pipedItemCount; i++) {
            NbtCompound tag = nbt.getCompound("Item_" + i);
            PipedItem pipedItem = new PipedItem();
            pipedItem.readFromTag(tag);
            pipedItems.add(pipedItem);
        }
        if (nbt.contains(ITEM_FLOW)) {
            byte count = nbt.getByte(ITEM_FLOW);
            for (byte i = 0; i < count; i++) {
                Direction from = Direction.values()[nbt.getByte("From_" + i)];
                Direction to = Direction.values()[nbt.getByte("To_" + i)];
                fromTo.put(from, to);
            }
        }
        northFilter.readFromTag(nbt.getCompound("North_filter"));
        southFilter.readFromTag(nbt.getCompound("South_filter"));
        eastFilter.readFromTag(nbt.getCompound("East_filter"));
        westFilter.readFromTag(nbt.getCompound("West_filter"));
        topFilter.readFromTag(nbt.getCompound("Top_filter"));
        bottomFilter.readFromTag(nbt.getCompound("Bottom_filter"));
    }
}
