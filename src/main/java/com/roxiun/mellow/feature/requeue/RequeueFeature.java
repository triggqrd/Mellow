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
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;

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
    private KeyBinding requeueKeybind;
    private LocationManager locationManager;
    private PartyManager partyManager;

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
        MinecraftForge.EVENT_BUS.register(chatListener);
        MinecraftForge.EVENT_BUS.register(tickListener);
        MinecraftForge.EVENT_BUS.register(new WorldListener());

        requeueKeybind = new KeyBinding("Requeue (/rq)", Keyboard.KEY_NONE, "Mellow");
        ClientRegistry.registerKeyBinding(requeueKeybind);
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
        String normalized = ip.toLowerCase(Locale.ROOT);
        return normalized.contains("hypixel.net") || normalized.contains("hypixel.io");
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

    public KeyBinding getRequeueKeybind() {
        return requeueKeybind;
    }

    public Set<String> getExcludedGames() {
        return EXCLUDED_GAMES;
    }

    public MellowOneConfig getConfig() {
        return config;
    }
}
