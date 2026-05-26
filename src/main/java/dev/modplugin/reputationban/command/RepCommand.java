package dev.modplugin.reputationban.command;

import dev.modplugin.reputationban.ReputationBanPlugin;
import dev.modplugin.reputationban.model.PlayerRecord;
import dev.modplugin.reputationban.service.PlayerDataService;
import dev.modplugin.reputationban.service.PunishmentService;
import dev.modplugin.reputationban.service.ScoreService;
import dev.modplugin.reputationban.util.ManualScoreChangeGate;
import dev.modplugin.reputationban.util.ReporterPenalty;
import dev.modplugin.reputationban.util.ScoreMath;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class RepCommand implements CommandExecutor {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter FULL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final ReputationBanPlugin plugin;
    private final PlayerDataService playerDataService;
    private final ScoreService scoreService;
    private final PunishmentService punishmentService;

    public RepCommand(
            ReputationBanPlugin plugin,
            PlayerDataService playerDataService,
            ScoreService scoreService,
            PunishmentService punishmentService
    ) {
        this.plugin = plugin;
        this.playerDataService = playerDataService;
        this.scoreService = scoreService;
        this.punishmentService = punishmentService;
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
        if ("history".equalsIgnoreCase(args[0])) {
            history(sender, args);
            return true;
        }
        if ("add".equalsIgnoreCase(args[0]) || "remove".equalsIgnoreCase(args[0]) || "set".equalsIgnoreCase(args[0])) {
            mutateScore(sender, args);
            return true;
        }
        if ("reload".equalsIgnoreCase(args[0])) {
            reload(sender);
            return true;
        }

        sender.sendMessage(ReputationBanPlugin.PREFIX + "使い方: /rep, /rep check <player>, /rep history <player> [limit]");
        sender.sendMessage(ReputationBanPlugin.PREFIX + "使い方: /rep add <player> <points>, /rep remove <player> <points>, /rep set <player> <score>");
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
        UUID playerUuid = player.getUniqueId();
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
        if (!canViewOthers(sender)) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "権限がありません。");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "使い方: /rep check <player>");
            return;
        }

        resolvePlayer(args[1], true)
                .thenAccept(record -> plugin.runSync(() -> {
                    if (record.isEmpty()) {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "対象プレイヤーが見つかりません。");
                        return;
                    }
                    PlayerRecord value = record.get();
                    sender.sendMessage(ReputationBanPlugin.PREFIX + value.name() + " の評判スコア: "
                            + value.score() + " / " + plugin.pluginConfig().maxScore());
                    sender.sendMessage(ReputationBanPlugin.PREFIX + "BAN回数: " + value.banCount()
                            + " / 虚偽通報回数: " + value.falseReportCount());
                    long now = System.currentTimeMillis();
                    if (ReporterPenalty.isReportBanned(value.reportBannedUntil(), now)) {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "通報停止中: はい / 解除予定: "
                                + FULL_FORMATTER.format(Instant.ofEpochMilli(value.reportBannedUntil())));
                    } else {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "通報停止中: いいえ");
                    }
                }))
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Failed to check reputation score: " + throwable.getMessage());
                    plugin.runSync(() -> sender.sendMessage(ReputationBanPlugin.PREFIX + "スコアの取得に失敗しました。"));
                    return null;
                });
    }

    private void history(CommandSender sender, String[] args) {
        if (!canViewOthers(sender)) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "権限がありません。");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "使い方: /rep history <player> [limit]");
            return;
        }
        int limit = args.length >= 3 ? parsePositiveInt(args[2], 10, 50) : 10;
        resolvePlayer(args[1], true)
                .thenCompose(record -> {
                    if (record.isEmpty()) {
                        return CompletableFuture.completedFuture(new HistoryResult(Optional.empty(), java.util.List.of()));
                    }
                    return scoreService.history(record.get().uuid(), limit)
                            .thenApply(history -> new HistoryResult(record, history));
                })
                .thenAccept(result -> plugin.runSync(() -> {
                    if (result.record().isEmpty()) {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "対象プレイヤーが見つかりません。");
                        return;
                    }
                    sender.sendMessage(ReputationBanPlugin.PREFIX + result.record().get().name() + " のスコア履歴: " + result.history().size() + "件");
                    for (ScoreService.ScoreHistoryEntry entry : result.history()) {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "%s %d -> %d (%+d) [%s] %s".formatted(
                                FORMATTER.format(Instant.ofEpochMilli(entry.createdAt())),
                                entry.oldScore(),
                                entry.newScore(),
                                entry.delta(),
                                entry.sourceType(),
                                entry.reason()
                        ));
                    }
                }))
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Failed to load score history: " + throwable.getMessage());
                    plugin.runSync(() -> sender.sendMessage(ReputationBanPlugin.PREFIX + "スコア履歴の取得に失敗しました。"));
                    return null;
                });
    }

    private void mutateScore(CommandSender sender, String[] args) {
        if (!sender.hasPermission("reputationban.admin.score")) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "権限がありません。");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "使い方: /rep " + args[0].toLowerCase() + " <player> <points|score> [reason]");
            return;
        }
        Integer amount = parseInteger(sender, args[2]);
        if (amount == null) {
            return;
        }
        if (amount < 0) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "数値は0以上で指定してください。");
            return;
        }
        String suppliedReason = args.length >= 4 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "理由未指定";
        String reason = "Admin " + sender.getName() + ": " + suppliedReason;
        boolean senderCanBan = sender.hasPermission("reputationban.admin.ban");
        resolvePlayer(args[1], false)
                .thenCompose(record -> {
                    if (record.isEmpty()) {
                        return CompletableFuture.completedFuture(new MutationResult(Optional.empty(), null, false));
                    }
                    PlayerRecord target = record.get();
                    int requestedScore = switch (args[0].toLowerCase()) {
                        case "add" -> target.score() + amount;
                        case "remove" -> target.score() - amount;
                        case "set" -> amount;
                        default -> throw new IllegalArgumentException("unknown command");
                    };
                    int newScore = ScoreMath.clampToMax(requestedScore, plugin.pluginConfig().maxScore());
                    if (ManualScoreChangeGate.requiresBanPermission(target.score(), newScore, plugin.pluginConfig().banThreshold())
                            && !senderCanBan) {
                        return CompletableFuture.completedFuture(new MutationResult(record, null, false));
                    }
                    CompletableFuture<ScoreService.ScoreChange> change = switch (args[0].toLowerCase()) {
                        case "add" -> scoreService.applyDelta(target.uuid(), target.name(), amount, reason, "admin", null);
                        case "remove" -> scoreService.applyDelta(target.uuid(), target.name(), -amount, reason, "admin", null);
                        case "set" -> scoreService.setScore(target.uuid(), target.name(), amount, reason, "admin", null);
                        default -> throw new IllegalArgumentException("unknown command");
                    };
                    return change.thenCompose(scoreChange -> {
                        if (!scoreChange.crossedBanThreshold()) {
                            return CompletableFuture.completedFuture(new MutationResult(record, scoreChange, false));
                        }
                        return punishmentService.punishIfNeeded(
                                scoreChange.targetUuid(),
                                scoreChange.targetName(),
                                scoreChange.oldScore(),
                                scoreChange.newScore(),
                                "Admin score change reached " + scoreChange.newScore()
                        ).thenApply(banned -> new MutationResult(record, scoreChange, banned));
                    });
                })
                .thenAccept(result -> plugin.runSync(() -> {
                    if (result.record().isEmpty()) {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "対象プレイヤーが見つかりません。");
                        return;
                    }
                    ScoreService.ScoreChange change = result.change();
                    if (change == null) {
                        sender.sendMessage(ReputationBanPlugin.PREFIX
                                + "この操作は対象をBANしきい値以下にするため、reputationban.admin.ban 権限が必要です。");
                        return;
                    }
                    sender.sendMessage(ReputationBanPlugin.PREFIX + result.record().get().name() + " のスコアを変更しました: "
                            + change.oldScore() + " -> " + change.newScore() + " (" + signed(change.delta()) + ")");
                    if (result.banned()) {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "対象プレイヤーは評判スコアによりBAN処理されました。");
                    }
                }))
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Failed to mutate reputation score: " + throwable.getMessage());
                    plugin.runSync(() -> sender.sendMessage(ReputationBanPlugin.PREFIX + "スコア変更に失敗しました。"));
                    return null;
                });
    }

    private CompletableFuture<Optional<PlayerRecord>> resolvePlayer(String name, boolean createOnlineRecord) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            UUID targetUuid = online.getUniqueId();
            String targetName = online.getName();
            if (createOnlineRecord) {
                return playerDataService.ensurePlayer(targetUuid, targetName)
                        .thenCompose(ignored -> playerDataService.getPlayerRecord(targetUuid));
            }
            return playerDataService.getPlayerRecord(targetUuid)
                    .thenCompose(record -> record.isPresent()
                            ? CompletableFuture.completedFuture(record)
                            : playerDataService.ensurePlayer(targetUuid, targetName)
                                    .thenCompose(ignored -> playerDataService.getPlayerRecord(targetUuid)));
        }
        return playerDataService.findByName(name);
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("reputationban.admin")) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "権限がありません。");
            return;
        }
        plugin.reloadPluginConfig();
        sender.sendMessage(ReputationBanPlugin.PREFIX + "設定を再読み込みしました。");
    }

    private static boolean canViewOthers(CommandSender sender) {
        return sender.hasPermission("reputationban.score.others") || sender.hasPermission("reputationban.admin.score");
    }

    private static Integer parseInteger(CommandSender sender, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "数値を指定してください。");
            return null;
        }
    }

    private static int parsePositiveInt(String value, int fallback, int max) {
        try {
            return Math.max(1, Math.min(max, Integer.parseInt(value)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static String signed(int value) {
        return value >= 0 ? "+" + value : Integer.toString(value);
    }

    private record HistoryResult(Optional<PlayerRecord> record, java.util.List<ScoreService.ScoreHistoryEntry> history) {
    }

    private record MutationResult(Optional<PlayerRecord> record, ScoreService.ScoreChange change, boolean banned) {
    }
}
