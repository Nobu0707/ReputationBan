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
        List<String> names = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .toList();
        if (args.length == 2) {
            return CommandSuggestionUtil.repSecondArgumentSuggestions(args[0], names, args[1]);
        }
        if (args.length == 3) {
            if ("audit".equalsIgnoreCase(args[0])) {
                return CommandSuggestionUtil.repAuditThirdArgumentSuggestions(args[1], names, args[2]);
            }
            return CommandSuggestionUtil.repThirdArgumentSuggestions(args[0], args[2]);
        }
        if (args.length == 4 && "audit".equalsIgnoreCase(args[0])) {
            return CommandSuggestionUtil.repAuditFourthArgumentSuggestions(args[1], args[3]);
        }
        return List.of();
    }
}
