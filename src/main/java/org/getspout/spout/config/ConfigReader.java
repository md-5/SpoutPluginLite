package org.getspout.spout.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.getspout.spout.Spout;

public class ConfigReader {

    private static boolean buildCheck = true;
    private static boolean forceClient = false;
    private static int authTicks = 200;
    private static String kickMessage = "This server requires Spoutcraft! http://bit.ly/unleashtheflow";
    private static boolean allowSkyCheat = false;
    private static boolean allowClearWaterCheat = false;
    private static boolean allowStarsCheat = false;
    private static boolean allowWeatherCheat = false;
    private static boolean allowTimeCheat = false;
    private static boolean allowCoordsCheat = false;
    private static boolean allowEntityLabelCheat = false;
    private static boolean allowVoidFogCheat = false;
    private static boolean chunkDataCache = true;
    private static boolean teleportSmoothing = true;
    private static boolean authenticateSpoutcraft = true;
    private static boolean runDeadlockMonitor = false;

    public void read() {
        Spout.getInstance().reloadConfig();
        FileConfiguration configuration = Spout.getInstance().getConfig();
        configuration.options().copyDefaults(true);

        buildCheck = configuration.getBoolean("ForceMinecraftVersionCheck", true);
        forceClient = configuration.getBoolean("ForceSinglePlayerClient", false);
        kickMessage = configuration.getString("ForceSinglePlayerClientKickMessage");
        authTicks = configuration.getInt("AuthenticateTicks", 200);
        allowSkyCheat = configuration.getBoolean("AllowSkyCheat", false);
        allowClearWaterCheat = configuration.getBoolean("AllowClearWaterCheat", false);
        allowStarsCheat = configuration.getBoolean("AllowStarsCheat", false);
        allowWeatherCheat = configuration.getBoolean("AllowWeatherCheat", false);
        allowTimeCheat = configuration.getBoolean("AllowTimeCheat", false);
        allowCoordsCheat = configuration.getBoolean("AllowCoordsCheat", false);
        allowEntityLabelCheat = configuration.getBoolean("AllowEntityLabelCheat", false);
        allowVoidFogCheat = configuration.getBoolean("AllowVoidFogCheat", false);
        chunkDataCache = configuration.getBoolean("ChunkDataCache", true);
        teleportSmoothing = configuration.getBoolean("TeleportSmoothing", true);
        authenticateSpoutcraft = configuration.getBoolean("AuthenticateSpoutcraft", true);
        runDeadlockMonitor = configuration.getBoolean("DeadlockMonitor", false);

        Spout.getInstance().saveConfig();
    }

    public static boolean isBuildCheck() {
        return buildCheck;
    }

    public static boolean isForceClient() {
        return forceClient;
    }

    public static String getKickMessage() {
        return kickMessage;
    }

    public static int getAuthenticateTicks() {
        return authTicks;
    }

    public static boolean isAllowSkyCheat() {
        return allowSkyCheat;
    }

    public static boolean isAllowClearWaterCheat() {
        return allowClearWaterCheat;
    }

    public static boolean isAllowStarsCheat() {
        return allowStarsCheat;
    }

    public static boolean isAllowWeatherCheat() {
        return allowWeatherCheat;
    }

    public static boolean isAllowTimeCheat() {
        return allowTimeCheat;
    }

    public static boolean isAllowVoidFogCheat() {
        return allowVoidFogCheat;
    }

    public static boolean isAllowCoordsCheat() {
        return allowCoordsCheat;
    }

    public static boolean isAllowEntityLabelCheat() {
        return allowEntityLabelCheat;
    }

    public static boolean isChunkDataCache() {
        return chunkDataCache;
    }

    public static boolean isTeleportSmoothing() {
        return teleportSmoothing;
    }

    public static boolean authenticateSpoutcraft() {
        return authenticateSpoutcraft;
    }

    public static boolean runDeadlockMonitor() {
        return runDeadlockMonitor;
    }
}
