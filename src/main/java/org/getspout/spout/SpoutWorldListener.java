package org.getspout.spout;

import java.lang.reflect.Field;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.getspout.spout.block.SpoutCraftChunk;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.chunkstore.SimpleChunkDataManager;

public class SpoutWorldListener implements Listener {

    public SpoutWorldListener(Spout plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (SpoutCraftChunk.replaceBukkitChunk(event.getChunk())) {
            //update the reference to the chunk in the event
            try {
                Field chunk = ChunkEvent.class.getDeclaredField("chunk");
                chunk.setAccessible(true);
                chunk.set(event, event.getChunk().getWorld().getChunkAt(event.getChunk().getX(), event.getChunk().getZ()));
            } catch (Exception e) {
                e.printStackTrace();
            }

            SimpleChunkDataManager dm = (SimpleChunkDataManager) SpoutManager.getChunkDataManager();
            dm.loadChunk(event.getChunk());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWorldLoad(WorldLoadEvent event) {
        SimpleChunkDataManager dm = (SimpleChunkDataManager) SpoutManager.getChunkDataManager();
        dm.loadWorldChunks(event.getWorld());
    }
}
