package dev.buildtool.smartpipes.client;

import dev.buildtool.satako.gui.*;
import dev.buildtool.smartpipes.ItemPipeEntity;
import dev.buildtool.smartpipes.SmartPipes;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;

/**
 * Created on 8/9/19.
 */
public class PipeDirectionsScreen extends BetterScreen {
    private final ItemPipeEntity pipeEntity;

    ArrayList<ButtonGroup> buttonGroups;
    BetterButton openFilters;

    public PipeDirectionsScreen(ItemPipeEntity pipeEntity) {
        super(new TranslatableText("pipe.directions"));
        this.pipeEntity = pipeEntity;
    }

    @Override
    public void init() {
        super.init();
        buttonGroups = new ArrayList<>(6);
        addDrawable(new Label(centerX - 50, centerY + 10 - height / 2, new LiteralText("Item flow direction"), button -> {
        }, (button, matrices, mouseX, mouseY) -> {
        }));
        Label fromlabel = addDrawable(new Label(20, centerY - 20 - height / 4, new LiteralText("From"), button -> {
        }, (button, matrices, mouseX, mouseY) -> {
        }));
        addDrawable(new Label(centerX - 25, fromlabel.y, new LiteralText("To"), button -> {
        }, (button, matrices, mouseX, mouseY) -> {
        }));

        for (Direction from : pipeEntity.fromTo.keySet()) {
            ButtonGroup nextGroup = new ButtonGroup();
            Direction to = pipeEntity.fromTo.get(from);
            Label fromDirection = new Label(20, centerY + 20 * from.ordinal() - height / 4, new LiteralText(from.getName() + ":"), button -> {
            }, (button, matrices, mouseX, mouseY) -> {
            });
            addDrawable(fromDirection);
            for (Direction value : Direction.values()) {
                if (value != from) {
                    RadioButton radioButton = new RadioButton(fromlabel.x + fromlabel.getWidth() + 45 * value.ordinal(), fromDirection.y - 6, new LiteralText(value.getName()), action -> pipeEntity.fromTo.put(from, value), (button, matrices, mouseX, mouseY) -> {
                    });
                    radioButton.setWidth(45);
                    addDrawableChild(radioButton);
                    nextGroup.add(radioButton);
                    if (value == to)
                        nextGroup.setSelected(radioButton);
                }
            }
            nextGroup.connect();
        }

        openFilters = new BetterButton(centerX - textRenderer.getWidth("Open filters"), height - 30, new LiteralText("Open filters"), action -> {
            PacketByteBuf packetByteBuf = new PacketByteBuf(Unpooled.buffer());
            packetByteBuf.writeBlockPos(pipeEntity.getPos());
            ClientPlayNetworking.send(SmartPipes.openPipeFilters, packetByteBuf);
        }, (button, matrices, mouseX, mouseY) -> {
        });
        addDrawableChild(openFilters);
    }

    @Override
    public void onClose() {
        PacketByteBuf byteBuf = new PacketByteBuf(Unpooled.buffer());
        byteBuf.writeBlockPos(pipeEntity.getPos());
        pipeEntity.fromTo.forEach((direction, direction2) -> {
            byteBuf.writeEnumConstant(direction);
            byteBuf.writeEnumConstant(direction2);
        });
        ClientPlayNetworking.send(SmartPipes.savePipeConfiguration, byteBuf);
        super.onClose();
    }
}
