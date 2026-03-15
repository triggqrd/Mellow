package com.roxiun.mellow.feature.requeue.commands;

import com.roxiun.mellow.feature.requeue.PartyManager;
import com.roxiun.mellow.feature.requeue.RequeueFeature;
import com.roxiun.mellow.feature.requeue.util.RequeueChatUtil;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

public class RequeuePartyListCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "requeueparty";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/requeueparty <add/remove/clear/list>";
    }

    @Override
    public List<String> addTabCompletionOptions(
        ICommandSender sender,
        String[] args,
        BlockPos pos
    ) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "add", "remove", "list", "clear");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return getListOfStringsMatchingLastWord(
                args,
                PartyManager.instance.getParty().toArray(new String[0])
            );
        }
        return Collections.emptyList();
    }

    private void list() {
        StringBuilder builder = new StringBuilder();
        if (
            RequeueFeature.INSTANCE.includeClientPlayer() &&
            Minecraft.getMinecraft().thePlayer != null
        ) {
            builder.append(Minecraft.getMinecraft().thePlayer.getName());
        }
        for (String player : PartyManager.instance.getParty()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(player);
        }
        if (builder.length() == 0) {
            RequeueChatUtil.sendMessage("Party tracking list is empty.");
        } else {
            RequeueChatUtil.sendMessage(builder.append('.').toString());
        }
    }

    private void add(String player) {
        PartyManager.instance.registerPlayer(player);
        RequeueChatUtil.sendMessage("Added " + player + " to the party list.");
    }

    private void remove(String player) {
        if (PartyManager.instance.removePlayer(player)) {
            RequeueChatUtil.sendMessage("Removed " + player + " from the party list.");
        } else {
            RequeueChatUtil.sendMessage("Couldn't find " + player + " in the party list.");
        }
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            RequeueChatUtil.sendMessage("Please specify <add/remove/clear/list>.");
            return;
        }
        String action = args[0].toLowerCase();
        switch (action) {
            case "list":
                list();
                return;
            case "clear":
                PartyManager.instance.clearParty();
                RequeueChatUtil.sendMessage("Cleared the party list.");
                return;
            case "add":
            case "remove":
                if (args.length < 2) {
                    RequeueChatUtil.sendMessage("Please specify a player name.");
                    return;
                }
                if (action.equals("add")) {
                    if (PartyManager.instance.partyContains(args[1])) {
                        RequeueChatUtil.sendMessage(
                            "The player " + args[1] + " is already tracked."
                        );
                        return;
                    }
                    add(args[1]);
                } else {
                    remove(args[1]);
                }
                return;
            default:
                RequeueChatUtil.sendMessage("Unknown action. Use add/remove/clear/list.");
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
