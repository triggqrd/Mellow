package com.roxiun.mellow.feature.requeue;

import com.roxiun.mellow.feature.requeue.listeners.ChatListener;
import java.util.Locale;
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
        // requeueCleanup() is handled by WorldListener.onWorldLoad(),
        // which is the only caller of invalidateLocraw()
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
            gameType = obj.has("gametype") ? obj.get("gametype").getAsString().trim().toUpperCase(Locale.ROOT) : null;
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
        chatHandler.setHideCriteria("{\"server\":");
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
