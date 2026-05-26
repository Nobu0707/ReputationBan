package dev.modplugin.reputationban.model;

import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public record CommandActor(UUID uuid, String name) {
    public static final String CONSOLE_NAME = "CONSOLE";

    public static CommandActor from(CommandSender sender) {
        if (sender instanceof Player player) {
            return new CommandActor(player.getUniqueId(), player.getName());
        }
        return console();
    }

    public static CommandActor console() {
        return new CommandActor(null, CONSOLE_NAME);
    }

    public String databaseActorId() {
        return uuid == null ? CONSOLE_NAME : uuid.toString();
    }
}
