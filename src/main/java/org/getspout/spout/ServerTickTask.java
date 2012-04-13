package org.getspout.spout;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.getspout.spout.player.SpoutCraftPlayer;

public class ServerTickTask implements Runnable {

    @Override
    public void run() {
        Spout.getInstance().playerListener.manager.onServerTick();
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (player instanceof SpoutCraftPlayer) {
                ((SpoutCraftPlayer) player).onTick();
            }
        }
    }
}
