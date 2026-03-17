package com.roxiun.mellow.feature.requeue;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.feature.requeue.auto.IAutoRequeue;
import com.roxiun.mellow.feature.requeue.auto.WhoRequeue;
import com.roxiun.mellow.feature.requeue.LocationManager;
import com.roxiun.mellow.feature.requeue.PartyManager;
import com.roxiun.mellow.feature.requeue.listeners.ChatListener;
import com.roxiun.mellow.feature.requeue.listeners.TickListener;
import com.roxiun.mellow.feature.requeue.listeners.WorldListener;
import com.roxiun.mellow.feature.requeue.util.Timer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

public class RequeueFeature {

    public static RequeueFeature INSTANCE;

    public static final String PRIMARY_COLOR = "§c";
    public static final String TEXT_COLOR = "§e";

    private static final Set<String> EXCLUDED_GAMES =
        Collections.unmodifiableSet(
            new HashSet<>(
                Arrays.asList(
                    "BEDWARS",
                    "PAINTBALL",
                    "QUAKECRAFT",
                    "ARENA",
                    "GINGERBREAD",
                    "WALLS3",
                    "PIT",
                    "SKYBLOCK",
                    "REPLAY",
                    "HOUSING"
                )
            )
        );

    private final Timer requeueTimer = new Timer();
    private final MellowOneConfig config;

    private ChatListener chatListener;
    private TickListener tickListener;
    private IAutoRequeue requeueStrategy;
    private LocationManager locationManager;
    private PartyManager partyManager;
    private AutododgeService autododgeService;

    public RequeueFeature(MellowOneConfig config) {
        this.config = config;
        this.requeueStrategy = new WhoRequeue();
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

    public Timer getRequeueTimer() {
        return requeueTimer;
    }

    public IAutoRequeue getRequeue() {
        return requeueStrategy;
    }

    public void setRequeue(IAutoRequeue strategy) {
        this.requeueStrategy = strategy;
    }

    public boolean isUsingWhoRequeue() {
        return requeueStrategy instanceof WhoRequeue;
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

    public Set<String> getExcludedGames() {
        return EXCLUDED_GAMES;
    }

    public MellowOneConfig getConfig() {
        return config;
    }
}
