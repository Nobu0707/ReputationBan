package dev.modplugin.reputationban.command;

import dev.modplugin.reputationban.ReputationBanPlugin;
import dev.modplugin.reputationban.model.PlayerRecord;
import dev.modplugin.reputationban.service.PlayerDataService;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class RepCommand implements CommandExecutor {
    private final ReputationBanPlugin plugin;
    private final PlayerDataService playerDataService;

    public RepCommand(ReputationBanPlugin plugin, PlayerDataService playerDataService) {
        this.plugin = plugin;
        this.playerDataService = playerDataService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showSelf(sender);
            return true;
        }
        if ("check".equalsIgnoreCase(args[0])) {
            checkOther(sender, args);
            return true;
        }
        if ("reload".equalsIgnoreCase(args[0])) {
            reload(sender);
            return true;
        }

        sender.sendMessage(ReputationBanPlugin.PREFIX + "使い方: /rep, /rep check <player>, /rep reload");
        return true;
    }

    private void showSelf(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "このコマンドはプレイヤーのみ実行できます。");
            return;
        }
        if (!player.hasPermission("reputationban.score.self")) {
            player.sendMessage(ReputationBanPlugin.PREFIX + "権限がありません。");
            return;
        }
        java.util.UUID playerUuid = player.getUniqueId();
        String playerName = player.getName();
        playerDataService.ensurePlayer(playerUuid, playerName)
                .thenCompose(ignored -> playerDataService.getPlayerRecord(playerUuid))
                .thenAccept(record -> plugin.runSync(() -> {
                    int score = record.map(PlayerRecord::score).orElse(plugin.pluginConfig().initialScore());
                    player.sendMessage(ReputationBanPlugin.PREFIX + "あなたの評判スコア: " + score + " / " + plugin.pluginConfig().maxScore());
                }))
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Failed to load reputation score: " + throwable.getMessage());
                    plugin.runSync(() -> player.sendMessage(ReputationBanPlugin.PREFIX + "スコアの取得に失敗しました。"));
                    return null;
                });
    }

    private void checkOther(CommandSender sender, String[] args) {
        if (!sender.hasPermission("reputationban.score.others") && !sender.hasPermission("reputationban.admin.score")) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "権限がありません。");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "使い方: /rep check <player>");
            return;
        }

        Player online = Bukkit.getPlayerExact(args[1]);
        java.util.concurrent.CompletableFuture<Optional<PlayerRecord>> lookup;
        if (online != null) {
            java.util.UUID targetUuid = online.getUniqueId();
            String targetName = online.getName();
            lookup = playerDataService.ensurePlayer(targetUuid, targetName)
                    .thenCompose(ignored -> playerDataService.getPlayerRecord(targetUuid));
        } else {
            lookup = playerDataService.findByName(args[1]);
        }

        lookup.thenAccept(record -> plugin.runSync(() -> {
                    if (record.isEmpty()) {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "対象プレイヤーが見つかりません。");
                        return;
                    }
                    PlayerRecord value = record.get();
                    sender.sendMessage(ReputationBanPlugin.PREFIX + value.name() + " の評判スコア: "
                            + value.score() + " / " + plugin.pluginConfig().maxScore());
                    sender.sendMessage(ReputationBanPlugin.PREFIX + "BAN回数: " + value.banCount()
                            + " / 虚偽通報回数: " + value.falseReportCount());
                }))
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Failed to check reputation score: " + throwable.getMessage());
                    plugin.runSync(() -> sender.sendMessage(ReputationBanPlugin.PREFIX + "スコアの取得に失敗しました。"));
                    return null;
                });
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("reputationban.admin")) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "権限がありません。");
            return;
        }
        plugin.reloadPluginConfig();
        sender.sendMessage(ReputationBanPlugin.PREFIX + "設定を再読み込みしました。");
    }
}
