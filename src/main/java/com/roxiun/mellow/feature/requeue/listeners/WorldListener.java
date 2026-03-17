package com.roxiun.mellow.feature.requeue.listeners;

import com.roxiun.mellow.feature.requeue.AutododgeService;
import com.roxiun.mellow.feature.requeue.LocationManager;
import com.roxiun.mellow.feature.requeue.RequeueFeature;
import com.roxiun.mellow.feature.requeue.auto.IAutoRequeue;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class WorldListener {

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        RequeueFeature feature = RequeueFeature.INSTANCE;
        if (feature == null || !feature.isFeatureEnabled()) {
            return;
        }
        if (LocationManager.instance != null) {
            LocationManager.instance.invalidateLocraw();
        }
        feature.getTickListener().resetTimer();
        IAutoRequeue requeue = feature.getRequeue();
        if (requeue != null) {
            requeue.requeueCleanup();
        }
        AutododgeService autododge = AutododgeService.getInstance();
        if (autododge != null) {
            autododge.resetForNewLobby();
        }
    }
}
