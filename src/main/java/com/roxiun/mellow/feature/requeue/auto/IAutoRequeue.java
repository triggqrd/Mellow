package com.roxiun.mellow.feature.requeue.auto;

public interface IAutoRequeue {
    void onTick();
    boolean canRequeue();
    void requeueCleanup();
}
