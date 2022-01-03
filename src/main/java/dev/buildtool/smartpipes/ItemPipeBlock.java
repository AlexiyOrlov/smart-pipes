package dev.buildtool.smartpipes;

import dev.buildtool.smartpipes.client.PipeDirectionsScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;

public class ItemPipeBlock extends ConnectingBlock implements BlockEntityProvider {
    public VoxelShape ALONE = VoxelShapes.cuboid(0.25, 0.25, 0.25, 0.75, 0.75, 0.75);

    protected ItemPipeBlock(Settings settings) {
        super(4 / 16f, settings);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(UP, DOWN, EAST, WEST, SOUTH, NORTH);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        World world = ctx.getWorld();
        BlockPos blockPos = ctx.getBlockPos();
        boolean up = checkConnection(world, blockPos, Direction.UP);
        boolean down = checkConnection(world, blockPos, Direction.DOWN);
        boolean south = checkConnection(world, blockPos, Direction.SOUTH);
        boolean north = checkConnection(world, blockPos, Direction.NORTH);
        boolean east = checkConnection(world, blockPos, Direction.EAST);
        boolean west = checkConnection(world, blockPos, Direction.WEST);
        return getDefaultState().with(UP, up).with(DOWN, down).with(SOUTH, south).with(NORTH, north).with(EAST, east).with(WEST, west);
    }

    protected boolean checkConnection(WorldAccess world, BlockPos ownposition, Direction at) {
        BlockPos side = ownposition.offset(at);
        BlockEntity blockEntity = world.getBlockEntity(side);
        if (blockEntity instanceof SidedInventory sidedInventory) {
            return sidedInventory.getAvailableSlots(at.getOpposite()).length > 0;
        }
        return world.getBlockEntity(ownposition.offset(at)) instanceof Inventory;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState newState, WorldAccess world, BlockPos pos, BlockPos posFrom) {
        boolean up = checkConnection(world, pos, Direction.UP);
        boolean down = checkConnection(world, pos, Direction.DOWN);
        boolean south = checkConnection(world, pos, Direction.SOUTH);
        boolean north = checkConnection(world, pos, Direction.NORTH);
        boolean east = checkConnection(world, pos, Direction.EAST);
        boolean west = checkConnection(world, pos, Direction.WEST);
        return state.with(UP, up).with(DOWN, down).with(SOUTH, south).with(NORTH, north).with(EAST, east).with(WEST, west);
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return false;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return SmartPipes.itemPipeEntity.instantiate(pos, state);
    }

    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<ItemPipeEntity> checkType(BlockEntityType<A> givenType, BlockEntityType<E> expectedType, BlockEntityTicker<ItemPipeEntity> ticker) {
        return expectedType == givenType ? ticker : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return !world.isClient ? (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof ItemPipeEntity itemPipeEntity)
                itemPipeEntity.tick();
        } : null;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (player.isSneaking() && player.getMainHandStack().isEmpty()) {
            ItemPipeEntity pipeEntity = (ItemPipeEntity) world.getBlockEntity(pos);
            pipeEntity.pipedItems.forEach(pipedItem -> ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), pipedItem.getContent()));
            return ActionResult.SUCCESS;
        }
        if (player.getMainHandStack().getItem() == SmartPipes.itemPipeItem)
            return ActionResult.PASS;
        if (world.isClient)
            openGui((ItemPipeEntity) world.getBlockEntity(pos));
        return ActionResult.SUCCESS;
    }

    @Environment(EnvType.CLIENT)
    private void openGui(ItemPipeEntity pipeEntity) {
        MinecraftClient.getInstance().setScreen(new PipeDirectionsScreen(pipeEntity));
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!world.isClient && state.getBlock() != newState.getBlock()) {
            ItemPipeEntity pipeEntity = (ItemPipeEntity) world.getBlockEntity(pos);
            if (pipeEntity != null) {
                pipeEntity.pipedItems.forEach(pipedItem -> ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), pipedItem.getContent()));
                pipeEntity.destroyItemsOnClient(new HashSet<>(pipeEntity.pipedItems));
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return ALONE;
    }
}
