package dev.buildtool.smartpipes.client;

import dev.buildtool.satako.UniqueList;
import dev.buildtool.smartpipes.FilterInventory;
import dev.buildtool.smartpipes.ItemPipeEntity;
import dev.buildtool.smartpipes.PipedItem;
import dev.buildtool.smartpipes.SmartPipes;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3f;
import org.lwjgl.glfw.GLFW;

public class PipeRenderer implements BlockEntityRenderer<ItemPipeEntity> {
    public static boolean adaptiveItemSize;
    BlockEntityRendererFactory.Context context;

    public PipeRenderer(BlockEntityRendererFactory.Context ctx) {
        context = ctx;
    }

    @Override
    public void render(ItemPipeEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        UniqueList<PipedItem> pipedItems = entity.pipedItems;
        pipedItems.forEach(pipedItem -> {
            ItemStack item = pipedItem.getContent();
            matrices.push();
            matrices.translate(pipedItem.x, pipedItem.y, pipedItem.z);
            matrices.scale(0.3f, 0.3f, 0.3f);
            if (adaptiveItemSize) {
                float size = Math.max((float) item.getCount() / item.getMaxCount(), 0.5f);
                matrices.scale(size, size, size);
            }
            MinecraftClient client = MinecraftClient.getInstance();
            matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(client.player.age * 6));
            client.getItemRenderer().renderItem(item, ModelTransformation.Mode.NONE, light, overlay, matrices, vertexConsumers, 0);
            matrices.pop();
        });
        MinecraftClient client = MinecraftClient.getInstance();
        final long handle = client.getWindow().getHandle();
        if (InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_CONTROL) || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_ALT)) {
            HitResult hitResult = client.crosshairTarget;
            if (hitResult instanceof BlockHitResult blockHitResult) {
                final BlockPos entityPos = entity.getPos();
                if (blockHitResult.getBlockPos().equals(entityPos)) {
                    Direction lookingat = blockHitResult.getSide();
                    matrices.push();
                    matrices.scale(0.03f, 0.03f, 0.03f);
                    matrices.translate(16, -16, 16);
                    matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(180));
                    matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(client.player.headYaw - 180));
//                    matrices.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(-client.player.pitch));
                    String string;
                    if (InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_CONTROL))
                        string = lookingat.asString() + "->" + entity.fromTo.get(lookingat);
                    else
                        string = lookingat.getOpposite().asString() + "->" + entity.fromTo.get(lookingat.getOpposite());
                    BlockState obstacle = entity.getWorld().getBlockState(entityPos.offset(lookingat));
                    if (obstacle.isAir() || obstacle.getBlock() == SmartPipes.itemPipeBlock) {
                        if (lookingat.getAxis() == Direction.Axis.Y)
                            matrices.translate(0, -lookingat.getOffsetY() * 16, 0);
                        else {
                            matrices.translate(0, -8, -16);
                        }
                    }
                    context.getTextRenderer().draw(new LiteralText(string), -context.getTextRenderer().getWidth(string) / 2f, -32, 0xffffffff, false, matrices.peek().getPositionMatrix(), vertexConsumers, false, 0x88111111, light);
                    matrices.pop();
                }
            }
        }

        for (Direction from : entity.fromTo.keySet()) {
            Direction to = entity.fromTo.get(from);
            // render yellow glass if non-direct flow
            if (from != to.getOpposite()) {
                BlockState yellow = Blocks.YELLOW_STAINED_GLASS.getDefaultState();
                matrices.push();
                switch (from) {
                    case NORTH -> matrices.translate(0.25, 0.25, 0.3);
                    case EAST -> matrices.translate(0.2, 0.25, 0.25);
                    case WEST -> matrices.translate(0.3, 0.25, 0.25);
                    case UP -> matrices.translate(0.25, 0.2, 0.25);
                    case DOWN -> matrices.translate(0.25, 0.3, 0.25);
                    case SOUTH -> matrices.translate(0.25, 0.25, 0.2);
                }
                matrices.scale(0.5f, 0.5f, 0.5f);
                vertexConsumers.getBuffer(RenderLayer.getTranslucent()).quad(matrices.peek(), client.getBlockRenderManager().getModel(yellow).getQuads(yellow, from, entity.getWorld().random).get(0), 1f, 1f, 1f, light, overlay);
                matrices.pop();
            }

            FilterInventory filterInventory = entity.filters.get(from);
            //render white glass
            if (filterInventory.whitelist) {
                //front face
                matrices.push();
                switch (from) {
                    case NORTH -> matrices.translate(0.25, 0.25, 0.2);
                    case EAST -> matrices.translate(0.3, 0.25, 0.25);
                    case WEST -> matrices.translate(0.2, 0.25, 0.25);
                    case UP -> matrices.translate(0.25, 0.3, 0.25);
                    case DOWN -> matrices.translate(0.25, 0.2, 0.25);
                    case SOUTH -> matrices.translate(0.25, 0.25, 0.3);
                }
                matrices.scale(0.5f, 0.5f, 0.5f);
                final BlockState white = Blocks.WHITE_STAINED_GLASS.getDefaultState();
                vertexConsumers.getBuffer(RenderLayer.getTranslucent()).quad(matrices.peek(), client.getBlockRenderManager().getModel(white).getQuads(white, from, entity.getWorld().random).get(0), 1f, 1f, 1f, light, overlay);
                matrices.pop();

                //back face
                matrices.push();
                switch (from) {
                    case NORTH -> matrices.translate(0.25, 0.25, -0.2);
                    case EAST -> matrices.translate(0.70, 0.25, 0.25);
                    case WEST -> matrices.translate(-0.2, 0.25, 0.25);
                    case UP -> matrices.translate(0.25, 0.70, 0.25);
                    case DOWN -> matrices.translate(0.25, -0.2, 0.25);
                    case SOUTH -> matrices.translate(0.25, 0.25, 0.70);
                }
                matrices.scale(0.5f, 0.5f, 0.5f);
                vertexConsumers.getBuffer(RenderLayer.getTranslucent()).quad(matrices.peek(), client.getBlockRenderManager().getModel(white).getQuads(white, from.getOpposite(), entity.getWorld().random).get(0), 1f, 1f, 1f, light, overlay);
                matrices.pop();
            } else {
                //render black glass if blacklist and is non-empty
                if (!filterInventory.isEmpty()) {
                    matrices.push();
                    switch (from) {
                        case NORTH -> matrices.translate(0.25, 0.25, 0.2);
                        case EAST -> matrices.translate(0.3, 0.25, 0.25);
                        case WEST -> matrices.translate(0.2, 0.25, 0.25);
                        case UP -> matrices.translate(0.25, 0.3, 0.25);
                        case DOWN -> matrices.translate(0.25, 0.2, 0.25);
                        case SOUTH -> matrices.translate(0.25, 0.25, 0.3);
                    }
                    matrices.scale(0.5f, 0.5f, 0.5f);
                    final BlockState black = Blocks.BLACK_STAINED_GLASS.getDefaultState();
                    vertexConsumers.getBuffer(RenderLayer.getTranslucent()).quad(matrices.peek(), client.getBlockRenderManager().getModel(black).getQuads(black, from, entity.getWorld().random).get(0), 1f, 1f, 1f, light, overlay);
                    matrices.pop();
                    //back face
                    matrices.push();
                    switch (from) {
                        case NORTH -> matrices.translate(0.25, 0.25, -0.2);
                        case EAST -> matrices.translate(0.70, 0.25, 0.25);
                        case WEST -> matrices.translate(-0.2, 0.25, 0.25);
                        case UP -> matrices.translate(0.25, 0.70, 0.25);
                        case DOWN -> matrices.translate(0.25, -0.2, 0.25);
                        case SOUTH -> matrices.translate(0.25, 0.25, 0.70);
                    }
                    matrices.scale(0.5f, 0.5f, 0.5f);
                    vertexConsumers.getBuffer(RenderLayer.getTranslucent()).quad(matrices.peek(), client.getBlockRenderManager().getModel(black).getQuads(black, from.getOpposite(), entity.getWorld().random).get(0), 1f, 1f, 1f, light, overlay);
                    matrices.pop();
                }
            }
        }
    }

}
