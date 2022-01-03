package dev.buildtool.smartpipes;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.HashMap;

public class SmartPipes implements ModInitializer {

    public final static String MOD = "smart_pipes";
    public static BlockEntityType<ItemPipeEntity> itemPipeEntity;
    public static Block itemPipeBlock;
    //TODO
    public static Item pipeFilterItem;
    static BlockItem itemPipeItem;
    static ItemGroup itemGroup = FabricItemGroupBuilder.create(new Identifier(MOD, "everything")).icon(() -> new ItemStack(itemPipeItem)).build();
    public static ScreenHandlerType<PipeScreenHandler> itemPipeScreenHandler;

    public static Identifier pipeItemUpdate = new Identifier(MOD, "pipe_item_update");
    public static Identifier pipeItemRemoval = new Identifier(MOD, "pipe_item_remove");
    public static Identifier pipeItemCreation = new Identifier(MOD, "pipe_item_create");
    public static Identifier openPipeFilters = new Identifier(MOD, "pipe_filters");
    public static Identifier savePipeConfiguration = new Identifier(MOD, "pipe_configuration");
    public static Identifier toggleFilterMode = new Identifier(MOD, "pipe_filter_mode");
    public static Identifier toggleFilterItemHolding = new Identifier(MOD, "pipe_item_holding");
    public static Identifier barrelItemCount = new Identifier(MOD, "barrel_item_count");

    public static BlockEntityType<BarrelEntity> barrelEntityBlockEntityType;

    @Override
    public void onInitialize() {
        itemPipeBlock = new ItemPipeBlock(FabricBlockSettings.of(Material.METAL, MapColor.RED).strength(0.3f, 29).breakByHand(true).nonOpaque());
        itemPipeEntity = FabricBlockEntityTypeBuilder.create((blockPos, blockState) -> new ItemPipeEntity(itemPipeEntity, blockPos, blockState), new Block[]{itemPipeBlock}).build();
        itemPipeItem = new BlockItem(itemPipeBlock, new Item.Settings().group(itemGroup));

        final Identifier smartPipe = new Identifier(MOD, "smart_pipe");

        Registry.register(Registry.BLOCK, smartPipe, itemPipeBlock);
        Registry.register(Registry.ITEM, smartPipe, itemPipeItem);
        Registry.register(Registry.BLOCK_ENTITY_TYPE, smartPipe, itemPipeEntity);

        itemPipeScreenHandler = ScreenHandlerRegistry.registerExtended(smartPipe, PipeScreenHandler::new);

        ServerPlayNetworking.registerGlobalReceiver(openPipeFilters, (server, player, handler, buf, responseSender) -> {
            BlockPos blockPos = buf.readBlockPos();
            server.execute(() -> {
                BlockEntity blockEntity = player.world.getBlockEntity(blockPos);
                if (blockEntity instanceof ItemPipeEntity pipeEntity) {
                    player.openHandledScreen(pipeEntity);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(toggleFilterItemHolding, (server, player, handler, buf, responseSender) -> {
            BlockPos b = buf.readBlockPos();
            Direction direction = buf.readEnumConstant(Direction.class);
            boolean holdItems = buf.readBoolean();
            server.execute(() -> {
                BlockEntity e = player.world.getBlockEntity(b);
                if (e instanceof ItemPipeEntity pipeEntity) {
                    pipeEntity.filters.get(direction).holdItems = holdItems;
                    pipeEntity.markDirty();
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(toggleFilterMode, (server, player, handler, buf, responseSender) -> {
            BlockPos blockPos = buf.readBlockPos();
            Direction direction = buf.readEnumConstant(Direction.class);
            boolean state = buf.readBoolean();
            server.execute(() -> {
                BlockEntity blockEntity = player.world.getBlockEntity(blockPos);
                if (blockEntity instanceof ItemPipeEntity pipeEntity) {
                    pipeEntity.filters.get(direction).whitelist = state;
                    pipeEntity.markDirty();
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(savePipeConfiguration, (server, player, handler, buf, responseSender) -> {
            BlockPos blockPos = buf.readBlockPos();
            ArrayList<Direction> arrayList = new ArrayList<>(12);
            for (int i = 0; i < Direction.values().length * 2; i++) {
                arrayList.add(buf.readEnumConstant(Direction.class));
            }
            server.execute(() -> {
                BlockEntity blockEntity = player.world.getBlockEntity(blockPos);
                if (blockEntity instanceof ItemPipeEntity pipeEntity) {
                    HashMap<Direction, Direction> itemFlow = pipeEntity.fromTo;
                    itemFlow.put(arrayList.get(0), arrayList.get(1));
                    itemFlow.put(arrayList.get(2), arrayList.get(3));
                    itemFlow.put(arrayList.get(4), arrayList.get(5));
                    itemFlow.put(arrayList.get(6), arrayList.get(7));
                    itemFlow.put(arrayList.get(8), arrayList.get(9));
                    itemFlow.put(arrayList.get(10), arrayList.get(11));
                    blockEntity.markDirty();
                }
            });
        });

        //barrel
        BarrelBlock barrelBlock = new BarrelBlock(FabricBlockSettings.of(Material.METAL).strength(0.6f, 29));

        Identifier barrelId = new Identifier(MOD, "barrel");
        Registry.register(Registry.BLOCK, barrelId, barrelBlock);
        Registry.register(Registry.ITEM, barrelId, new BlockItem(barrelBlock, new Item.Settings().group(itemGroup).maxCount(16)));
        barrelEntityBlockEntityType = Registry.register(Registry.BLOCK_ENTITY_TYPE, barrelId, FabricBlockEntityTypeBuilder.create((blockPos, blockState) -> new BarrelEntity(barrelEntityBlockEntityType, blockPos, blockState), barrelBlock).build());
    }


}
