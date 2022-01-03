package dev.buildtool.smartpipes.client;

import dev.buildtool.smartpipes.BarrelEntity;
import dev.buildtool.smartpipes.ItemPipeEntity;
import dev.buildtool.smartpipes.PipedItem;
import dev.buildtool.smartpipes.SmartPipes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

@Environment(EnvType.CLIENT)
public class SmartPipesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(SmartPipes.itemPipeBlock, RenderLayer.getCutout());

        BlockEntityRendererRegistry.register(SmartPipes.itemPipeEntity, PipeRenderer::new);

        ClientPlayNetworking.registerGlobalReceiver(SmartPipes.pipeItemCreation, (client, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            ItemStack stack = buf.readItemStack();
            float px = buf.readFloat();
            float py = buf.readFloat();
            float pz = buf.readFloat();
            short angle = buf.readShort();
            int id = buf.readInt();
            Direction to = buf.readEnumConstant(Direction.class);
            PipedItem pipedItem = new PipedItem(stack, px, py, pz, id, angle, to);
            client.execute(() -> {
                ClientWorld world = (ClientWorld) client.player.world;
                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity instanceof ItemPipeEntity) {
                    ItemPipeEntity entity = (ItemPipeEntity) blockEntity;
                    entity.pipedItems.add(pipedItem);
                }
            });
        });


        ClientPlayNetworking.registerGlobalReceiver(SmartPipes.pipeItemRemoval, (client, handler, buf, responseSender) -> {
            BlockPos blockPos = buf.readBlockPos();
            int itemid = buf.readInt();
            client.execute(() -> {
                BlockEntity blockEntity = client.player.world.getBlockEntity(blockPos);
                if (blockEntity instanceof ItemPipeEntity) {
                    ItemPipeEntity pipeEntity = (ItemPipeEntity) blockEntity;
                    pipeEntity.pipedItems.removeIf(item -> item.id == itemid);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SmartPipes.pipeItemUpdate, (client, handler, buf, responseSender) -> {
            BlockPos blockPos = buf.readBlockPos();
            int itemid = buf.readInt();
            float x = buf.readFloat();
            float y = buf.readFloat();
            float z = buf.readFloat();
            short rotation = buf.readShort();
            client.execute(() -> {
                BlockEntity blockEntity = client.player.world.getBlockEntity(blockPos);
                if (blockEntity instanceof ItemPipeEntity) {
                    ItemPipeEntity itemPipeEntity = (ItemPipeEntity) blockEntity;
                    for (PipedItem item : itemPipeEntity.pipedItems) {
                        if (item.id == itemid) {
                            item.x = x;
                            item.y = y;
                            item.z = z;
                            item.rotation = rotation;
                            break;
                        }
                    }
                }
            });
        });

        ScreenRegistry.register(SmartPipes.itemPipeScreenHandler, PipeFilterScreen::new);

        //barrel
        BlockEntityRendererRegistry.register(SmartPipes.barrelEntityBlockEntityType, BarrelRenderer::new);

        ClientPlayNetworking.registerGlobalReceiver(SmartPipes.barrelItemCount, (client, handler, buf, responseSender) -> {
            BlockPos blockPos = buf.readBlockPos();
            int count = buf.readInt();
            ItemStack stack = buf.readItemStack();
            client.execute(() -> {
                BlockEntity blockEntity = client.player.world.getBlockEntity(blockPos);
                if (blockEntity instanceof BarrelEntity) {
                    BarrelEntity barrelEntity = (BarrelEntity) blockEntity;
                    barrelEntity.itemCountIndicator = count;
                    barrelEntity.contentIndicator = stack;
                }
            });
        });
    }
}
