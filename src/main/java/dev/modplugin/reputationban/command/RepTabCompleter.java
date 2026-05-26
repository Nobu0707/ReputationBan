package dev.modplugin.reputationban.command;

import dev.modplugin.reputationban.util.CommandSuggestionUtil;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class RepTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandSuggestionUtil.repSubcommands(sender::hasPermission, args[0]);
        }
        if (args.length == 2 && CommandSuggestionUtil.repSubcommandNeedsPlayer(args[0])) {
            List<String> names = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList();
            return CommandSuggestionUtil.filterByPrefix(names, args[1]);
        }
        if (args.length == 3) {
            return CommandSuggestionUtil.repThirdArgumentSuggestions(args[0], args[2]);
        }
        return List.of();
    }
}
