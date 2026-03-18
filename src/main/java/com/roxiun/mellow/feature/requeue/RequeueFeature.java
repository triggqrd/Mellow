package com.roxiun.mellow.feature.requeue;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.feature.requeue.auto.TabRequeue;
import com.roxiun.mellow.feature.requeue.listeners.ChatListener;
import com.roxiun.mellow.feature.requeue.listeners.TickListener;
import com.roxiun.mellow.feature.requeue.listeners.WorldListener;
import com.roxiun.mellow.feature.requeue.util.Timer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

public class RequeueFeature {

    public static RequeueFeature INSTANCE;

    public static final String PRIMARY_COLOR = "§c";
    public static final String TEXT_COLOR = "§e";

    private final Timer requeueTimer = new Timer();
    private final MellowOneConfig config;

    private ChatListener chatListener;
    private TickListener tickListener;
    private TabRequeue requeueStrategy;
    private LocationManager locationManager;
    private PartyManager partyManager;
    private AutododgeService autododgeService;

    public RequeueFeature(MellowOneConfig config) {
        this.config = config;
        this.requeueStrategy = new TabRequeue();
        INSTANCE = this;
    }

    public static RequeueFeature init(MellowOneConfig config) {
        RequeueFeature feature = new RequeueFeature(config);
        feature.registerEventHandlers();
        return feature;
    }

    private void registerEventHandlers() {
        partyManager = new PartyManager();
        chatListener = new ChatListener();
        locationManager = new LocationManager();
        tickListener = new TickListener();
        autododgeService = new AutododgeService(config);
        MinecraftForge.EVENT_BUS.register(chatListener);
        MinecraftForge.EVENT_BUS.register(tickListener);
        MinecraftForge.EVENT_BUS.register(new WorldListener());
    }

    public boolean isFeatureEnabled() {
        return Mellow.isEnabled() && config.requeueEnabled;
    }

    public boolean modEnabled() {
        if (!isFeatureEnabled()) {
            return false;
        }
        if (Minecraft.getMinecraft().getCurrentServerData() == null) {
            return false;
        }
        if (!config.requeueHypixelOnly) {
            return true;
        }
        String ip = Minecraft
            .getMinecraft()
            .getCurrentServerData()
            .serverIP;
        if (ip == null) {
            return false;
        }
        return containsIgnoreCase(ip, "hypixel.net")
            || containsIgnoreCase(ip, "hypixel.io");
    }

    private static boolean containsIgnoreCase(String str, String sub) {
        int subLen = sub.length();
        int max = str.length() - subLen;
        for (int i = 0; i <= max; i++) {
            if (str.regionMatches(true, i, sub, 0, subLen)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAutoEnabled() {
        return config.requeueAuto;
    }

    public boolean isSafeguardEnabled() {
        return config.requeueSafeguard;
    }

    public boolean includeClientPlayer() {
        return config.requeueConsiderClient;
    }

    public boolean isKickOfflineEnabled() {
        return config.requeueKickOffline;
    }

    public boolean shouldRequeueOnWin() {
        return config.requeueOnWin;
    }

    public long getAutoRequeueDelayMs() {
        String delay = config.requeueAutoDelay;
        if (delay == null || delay.isEmpty()) return 10000;
        try {
            double seconds = Double.parseDouble(delay.trim());
            if (seconds < 0) seconds = 0;
            return (long) (seconds * 1000);
        } catch (NumberFormatException e) {
            return 10000;
        }
    }

    public Timer getRequeueTimer() {
        return requeueTimer;
    }

    public TabRequeue getRequeue() {
        return requeueStrategy;
    }

    public ChatListener getChatListener() {
        return chatListener;
    }

    public TickListener getTickListener() {
        return tickListener;
    }

    public LocationManager getLocationManager() {
        return locationManager;
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public AutododgeService getAutododgeService() {
        return autododgeService;
    }

    public MellowOneConfig getConfig() {
        return config;
    }
}
