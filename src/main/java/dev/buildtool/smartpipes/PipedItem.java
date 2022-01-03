package dev.buildtool.smartpipes;

import dev.buildtool.satako.NBTWriter;
import dev.buildtool.satako.api.TagConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Direction;

import java.util.Objects;

/**
 * Created on 8/6/19.
 */
public class PipedItem implements TagConvertible<PipedItem> {
    public static final String ALIGNED = "Aligned";
    private ItemStack content = ItemStack.EMPTY;
    private static final String STACK = "ItemStack", X = "x", Y = "y", Z = "z", FROM = "From side", TO = "To side", ID = "Id";
    public float x, y, z;
    public Direction to, from;
    public int id;
    public short rotation;
    boolean aligned;

    @Override
    public String toString() {
        return "PipedItem{" +
                "content=" + content +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", to=" + to +
                ", from=" + from +
                ", id=" + id +
                ", rotation=" + rotation +
                ", aligned=" + aligned +
                '}';
    }

    PipedItem() {

    }

    public PipedItem(ItemStack content, float x, float y, float z, int id, short rotation, Direction to_) {
        this.content = content;
        this.x = x;
        this.y = y;
        this.z = z;
        this.id = id;
        this.rotation = rotation;
        to = to_;
    }

    public ItemStack getContent() {
        return content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PipedItem pipedItem = (PipedItem) o;
        return id == pipedItem.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public NbtCompound writeToTag() {
        NbtCompound tag = new NbtCompound();
        NBTWriter writer = new NBTWriter(tag).setFloat(X, x).setFloat(Y, y).setFloat(Z, z).setInt(ID, id).setBoolean(ALIGNED, aligned);
        NbtCompound stack = content.writeNbt(new NbtCompound());
        tag.put(STACK, stack);
        tag.putByte(TO, (byte) to.ordinal());
        tag.putByte(FROM, (byte) from.ordinal());
        return writer.getResult();
    }

    @Override
    public PipedItem readFromTag(NbtCompound tag) {
        content = ItemStack.fromNbt(tag.getCompound(STACK));
        x = tag.getFloat(X);
        y = tag.getFloat(Y);
        z = tag.getFloat(Z);
        to = Direction.values()[tag.getByte(TO)];
        from = Direction.values()[tag.getByte(FROM)];
        id = tag.getInt(ID);
        aligned = tag.getBoolean(ALIGNED);
        return this;
    }

    public PipedItem copy() {
        final PipedItem pipedItem = new PipedItem(content, x, y, z, id, rotation, to);
        pipedItem.from = from;
        return pipedItem;
    }


}
