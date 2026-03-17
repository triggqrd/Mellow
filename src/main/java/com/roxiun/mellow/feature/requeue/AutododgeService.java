package com.roxiun.mellow.feature.requeue;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.feature.requeue.util.GameUtil;
import com.roxiun.mellow.feature.requeue.util.RequeueChatUtil;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutododgeService {

    private static AutododgeService instance;

    private final MellowOneConfig config;
    private final AtomicBoolean dodgedThisLobby = new AtomicBoolean(false);

    public AutododgeService(MellowOneConfig config) {
        this.config = config;
        instance = this;
    }

    public static AutododgeService getInstance() {
        return instance;
    }

    public void resetForNewLobby() {
        dodgedThisLobby.set(false);
    }

    public boolean hasDodgedThisLobby() {
        return dodgedThisLobby.get();
    }

    public void checkNickedAndDodge(String playerName, boolean isPregame) {
        if (!config.autododgeEnabled) return;
        if (!config.autododgeNicked) return;
        if (dodgedThisLobby.get()) return;
        if (!isPregame && config.autododgeMode == 0) return;

        RequeueFeature feature = RequeueFeature.INSTANCE;
        if (feature == null || !feature.modEnabled()) return;

        if (!dodgedThisLobby.compareAndSet(false, true)) return;

        MainThreadDispatcher.run(() -> {
            RequeueChatUtil.sendMessage("Dodging " + playerName + " (Nicked)");
            GameUtil.safeRequeue();
        });
    }

    public void checkAndDodge(String playerName, BedwarsPlayer player, boolean isPregame) {
        // Cheapest checks first: field reads and atomic read
        if (!config.autododgeEnabled) return;
        if (dodgedThisLobby.get()) return;
        if (!isPregame && config.autododgeMode == 0) return;
        if (config.autododgeMinFkdr <= 0f && config.autododgeMinStars <= 0) return;

        // More expensive check: involves getCurrentServerData()
        RequeueFeature feature = RequeueFeature.INSTANCE;
        if (feature == null || !feature.modEnabled()) return;

        double fkdr = player.getFkdr();
        int stars = parseStars(player.getStars());

        boolean fkdrTriggered = config.autododgeMinFkdr > 0 && fkdr >= config.autododgeMinFkdr;
        boolean starsTriggered = config.autododgeMinStars > 0 && stars >= config.autododgeMinStars;

        if (fkdrTriggered || starsTriggered) {
            if (!dodgedThisLobby.compareAndSet(false, true)) return;

            String reason;
            if (fkdrTriggered && starsTriggered) {
                reason = "FKDR: " + player.getFormattedFkdr() + ", Stars: " + stars;
            } else if (fkdrTriggered) {
                reason = "FKDR: " + player.getFormattedFkdr();
            } else {
                reason = "Stars: " + stars;
            }

            String finalReason = reason;
            MainThreadDispatcher.run(() -> {
                RequeueChatUtil.sendMessage("Dodging " + playerName + " (" + finalReason + ")");
                GameUtil.safeRequeue();
            });
        }
    }

    /**
     * Extracts the numeric star value from a formatted stars string
     * like "§6[100✫]". Single pass, zero allocations.
     */
    static int parseStars(String stars) {
        if (stars == null) return 0;
        int result = 0;
        boolean skipNext = false;
        boolean foundDigit = false;
        for (int i = 0; i < stars.length(); i++) {
            char c = stars.charAt(i);
            if (c == '\u00A7') { // §
                skipNext = true;
                continue;
            }
            if (skipNext) {
                skipNext = false;
                continue;
            }
            if (c >= '0' && c <= '9') {
                result = result * 10 + (c - '0');
                foundDigit = true;
            }
        }
        return foundDigit ? result : 0;
    }
}
