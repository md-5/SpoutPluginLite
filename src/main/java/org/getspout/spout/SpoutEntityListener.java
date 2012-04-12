package org.getspout.spout;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;

import org.getspout.spoutapi.block.SpoutBlock;
import org.getspout.spoutapi.player.SpoutPlayer;

public class SpoutEntityListener implements Listener {

    public SpoutEntityListener(Spout plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof SpoutPlayer) {
            event.setCancelled(event.isCancelled() || !((SpoutPlayer) event.getEntity()).isPreCachingComplete());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getTarget() instanceof SpoutPlayer) {
            event.setCancelled(event.isCancelled() || !((SpoutPlayer) event.getTarget()).isPreCachingComplete());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            SpoutBlock sb = (SpoutBlock) block;
            sb.removeCustomBlockData();
        }
    }
}
