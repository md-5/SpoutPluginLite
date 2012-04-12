package org.getspout.spout;

import java.util.concurrent.LinkedBlockingDeque;
import org.getspout.spoutapi.packet.CompressablePacket;
import org.getspout.spoutapi.player.SpoutPlayer;

public class PacketCompressionThread extends Thread {

    private static PacketCompressionThread instance = null;
    private static final int QUEUE_CAPACITY = 1024 * 10;
    private final LinkedBlockingDeque<QueuedPacket> queue = new LinkedBlockingDeque<QueuedPacket>(QUEUE_CAPACITY);

    private PacketCompressionThread() {
        super("Spout Packet Compression Thread");
    }

    public static void startThread() {
        instance = new PacketCompressionThread();
        instance.start();
    }

    public static void endThread() {
        instance.interrupt();
        try {
            instance.join();
        } catch (InterruptedException ie) {
        }
        instance = null;
    }

    public static PacketCompressionThread getInstance() {
        return instance;
    }

    public static void add(CompressablePacket packet, SpoutPlayer player) {
        if (instance != null) {
            instance.queue.add(new QueuedPacket(player, packet));
        }
    }

    public void run() {
        while (!isInterrupted()) {
            try {
                QueuedPacket packet = queue.take();
                packet.packet.compress();
                packet.player.sendPacket(packet.packet);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private static class QueuedPacket {

        final CompressablePacket packet;
        final SpoutPlayer player;

        QueuedPacket(SpoutPlayer player, CompressablePacket packet) {
            this.player = player;
            this.packet = packet;
        }
    }
}
