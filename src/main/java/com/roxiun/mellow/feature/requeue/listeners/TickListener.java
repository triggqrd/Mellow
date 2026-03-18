package com.roxiun.mellow.feature.requeue.listeners;

import cc.polyfrost.oneconfig.config.core.OneKeyBind;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.feature.requeue.AutododgeService;
import com.roxiun.mellow.feature.requeue.LocationManager;
import com.roxiun.mellow.feature.requeue.RequeueFeature;
import com.roxiun.mellow.feature.requeue.util.GameUtil;
import com.roxiun.mellow.feature.requeue.util.RequeueChatUtil;
import com.roxiun.mellow.feature.requeue.util.Timer;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class TickListener {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Timer locrawTimer = new Timer();
    private final Timer kickofflineTimer = new Timer();
    private final Timer endRequeueTimer = new Timer();
    private final Timer endTriggerTimer = new Timer();

    private static final int LOCRAW_MAX_RETRIES = 3;

    private boolean endRequeueTriggered = false;
    private boolean awaitingKickOffline = false;
    private boolean locrawSentThisCycle = false;
    private int locrawRetries = 0;
    private boolean requeueKeyWasActive = false;
    private boolean nickScanDone = false;

    public void prepareKickOffline() {
        if (!RequeueFeature.INSTANCE.isKickOfflineEnabled()) return;
        kickofflineTimer.reset();
        awaitingKickOffline = true;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        RequeueFeature feature = RequeueFeature.INSTANCE;
        if (feature == null || !feature.modEnabled()) {
            return;
        }

        // Only fire keybind when no GUI/chat is open
        if (Mellow.config != null && mc.thePlayer != null && mc.currentScreen == null) {
            OneKeyBind requeueBind = Mellow.config.requeueKeybind;
            boolean active = requeueBind != null && requeueBind.isActive();
            if (active && !requeueKeyWasActive) {
                GameUtil.safeRequeue();
            }
            requeueKeyWasActive = active;
        } else {
            requeueKeyWasActive = false;
        }

        handleNickScan();
        handleKickOffline(feature);

        LocationManager location = LocationManager.instance;
        handleLocraw(location);

        if (location == null) return;
        String type = location.getType();
        String mode = location.getMode();
        if (type == null || mode == null) {
            endRequeueTriggered = false;
            return;
        }
        if (!"BEDWARS".equalsIgnoreCase(type)) {
            return;
        }

        handleWinRequeue();
        handleAuto();
    }

    private void handleNickScan() {
        if (nickScanDone) return;

        MellowOneConfig config = Mellow.config;
        if (config == null || !config.autododgeEnabled || !config.autododgeNicked) return;
        if (config.autododgeMode == 0) return;

        // Only scan once when the game starts (not pregame/lobby)
        if (!HypixelFeatures.getInstance().getGameSnapshot().isInBedwarsMatch()) return;
        nickScanDone = true;

        AutododgeService autododge = AutododgeService.getInstance();
        if (autododge == null || autododge.hasDodgedThisLobby()) return;
        if (mc.getNetHandler() == null || mc.getNetHandler().getPlayerInfoMap() == null) return;

        UUID localUuid = mc.thePlayer != null ? mc.thePlayer.getGameProfile().getId() : null;
        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            UUID uuid = info.getGameProfile().getId();
            if (uuid == null || uuid.equals(localUuid)) continue;
            if (uuid.version() == 1 || uuid.version() == 3) {
                autododge.checkNickedAndDodge(info.getGameProfile().getName(), false);
                return;
            }
        }
    }

    private void handleKickOffline(RequeueFeature feature) {
        if (!awaitingKickOffline) return;
        if (!feature.isKickOfflineEnabled()) return;
        if (mc.thePlayer != null && kickofflineTimer.hasTimeElapsed(5000, true)) {
            awaitingKickOffline = false;
            mc.thePlayer.sendChatMessage("/p kickoffline");
        }
    }

    public void resetTimer() {
        locrawTimer.reset();
        locrawSentThisCycle = false;
        locrawRetries = 0;
        nickScanDone = false;
        endRequeueTriggered = false;
    }

    private void requeue(RequeueFeature feature, String type, String mode) {
        String id = GameUtil.getGameID(type, mode);
        if (id == null) {
            RequeueChatUtil.sendMessage("There was an issue finding your game mode right now!");
            return;
        }
        feature.getRequeueTimer().reset();
        RequeueChatUtil.sendMessage("Attempted requeue.");
        feature.getRequeue().requeueCleanup();
        if (mc.thePlayer != null) {
            mc.thePlayer.sendChatMessage("/play " + id);
        }
    }

    private void handleWinRequeue() {
        if (
            mc.theWorld != null &&
            endRequeueTriggered &&
            endRequeueTimer.hasTimeElapsed(500, false)
        ) {
            endRequeueTriggered = false;
            RequeueFeature feature = RequeueFeature.INSTANCE;
            LocationManager location = LocationManager.instance;
            if (feature != null && location != null
                && location.getType() != null && location.getMode() != null) {
                requeue(feature, location.getType(), location.getMode());
            }
        }
    }

    private void handleAuto() {
        if (!RequeueFeature.INSTANCE.isAutoEnabled()) return;

        if (RequeueFeature.INSTANCE.getRequeue().onTick()) {
            // Auto requeue fired — cancel any pending win requeue
            // to prevent sending /play twice
            endRequeueTriggered = false;
        }
    }

    private void handleLocraw(LocationManager location) {
        if (location == null) return;
        if (!location.isAwaitingLocraw()) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (mc.currentScreen instanceof GuiDownloadTerrain) return;

        if (!locrawSentThisCycle) {
            // Send immediately on first ready tick
            location.sendLocraw();
            locrawSentThisCycle = true;
            locrawRetries = 1;
            locrawTimer.reset();
        } else if (locrawRetries < LOCRAW_MAX_RETRIES
            && locrawTimer.hasTimeElapsed(2000, true)) {
            // Retry every 2s, up to max retries
            location.sendLocraw();
            locrawRetries++;
        }
    }

    public void onGameEnd() {
        if (
            endTriggerTimer.hasTimeElapsed(5000, true) &&
            RequeueFeature.INSTANCE.getRequeueTimer().hasTimeElapsed(10000, false)
        ) {
            endRequeueTriggered = true;
            endRequeueTimer.reset();
        }
    }
}
