package org.getspout.spout;

import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.getspout.spout.config.ConfigReader;
import org.getspout.spout.keyboard.SimpleKeyBindingManager;
import org.getspout.spout.packet.CustomPacket;
import org.getspout.spout.player.SimpleBiomeManager;
import org.getspout.spout.player.SimpleFileManager;
import org.getspout.spout.player.SimpleSkyManager;
import org.getspout.spout.player.SpoutCraftPlayer;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.event.spout.SpoutCraftEnableEvent;
import org.getspout.spoutapi.event.spout.SpoutcraftFailedEvent;
import org.getspout.spoutapi.material.CustomBlock;
import org.getspout.spoutapi.material.MaterialData;
import org.getspout.spoutapi.packet.*;
import org.getspout.spoutapi.player.SpoutPlayer;

public class PlayerManager {

    private HashMap<String, Integer> timer = new HashMap<String, Integer>();

    public void onPlayerJoin(Player player) {
        timer.put(player.getName(), ConfigReader.getAuthenticateTicks());
    }

    public void onServerTick() {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (timer.containsKey(player.getName())) {
                int ticksLeft = timer.get(player.getName());
                if (--ticksLeft > 0) {
                    timer.put(player.getName(), ticksLeft);
                } else {
                    timer.remove(player.getName());
                    SpoutCraftPlayer scp = (SpoutCraftPlayer) SpoutManager.getPlayer(player);
                    Bukkit.getServer().getPluginManager().callEvent(new SpoutcraftFailedEvent(scp));
                    scp.queued = null;
                    if (ConfigReader.isForceClient()) {
                        System.out.println("[Spout] Failed to authenticate " + player.getName() + "'s Spoutcraft client in " + ConfigReader.getAuthenticateTicks() + " server ticks.");
                        System.out.println("[Spout] Kicking " + player.getName() + " for not running Spoutcraft");
                        player.kickPlayer(ConfigReader.getKickMessage());
                    }
                }
            }
        }
    }

    public void onSpoutcraftEnable(SpoutPlayer player) {
        timer.remove(player.getName());
        player.sendPacket(new PacketServerPlugins(Bukkit.getServer().getPluginManager().getPlugins()));

        ((SpoutCraftPlayer) player).updateAppearance();
        ((SimpleSkyManager) SpoutManager.getSkyManager()).onPlayerJoin(player);
        ((SimpleBiomeManager) SpoutManager.getBiomeManager()).onPlayerJoin(player);
        ((SimpleFileManager) SpoutManager.getFileManager()).onPlayerJoin(player);
        ((SimpleKeyBindingManager) SpoutManager.getKeyBindingManager()).onPlayerJoin(player);
        player.sendPacket(new PacketAllowVisualCheats(ConfigReader.isAllowSkyCheat(), ConfigReader.isAllowClearWaterCheat(), ConfigReader.isAllowStarsCheat(), ConfigReader.isAllowWeatherCheat(), ConfigReader.isAllowTimeCheat(), ConfigReader.isAllowCoordsCheat(), ConfigReader.isAllowEntityLabelCheat(), ConfigReader.isAllowVoidFogCheat()));

        for (CustomBlock block : MaterialData.getCustomBlocks()) {
            player.sendPacket(new PacketCustomBlockDesign((short) block.getCustomId(), block.getBlockDesign()));
        }

        PacketCacheHashUpdate p = new PacketCacheHashUpdate();
        p.reset = true;
        ((SpoutCraftPlayer) player).getNetServerHandler().sendPacket(new CustomPacket(p));

        Spout.getInstance().getPlayerTrackingManager().onPlayerJoin(player);

        player.sendPacket(new PacketBlockData(SpoutManager.getMaterialManager().getModifiedBlocks()));
        Bukkit.getServer().getPluginManager().callEvent(new SpoutCraftEnableEvent(player));
    }
}
