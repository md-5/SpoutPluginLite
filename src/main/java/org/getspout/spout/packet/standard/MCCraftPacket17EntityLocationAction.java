package org.getspout.spout.packet.standard;

import net.minecraft.server.Packet17EntityLocationAction;
import org.getspout.spoutapi.packet.standard.MCPacket17;

public class MCCraftPacket17EntityLocationAction extends MCCraftPacket implements MCPacket17 {

    public Packet17EntityLocationAction getPacket() {
        return (Packet17EntityLocationAction) packet;
    }

    public int getBed() {
        return getPacket().b;
    }

    public int getBlockX() {
        return getPacket().c;
    }

    public int getBlockY() {
        return getPacket().d;
    }

    public int getBlockZ() {
        return getPacket().e;
    }

    public int getEntityId() {
        return getPacket().a;
    }

    public void setBed(int bed) {
        getPacket().b = bed;
    }

    public void setBlockX(int x) {
        getPacket().c = x;
    }

    public void setBlockY(int y) {
        getPacket().d = y;
    }

    public void setBlockZ(int z) {
        getPacket().e = z;
    }

    public void setEntityId(int id) {
        getPacket().a = id;
    }
}
