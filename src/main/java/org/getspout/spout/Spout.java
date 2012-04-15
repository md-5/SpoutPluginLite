package org.getspout.spout;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.Packet18ArmAnimation;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.getspout.commons.inventory.ItemMap;
import org.getspout.spout.config.ConfigReader;
import org.getspout.spout.keyboard.SimpleKeyBindingManager;
import org.getspout.spout.packet.CustomPacket;
import org.getspout.spout.player.*;
import org.getspout.spout.sound.SimpleSoundManager;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.chunkstore.PlayerTrackingManager;
import org.getspout.spoutapi.chunkstore.SimpleChunkDataManager;
import org.getspout.spoutapi.packet.PacketRenderDistance;
import org.getspout.spoutapi.player.SpoutPlayer;

public class Spout extends JavaPlugin {

    public SpoutPlayerListener playerListener;
    protected final PlayerTrackingManager playerTrackingManager;
    protected SpoutEntityListener entityListener;
    protected PluginListener pluginListener;
    protected static Spout instance;
    protected ItemMap serverItemMap;
    protected List<SpoutPlayer> playersOnline = new ArrayList<SpoutPlayer>();
    protected Thread shutdownThread = null;
    private boolean hardDisable = false;

    public Spout() {
        Spout.instance = this;
        SpoutManager.getInstance().setSoundManager(new SimpleSoundManager());
        SpoutManager.getInstance().setSkyManager(new SimpleSkyManager());
        SpoutManager.getInstance().setPlayerManager(new SimplePlayerManager());
        SpoutManager.getInstance().setBiomeManager(new SimpleBiomeManager());
        SpoutManager.getInstance().setFileManager(new SimpleFileManager());
        SpoutManager.getInstance().setKeyBindingManager(new SimpleKeyBindingManager());
        SpoutManager.getInstance().setWorldManager(new SimpleWorldManager());
        playerTrackingManager = new PlayerTrackingManager();
        shutdownThread = new ShutdownThread();
        Runtime.getRuntime().addShutdownHook(shutdownThread);
    }

    @Override
    public void onDisable() {
        if (hardDisable) {
            Runtime.getRuntime().removeShutdownHook(shutdownThread);
            return;
        }
        //order matters
        ((SimpleSkyManager) SpoutManager.getSkyManager()).reset();
        ((SimplePlayerManager) SpoutManager.getPlayerManager()).onPluginDisable();
        Player[] online = getServer().getOnlinePlayers();
        for (Player player : online) {
            try {
                SpoutCraftPlayer scp = (SpoutCraftPlayer) SpoutCraftPlayer.getPlayer(player);
                scp.resetMovement();
                if (scp.isSpoutCraftEnabled()) {
                    scp.sendPacket(new PacketRenderDistance(true, true));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //for (Player player : online) {
        //	SpoutCraftPlayer.removeBukkitEntity(player);
        //	SpoutCraftPlayer.resetNetServerHandler(player);
        //}

        getServer().getScheduler().cancelTasks(this);

        SimpleFileManager.clearTempDirectory();

        Runtime.getRuntime().removeShutdownHook(shutdownThread);
        super.onDisable();
    }

    @Override
    public void onEnable() {
        new ConfigReader().read();

        if (!hardDisable) {
            playerListener = new SpoutPlayerListener(this);
            pluginListener = new PluginListener(this);
            entityListener = new SpoutEntityListener(this);

            for (SpoutPlayer player : org.getspout.spoutapi.Spout.getServer().getOnlinePlayers()) {
                SpoutCraftPlayer.resetNetServerHandler(player);
                SpoutCraftPlayer.updateNetServerHandler(player);
                SpoutCraftPlayer.updateBukkitEntity(player);
                authenticate(player);
                playerListener.manager.onPlayerJoin(player);
                ((SimplePlayerManager) SpoutManager.getPlayerManager()).onPlayerJoin(player);
                player.setPreCachingComplete(true); //already done if we are already online!
                synchronized (playersOnline) {
                    playersOnline.add(player);
                }
            }

            ((SimplePlayerManager) SpoutManager.getPlayerManager()).onPluginEnable();

            //Start counting ticks
            Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new ServerTickTask(), 0, 1);

            //Remove mappings from previous loads
            //Can not remove them on disable because the packets will still be in the send queue
            CustomPacket.removeClassMapping();
            CustomPacket.addClassMapping();
        }
        setupPermissions();
    }

    /**
     * Initializes Spouts permissions
     */
    private void setupPermissions() {
        String defaults[] = {
            "spout.client.minimap",
            "spout.client.overviewmap",
            "spout.client.sortinventory",
            "spout.client.signcolors",
            "spout.client.chatcolors",};
        PluginManager pm = Bukkit.getPluginManager();
        for (String d : defaults) {
            pm.addPermission(new Permission(d, PermissionDefault.TRUE));
        }
    }

    /**
     * Gets the singleton instance of the Spout plugin
     *
     * @return Spout plugin
     */
    public static Spout getInstance() {
        return instance;
    }

    public PlayerTrackingManager getPlayerTrackingManager() {
        return playerTrackingManager;
    }

    public void authenticate(Player player) {
        if (ConfigReader.authenticateSpoutcraft()) {
            Packet18ArmAnimation packet = new Packet18ArmAnimation();
            packet.a = -42;
            ((SpoutCraftPlayer) SpoutCraftPlayer.getPlayer(player)).getNetServerHandler().networkManager.queue(packet);
        }
    }
}

class ShutdownThread extends Thread {

    public void run() {
        SimpleChunkDataManager dm = (SimpleChunkDataManager) SpoutManager.getChunkDataManager();
        dm.unloadAllChunks();
        dm.closeAllFiles();
    }
}
