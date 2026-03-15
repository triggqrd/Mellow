package com.roxiun.mellow.feature.requeue.auto;

import com.roxiun.mellow.feature.requeue.LocationManager;
import com.roxiun.mellow.feature.requeue.PartyManager;
import com.roxiun.mellow.feature.requeue.RequeueFeature;
import com.roxiun.mellow.feature.requeue.listeners.ChatListener;
import com.roxiun.mellow.feature.requeue.util.GameUtil;
import com.roxiun.mellow.feature.requeue.util.RequeueChatUtil;
import com.roxiun.mellow.feature.requeue.util.Timer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.network.NetworkPlayerInfo;

public class WhoRequeue implements IAutoRequeue {

    private static final Set<String> EXCEPTIONS =
        new HashSet<>(Arrays.asList("SKYWARS", "WALLS", "MCGO"));

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Timer whoTimer = new Timer();
    private List<String> lastTickNames = new ArrayList<>();
    private final List<String> whoNames = new ArrayList<>();
    private boolean delayedValid = false;
    private boolean unhandledPlayer = false;

    public void setDelayedValid(boolean state) {
        delayedValid = state;
    }

    public void handlePlayer() {
        unhandledPlayer = true;
    }

    private boolean isWhoValid() {
        if (
            LocationManager.instance != null &&
            "SURVIVAL_GAMES".equalsIgnoreCase(LocationManager.instance.getType())
        ) {
            if (whoNames.size() == 1) {
                try {
                    int number = Integer.parseInt(whoNames.get(0));
                    if (number <= 99) {
                        return false;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        if (
            LocationManager.instance != null &&
            LocationManager.instance.getType() != null &&
            EXCEPTIONS.contains(LocationManager.instance.getType().toUpperCase().trim())
        ) {
            return delayedValid;
        }
        return true;
    }

    public void addWhoName(String name) {
        if (name == null || name.isEmpty()) {
            return;
        }
        whoNames.add(name.trim());
    }

    public void clearWhoNames() {
        whoNames.clear();
    }

    private boolean playerLeft(List<String> from, List<String> to) {
        if (from.isEmpty()) {
            return false;
        }
        Set<String> current = new HashSet<>(to);
        for (String s : from) {
            if (!current.contains(s)) {
                return true;
            }
        }
        return false;
    }

    public void handleSendWho() {
        List<String> names = new ArrayList<>(mc.getNetHandler().getPlayerInfoMap().size());
        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            names.add(info.getGameProfile().getName().toLowerCase().trim());
        }
        boolean playerChanged = playerLeft(lastTickNames, names);
        unhandledPlayer = playerChanged || unhandledPlayer;
        boolean canRecheckWho = whoNames.isEmpty() || playerChanged;
        lastTickNames = names;

        String mode = LocationManager.instance != null ? LocationManager.instance.getMode() : null;
        if (
            (canRecheckWho || unhandledPlayer) &&
            mode != null &&
            whoTimer.hasTimeElapsed(5000, true)
        ) {
            if (mc.thePlayer != null) {
                mc.thePlayer.sendChatMessage("/who");
            }
            ChatListener chatHandler = RequeueFeature.INSTANCE.getChatListener();
            if (chatHandler != null) {
                chatHandler.criteria.clear();
                if (
                    LocationManager.instance != null &&
                    LocationManager.instance.getType() != null &&
                    LocationManager.instance.getType().equalsIgnoreCase("SKYWARS")
                ) {
                    chatHandler.criteria.add("Mode:");
                }
                chatHandler.criteria.add("ONLINE:");
                chatHandler.criteria.add("ALIVE:");
                chatHandler.criteria.add("This command is not available on this server!");
                chatHandler.criteria.add("Couldn't find players, sorry!");
                chatHandler.criteria.add("Command not supported!");
                chatHandler.criteria.add("This command is not available while player names are scrambled!");
                chatHandler.criteria.add("Game hasn't started yet!");
                chatHandler.criteria.add("You cannot use that right now!");
                chatHandler.criteria.add("You cannot use this right now.");
                chatHandler.criteria.add("None!");
                chatHandler.criteria.add("Picked Teams");
                chatHandler.criteria.add("Players Alive");
                chatHandler.criteria.add("Cops:");
            }
            unhandledPlayer = false;
        }
    }

    @Override
    public boolean canRequeue() {
        if (PartyManager.instance == null) return false;
        for (String s : whoNames) {
            if (PartyManager.instance.partyContains(s)) {
                return false;
            }
        }
        if (
            RequeueFeature.INSTANCE.includeClientPlayer() &&
            mc.thePlayer != null
        ) {
            return !whoNames.contains(mc.thePlayer.getName().trim());
        }
        return true;
    }

    @Override
    public void requeueCleanup() {
        lastTickNames.clear();
        clearWhoNames();
    }

    @Override
    public void onTick() {
        RequeueFeature feature = RequeueFeature.INSTANCE;
        if (feature == null || !feature.modEnabled()) return;
        if (!feature.isAutoEnabled()) return;
        if (LocationManager.instance == null || !LocationManager.instance.isLocrawValid()) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (mc.currentScreen instanceof GuiDownloadTerrain) return;
        String type = LocationManager.instance.getType();
        String mode = LocationManager.instance.getMode();
        if (type == null || mode == null) return;
        if (
            (type.equalsIgnoreCase("SKYWARS") || type.equalsIgnoreCase("SURVIVAL_GAMES")) &&
            (mode.contains("teams") || mode.contains("mega"))
        ) {
            return;
        }

        handleSendWho();
        if (whoNames.isEmpty()) return;
        if (
            isWhoValid() &&
            canRequeue() &&
            feature.getRequeueTimer().hasTimeElapsed(10000, true)
        ) {
            String gameId = GameUtil.getGameID(type, mode);
            if (gameId == null) {
                RequeueChatUtil.sendMessage("There was an issue finding your game mode right now!");
                return;
            }
            RequeueChatUtil.sendMessage("Attempted requeue.");
            requeueCleanup();
            mc.thePlayer.sendChatMessage("/play " + gameId);
        }
    }
}
