package org.getspout.spout;

import java.lang.reflect.Field;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.getspout.spout.player.SimplePlayerManager;
import org.getspout.spout.player.SpoutCraftPlayer;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.SpoutPlayer;

public class SpoutPlayerListener implements Listener {

    public final PlayerManager manager = new PlayerManager();

    public SpoutPlayerListener(Spout plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        if (!event.getPlayer().getClass().equals(SpoutCraftPlayer.class)) {
            SpoutCraftPlayer.updateNetServerHandler(event.getPlayer());
            SpoutCraftPlayer.updateBukkitEntity(event.getPlayer());
            updatePlayerEvent(event);
            Spout.getInstance().authenticate(event.getPlayer());
            SpoutCraftPlayer player = (SpoutCraftPlayer) SpoutCraftPlayer.getPlayer(event.getPlayer());

            //This forces EXISTING players to see the new player's skin, cape, and title
            player.setSkin(player.getSkin());
            player.setCape(player.getCape());
            player.setTitle(player.getTitle());
        }
        ((SimplePlayerManager) SpoutManager.getPlayerManager()).onPlayerJoin(event.getPlayer());
        manager.onPlayerJoin(event.getPlayer());
        synchronized (Spout.getInstance().playersOnline) {
            Spout.getInstance().playersOnline.add((SpoutPlayer) event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerTeleport(final PlayerTeleportEvent event) {
        if (!(event.getPlayer() instanceof SpoutPlayer)) {
            updatePlayerEvent(event);
        }
        if (event.isCancelled()) {
            return;
        }

        Runnable update = null;
        final SpoutCraftPlayer scp = (SpoutCraftPlayer) SpoutCraftPlayer.getPlayer(event.getPlayer());

        if (event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            update = new PostTeleport(scp);
        }
        if (update != null) {
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Spout.getInstance(), update, 2);
        }
    }

    private void updatePlayerEvent(PlayerEvent event) {
        try {
            Field player = PlayerEvent.class.getDeclaredField("player");
            player.setAccessible(true);
            player.set(event, (SpoutCraftPlayer) SpoutCraftPlayer.getPlayer(event.getPlayer()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Spout.getInstance().getPlayerTrackingManager().onPlayerQuit(player);
        synchronized (Spout.getInstance().playersOnline) {
            Spout.getInstance().playersOnline.remove((SpoutPlayer) player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (player instanceof SpoutCraftPlayer) {
            SpoutCraftPlayer scp = (SpoutCraftPlayer) player;
            if (scp.isSpoutCraftEnabled()) {
                Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Spout.getInstance(), new PostRespawn(scp), 2);
            }
        }
    }
}

class PostRespawn implements Runnable {

    SpoutCraftPlayer player;

    public PostRespawn(SpoutCraftPlayer player) {
        this.player = player;
    }

    @Override
    public void run() {
        Spout.getInstance().getPlayerTrackingManager().onPlayerQuit(player);
        Spout.getInstance().getPlayerTrackingManager().onPlayerJoin(player);
    }
}

class PostTeleport implements Runnable {

    SpoutCraftPlayer player;

    public PostTeleport(SpoutCraftPlayer player) {
        this.player = player;
    }

    @Override
    public void run() {
        player.updateAppearance();
    }
}
