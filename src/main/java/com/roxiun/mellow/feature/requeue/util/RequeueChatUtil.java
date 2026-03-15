package com.roxiun.mellow.feature.requeue.util;

import com.roxiun.mellow.util.ChatUtils;

public class RequeueChatUtil {

    public static void sendMessage(String message) {
        ChatUtils.sendMessage("§cRequeue §7» §e" + message);
    }

    public static String removeColorCodes(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("§[0-9a-fk-or]", "");
    }
}
