package com.roxiun.mellow.feature.requeue.listeners;

import com.roxiun.mellow.feature.requeue.LocationManager;
import com.roxiun.mellow.feature.requeue.PartyManager;
import com.roxiun.mellow.feature.requeue.RequeueFeature;
import com.roxiun.mellow.feature.requeue.auto.WhoRequeue;
import com.roxiun.mellow.feature.requeue.util.RequeueChatUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ChatListener {

    public final List<String> criteria = new ArrayList<>();
    private long waitingSince = Long.MAX_VALUE;
    private final Minecraft mc = Minecraft.getMinecraft();

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

    private void parseAsWho(String message) {
        String[] playerList = message.split(":", 2)[1].split(",");
        WhoRequeue who = (WhoRequeue) RequeueFeature.INSTANCE.getRequeue();
        who.clearWhoNames();
        for (String p : playerList) {
            who.addWhoName(p.trim());
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

        if (message.startsWith("{\"server\":")) {
            if (LocationManager.instance != null) {
                LocationManager.instance.setLocraw(message);
            }
        }

        if (
            LocationManager.instance != null &&
            LocationManager.instance.isLocrawValid() &&
            LocationManager.instance.getMode() != null &&
            LocationManager.instance.getMode().equalsIgnoreCase("DROPPER") &&
            feature.isUsingWhoRequeue()
        ) {
            String[] timeArgs;
            if (
                message.contains(":") &&
                (timeArgs = message.split(":", 2)).length >= 1 &&
                timeArgs[0].contains("finished all maps")
            ) {
                ((WhoRequeue) feature.getRequeue()).handlePlayer();
            }
        }

        if (
            feature.isUsingWhoRequeue() &&
            (removedColors.startsWith("ONLINE:") || removedColors.startsWith("ALIVE:"))
        ) {
            parseAsWho(removedColors);
        }

        hideCriteria(removedColors, event);

        if (feature.isUsingWhoRequeue() && LocationManager.instance != null) {
            String type = LocationManager.instance.getType();
            if (type == null) return;
            switch (type) {
                case "SKYWARS":
                    handleSkywars(removedColors);
                    break;
                case "WALLS":
                    handleWalls(removedColors);
                    break;
                case "MCGO":
                    handleCvc(removedColors);
                    break;
                default:
                    break;
            }
        }
    }

    private void hideCriteria(String message, ClientChatReceivedEvent event) {
        if (criteria.isEmpty()) return;
        boolean blocked = false;
        for (String s : criteria) {
            if (message.contains(s)) {
                event.setCanceled(true);
                blocked = true;
                break;
            }
        }
        if (blocked) {
            criteria.clear();
            waitingSince = Long.MAX_VALUE;
            if (message.startsWith("ALIVE:")) {
                criteria.add("DEAD:");
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        RequeueFeature feature = RequeueFeature.INSTANCE;
        if (feature != null && feature.isUsingWhoRequeue()) {
            LocationManager location = LocationManager.instance;
            if (
                location != null &&
                location.getType() != null &&
                !location.getType().equals("SKYWARS")
            ) {
                ((WhoRequeue) feature.getRequeue()).setDelayedValid(false);
            }
        }
        if (criteria.isEmpty()) return;
        if (waitingSince == Long.MAX_VALUE) {
            waitingSince = System.currentTimeMillis();
        } else if (System.currentTimeMillis() - waitingSince > 5000) {
            criteria.clear();
            waitingSince = Long.MAX_VALUE;
        }
    }

    private void handleSkywars(String message) {
        WhoRequeue who = (WhoRequeue) RequeueFeature.INSTANCE.getRequeue();
        if (message.startsWith("Mode:")) {
            who.clearWhoNames();
            criteria.add("Team #");
            who.setDelayedValid(false);
            return;
        }
        if (!message.startsWith("Team #")) {
            criteria.clear();
            who.setDelayedValid(true);
            return;
        }
        String[] split = message.split(" ");
        if (split.length > 2) {
            String playerName = split[2];
            who.addWhoName(playerName);
        }
        criteria.add("Team #");
    }

    private static final String[] WALLS_COLORS = { "RED", "BLUE", "GREEN", "YELLOW" };

    private void handleWalls(String message) {
        WhoRequeue who = (WhoRequeue) RequeueFeature.INSTANCE.getRequeue();
        if (message.startsWith("Players Alive")) {
            who.clearWhoNames();
            criteria.add("RED: ");
            who.setDelayedValid(false);
            return;
        }
        for (int i = 0; i < WALLS_COLORS.length; i++) {
            String color = WALLS_COLORS[i];
            if (!message.startsWith(color + ": ")) continue;
            if (i < WALLS_COLORS.length - 1) {
                criteria.add(WALLS_COLORS[i + 1] + ": ");
            }
            String[] players = message.split(": ", 2)[1].trim().split(", ");
            for (String player : players) {
                who.addWhoName(player.trim());
            }
            if ("YELLOW".equals(color)) {
                who.setDelayedValid(true);
            }
            break;
        }
    }

    private void handleCvc(String message) {
        WhoRequeue who = (WhoRequeue) RequeueFeature.INSTANCE.getRequeue();
        if (message.startsWith("Crims: ") || message.startsWith("Cops: ")) {
            if (message.startsWith("Cops: ")) {
                who.clearWhoNames();
                criteria.add("Crims: ");
                who.setDelayedValid(false);
                return;
            }
            String[] playerNames = message.split(": ", 2)[1].trim().split(", ");
            for (String playerName : playerNames) {
                who.addWhoName(playerName.trim());
            }
            if (message.startsWith("Crims: ")) {
                who.setDelayedValid(true);
            }
        }
    }
}
