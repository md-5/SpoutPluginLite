package org.getspout.spout.player;

import gnu.trove.map.hash.TIntObjectHashMap;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.getspout.spoutapi.player.PlayerInformation;
import org.getspout.spoutapi.player.PlayerManager;
import org.getspout.spoutapi.player.SpoutPlayer;

public class SimplePlayerManager implements PlayerManager {

    private Map<String, PlayerInformation> infoMap = new HashMap<String, PlayerInformation>();
    private PlayerInformation globalInfo = new SimplePlayerInformation();
    private TIntObjectHashMap<WeakReference<Entity>> entityIdMap = new TIntObjectHashMap<WeakReference<Entity>>();
    private Map<UUID, WeakReference<Entity>> entityUniqueIdMap = new HashMap<UUID, WeakReference<Entity>>();

    @Override
    public SpoutPlayer getPlayer(Player player) {
        return SpoutCraftPlayer.getPlayer(player);
    }

    @Override
    public SpoutPlayer getPlayer(UUID id) {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (player.getUniqueId().equals(id)) {
                return getPlayer(player);
            }
        }
        return null;
    }

    @Override
    public SpoutPlayer getPlayer(int entityId) {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (player.getEntityId() == entityId) {
                return getPlayer(player);
            }
        }
        return null;
    }

    @Override
    public PlayerInformation getPlayerInfo(Player player) {
        PlayerInformation info = infoMap.get(player.getName());
        if (info == null) {
            info = new SimplePlayerInformation();
            infoMap.put(player.getName(), info);
        }
        return info;
    }

    public void onPlayerJoin(Player player) {
        if (getPlayerInfo(player) == null) {
            infoMap.put(player.getName(), new SimplePlayerInformation());
        }
    }

    public void onPluginEnable() {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            infoMap.put(player.getName(), new SimplePlayerInformation());
        }
    }

    public void onPluginDisable() {
        infoMap.clear();
    }

    @Override
    public PlayerInformation getGlobalInfo() {
        return globalInfo;
    }

    @Override
    public SpoutPlayer[] getOnlinePlayers() {
        Player[] online = Bukkit.getServer().getOnlinePlayers();
        SpoutPlayer[] spoutPlayers = new SpoutPlayer[online.length];
        for (int i = 0; i < online.length; i++) {
            spoutPlayers[i] = getPlayer(online[i]);
        }
        return spoutPlayers;
    }

    @Override
    public void setVersionString(int playerId, String versionString) {
        SpoutPlayer sp = getPlayer(playerId);
        if (sp instanceof SpoutCraftPlayer) {
            SpoutCraftPlayer scp = (SpoutCraftPlayer) sp;
            scp.setVersionString(versionString);
            System.out.println("[Spout] Successfully authenticated " + scp.getName() + "'s Spoutcraft client. Running client version: " + scp.getVersionString());
        }

    }

    @Override
    public Entity getEntity(UUID id) {
        WeakReference<Entity> result = entityUniqueIdMap.get(id);
        Entity found = null;
        if (result != null && result.get() != null) {
            found = result.get();
        } else {
            loop:
            for (World world : Bukkit.getServer().getWorlds()) {
                for (Entity e : world.getEntities()) {
                    if (e.getUniqueId().equals(id)) {
                        found = e;
                        break loop;
                    }
                }
            }
        }
        if (found != null) {
            result = new WeakReference<Entity>(found);
            entityUniqueIdMap.put(id, result);
        }
        return found;
    }

    @Override
    public Entity getEntity(int entityId) {
        WeakReference<Entity> result = entityIdMap.get(entityId);
        Entity found = null;
        if (result != null && result.get() != null) {
            found = result.get();
        } else {
            loop:
            for (World world : Bukkit.getServer().getWorlds()) {
                for (Entity e : world.getEntities()) {
                    if (e.getEntityId() == entityId) {
                        found = e;
                        break loop;
                    }
                }
            }
        }
        if (found != null) {
            result = new WeakReference<Entity>(found);
            entityIdMap.put(entityId, result);
        }
        return found;
    }
}
