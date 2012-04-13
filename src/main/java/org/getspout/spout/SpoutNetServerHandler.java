package org.getspout.spout;

import java.lang.reflect.Field;
import net.minecraft.server.*;
import org.getspout.spout.player.SpoutCraftPlayer;
import org.getspout.spoutapi.player.SpoutPlayer;

public class SpoutNetServerHandler extends NetServerHandler {

    public SpoutNetServerHandler(MinecraftServer minecraftserver, NetworkManager networkmanager, EntityPlayer entityplayer) {
        super(minecraftserver, networkmanager, entityplayer);

        //Lower the active packet queue size in bytes by 9 megabytes, to allow for 10mb of data in a players queue
        try {
            Field x = NetworkManager.class.getDeclaredField("x");
            x.setAccessible(true);
            int size = (Integer) x.get(this.networkManager);
            x.set(this.networkManager, size - 1024 * 1024 * 9);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void a(Packet18ArmAnimation packet) {
        if (packet.a == -42) {
            SpoutCraftPlayer player = (SpoutCraftPlayer) SpoutCraftPlayer.getPlayer(getPlayer());
            player.setBuildVersion(1); //Don't know yet, just set above zero
            try {
                Spout.getInstance().playerListener.manager.onSpoutcraftEnable((SpoutPlayer) getPlayer());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            super.a(packet);
        }
    }

    @Override
    public void a(Packet14BlockDig packet) {
        SpoutCraftPlayer player = (SpoutCraftPlayer) SpoutCraftPlayer.getPlayer(getPlayer());
        boolean inAir = false;
        if (player.canFly() && !player.getHandle().onGround) {
            inAir = true;
            player.getHandle().onGround = true;
        }
        super.a(packet);
        if (inAir) {
            player.getHandle().onGround = false;
        }
    }
}
