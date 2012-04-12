package org.getspout.spout.packet.standard;

import net.minecraft.server.Packet51MapChunk;
import org.getspout.spoutapi.packet.standard.MCPacket51MapChunk;

public class MCCraftPacket51MapChunk extends MCCraftPacket implements MCPacket51MapChunk {

    @Override
    public Packet51MapChunk getPacket() {
        return (Packet51MapChunk) packet;
    }

    @Override
    public int getX() {
        return getPacket().a;
    }

    @Override
    public int getZ() {
        return getPacket().b;
    }

    @Override
    public void setX(int x) {
        getPacket().a = x;
    }

    @Override
    public void setZ(int z) {
        getPacket().b = z;
    }

    @Override
    public byte[] getCompressedChunkData() {
        return getPacket().buffer;
    }

    @Override
    public int getSizeX() {
        return 16;
    }

    @Override
    public int getSizeY() {
        return 256;
    }

    @Override
    public int getSizeZ() {
        return 16;
    }

    @Override
    @Deprecated
    public void setSizeX(int x) {
    }

    @Override
    @Deprecated
    public void setSizeY(int y) {
    }

    @Override
    @Deprecated
    public void setSizeZ(int z) {
    }

    @Override
    public int getY() {
        return 0;
    }

    @Override
    @Deprecated
    public void setY(int y) {
    }
}
