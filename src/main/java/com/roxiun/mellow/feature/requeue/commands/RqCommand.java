package com.roxiun.mellow.feature.requeue.commands;

import com.roxiun.mellow.feature.requeue.util.GameUtil;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

public class RqCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "rq";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/rq";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        GameUtil.safeRequeue();
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
