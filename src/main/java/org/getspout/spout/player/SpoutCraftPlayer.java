package org.getspout.spout.player;

import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.server.*;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.getspout.commons.io.CRCStore.URLCheck;
import org.getspout.commons.io.CRCStoreRunnable;
import org.getspout.spout.Spout;
import org.getspout.spout.SpoutNetServerHandler;
import org.getspout.spout.packet.CustomPacket;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.event.permission.PlayerPermissionEvent;
import org.getspout.spoutapi.gui.*;
import org.getspout.spoutapi.keyboard.Keyboard;
import org.getspout.spoutapi.packet.*;
import org.getspout.spoutapi.packet.standard.MCPacket;
import org.getspout.spoutapi.player.EntitySkinType;
import org.getspout.spoutapi.player.PlayerInformation;
import org.getspout.spoutapi.player.RenderDistance;
import org.getspout.spoutapi.player.SpoutPlayer;

public class SpoutCraftPlayer extends CraftPlayer implements SpoutPlayer {

    protected Keyboard forward = Keyboard.KEY_UNKNOWN;
    protected Keyboard back = Keyboard.KEY_UNKNOWN;
    protected Keyboard left = Keyboard.KEY_UNKNOWN;
    protected Keyboard right = Keyboard.KEY_UNKNOWN;
    protected Keyboard jump = Keyboard.KEY_UNKNOWN;
    protected Keyboard inventoryKey = Keyboard.KEY_UNKNOWN;
    protected Keyboard drop = Keyboard.KEY_UNKNOWN;
    protected Keyboard chat = Keyboard.KEY_UNKNOWN;
    protected Keyboard togglefog = Keyboard.KEY_UNKNOWN;
    protected Keyboard sneak = Keyboard.KEY_UNKNOWN;
    private int buildVersion = -1;
    public RenderDistance currentRender = null;
    protected RenderDistance maximumRender = null;
    protected RenderDistance minimumRender = null;
    protected String clipboard = null;
    protected InGameScreen mainScreen;
    protected Permissible perm;
    private double gravityMod = 1;
    private double swimmingMod = 1;
    private double walkingMod = 1;
    private double jumpingMod = 1;
    private double airspeedMod = 1;
    private boolean fly;
    private String versionString = "not set";
    private Location lastClicked = null;
    private boolean precachingComplete = false;
    private ScreenType activeScreen = ScreenType.GAME_SCREEN;
    private GenericOverlayScreen currentScreen = null;
    private Location lastTickLocation = null;
    public org.bukkit.Chunk lastTickChunk = null;
    public Set<org.bukkit.Chunk> lastTickAdjacentChunks = new HashSet<org.bukkit.Chunk>();
    private boolean screenOpenThisTick = false;
    public LinkedList<SpoutPacket> queued = new LinkedList<SpoutPacket>();
    private LinkedList<SpoutPacket> delayedPackets = new LinkedList<SpoutPacket>();
    public long velocityAdjustmentTime = System.currentTimeMillis();
    private GameMode prevMode;
    private Map<String, String> addons;

    public SpoutCraftPlayer(CraftServer server, EntityPlayer entity) {
        super(server, entity);
        if (entity.netServerHandler != null) {
            CraftPlayer player = entity.netServerHandler.getPlayer();
            perm = new PermissibleBase(player.addAttachment(Bukkit.getServer().getPluginManager().getPlugin("Spout")).getPermissible());
            perm.recalculatePermissions();
        } else {
            perm = new PermissibleBase(this);
            perm.recalculatePermissions();
        }
        mainScreen = new InGameScreen(this.getEntityId());

        mainScreen.toggleSurvivalHUD(!getGameMode().equals(GameMode.CREATIVE));
        prevMode = getGameMode();
        fly = ((CraftServer) Bukkit.getServer()).getHandle().server.allowFlight;
    }


    /*
     * Interace Overriden Public Methods
     */
    @Override
    public boolean isPermissionSet(String name) {
        return perm.isPermissionSet(name);
    }

    @Override
    public boolean isPermissionSet(Permission perm) {
        return this.perm.isPermissionSet(perm);
    }

    @Override
    public boolean hasPermission(String name) {
        boolean defaultResult = this.perm.hasPermission(name);
        PlayerPermissionEvent event = new PlayerPermissionEvent(this, name, Bukkit.getServer().getPluginManager().getPermission(name), defaultResult);
        Bukkit.getServer().getPluginManager().callEvent(event);
        return event.getResult();
    }

    @Override
    public boolean hasPermission(Permission perm) {
        boolean defaultResult = this.perm.hasPermission(perm);
        PlayerPermissionEvent event = new PlayerPermissionEvent(this, perm.getName(), perm, defaultResult);
        Bukkit.getServer().getPluginManager().callEvent(event);
        return event.getResult();
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
        return perm.addAttachment(plugin, name, value);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        return perm.addAttachment(plugin);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
        return perm.addAttachment(plugin, name, value, ticks);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
        return perm.addAttachment(plugin, ticks);
    }

    @Override
    public void removeAttachment(PermissionAttachment attachment) {
        perm.removeAttachment(attachment);
    }

    @Override
    public void recalculatePermissions() {
        perm.recalculatePermissions();
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return perm.getEffectivePermissions();
    }

    @Override
    public void setMaximumAir(int time) {
        if (time <= 0) {
            throw new IllegalArgumentException("The Maximum Air can not be below 1");
        }
        if (isSpoutCraftEnabled()) {
            sendPacket(new PacketAirTime(time, this.getRemainingAir()));
        }
        super.setMaximumAir(time);
    }

    @Override
    public void setRemainingAir(int time) {
        if (time < 0) {
            throw new IllegalArgumentException("The Remaining Air can not be below 0");
        }
        if (isSpoutCraftEnabled()) {
            sendPacket(new PacketAirTime(this.getMaximumAir(), time));
        }
        super.setRemainingAir(time);
    }

    @Override
    public void setVelocity(Vector velocity) {
        super.setVelocity(velocity);
        if (isSpoutCraftEnabled()) {
            PlayerVelocityEvent event = new PlayerVelocityEvent(this, velocity);
            Bukkit.getServer().getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                sendPacket(new PacketSetVelocity(getEntityId(), event.getVelocity().getX(), event.getVelocity().getY(), event.getVelocity().getZ()));
            }
            double speedX = Math.abs(event.getVelocity().getX() * event.getVelocity().getX());
            double speedY = Math.abs(event.getVelocity().getY() * event.getVelocity().getY());
            double speedZ = Math.abs(event.getVelocity().getZ() * event.getVelocity().getZ());
            double speed = speedX + speedY + speedZ;

            velocityAdjustmentTime = System.currentTimeMillis() + (long) (speed * 5);
            getHandle().velocityChanged = false; //prevents nms from sending an override packet later, but still tells the server about the new velocity
        }
    }

    @Override
    public InGameScreen getMainScreen() {
        return mainScreen;
    }

    @Override
    public Screen getCurrentScreen() {
        if (getActiveScreen() == ScreenType.GAME_SCREEN) {
            return getMainScreen();
        } else {
            return currentScreen;
        }
    }

    @Override
    public boolean isSpoutCraftEnabled() {
        return getBuildVersion() > -1;
    }

    @Override
    public Keyboard getForwardKey() {
        return forward;
    }

    @Override
    public Keyboard getBackwardKey() {
        return back;
    }

    @Override
    public Keyboard getLeftKey() {
        return left;
    }

    @Override
    public Keyboard getRightKey() {
        return right;
    }

    @Override
    public Keyboard getJumpKey() {
        return jump;
    }

    @Override
    public Keyboard getInventoryKey() {
        return inventoryKey;
    }

    @Override
    public Keyboard getDropItemKey() {
        return drop;
    }

    @Override
    public Keyboard getChatKey() {
        return chat;
    }

    @Override
    public Keyboard getToggleFogKey() {
        return togglefog;
    }

    @Override
    public Keyboard getSneakKey() {
        return sneak;
    }

    @Override
    public RenderDistance getRenderDistance() {
        return currentRender;
    }

    @Override
    public void setRenderDistance(RenderDistance distance) {
        if (isSpoutCraftEnabled()) {
            currentRender = distance;
            sendPacket(new PacketRenderDistance(distance, null, null));
        }
    }

    @Override
    public void setRenderDistance(RenderDistance distance, boolean update) {
        if (update) {
            setRenderDistance(distance);
        } else {
            currentRender = distance;
        }
    }

    @Override
    public RenderDistance getMaximumRenderDistance() {
        return maximumRender;
    }

    @Override
    public void setMaximumRenderDistance(RenderDistance maximum) {
        if (isSpoutCraftEnabled()) {
            maximumRender = maximum;
            sendPacket(new PacketRenderDistance(null, maximum, null));
        }
    }

    @Override
    public void resetMaximumRenderDistance() {
        if (isSpoutCraftEnabled()) {
            maximumRender = null;
            sendPacket(new PacketRenderDistance(true, false));
        }
    }

    @Override
    public RenderDistance getMinimumRenderDistance() {
        return minimumRender;
    }

    @Override
    public void setMinimumRenderDistance(RenderDistance minimum) {
        if (isSpoutCraftEnabled()) {
            minimumRender = minimum;
            sendPacket(new PacketRenderDistance(null, null, minimum));
        }
    }

    @Override
    public void resetMinimumRenderDistance() {
        if (isSpoutCraftEnabled()) {
            minimumRender = null;
            sendPacket(new PacketRenderDistance(false, true));
        }
    }

    @Override
    public void sendNotification(String title, String message, Material toRender) {
        if (isSpoutCraftEnabled()) {
            if (toRender == null || toRender == Material.AIR) {
                throw new IllegalArgumentException("The item to render may not be null or air");
            }
            if (ChatColor.stripColor(title).length() > 26 || title.length() > 78) {
                throw new UnsupportedOperationException("Notification titles can not be greater than 26 chars + 26 colors");
            }
            if (ChatColor.stripColor(message).length() > 26 || message.length() > 78) {
                throw new UnsupportedOperationException("Notification messages can not be greater than 26 chars + 26 colors");
            }
            sendPacket(new PacketAlert(title, message, toRender.getId()));
        }
    }

    @Override
    public void sendNotification(String title, String message, Material toRender, short data, int time) {
        if (isSpoutCraftEnabled()) {
            if (toRender == null || toRender == Material.AIR) {
                throw new IllegalArgumentException("The item to render may not be null or air");
            }
            if (ChatColor.stripColor(title).length() > 26 || title.length() > 78) {
                throw new UnsupportedOperationException("Notification titles can not be greater than 26 chars + 26 colors");
            }
            if (ChatColor.stripColor(message).length() > 26 || message.length() > 78) {
                throw new UnsupportedOperationException("Notification messages can not be greater than 26 chars + 26 colors");
            }
            sendPacket(new PacketNotification(title, message, toRender.getId(), data, time));
        }
    }

    @Override
    public void sendNotification(String title, String message, ItemStack item, int time) {
        if (isSpoutCraftEnabled()) {
            if (item == null || item.getTypeId() == Material.AIR.getId()) {
                throw new IllegalArgumentException("The item to render may not be null or air");
            }
            if (ChatColor.stripColor(title).length() > 26 || title.length() > 78) {
                throw new UnsupportedOperationException("Notification titles can not be greater than 26 chars + 26 colors");
            }
            if (ChatColor.stripColor(message).length() > 26 || message.length() > 78) {
                throw new UnsupportedOperationException("Notification messages can not be greater than 26 chars + 26 colors");
            }
            sendPacket(new PacketNotification(title, message, item.getTypeId(), item.getDurability(), time));
        }
    }

    @Override
    public String getClipboardText() {
        return clipboard;
    }

    @Override
    public void setClipboardText(String text) {
        setClipboardText(text, true);
    }

    @Override
    public void setTexturePack(String url) {
        if (isSpoutCraftEnabled()) {
            if (url == null || url.length() < 5) {
                throw new IllegalArgumentException("Invalid URL!");
            }
            if (!url.toLowerCase().endsWith(".zip")) {
                throw new IllegalArgumentException("A Texture Pack must be in a .zip format");
            }
            final String finalURL = url;
            URLCheck urlCheck = new URLCheck(url, new byte[16384], new CRCStoreRunnable() {

                Long CRC;

                @Override
                public void setCRC(Long CRC) {
                    this.CRC = CRC;
                }

                @Override
                public void run() {
                    sendPacket(new PacketTexturePack(finalURL, CRC));
                }
            });
            urlCheck.start();
        }
    }

    @Override
    public void resetTexturePack() {
        if (isSpoutCraftEnabled()) {
            sendPacket(new PacketTexturePack("[none]", 0));
        }
    }

    @Override
    public void setClipboardText(String text, boolean updateClient) {
        if (isSpoutCraftEnabled()) {
            clipboard = text;
            if (updateClient) {
                sendPacket(new PacketClipboardText(text));
            }
        }
    }

    @Override
    public Location getActiveInventoryLocation() {
        return null;
    }

    @Override
    public void setActiveInventoryLocation(Location loc) {
    }

    @Override
    public void reconnect(String hostname) {
        reconnect(null, hostname);
    }

    @Override
    public void reconnect(String hostname, int port) {
        reconnect(null, hostname, port);
    }

    @Override
    public void reconnect(String message, String hostname, int port) {
        if (hostname.indexOf(":") != -1) {
            throw new IllegalArgumentException("Hostnames may not contain the : symbol");
        }
        if (message == null) {
            message = "[Redirect] Please reconnect to";
        } else if (message.indexOf(":") != -1) {
            throw new IllegalArgumentException("Kick messages may not contain the : symbol");
        }
        if (port == 25565) {
            this.kickPlayer("[Redirect] Please reconnect to : " + hostname);
        } else {
            this.kickPlayer("[Redirect] Please reconnect to : " + hostname + ":" + port);
        }
    }

    @Override
    public void reconnect(String message, String hostname) {
        if (hostname.indexOf(":") != -1) {
            String[] split = hostname.split(":");
            if (split.length != 2) {
                throw new IllegalArgumentException("Improperly formatted hostname: " + hostname);
            }
            int port;
            try {
                port = Integer.parseInt(split[1]);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Unable to parse port number: " + split[1] + " in " + hostname);
            }
            reconnect(message, split[0], port);
        } else {
            reconnect(message, hostname, 25565);
        }
    }

    @Override
    public PlayerInformation getInformation() {
        return SpoutManager.getPlayerManager().getPlayerInfo(this);
    }

    @Override
    public ScreenType getActiveScreen() {
        return activeScreen;
    }

    @Override
    public void openScreen(ScreenType type) {
        openScreen(type, true);
    }

    @Override
    public void sendScreenshotRequest() {
        PacketScreenshot packets = new PacketScreenshot();
        sendPacket(packets);
    }

    @Override
    public void openScreen(ScreenType type, boolean packet) {
        if (type == activeScreen || screenOpenThisTick) {
            return;
        }
        screenOpenThisTick = packet;
        activeScreen = type;
        if (packet) {
            sendPacket(new PacketOpenScreen(type));
        }
        if (activeScreen != ScreenType.GAME_SCREEN && activeScreen != ScreenType.CUSTOM_SCREEN) {
            currentScreen = (GenericOverlayScreen) new GenericOverlayScreen(getEntityId(), getActiveScreen()).setX(0).setY(0);
            PacketWidget packetw = new PacketWidget(currentScreen, currentScreen.getId());
            sendPacket(packetw);
            currentScreen.onTick();
        } else {
            currentScreen = null;
        }
    }

    @Override
    public double getGravityMultiplier() {
        return gravityMod;
    }

    @Override
    public double getSwimmingMultiplier() {
        return swimmingMod;
    }

    @Override
    public double getWalkingMultiplier() {
        return walkingMod;
    }

    @Override
    public void setGravityMultiplier(double multiplier) {
        gravityMod = multiplier;
        updateMovement();
    }

    @Override
    public void setSwimmingMultiplier(double multiplier) {
        swimmingMod = multiplier;
        updateMovement();
    }

    @Override
    public void setWalkingMultiplier(double multiplier) {
        walkingMod = multiplier;
        updateMovement();
    }

    @Override
    public double getJumpingMultiplier() {
        return jumpingMod;
    }

    @Override
    public void setJumpingMultiplier(double multiplier) {
        this.jumpingMod = multiplier;
        updateMovement();
    }

    @Override
    public double getAirSpeedMultiplier() {
        return airspeedMod;
    }

    @Override
    public void setAirSpeedMultiplier(double multiplier) {
        airspeedMod = multiplier;
        updateMovement();

    }

    @Override
    public void resetMovement() {
        gravityMod = 1;
        walkingMod = 1;
        swimmingMod = 1;
        jumpingMod = 1;
        airspeedMod = 1;
        updateMovement();
    }

    @Override
    public boolean canFly() {
        return fly;
    }

    @Override
    public void setCanFly(boolean fly) {
        this.fly = fly;
    }

    @Override
    public boolean sendInventoryEvent() {
        return true;
    }

    @Override
    public Location getLastClickedLocation() {
        if (lastClicked != null) {
            return lastClicked.clone();
        }
        return null;
    }

    @Override
    public void setPreCachingComplete(boolean complete) {
        if (!precachingComplete) {
            precachingComplete = complete;
        }
    }

    @Override
    public boolean isPreCachingComplete() {
        if (isSpoutCraftEnabled()) {
            return precachingComplete;
        } else {
            return true;
        }
    }

    @Override
    public void openSignEditGUI(Sign sign) {
        if (sign != null && isSpoutCraftEnabled()) {
            sendPacket(new PacketOpenSignGUI(sign.getX(), sign.getY(), sign.getZ()));
            TileEntitySign tes = (TileEntitySign) ((CraftWorld) ((CraftBlock) sign.getBlock()).getWorld()).getTileEntityAt(sign.getX(), sign.getY(), sign.getZ()); // Found a hidden trace to The Elder Scrolls. Bethesdas Lawyers are right!
            tes.isEditable = true;
        }
    }

    @Override
    public void updateKeys(byte[] keys) {
        this.forward = Keyboard.getKey(keys[0]);
        this.back = Keyboard.getKey(keys[2]);
        this.left = Keyboard.getKey(keys[1]);
        this.right = Keyboard.getKey(keys[3]);
        this.jump = Keyboard.getKey(keys[4]);
        this.inventoryKey = Keyboard.getKey(keys[5]);
        this.drop = Keyboard.getKey(keys[6]);
        this.chat = Keyboard.getKey(keys[7]);
        this.togglefog = Keyboard.getKey(keys[8]);
        this.sneak = Keyboard.getKey(keys[9]);
    }

    //Sends a packet delayed by 1 tick
    @Override
    public void sendDelayedPacket(SpoutPacket packet) {
        delayedPackets.add(packet);
    }

    @Override
    public void sendPacket(SpoutPacket packet) {
        if (!isSpoutCraftEnabled()) {
            if (queued != null) {
                queued.add(packet);
            }
        } else {
            getNetServerHandler().sendPacket(new CustomPacket(packet));
        }
    }

    @Override
    public void checkUrl(String url) {
        if (url == null || url.length() < 5) {
            throw new UnsupportedOperationException("Invalid URL");
        }
        if (!url.substring(url.length() - 4, url.length()).equalsIgnoreCase(".png")) {
            throw new UnsupportedOperationException("All skins must be a PNG image");
        }
        if (url.length() > 255) {
            throw new UnsupportedOperationException("All URL's must be shorter than 256 characters");
        }
    }
    private String skin = "http://s3.amazonaws.com/MinecraftSkins/" + getName() + ".png";
    private HashMap<String, String> skinsFor = new HashMap<String, String>();
    private String cape = "http://s3.amazonaws.com/MinecraftCloaks/" + getName() + ".png";
    private HashMap<String, String> capesFor = new HashMap<String, String>();
    private String title = getName();
    private HashMap<String, String> titlesFor = new HashMap<String, String>();

    @Override
    public void updateEntitySkins(List<LivingEntity> entities) {
        PlayerInformation info = getInformation();
        PlayerInformation global = SpoutManager.getPlayerManager().getGlobalInfo();
        for (LivingEntity le : entities) {
            for (EntitySkinType type : EntitySkinType.values()) {
                String skin = null;
                if (info != null) {
                    skin = getInformation().getEntitySkin(le, type);
                }
                if (skin == null) {
                    skin = global.getEntitySkin(le, type);
                }
                if (skin != null) {
                    sendDelayedPacket(new PacketEntitySkin(le, skin, type.getId()));
                }
            }
            String title = org.getspout.spoutapi.Spout.getServer().getTitle(le);
            if (title != null) {
                sendDelayedPacket(new PacketEntityTitle(le.getEntityId(), title));
            }
        }
    }

    public void updateAppearance() {
        if (!isSpoutCraftEnabled()) {
            return;
        }
        for (Player p : getWorld().getPlayers()) {
            if (p instanceof SpoutPlayer && p != this) {
                SpoutPlayer player = (SpoutPlayer) p;
                sendDelayedPacket(new PacketSkinURL(player.getEntityId(), player.getSkin(this), player.getCape(this)));
                sendDelayedPacket(new PacketEntityTitle(player.getEntityId(), player.getTitleFor(this)));
            }
        }
    }

    @Override
    public void setSkin(String url) {
        checkUrl(url);
        skin = url;

        for (Player p : getWorld().getPlayers()) {
            if (p instanceof SpoutPlayer) {
                ((SpoutPlayer) p).sendPacket(new PacketSkinURL(getEntityId(), getSkin((SpoutPlayer) p)));
            }
        }
    }

    @Override
    public void setSkinFor(SpoutPlayer viewingPlayer, String url) {
        checkUrl(url);
        skinsFor.put(viewingPlayer.getName(), url);
        viewingPlayer.sendPacket(new PacketSkinURL(getEntityId(), url));
    }

    @Override
    public String getSkin() {
        return skin;
    }

    @Override
    public String getSkin(SpoutPlayer viewingPlayer) {
        if (skinsFor.containsKey(viewingPlayer.getName())) {
            return skinsFor.get(viewingPlayer.getName());
        }
        return getSkin();
    }

    @Override
    public void resetSkin() {
        setSkin("http://s3.amazonaws.com/MinecraftSkins/" + getName() + ".png");
    }

    @Override
    public void resetSkinFor(SpoutPlayer viewingPlayer) {
        setSkinFor(viewingPlayer, "http://s3.amazonaws.com/MinecraftSkins/" + getName() + ".png");
    }

    @Override
    public void setCape(String url) {
        checkUrl(url);
        cape = url;

        for (Player p : getWorld().getPlayers()) {
            if (p instanceof SpoutPlayer) {
                ((SpoutPlayer) p).sendPacket(new PacketSkinURL(getCape((SpoutPlayer) p), getEntityId()));
            }
        }
    }

    @Override
    public void setCapeFor(SpoutPlayer viewingPlayer, String url) {
        checkUrl(url);
        capesFor.put(viewingPlayer.getName(), url);
        viewingPlayer.sendPacket(new PacketSkinURL(url, getEntityId()));
    }

    @Override
    public String getCape() {
        return cape;
    }

    @Override
    public String getCape(SpoutPlayer viewingPlayer) {
        if (capesFor.containsKey(viewingPlayer.getName())) {
            return capesFor.get(viewingPlayer.getName());
        }
        return getCape();
    }

    @Override
    public void resetCape() {
        setCape("http://s3.amazonaws.com/MinecraftCloaks/" + getName() + ".png");
    }

    @Override
    public void resetCapeFor(SpoutPlayer viewingPlayer) {
        setCapeFor(viewingPlayer, "http://s3.amazonaws.com/MinecraftCloaks/" + getName() + ".png");
    }

    @Override
    public void setTitle(String title) {
        this.title = title;

        for (Player p : getWorld().getPlayers()) {
            if (p instanceof SpoutPlayer) {
                ((SpoutPlayer) p).sendPacket(new PacketEntityTitle(getEntityId(), getTitleFor((SpoutPlayer) p)));
            }
        }
    }

    @Override
    public void setTitleFor(SpoutPlayer viewingPlayer, String title) {
        titlesFor.put(viewingPlayer.getName(), title);
        viewingPlayer.sendPacket(new PacketEntityTitle(getEntityId(), title));
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getTitleFor(SpoutPlayer viewingPlayer) {
        if (titlesFor.containsKey(viewingPlayer.getName())) {
            return titlesFor.get(viewingPlayer.getName());
        }
        return getTitle();
    }

    @Override
    public void hideTitle() {
        setTitle("[hide]");
    }

    @Override
    public void hideTitleFrom(SpoutPlayer viewingPlayer) {
        setTitleFor(viewingPlayer, "[hide]");
    }

    @Override
    public void resetTitle() {
        setTitle(getName());
    }

    @Override
    public void resetTitleFor(SpoutPlayer viewingPlayer) {
        setTitleFor(viewingPlayer, getName());
    }

    @Override
    public void setEntitySkin(LivingEntity target, String url, EntitySkinType type) {
        getInformation().setEntitySkin(target, url, type);
        sendDelayedPacket(new PacketEntitySkin(target, url, type.getId()));
    }

    @Override
    public void resetEntitySkin(LivingEntity target) {
        getInformation().setEntitySkin(target, null);
        sendPacket(new PacketEntitySkin(target, "[reset]", (byte) 0));
    }

    /*
     * Non Inteface public methods
     */
    public Location getLastTickLocation() {
        return lastTickLocation;
    }

    public void setLastTickLocation(Location loc) {
        lastTickLocation = loc;
    }

    public Location getRawLastClickedLocation() {
        return lastClicked;
    }

    public void setLastClickedLocation(Location location) {
        lastClicked = location;
    }

    public SpoutNetServerHandler getNetServerHandler() {
        if (!(getHandle().netServerHandler instanceof SpoutNetServerHandler)) {
            updateNetServerHandler(this);
        }
        return (SpoutNetServerHandler) getHandle().netServerHandler;
    }

    public int getBuildVersion() {
        return buildVersion;
    }

    public void setBuildVersion(int build) {
        buildVersion = build;
        if (isSpoutCraftEnabled() && queued != null) {
            updatePermissions();

            for (SpoutPacket packet : queued) {
                sendPacket(packet);
            }
        }
        queued = null;
    }

    public void setVersionString(String versionString) {
        this.versionString = versionString;
    }

    public String getVersionString() {
        return versionString;
    }

    public void onTick() {
        mainScreen.onTick();
        Screen currentScreen = getCurrentScreen();
        if (currentScreen != null && currentScreen instanceof OverlayScreen) {
            currentScreen.onTick();
        }
        screenOpenThisTick = false;

        //Because the player teleport event doesn't always fire :(
        Location current = getLocation();
        if (lastTickLocation != null) {
            if (!lastTickLocation.getWorld().equals(current.getWorld())) {
                doPostPlayerChangeWorld(lastTickLocation.getWorld(), current.getWorld());
            }
        }
        lastTickLocation = current;

        for (SpoutPacket packet : delayedPackets) {
            sendPacket(packet);
        }
        delayedPackets.clear();

        if (!getGameMode().equals(prevMode)) {
            prevMode = getGameMode();
            mainScreen.toggleSurvivalHUD(!getGameMode().equals(GameMode.CREATIVE));
        }
    }

    public void doPostPlayerChangeWorld(World oldWorld, World newWorld) {
        SpoutCraftPlayer.updateBukkitEntity(this);
        if (isSpoutCraftEnabled()) {
            updateMovement();
            Spout.getInstance().getPlayerTrackingManager().onWorldChange(this);
        }
        updateAppearance();
    }

    public void updateMovement() {
        if (isSpoutCraftEnabled()) {
            sendPacket(new PacketMovementModifiers(gravityMod, walkingMod, swimmingMod, jumpingMod, airspeedMod));
        }
    }

    /*
     * Non Interface public static methods
     */
    public static boolean setNetServerHandler(NetworkManager nm, NetServerHandler nsh) {
        try {
            Field p = NetworkManager.class.getDeclaredField("packetListener");
            p.setAccessible(true);
            p.set(nm, nsh);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public static boolean resetNetServerHandler(Player player) {
        CraftPlayer cp = (CraftPlayer) player;
        CraftServer server = (CraftServer) Bukkit.getServer();

        if (cp.getHandle().netServerHandler instanceof SpoutNetServerHandler) {
            NetServerHandler oldHandler = cp.getHandle().netServerHandler;
            //Set<ChunkCoordIntPair> chunkUpdateQueue = ((SpoutNetServerHandler) cp.getHandle().netServerHandler).getChunkUpdateQueue();
            //for (ChunkCoordIntPair c : chunkUpdateQueue) {
            //	cp.getHandle().chunkCoordIntPairQueue.add(c);
            //}
            //((SpoutNetServerHandler) cp.getHandle().netServerHandler).flushUnloadQueue();
            cp.getHandle().netServerHandler.a();
            Location loc = player.getLocation();
            NetServerHandler handler = new NetServerHandler(server.getHandle().server, cp.getHandle().netServerHandler.networkManager, cp.getHandle());
            handler.a(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            cp.getHandle().netServerHandler = handler;
            NetworkManager nm = cp.getHandle().netServerHandler.networkManager;
            setNetServerHandler(nm, cp.getHandle().netServerHandler);
            oldHandler.disconnected = true;
            ((CraftServer) Spout.getInstance().getServer()).getServer().networkListenThread.a(handler);
            return true;
        }
        return false;
    }

    public static boolean updateNetServerHandler(Player player) {
        CraftPlayer cp = (CraftPlayer) player;
        CraftServer server = (CraftServer) Bukkit.getServer();
        if (!(cp.getHandle().netServerHandler instanceof SpoutNetServerHandler)) {
            NetServerHandler oldHandler = cp.getHandle().netServerHandler;
            Location loc = player.getLocation();
            SpoutNetServerHandler handler = new SpoutNetServerHandler(server.getHandle().server, cp.getHandle().netServerHandler.networkManager, cp.getHandle());
            //for (Object o : cp.getHandle().playerChunkCoordIntPairs) {
            //	ChunkCoordIntPair c = (ChunkCoordIntPair) o;
            //	handler.addActiveChunk(c);
            //}
            handler.a(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            cp.getHandle().netServerHandler = handler;
            NetworkManager nm = cp.getHandle().netServerHandler.networkManager;
            setNetServerHandler(nm, cp.getHandle().netServerHandler);
            oldHandler.disconnected = true;
            ((CraftServer) Spout.getInstance().getServer()).getServer().networkListenThread.a(handler);
            return true;
        }
        return false;
    }

    public static boolean updateBukkitEntity(Player player) {
        if (!(player instanceof SpoutCraftPlayer)) {
            CraftPlayer cp = (CraftPlayer) player;
            EntityPlayer ep = cp.getHandle();
            return updateBukkitEntity(ep);
        }
        return false;
    }

    public static boolean updateBukkitEntity(EntityPlayer ep) {
        Field bukkitEntity;
        try {
            bukkitEntity = Entity.class.getDeclaredField("bukkitEntity");
            bukkitEntity.setAccessible(true);
            org.bukkit.entity.Entity e = (org.bukkit.entity.Entity) bukkitEntity.get(ep);
            if (e != null && !(e instanceof SpoutCraftPlayer)) {
                bukkitEntity.set(ep, new SpoutCraftPlayer((CraftServer) Bukkit.getServer(), ep));
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static SpoutPlayer getPlayer(Player player) {
        if (player instanceof SpoutCraftPlayer) {
            return (SpoutCraftPlayer) player;
        }
        if ((((CraftPlayer) player).getHandle()).getBukkitEntity() instanceof SpoutCraftPlayer) {
            return (SpoutCraftPlayer) ((((CraftPlayer) player).getHandle()).getBukkitEntity());
        }
        //We should never get here
        //Logger.getLogger("Minecraft").warning("Player: " + player.getName() + " was not properly updated during login!");
        updateBukkitEntity(player);
        return (SpoutCraftPlayer) ((((CraftPlayer) player).getHandle()).getBukkitEntity());
    }

    @Override
    public Map<String, String> getAddons() {
        return addons;
    }

    @Override
    public void setAddons(String[] addons, String[] versions) {
        this.addons = new HashMap<String, String>();
        for (int i = 0; i < addons.length; i++) {
            this.addons.put(addons[i], versions[i]);
        }
    }

    @Override
    public void updatePermission(String node) {
        updatePermissions(node);
    }

    @Override
    public void updatePermissions(String... nodes) {
        HashMap<String, Boolean> values = new HashMap<String, Boolean>();
        for (String node : nodes) {
            boolean allow = hasPermission(node);
            values.put(node, allow);
        }
        sendPacket(new PacketPermissionUpdate(values));
    }

    @Override
    public void updatePermissions() {
        recalculatePermissions();
        HashMap<String, Boolean> values = new HashMap<String, Boolean>();
        HashSet<String> allPerms = new HashSet<String>();

        //Hackish workaround for bukkit not giving us all the permissions
        Set<Permission> defaults = Bukkit.getServer().getPluginManager().getDefaultPermissions(false);
        for (Permission permission : defaults) {
            allPerms.add(permission.getName());
        }
        defaults = Bukkit.getServer().getPluginManager().getDefaultPermissions(true);
        for (Permission permission : defaults) {
            allPerms.add(permission.getName());
        }

        //Overwrite with actual permissions, if applicable
        for (PermissionAttachmentInfo info : perm.getEffectivePermissions()) {
            allPerms.add(info.getPermission());
        }

        for (String p : allPerms) {
            values.put(p, hasPermission(p));
        }

        sendPacket(new PacketPermissionUpdate(values));
    }

    @Override
    public boolean spawnTextEntity(String text, Location location, float scale, int duration, Vector movement) {
        if (isSpoutCraftEnabled()) {
            sendPacket(new PacketSpawnTextEntity(text, location, scale, duration, movement));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void sendPacket(MCPacket packet) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void sendImmediatePacket(MCPacket packet) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean closeActiveWindow() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean openInventoryWindow(Inventory inventory) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean openInventoryWindow(Inventory inventory, Location location) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean openInventoryWindow(Inventory inventory, Location location, boolean ignoreDistance) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean openWorkbenchWindow(Location location) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
