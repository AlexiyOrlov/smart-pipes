package dev.buildtool.smartpipes;

import dev.buildtool.satako.DefaultInventory;
import dev.buildtool.satako.IntegerColor;
import dev.buildtool.satako.gui.BetterScreenHandler;
import dev.buildtool.satako.gui.BetterSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.Direction;

public class PipeScreenHandler extends BetterScreenHandler {
    public ItemPipeEntity itemPipeEntity;

    public PipeScreenHandler(int syncId, PlayerEntity playerEntity, PacketByteBuf byteBuf) {
        super(SmartPipes.itemPipeScreenHandler, syncId);
        ItemPipeEntity pipeEntity = (ItemPipeEntity) playerEntity.world.getBlockEntity(byteBuf.readBlockPos());
        assert pipeEntity != null;
        initialize(playerEntity, pipeEntity);
    }

    public PipeScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory.player, buf);
    }

    private void initialize(PlayerEntity playerEntity, ItemPipeEntity pipeEntity) {
        itemPipeEntity = pipeEntity;
        int y = 2;
        boolean alternate = false;
        for (Direction direction : pipeEntity.filters.keySet()) {
            DefaultInventory defaultInventory = pipeEntity.filters.get(direction);
            for (int i = 0; i < defaultInventory.size(); i++) {
                BetterSlot slot = new BetterSlot(defaultInventory, i, 2 + i * 18, y);
                if (alternate)
                    slot.setColor(new IntegerColor(0xff3F8A2E)); //green
                else
                    slot.setColor(new IntegerColor(0xffB16C23)); //orange
                addSlot(slot);
            }
            alternate = !alternate;
            y += 18;
        }

        addPlayerInventory(2, y + 18, playerEntity);
    }

    @Override
    public void onSlotClick(int slot, int button, SlotActionType actionType, PlayerEntity playerEntity) {
        if (slot >= 0 && slot < 54) {
            ItemStack itemStack = getCursorStack();
            if (itemStack.isEmpty()) {
                setStackInSlot(slot, 0, ItemStack.EMPTY);
            } else {
                if (itemStack.getItem() == SmartPipes.pipeFilterItem) {
                    super.onSlotClick(slot, button, actionType, playerEntity);
                } else {
                    ItemStack stack = itemStack.copy();
                    stack.setCount(1);
                    setStackInSlot(slot, 0, stack);
                }
            }
            //mark for saving slot contents
            itemPipeEntity.markDirty();
            return;
        }
        super.onSlotClick(slot, button, actionType, playerEntity);
    }
}
