package org.getspout.spout;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.getspout.spout.block.SpoutCraftChunk;
import org.getspout.spout.inventory.SimpleMaterialManager;
import org.getspout.spout.player.SpoutCraftPlayer;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.event.spout.ServerTickEvent;

public class ServerTickTask implements Runnable {

    @Override
    public void run() {
        Spout.getInstance().playerListener.manager.onServerTick();
        ((SimpleMaterialManager) SpoutManager.getMaterialManager()).onTick();
        Player[] online = Bukkit.getServer().getOnlinePlayers();
        for (Player player : online) {
            if (player instanceof SpoutCraftPlayer) {
                ((SpoutCraftPlayer) player).onTick();
            }
        }
        SpoutCraftChunk.updateTicks();
        ServerTickEvent event = new ServerTickEvent();
        Bukkit.getServer().getPluginManager().callEvent(event);
    }
}
