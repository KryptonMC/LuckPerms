package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.user.UserSubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.users.User;

import java.util.List;

public class UserRemoveGroupCommand extends UserSubCommand {
    public UserRemoveGroupCommand() {
        super("removegroup", "Removes a user from a group", "/perms user <user> removegroup <group> [server]", Permission.USER_REMOVEGROUP);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args) {
        String groupName = args.get(0).toLowerCase();

        if ((args.size() == 1 || (args.size() == 2 && args.get(1).equalsIgnoreCase("global")))
                && user.getPrimaryGroup().equalsIgnoreCase(groupName)) {
            Message.USER_REMOVEGROUP_ERROR_PRIMARY.send(sender);
            return;
        }

        try {
            if (args.size() == 2) {
                final String server = args.get(1).toLowerCase();
                user.unsetPermission("group." + groupName, server);
                Message.USER_REMOVEGROUP_SERVER_SUCCESS.send(sender, user.getName(), groupName, server);
            } else {
                user.unsetPermission("group." + groupName);
                Message.USER_REMOVEGROUP_SUCCESS.send(sender, user.getName(), groupName);
            }

            saveUser(user, sender, plugin);
        } catch (ObjectLacksException e) {
            Message.USER_NOT_MEMBER_OF.send(sender, user.getName(), groupName);
        }
    }

    @Override
    public List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        return getGroupTabComplete(args, plugin);
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return (argLength != 1 && argLength != 2);
    }
}
