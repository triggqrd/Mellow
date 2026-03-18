package com.roxiun.mellow.feature.requeue.auto;

import com.roxiun.mellow.feature.requeue.LocationManager;
import com.roxiun.mellow.feature.requeue.PartyManager;
import com.roxiun.mellow.feature.requeue.RequeueFeature;
import com.roxiun.mellow.feature.requeue.util.GameUtil;
import com.roxiun.mellow.feature.requeue.util.RequeueChatUtil;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;

/**
 * Tracks final kills in BedWars and auto-requeues when all tracked
 * players (party + optionally client) have been eliminated.
 *
 * <p>All preconditions (modEnabled, isAutoEnabled, BEDWARS type, locraw
 * validity, player/world presence) are validated by {@code TickListener}
 * before {@link #onTick()} is called — no redundant checks here.</p>
 */
public class TabRequeue {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Set<String> finalKilled = new HashSet<>();
    private int tickCounter = 0;

    public void addFinalKill(String player) {
        if (player != null && !player.isEmpty()) {
            finalKilled.add(player.trim().toLowerCase());
        }
    }

    /**
     * Called every tick by TickListener (after all preconditions pass).
     * Throttles actual work to every 10 ticks (~500ms).
     *
     * @return true if a requeue was fired this tick
     */
    public boolean onTick() {
        if (++tickCounter < 10) return false;
        tickCounter = 0;

        RequeueFeature feature = RequeueFeature.INSTANCE;
        if (feature == null) return false;

        if (!canRequeue()) return false;
        if (!feature.getRequeueTimer().hasTimeElapsed(feature.getAutoRequeueDelayMs(), true)) {
            return false;
        }

        String gameId = GameUtil.getGameID(
            LocationManager.instance.getType(),
            LocationManager.instance.getMode()
        );
        if (gameId == null) {
            RequeueChatUtil.sendMessage("There was an issue finding your game mode right now!");
            return false;
        }
        if (mc.thePlayer == null) return false;
        RequeueChatUtil.sendMessage("Attempted requeue.");
        requeueCleanup();
        mc.thePlayer.sendChatMessage("/play " + gameId);
        return true;
    }

    /**
     * Returns true only if every tracked player has been final-killed.
     * Returns false if there are no players to track (empty party +
     * client not included), preventing spurious requeues in pregame.
     */
    public boolean canRequeue() {
        if (PartyManager.instance == null) {
            return false;
        }
        // Check party members directly without copying the list
        List<String> party = PartyManager.instance.getParty();
        boolean hasTrackedPlayer = false;
        for (String player : party) {
            hasTrackedPlayer = true;
            if (!finalKilled.contains(player.toLowerCase())) {
                return false;
            }
        }
        // Check client player
        RequeueFeature feature = RequeueFeature.INSTANCE;
        if (feature != null && feature.includeClientPlayer() && mc.thePlayer != null) {
            hasTrackedPlayer = true;
            if (!finalKilled.contains(mc.thePlayer.getName().trim().toLowerCase())) {
                return false;
            }
        }
        return hasTrackedPlayer;
    }

    public void requeueCleanup() {
        finalKilled.clear();
        tickCounter = 0;
    }
}
