package com.roxiun.mellow.feature.requeue;

import com.roxiun.mellow.feature.requeue.listeners.ChatListener;
import net.minecraft.client.Minecraft;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class LocationManager {

    public static LocationManager instance;

    private String locraw;
    private String gameType;
    private String gameMode;
    private final Minecraft mc;
    private final ChatListener chatHandler;
    private final JsonParser parser = new JsonParser();
    private boolean awaitingLocraw = true;

    public LocationManager() {
        this.mc = Minecraft.getMinecraft();
        this.chatHandler = RequeueFeature.INSTANCE.getChatListener();
        instance = this;
    }

    public void invalidateLocraw() {
        locraw = null;
        gameType = null;
        gameMode = null;
        awaitingLocraw = true;
        if (RequeueFeature.INSTANCE != null && RequeueFeature.INSTANCE.getRequeue() != null) {
            RequeueFeature.INSTANCE.getRequeue().requeueCleanup();
        }
    }

    public boolean isAwaitingLocraw() {
        return awaitingLocraw;
    }

    public void setLocraw(String message) {
        locraw = message;
        if (message == null) {
            gameType = null;
            gameMode = null;
            awaitingLocraw = true;
            return;
        }
        try {
            JsonObject obj = parser.parse(message).getAsJsonObject();
            gameType = obj.has("gametype") ? obj.get("gametype").getAsString().trim() : null;
            gameMode = obj.has("mode") ? obj.get("mode").getAsString() : null;
            awaitingLocraw = false;
        } catch (IllegalStateException | JsonSyntaxException e) {
            gameType = null;
            gameMode = null;
            awaitingLocraw = true;
        }
    }

    public boolean isLocrawValid() {
        return locraw != null;
    }

    public void sendLocraw() {
        if (chatHandler == null) {
            awaitingLocraw = true;
            return;
        }
        chatHandler.criteria.clear();
        chatHandler.criteria.add("{\"server\":");
        awaitingLocraw = false;
        if (mc.thePlayer != null) {
            mc.thePlayer.sendChatMessage("/locraw");
        }
    }

    public String getType() {
        return gameType;
    }

    public String getMode() {
        return gameMode;
    }

    public String getLocraw() {
        return locraw;
    }
}
