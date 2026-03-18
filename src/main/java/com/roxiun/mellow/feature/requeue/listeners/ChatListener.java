package com.roxiun.mellow.feature.requeue.listeners;

import com.roxiun.mellow.feature.requeue.LocationManager;
import com.roxiun.mellow.feature.requeue.PartyManager;
import com.roxiun.mellow.feature.requeue.RequeueFeature;
import com.roxiun.mellow.feature.requeue.util.RequeueChatUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ChatListener {

    private final List<String> criteria = new ArrayList<>();
    private long waitingSince = Long.MAX_VALUE;
    private final Minecraft mc = Minecraft.getMinecraft();

    /**
     * Sets the criteria for hiding the next matching chat message.
     * Used by LocationManager to suppress /locraw JSON output.
     * Resets the timeout so retries get a fresh 5-second window.
     */
    public void setHideCriteria(String criterion) {
        criteria.clear();
        criteria.add(criterion);
        waitingSince = Long.MAX_VALUE;
    }

    public void listenForJoins(String noColors) {
        if (noColors.contains(":")) return;
        if (noColors.startsWith("You left the party.")) {
            PartyManager.instance.clearParty();
            return;
        }
        if (noColors.startsWith("You have joined") && noColors.contains("'s party")) {
            PartyManager.instance.clearParty();
            String[] split = noColors.split(" ");
            String player = split.length > 3 ? split[3] : "";
            if (player.contains("[")) player = split.length > 4 ? split[4] : player;
            if (player.endsWith("'s")) {
                player = player.substring(0, player.length() - 2);
            }
            PartyManager.instance.registerPlayer(player);
            return;
        }
        if (noColors.contains("has left the party")) {
            String player = noColors.split(" ")[0];
            if (player.contains("[")) player = noColors.split(" ")[1];
            PartyManager.instance.removePlayer(player);
            return;
        }
        if (
            noColors.contains("has been removed from the party.") ||
            noColors.contains("was removed from your party")
        ) {
            String player = noColors.split(" ")[0];
            if (player.contains("[")) player = noColors.split(" ")[1];
            PartyManager.instance.removePlayer(player);
            return;
        }
        if (noColors.contains("joined the party")) {
            String player = noColors.split(" ")[0];
            if (player.contains("[")) player = noColors.split(" ")[1];
            PartyManager.instance.registerPlayer(player);
            return;
        }
        if (
            noColors.startsWith("Kicked") &&
            noColors.endsWith("because they were offline.")
        ) {
            String[] args = noColors.split(" ");
            for (int i = 1; i < args.length - 4; i++) {
                String player = args[i];
                if (player.contains("[")) continue;
                if (player.endsWith(",")) {
                    player = player.substring(0, player.length() - 1);
                }
                PartyManager.instance.removePlayer(player);
            }
            return;
        }

        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (
            noColors.contains("has disconnected") &&
            RequeueFeature.INSTANCE.isKickOfflineEnabled()
        ) {
            RequeueFeature.INSTANCE.getTickListener().prepareKickOffline();
        }
    }

    private void listenForParty(String msg) {
        String noColors = RequeueChatUtil.removeColorCodes(msg).trim();
        listenForJoins(noColors);
        if (!noColors.contains(":")) return;
        if (!noColors.startsWith("You'll be partying")) return;
        String secondHalf = noColors.split(":", 2)[1];
        String[] args = secondHalf.split(",\\s*|\\s+");
        for (String s : args) {
            if (s.contains("[")) continue;
            PartyManager.instance.registerPlayer(s);
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        RequeueFeature feature = RequeueFeature.INSTANCE;
        if (feature == null || !feature.modEnabled()) return;
        String message = event.message.getUnformattedText();
        String removedColors = RequeueChatUtil.removeColorCodes(message).trim();

        if (
            feature.shouldRequeueOnWin() &&
            (removedColors.contains("Reward Summary") ||
                removedColors.contains("WINNER!") ||
                removedColors.contains("GAME WIN - ")) &&
            !removedColors.contains(":")
        ) {
            feature.getTickListener().onGameEnd();
        }

        listenForParty(message);
        listenForFinalKills(removedColors);

        if (message.startsWith("{\"server\":")) {
            if (LocationManager.instance != null) {
                LocationManager.instance.setLocraw(message);
            }
        }

        hideCriteria(removedColors, event);
    }

    private void listenForFinalKills(String message) {
        if (!message.contains("FINAL KILL!")) return;
        if (LocationManager.instance == null) return;
        if (!"BEDWARS".equalsIgnoreCase(LocationManager.instance.getType())) return;
        // Eliminated player is always the first word regardless of kill message variant.
        // Use indexOf instead of split to avoid allocating an entire String array.
        int space = message.indexOf(' ');
        if (space <= 0) return;
        String eliminated = message.substring(0, space);
        RequeueFeature feature = RequeueFeature.INSTANCE;
        if (feature != null) {
            feature.getRequeue().addFinalKill(eliminated);
        }
    }

    private void hideCriteria(String message, ClientChatReceivedEvent event) {
        if (criteria.isEmpty()) return;
        for (String s : criteria) {
            if (message.contains(s)) {
                event.setCanceled(true);
                criteria.clear();
                waitingSince = Long.MAX_VALUE;
                return;
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (criteria.isEmpty()) return;
        if (waitingSince == Long.MAX_VALUE) {
            waitingSince = System.currentTimeMillis();
        } else if (System.currentTimeMillis() - waitingSince > 5000) {
            criteria.clear();
            waitingSince = Long.MAX_VALUE;
        }
    }
}
