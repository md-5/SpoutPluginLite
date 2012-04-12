package org.getspout.spout;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.SpoutPlayer;

public class PluginListener implements Listener {

    public PluginListener(Spout plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        SpoutManager.getKeyboardManager().removeAllKeyBindings(event.getPlugin());
        for (Player i : Bukkit.getServer().getOnlinePlayers()) {
            SpoutPlayer p = SpoutManager.getPlayer(i);
            p.getMainScreen().removeWidgets(event.getPlugin());
        }
    }
}
