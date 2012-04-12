package org.getspout.spout;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.chunkstore.SimpleChunkDataManager;

public class SpoutWorldMonitorListener implements Listener {

    public SpoutWorldMonitorListener(Spout plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        SimpleChunkDataManager dm = (SimpleChunkDataManager) SpoutManager.getChunkDataManager();
        dm.saveChunk(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldSave(WorldSaveEvent event) {
        SimpleChunkDataManager dm = (SimpleChunkDataManager) SpoutManager.getChunkDataManager();
        dm.saveWorldChunks(event.getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        SimpleChunkDataManager dm = (SimpleChunkDataManager) SpoutManager.getChunkDataManager();
        dm.unloadWorldChunks(event.getWorld());
    }
}
