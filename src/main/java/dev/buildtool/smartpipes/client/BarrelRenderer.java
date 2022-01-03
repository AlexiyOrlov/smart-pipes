package dev.buildtool.smartpipes.client;

import dev.buildtool.smartpipes.BarrelBlock;
import dev.buildtool.smartpipes.BarrelEntity;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3f;

public class BarrelRenderer implements BlockEntityRenderer<BarrelEntity> {
    BlockEntityRendererFactory.Context context;

    public BarrelRenderer(BlockEntityRendererFactory.Context context) {
        this.context = context;
    }

    @Override
    public void render(BarrelEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        BlockState blockState = entity.getCachedState();
        Direction direction = blockState.get(BarrelBlock.FACING);
        int amount = entity.itemCountIndicator;
        if (amount > 0) {
            matrices.push();
            //translation goes before rotation
            //move to center
            matrices.translate(0.5, 0, 0.5);
            //move to front
            matrices.translate(direction.getOffsetX() * 0.5, 0, direction.getOffsetZ() * 0.5);
            matrices.translate(0.01 * direction.getOffsetX(), 0, 0.01 * direction.getOffsetZ());
            //rotate
            matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(180));
            matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(direction.asRotation()));

            matrices.scale(0.03f, 0.03f, 0);
            context.getTextRenderer().draw(matrices, amount + "", -context.getTextRenderer().getWidth(amount + "") / 2f, -13, 0xffffffff);
            matrices.pop();
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        ItemStack firstItem = entity.contentIndicator;
        matrices.push();
        matrices.multiply(direction.getRotationQuaternion());
        matrices.multiply(new Quaternion(-90f, 0f, 0f, true));
        switch (direction.getName()) {
            case "west" -> matrices.translate(0.5, 0.7, 0.01);
            case "north" -> matrices.translate(-0.5, 0.7, 0.01);
            case "south" -> matrices.translate(0.5, 0.7, 1.01);
            case "east" -> matrices.translate(-0.5, 0.7, 1.01);
        }
        matrices.scale(0.48f, 0.48f, 0.00f);
        if (!firstItem.isEmpty()) {
            matrices.push();
            int lightmapCoordinates = WorldRenderer.getLightmapCoordinates(entity.getWorld(), entity.getPos().offset(direction));
            minecraftClient.getItemRenderer().renderItem(firstItem, ModelTransformation.Mode.GUI, lightmapCoordinates, overlay, matrices, vertexConsumers, 0);
            matrices.pop();
        }
        matrices.pop();
    }
}
