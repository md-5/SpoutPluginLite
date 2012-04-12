package org.getspout.spout;

import java.lang.reflect.Field;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.*;
import org.bukkit.ChatColor;
import org.getspout.spout.packet.listener.PacketListeners;
import org.getspout.spout.packet.standard.MCCraftPacket;
import org.getspout.spout.player.SpoutCraftPlayer;
import org.getspout.spoutapi.gui.GenericLabel;
import org.getspout.spoutapi.gui.Label;
import org.getspout.spoutapi.gui.RenderPriority;
import org.getspout.spoutapi.player.SpoutPlayer;

public class SpoutNetServerHandler extends NetServerHandler {

    protected Field entityListField = null;
    protected ItemStack lastOverrideDisplayStack = null;
    private MCCraftPacket[] packetWrappers = new MCCraftPacket[256];

    public SpoutNetServerHandler(MinecraftServer minecraftserver, NetworkManager networkmanager, EntityPlayer entityplayer) {
        super(minecraftserver, networkmanager, entityplayer);
        //cache the field for later use
        try {
            entityListField = NetServerHandler.class.getDeclaredField("s");
            entityListField.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

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

    public IntHashMap getEntityList() {
        try {
            return (IntHashMap) entityListField.get(this);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
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

    @Override
    public void sendPacket(Packet packet) {
        if (packet != null) {
            //if (packet.lowPriority) {
            //	MapChunkThread.sendPacket(this.player, packet);
            //} else {
            queueOutputPacket(packet);
            //}
        }
    }
    private LinkedBlockingDeque<Packet> resyncQueue = new LinkedBlockingDeque<Packet>();

    public void queueOutputPacket(Packet packet) {
        if (packet == null) {
            return;
        }
        resyncQueue.addLast(packet);
        if (processingKick.get()) {
            this.syncFlushPacketQueue(new MCCraftPacket[256]);
        }
    }

    public void sendImmediatePacket(Packet packet) {
        if (packet == null) {
            return;
        }

        resyncQueue.addFirst(packet);
    }

    @Override
    public void a() {
        syncFlushPacketQueue();
        super.a();
    }
    AtomicBoolean processingKick = new AtomicBoolean(false);

    @Override
    public void disconnect(String kick) {

        processingKick.set(true); // If any packets are sent while this flag is true, it will flush the sync queue

        super.disconnect(kick);
        if (this.disconnected) {
            syncFlushPacketQueue(new MCCraftPacket[256]);
        }

        processingKick.set(false);
    }

    public void syncFlushPacketQueue() {
        syncFlushPacketQueue(packetWrappers);
    }

    public void syncFlushPacketQueue(MCCraftPacket[] packetWrappers) {
        while (!resyncQueue.isEmpty()) {
            Packet p = resyncQueue.pollFirst();
            if (p != null) {
                syncedSendPacket(p, packetWrappers);
            }
        }
    }

    // Called from the main thread only
    private void syncedSendPacket(Packet packet, MCCraftPacket[] packetWrappers) {
        int packetId = -1;
        try {
            packetId = packet.b();
        } catch (Exception e) {
            return;
        }

        try {
            if (!PacketListeners.canSend(getPlayer(), packet, packetWrappers, packetId)) {
                return;
            } else {
                super.sendPacket(packet);
            }
        } catch (NullPointerException npe) {
            throw new RuntimeException("Null pointer exception thrown when trying to process packet of type " + packet.getClass().getName(), npe);
        }
    }
}

class DecayingLabel extends GenericLabel {

    private int ticksAlive = 0;

    public DecayingLabel(int ticks, String s) {
        super(s);
        ticksAlive = ticks;
    }

    @Override
    public void onTick() {
        ticksAlive--;
        if (ticksAlive < 0) {
            setVisible(false);
        }
    }
}