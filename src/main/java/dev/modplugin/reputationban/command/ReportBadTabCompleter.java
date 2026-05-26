package dev.modplugin.reputationban.command;

import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.util.CommandSuggestionUtil;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class ReportBadTabCompleter implements TabCompleter {
    private final java.util.function.Supplier<PluginConfig> configSupplier;

    public ReportBadTabCompleter(java.util.function.Supplier<PluginConfig> configSupplier) {
        this.configSupplier = configSupplier;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("reputationban.report")) {
            return List.of();
        }
        if (args.length == 1) {
            String senderName = sender instanceof Player player ? player.getName() : null;
            List<String> names = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> senderName == null || !name.equalsIgnoreCase(senderName))
                    .toList();
            return CommandSuggestionUtil.filterByPrefix(names, args[0]);
        }
        if (args.length == 2) {
            return CommandSuggestionUtil.filterByPrefix(configSupplier.get().categories().keySet(), args[1]);
        }
        return List.of();
    }
}
