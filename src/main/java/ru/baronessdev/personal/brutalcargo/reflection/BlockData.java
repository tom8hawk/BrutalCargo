package ru.baronessdev.personal.brutalcargo.reflection;

import lombok.SneakyThrows;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class BlockData {
    public short data;
    public Material material;

    /**
     * Constructs a new BlockData representing the type of the given block
     *
     * @param b - Target Block
     */
    public BlockData(Block b) {
        material = b.getType();
    }

    /**
     * Sets the given blocks type and id to the one stored by this BlockData with allowing Minecraft physics calculations.
     *
     * @param b - Setable block
     */
    @SneakyThrows
    public void setBlock(Block b) {
        b.setType(material);
        Reflection.getMethod(Block.class, "setData", byte.class).invoke(b, (byte) data);
    }
}
