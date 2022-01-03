package dev.buildtool.smartpipes.client;

import dev.buildtool.satako.Constants;
import dev.buildtool.satako.gui.BetterButton;
import dev.buildtool.satako.gui.InventoryScreen;
import dev.buildtool.satako.gui.Label;
import dev.buildtool.satako.gui.SwitchButton;
import dev.buildtool.smartpipes.FilterInventory;
import dev.buildtool.smartpipes.ItemPipeEntity;
import dev.buildtool.smartpipes.PipeScreenHandler;
import dev.buildtool.smartpipes.SmartPipes;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class PipeFilterScreen extends InventoryScreen<PipeScreenHandler> {
    private final ItemPipeEntity entity;

    public PipeFilterScreen(PipeScreenHandler container, PlayerInventory playerInventory, Text name) {
        super(container, playerInventory, name, true);
        entity = container.itemPipeEntity;
    }

    @Override
    protected void init() {
        super.init();
        int i = 0;
        for (Map.Entry<Direction, FilterInventory> directionDefaultInventoryEntry : entity.filters.entrySet()) {
            Direction direction = directionDefaultInventoryEntry.getKey();
            FilterInventory filter = directionDefaultInventoryEntry.getValue();
            Label label = new Label(centerX - 180, centerY - 94 + Constants.SLOT_WITH_BORDER_SIZE * i, new LiteralText(StringUtils.capitalize(direction.getName())), button -> {
            }, (button, matrices, mouseX, mouseY) -> {
            });
            addDrawable(label);

            addDrawableChild(new SwitchButton(label.x + 40, label.y - 6, new LiteralText("Whitelist"), new LiteralText("Blacklist"), filter.whitelist, action -> {
                PacketByteBuf packetByteBuf = new PacketByteBuf(Unpooled.buffer());
                packetByteBuf.writeBlockPos(entity.getPos());
                packetByteBuf.writeEnumConstant(direction);
                packetByteBuf.writeBoolean(!filter.whitelist);
                ClientPlayNetworking.send(SmartPipes.toggleFilterMode, packetByteBuf);
                filter.whitelist = !filter.whitelist;
                SwitchButton switchButton = (SwitchButton) action;
                switchButton.state = !switchButton.state;
                entity.markDirty();
            }));
            addDrawableChild(new SwitchButton(label.x + 15 * Constants.SLOT_WITH_BORDER_SIZE, label.y - 6, new LiteralText("Hold"), new LiteralText("Pass"), filter.holdItems, action -> {
                PacketByteBuf packetByteBuf = new PacketByteBuf(Unpooled.buffer());
                packetByteBuf.writeBlockPos(entity.getPos());
                packetByteBuf.writeEnumConstant(direction);
                filter.holdItems = !filter.holdItems;
                packetByteBuf.writeBoolean(filter.holdItems);
                ClientPlayNetworking.send(SmartPipes.toggleFilterItemHolding, packetByteBuf);
                SwitchButton switchButton = (SwitchButton) action;
                switchButton.state = !switchButton.state;
                entity.markDirty();
            }));
            i++;
        }
        addDrawableChild(new BetterButton(20, height - 40, new LiteralText("Go back"), var1 -> MinecraftClient.getInstance().setScreen(new PipeDirectionsScreen(entity)), (button, matrices, mouseX, mouseY) -> {
        }));
    }
}
