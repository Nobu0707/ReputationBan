package dev.modplugin.reputationban.command;

import dev.modplugin.reputationban.util.CommandSuggestionUtil;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public final class ReportsTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandSuggestionUtil.reportsSubcommands(sender.hasPermission("reputationban.admin.reports"), args[0]);
        }
        if (!sender.hasPermission("reputationban.admin.reports")) {
            return List.of();
        }
        if (args.length == 2) {
            return CommandSuggestionUtil.reportsSecondArgumentSuggestions(args[0], List.of(), args[1]);
        }
        if (args.length == 3) {
            return CommandSuggestionUtil.reportsThirdArgumentSuggestions(args[0], args[2]);
        }
        return List.of();
    }
}
