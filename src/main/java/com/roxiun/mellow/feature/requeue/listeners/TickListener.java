package com.roxiun.mellow.feature.requeue.listeners;

import com.roxiun.mellow.feature.requeue.LocationManager;
import com.roxiun.mellow.feature.requeue.RequeueFeature;
import com.roxiun.mellow.feature.requeue.auto.TabRequeue;
import com.roxiun.mellow.feature.requeue.auto.WhoRequeue;
import com.roxiun.mellow.feature.requeue.util.GameUtil;
import com.roxiun.mellow.feature.requeue.util.RequeueChatUtil;
import com.roxiun.mellow.feature.requeue.util.Timer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class TickListener {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Timer locrawTimer = new Timer();
    private final Timer kickofflineTimer = new Timer();
    private final Timer endRequeueTimer = new Timer();
    private final Timer endTriggerTimer = new Timer();

    private boolean endRequeueTriggered = false;
    private boolean awaitingKickOffline = false;
    private boolean returnedLastTick = false;

    public void prepareKickOffline() {
        if (!RequeueFeature.INSTANCE.isKickOfflineEnabled()) return;
        kickofflineTimer.reset();
        awaitingKickOffline = true;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        RequeueFeature feature = RequeueFeature.INSTANCE;
        if (feature == null || !feature.modEnabled()) {
            return;
        }

        KeyBinding requeueBind = feature.getRequeueKeybind();
        if (requeueBind != null && mc.thePlayer != null) {
            while (requeueBind.isPressed()) {
                GameUtil.safeRequeue();
            }
        }

        handleKickOffline();
        handleLocraw();

        if (LocationManager.instance == null) return;
        if (LocationManager.instance.getType() == null) {
            endRequeueTriggered = false;
            return;
        }
        if (LocationManager.instance.getMode() == null) {
            endRequeueTriggered = false;
            return;
        }
        if (
            feature
                .getExcludedGames()
                .contains(LocationManager.instance.getType().toUpperCase().trim())
        ) {
            return;
        }

        handleWinRequeue();
        handleAuto();
    }

    private void handleKickOffline() {
        if (!RequeueFeature.INSTANCE.isKickOfflineEnabled()) return;
        if (
            awaitingKickOffline &&
            mc.thePlayer != null &&
            kickofflineTimer.hasTimeElapsed(5000, true)
        ) {
            awaitingKickOffline = false;
            mc.thePlayer.sendChatMessage("/p kickoffline");
        }
    }

    public void resetTimer() {
        locrawTimer.reset();
        endRequeueTriggered = false;
    }

    private void requeue() {
        String id = GameUtil.getGameID(
            LocationManager.instance.getType(),
            LocationManager.instance.getMode()
        );
        if (id == null) {
            RequeueChatUtil.sendMessage("There was an issue finding your game mode right now!");
            return;
        }
        RequeueFeature.INSTANCE.getRequeueTimer().reset();
        RequeueChatUtil.sendMessage("Attempted requeue.");
        RequeueFeature.INSTANCE.getRequeue().requeueCleanup();
        mc.thePlayer.sendChatMessage("/play " + id);
    }

    private void handleWinRequeue() {
        if (
            mc.theWorld != null &&
            endRequeueTriggered &&
            endRequeueTimer.hasTimeElapsed(500, false)
        ) {
            endRequeueTriggered = false;
            requeue();
        }
    }

    private void handleAuto() {
        if (!RequeueFeature.INSTANCE.isAutoEnabled()) return;
        String type = LocationManager.instance.getType();
        String mode = LocationManager.instance.getMode();
        if (type == null || mode == null) return;
        if (type.equals("DUELS")) return;
        if (type.equals("ARCADE") && mode.equals("PARTY")) return;

        boolean useTab = type.equals("PROTOTYPE");
        if (useTab && !(RequeueFeature.INSTANCE.getRequeue() instanceof TabRequeue)) {
            RequeueFeature.INSTANCE.setRequeue(new TabRequeue());
        }
        if (!useTab && !RequeueFeature.INSTANCE.isUsingWhoRequeue()) {
            RequeueFeature.INSTANCE.setRequeue(new WhoRequeue());
        }
        RequeueFeature.INSTANCE.getRequeue().onTick();
    }

    private void handleLocraw() {
        if (LocationManager.instance == null) return;
        if (!LocationManager.instance.isAwaitingLocraw()) {
            returnedLastTick = true;
            return;
        }
        if (mc.theWorld == null || mc.thePlayer == null) {
            returnedLastTick = true;
            return;
        }
        if (mc.currentScreen instanceof GuiDownloadTerrain) {
            returnedLastTick = true;
            return;
        }

        if (returnedLastTick) {
            locrawTimer.reset();
        }
        if (locrawTimer.hasTimeElapsed(5000, true)) {
            LocationManager.instance.sendLocraw();
        }
        returnedLastTick = false;
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
