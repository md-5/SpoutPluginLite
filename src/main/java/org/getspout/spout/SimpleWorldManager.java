package org.getspout.spout;

import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.getspout.spoutapi.WorldManager;

public class SimpleWorldManager implements WorldManager {

    @Override
    public int getWorldHeightBits(World world) {
        if (world instanceof CraftWorld) {
            return 0x8;
        }
        return 7;
    }

    @Override
    public int getWorldXShiftBits(World world) {
        if (world instanceof CraftWorld) {
            return 0xC;
        }
        return 11;
    }

    @Override
    public int getWorldZShiftBits(World world) {
        return getWorldHeightBits(world);
    }
}
