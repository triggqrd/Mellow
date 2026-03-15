package com.roxiun.mellow.feature.requeue.auto;

import com.roxiun.mellow.feature.requeue.LocationManager;
import com.roxiun.mellow.feature.requeue.PartyManager;
import com.roxiun.mellow.feature.requeue.RequeueFeature;
import com.roxiun.mellow.feature.requeue.util.GameUtil;
import com.roxiun.mellow.feature.requeue.util.RequeueChatUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.network.NetworkPlayerInfo;

public class TabRequeue implements IAutoRequeue {

    private final Minecraft mc = Minecraft.getMinecraft();

    @Override
    public void onTick() {
        RequeueFeature feature = RequeueFeature.INSTANCE;
        if (feature == null || !feature.modEnabled()) return;
        if (!feature.isAutoEnabled()) return;
        if (LocationManager.instance == null || !LocationManager.instance.isLocrawValid()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen instanceof GuiDownloadTerrain) return;
        if (LocationManager.instance.getType() == null) return;

        if (canRequeue() && feature.getRequeueTimer().hasTimeElapsed(10000, true)) {
            String gameId = GameUtil.getGameID(
                LocationManager.instance.getType(),
                LocationManager.instance.getMode()
            );
            if (gameId == null) {
                RequeueChatUtil.sendMessage("There was an issue finding your game mode right now!");
                return;
            }
            RequeueChatUtil.sendMessage("Attempted requeue.");
            requeueCleanup();
            mc.thePlayer.sendChatMessage("/play " + gameId);
        }
    }

    @Override
    public boolean canRequeue() {
        if (PartyManager.instance == null) {
            return false;
        }
        Set<String> dead = new HashSet<>();
        List<String> players = new ArrayList<>(PartyManager.instance.getParty());
        if (RequeueFeature.INSTANCE.includeClientPlayer() && mc.thePlayer != null) {
            players.add(mc.thePlayer.getName().trim());
        }
        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            try {
                if (
                    info.getPlayerTeam() != null &&
                    info.getPlayerTeam().getColorPrefix() != null &&
                    info.getPlayerTeam().getColorPrefix().contains("§7")
                ) {
                    dead.add(info.getGameProfile().getName());
                }
            } catch (Exception ignored) {}
        }
        return dead.containsAll(players);
    }

    @Override
    public void requeueCleanup() {}
}
