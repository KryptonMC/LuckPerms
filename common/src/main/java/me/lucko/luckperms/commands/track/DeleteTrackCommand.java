package me.lucko.luckperms.commands.track;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.MainCommand;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.tracks.Track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DeleteTrackCommand extends MainCommand {
    public DeleteTrackCommand() {
        super("DeleteTrack", "/perms deletetrack <track>", 1);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        if (args.size() == 0) {
            sendUsage(sender);
            return;
        }

        String trackName = args.get(0).toLowerCase();

        plugin.getDatastore().loadTrack(trackName, success -> {
            if (!success) {
                Message.TRACK_DOES_NOT_EXIST.send(sender);
            } else {

                Track track = plugin.getTrackManager().getTrack(trackName);
                if (track == null) {
                    Message.TRACK_LOAD_ERROR.send(sender);
                } else {
                    plugin.getDatastore().deleteTrack(track, success1 -> {
                        if (!success1) {
                            Message.DELETE_TRACK_ERROR.send(sender);
                        } else {
                            Message.DELETE_SUCCESS.send(sender, trackName);
                            plugin.runUpdateTask();
                        }
                    });
                }
            }
        });
    }

    @Override
    protected List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        final List<String> tracks = new ArrayList<>(plugin.getTrackManager().getTracks().keySet());

        if (args.size() <= 1) {
            if (args.isEmpty() || args.get(0).equalsIgnoreCase("")) {
                return tracks;
            }

            return tracks.stream().filter(s -> s.toLowerCase().startsWith(args.get(0).toLowerCase())).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    @Override
    public List<SubCommand> getSubCommands() {
        return Collections.emptyList();
    }

    @Override
    protected boolean canUse(Sender sender) {
        return Permission.DELETE_TRACK.isAuthorized(sender);
    }
}
