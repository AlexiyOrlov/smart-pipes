package dev.buildtool.smartpipes;

import dev.buildtool.satako.Functions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BarrelBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    public BarrelBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getPlayerFacing().getOpposite());
    }

    @Override
    public void onBlockBreakStart(BlockState state, World world, BlockPos pos, PlayerEntity player) {
        if (!world.isClient) {
            BarrelEntity barrelEntity = (BarrelEntity) world.getBlockEntity(pos);
            for (int i = 0; i < barrelEntity.size(); i++) {
                ItemStack next = barrelEntity.getStack(i);
                if (!next.isEmpty()) {
                    if (player.isSneaking()) {
                        ItemStack full = next.split(next.getMaxCount());
                        if (!player.getInventory().insertStack(full)) {
                            ItemScatterer.spawn(world, player.getX(), player.getY(), player.getZ(), full);
                        }
                        barrelEntity.markDirty();
                        break;
                    } else {
                        ItemStack split = next.split(1);
                        if (!player.getInventory().insertStack(split)) {
                            ItemScatterer.spawn(world, player.getX(), player.getY(), player.getZ(), split);
                        }
                        barrelEntity.markDirty();
                        break;
                    }
                }
            }
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof BarrelEntity barrelEntity) {
                ItemStack stack = player.getMainHandStack();
                boolean insert = false;
                for (int i = 0; i < barrelEntity.size(); i++) {
                    ItemStack next = barrelEntity.getStack(i);
                    if (Functions.areItemTypesEqual(stack, next)) {
                        insert = true;
                        break;
                    }
                }
                if (insert || barrelEntity.isEmpty()) {

                    if (Functions.insertInto(barrelEntity, stack)) {
                        player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                        barrelEntity.markDirty();
                    }
                }
            }
        }
        return ActionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return SmartPipes.barrelEntityBlockEntityType.instantiate(pos, state);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
        super.appendTooltip(stack, world, tooltip, options);
        tooltip.add(new LiteralText("[WIP]"));
    }
}
