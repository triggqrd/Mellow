package com.roxiun.mellow.feature.requeue.commands;

import com.roxiun.mellow.feature.requeue.RequeueFeature;
import com.roxiun.mellow.feature.requeue.util.GameUtil;
import com.roxiun.mellow.feature.requeue.util.RequeueChatUtil;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

public class RequeueCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "requeue";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/requeue";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
            sendHelp();
            return;
        }
        GameUtil.safeRequeue();
    }

    private void sendHelp() {
        RequeueChatUtil.sendMessage(
            "Use the Mellow OneConfig menu (Right Shift) → Requeue to configure auto requeue behavior."
        );
        RequeueChatUtil.sendMessage(
            "Commands: /requeue, /rq, /requeueparty add|remove|list|clear"
        );
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
