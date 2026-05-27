package dev.modplugin.reputationban.command;

import dev.modplugin.reputationban.ReputationBanPlugin;
import dev.modplugin.reputationban.model.AuditEvent;
import dev.modplugin.reputationban.model.AuditEventType;
import dev.modplugin.reputationban.model.CommandActor;
import dev.modplugin.reputationban.model.DiagnosticReport;
import dev.modplugin.reputationban.model.PlayerRecord;
import dev.modplugin.reputationban.notification.DiscordWebhookConfig;
import dev.modplugin.reputationban.notification.NotificationEventType;
import dev.modplugin.reputationban.service.AuditService;
import dev.modplugin.reputationban.service.DiagnosticService;
import dev.modplugin.reputationban.service.PlayerDataService;
import dev.modplugin.reputationban.service.PunishmentService;
import dev.modplugin.reputationban.service.ScoreService;
import dev.modplugin.reputationban.util.AuditCommandArgument;
import dev.modplugin.reputationban.util.AuditCommandArgumentParser;
import dev.modplugin.reputationban.util.AuditMetadata;
import dev.modplugin.reputationban.util.BanAuditMetadata;
import dev.modplugin.reputationban.util.CommandArgumentParser;
import dev.modplugin.reputationban.util.ManualScoreChangeGate;
import dev.modplugin.reputationban.util.MaintenancePolicy;
import dev.modplugin.reputationban.util.ReporterPenalty;
import dev.modplugin.reputationban.util.ScoreMath;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
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
    private final AuditService auditService;
    private final DiagnosticService diagnosticService;

    public RepCommand(
            ReputationBanPlugin plugin,
            PlayerDataService playerDataService,
            ScoreService scoreService,
            PunishmentService punishmentService,
            AuditService auditService,
            DiagnosticService diagnosticService
    ) {
        this.plugin = plugin;
        this.playerDataService = playerDataService;
        this.scoreService = scoreService;
        this.punishmentService = punishmentService;
        this.auditService = auditService;
        this.diagnosticService = diagnosticService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showSelf(sender);
            return true;
        }
        if ("help".equalsIgnoreCase(args[0])) {
            sendHelp(sender);
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
        if ("banhistory".equalsIgnoreCase(args[0])) {
            banHistory(sender, args);
            return true;
        }
        if ("baninfo".equalsIgnoreCase(args[0])) {
            banInfo(sender, args);
            return true;
        }
        if ("unban".equalsIgnoreCase(args[0])) {
            unban(sender, args);
            return true;
        }
        if ("pardon".equalsIgnoreCase(args[0])) {
            pardon(sender, args);
            return true;
        }
        if ("audit".equalsIgnoreCase(args[0])) {
            audit(sender, args);
            return true;
        }
        if ("maintenance".equalsIgnoreCase(args[0])) {
            maintenance(sender, args);
            return true;
        }
        if ("doctor".equalsIgnoreCase(args[0]) || "diagnostics".equalsIgnoreCase(args[0])) {
            doctor(sender);
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

        sender.sendMessage(ReputationBanPlugin.PREFIX + "不明なサブコマンドです。/rep help を確認してください。");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        if (sender instanceof Player && sender.hasPermission("reputationban.score.self")) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "/rep - 自分のスコアを表示");
        }
        if (canViewOthers(sender)) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "/rep check <player> - プレイヤーのスコア確認");
            sender.sendMessage(ReputationBanPlugin.PREFIX + "/rep history <player> [limit] - スコア履歴");
        }
        if (sender.hasPermission("reputationban.admin.score")) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "/rep add <player> <points> [reason] - スコア加算");
            sender.sendMessage(ReputationBanPlugin.PREFIX + "/rep remove <player> <points> [reason] - スコア減算");
            sender.sendMessage(ReputationBanPlugin.PREFIX + "/rep set <player> <score> [reason] - スコア設定");
        }
        if (sender.hasPermission("reputationban.admin.ban")) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "/rep banhistory <player> [limit] - BAN履歴");
            sender.sendMessage(ReputationBanPlugin.PREFIX + "/rep baninfo <player> - BAN状態確認");
            sender.sendMessage(ReputationBanPlugin.PREFIX + "/rep unban <player> [reason] - BAN解除");
        }
        if (sender.hasPermission("reputationban.admin.score") && sender.hasPermission("reputationban.admin.ban")) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "/rep pardon <player> [reason] - BAN解除 + スコア回復");
        }
        if (sender.hasPermission("reputationban.admin")) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "/rep reload - 設定再読み込み");
        }
        if (sender.hasPermission("reputationban.admin.audit")) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "/rep audit recent [limit] - 直近の監査ログ");
            sender.sendMessage(ReputationBanPlugin.PREFIX + "/rep audit <player> [limit] - 対象の監査ログ");
            sender.sendMessage(ReputationBanPlugin.PREFIX + "/rep audit type <eventType> [limit] - 種別別の監査ログ");
            sender.sendMessage(ReputationBanPlugin.PREFIX + "/rep audit export <recent|player> [limit] - CSVエクスポート");
        }
        if (sender.hasPermission("reputationban.admin.maintenance")) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "/rep maintenance preview - データ保持メンテナンスの予定件数");
            sender.sendMessage(ReputationBanPlugin.PREFIX + "/rep maintenance run confirm - バックアップ後にデータ保持メンテナンスを実行");
        }
        if (sender.hasPermission("reputationban.admin.diagnostics")) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "/rep doctor - ReputationBanの診断情報を表示");
        }
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
        OptionalInt parsedLimit = args.length >= 3 ? CommandArgumentParser.parseLimit(args[2], 50) : OptionalInt.of(10);
        if (parsedLimit.isEmpty()) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "limit は 1〜50 の数値で指定してください。");
            return;
        }
        int limit = parsedLimit.getAsInt();
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
                    auditService.recordEvent(AuditEvent.create(
                            AuditEventType.SCORE_CHANGED_ADMIN,
                            senderUuid(sender),
                            sender.getName(),
                            change.targetUuid(),
                            change.targetName(),
                            null,
                            null,
                            change.scoreHistoryId(),
                            change.oldScore(),
                            change.newScore(),
                            change.delta(),
                            reason,
                            AuditMetadata.create().put("command", args[0].toLowerCase()).toJson(),
                            System.currentTimeMillis()
                    ));
                    plugin.notifyScoreThresholdCrossings(
                            change.targetUuid(),
                            change.targetName(),
                            change.oldScore(),
                            change.newScore(),
                            "管理者スコア変更: " + sender.getName()
                    );
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

    private void audit(CommandSender sender, String[] args) {
        if (!sender.hasPermission("reputationban.admin.audit")) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "権限がありません。");
            return;
        }
        AuditCommandArgument parsed = AuditCommandArgumentParser.parse(args);
        switch (parsed.mode()) {
            case RECENT -> {
                OptionalInt limit = parseAuditLimit(sender, args.length >= 3 ? args[2] : null, plugin.pluginConfig().auditMaxDisplayLimit());
                limit.ifPresent(value -> auditService.listRecent(value)
                        .thenAccept(events -> plugin.runSync(() -> sendAuditEvents(sender, "recent", events)))
                        .exceptionally(throwable -> auditFailure(sender, throwable)));
            }
            case TARGET -> {
                OptionalInt limit = parseAuditLimit(sender, args.length >= 3 ? args[2] : null, plugin.pluginConfig().auditMaxDisplayLimit());
                if (limit.isEmpty()) {
                    return;
                }
                resolvePlayer(parsed.value(), true)
                        .thenCompose(record -> record
                                .map(player -> auditService.listForTarget(player.uuid(), limit.getAsInt())
                                        .thenApply(events -> new AuditListResult(record, events, player.name())))
                                .orElseGet(() -> CompletableFuture.completedFuture(new AuditListResult(Optional.empty(), List.of(), parsed.value()))))
                        .thenAccept(result -> plugin.runSync(() -> {
                            if (result.record().isEmpty()) {
                                sender.sendMessage(ReputationBanPlugin.PREFIX + "対象プレイヤーが見つかりません。");
                                return;
                            }
                            sendAuditEvents(sender, result.label(), result.events());
                        }))
                        .exceptionally(throwable -> auditFailure(sender, throwable));
            }
            case TYPE -> {
                if (parsed.value().isBlank() || !AuditEventType.isValid(parsed.value())) {
                    sender.sendMessage(ReputationBanPlugin.PREFIX + "使い方: /rep audit type <eventType> [limit]");
                    return;
                }
                OptionalInt limit = parseAuditLimit(sender, args.length >= 4 ? args[3] : null, plugin.pluginConfig().auditMaxDisplayLimit());
                limit.ifPresent(value -> auditService.listByType(AuditEventType.parse(parsed.value()), value)
                        .thenAccept(events -> plugin.runSync(() -> sendAuditEvents(sender, "type " + AuditEventType.parse(parsed.value()).databaseValue(), events)))
                        .exceptionally(throwable -> auditFailure(sender, throwable)));
            }
            case EXPORT_RECENT -> {
                OptionalInt limit = parseAuditLimit(sender, args.length >= 4 ? args[3] : null, plugin.pluginConfig().auditMaxExportLimit());
                limit.ifPresent(value -> auditService.exportRecentCsv(value)
                        .thenAccept(path -> plugin.runSync(() -> sender.sendMessage(ReputationBanPlugin.PREFIX
                                + "監査ログをエクスポートしました: " + path)))
                        .exceptionally(throwable -> auditFailure(sender, throwable)));
            }
            case EXPORT_TARGET -> {
                OptionalInt limit = parseAuditLimit(sender, args.length >= 4 ? args[3] : null, plugin.pluginConfig().auditMaxExportLimit());
                if (limit.isEmpty()) {
                    return;
                }
                resolvePlayer(parsed.value(), true)
                        .thenCompose(record -> record
                                .map(player -> auditService.exportTargetCsv(player.uuid(), player.name(), limit.getAsInt())
                                        .thenApply(path -> new AuditExportResult(record, path)))
                                .orElseGet(() -> CompletableFuture.completedFuture(new AuditExportResult(Optional.empty(), null))))
                        .thenAccept(result -> plugin.runSync(() -> {
                            if (result.record().isEmpty()) {
                                sender.sendMessage(ReputationBanPlugin.PREFIX + "対象プレイヤーが見つかりません。");
                                return;
                            }
                            sender.sendMessage(ReputationBanPlugin.PREFIX + "監査ログをエクスポートしました: " + result.path());
                        }))
                        .exceptionally(throwable -> auditFailure(sender, throwable));
            }
        }
    }

    private void maintenance(CommandSender sender, String[] args) {
        if (!sender.hasPermission("reputationban.admin.maintenance")) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "権限がありません。");
            return;
        }
        CommandActor actor = CommandActor.from(sender);
        switch (MaintenancePolicy.parse(args)) {
            case PREVIEW -> auditService.previewMaintenance(actor)
                    .thenAccept(result -> plugin.runSync(() -> sendMaintenanceResult(sender, "メンテナンス予定:", result, false)))
                    .exceptionally(throwable -> auditFailure(sender, throwable));
            case RUN_CONFIRMED -> auditService.runMaintenance(actor)
                    .thenAccept(result -> plugin.runSync(() -> sendMaintenanceResult(sender, "メンテナンス完了:", result, true)))
                    .exceptionally(throwable -> auditFailure(sender, throwable));
            case RUN_REQUIRES_CONFIRMATION -> {
                sender.sendMessage(ReputationBanPlugin.PREFIX + "データ削除を実行するには /rep maintenance run confirm を使用してください。");
                sender.sendMessage(ReputationBanPlugin.PREFIX + "事前確認は /rep maintenance preview で確認できます。");
            }
            case HELP -> sender.sendMessage(ReputationBanPlugin.PREFIX + "使い方: /rep maintenance <preview|run confirm>");
        }
    }

    private void banHistory(CommandSender sender, String[] args) {
        if (!sender.hasPermission("reputationban.admin.ban")) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "権限がありません。");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "使い方: /rep banhistory <player> [limit]");
            return;
        }
        OptionalInt parsedLimit = args.length >= 3 ? CommandArgumentParser.parseLimit(args[2], 50) : OptionalInt.of(10);
        if (parsedLimit.isEmpty()) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "limit は 1〜50 の数値で指定してください。");
            return;
        }
        int limit = parsedLimit.getAsInt();
        resolvePlayer(args[1], true)
                .thenCompose(record -> {
                    if (record.isEmpty()) {
                        return CompletableFuture.completedFuture(new BanHistoryResult(Optional.empty(), java.util.List.of()));
                    }
                    return punishmentService.banHistory(record.get().uuid(), limit)
                            .thenApply(history -> new BanHistoryResult(record, history));
                })
                .thenAccept(result -> plugin.runSync(() -> {
                    if (result.record().isEmpty()) {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "対象プレイヤーが見つかりません。");
                        return;
                    }
                    sender.sendMessage(ReputationBanPlugin.PREFIX + result.record().get().name()
                            + " のBAN履歴: " + result.history().size() + "件");
                    for (PunishmentService.BanHistoryEntry entry : result.history()) {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "#%d [%s] 作成:%s 期限:%s 解除:%s".formatted(
                                entry.id(),
                                entry.banType(),
                                FORMATTER.format(Instant.ofEpochMilli(entry.createdAt())),
                                formatNullable(entry.expiresAt()),
                                formatNullable(entry.unbannedAt())
                        ));
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "理由: " + entry.reason()
                                + " / 作成者: " + entry.createdBy()
                                + " / 解除者ID: " + fallbackDash(entry.unbannedBy())
                                + " / 解除者名: " + fallbackDash(entry.unbannedByName())
                                + " / 解除理由: " + fallbackDash(entry.unbanReason()));
                    }
                }))
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Failed to load ban history: " + throwable.getMessage());
                    plugin.runSync(() -> sender.sendMessage(ReputationBanPlugin.PREFIX + "BAN履歴の取得に失敗しました。"));
                    return null;
                });
    }

    private void banInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("reputationban.admin.ban")) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "権限がありません。");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "使い方: /rep baninfo <player>");
            return;
        }
        resolvePlayer(args[1], true)
                .thenCompose(record -> {
                    if (record.isEmpty()) {
                        return CompletableFuture.completedFuture(new BanInfoResult(Optional.empty(), false, null));
                    }
                    UUID targetUuid = record.get().uuid();
                    CompletableFuture<Boolean> profileBanned = plugin.supplySync(() -> Bukkit.getOfflinePlayer(targetUuid).isBanned());
                    CompletableFuture<PunishmentService.CurrentBanInfo> dbInfo = punishmentService.currentBanInfo(targetUuid);
                    return profileBanned.thenCombine(dbInfo, (paperBanned, info) -> new BanInfoResult(record, paperBanned, info));
                })
                .thenAccept(result -> plugin.runSync(() -> {
                    if (result.record().isEmpty()) {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "対象プレイヤーが見つかりません。");
                        return;
                    }
                    PlayerRecord record = result.record().get();
                    PunishmentService.CurrentBanInfo dbInfo = result.dbInfo();
                    sender.sendMessage(ReputationBanPlugin.PREFIX + record.name() + " のBAN情報");
                    sender.sendMessage(ReputationBanPlugin.PREFIX + "Paper/Profile BAN中: " + yesNo(result.profileBanned()));
                    sender.sendMessage(ReputationBanPlugin.PREFIX + "ReputationBan DB有効BAN: "
                            + yesNo(dbInfo.activeBanCount() > 0) + " (" + dbInfo.activeBanCount() + "件)");
                    sender.sendMessage(ReputationBanPlugin.PREFIX + "BAN回数: " + dbInfo.banCount());
                    if (dbInfo.latestActiveBan().isPresent()) {
                        PunishmentService.BanHistoryEntry latest = dbInfo.latestActiveBan().get();
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "最新BAN ID: #" + latest.id()
                                + " / 理由: " + latest.reason()
                                + " / 期限: " + formatNullable(latest.expiresAt()));
                    }
                    if (result.profileBanned() != (dbInfo.activeBanCount() > 0)) {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "注意: Paper側とReputationBan DB側のBAN状態が一致していません。");
                    }
                }))
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Failed to load ban info: " + throwable.getMessage());
                    plugin.runSync(() -> sender.sendMessage(ReputationBanPlugin.PREFIX + "BAN情報の取得に失敗しました。"));
                    return null;
                });
    }

    private void unban(CommandSender sender, String[] args) {
        if (!sender.hasPermission("reputationban.admin.ban")) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "権限がありません。");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "使い方: /rep unban <player> [reason]");
            return;
        }
        String reason = BanAuditMetadata.reasonFromArgs(args, 2);
        CommandActor actor = CommandActor.from(sender);
        resolvePlayer(args[1], true)
                .thenCompose(record -> {
                    if (record.isEmpty()) {
                        return CompletableFuture.completedFuture(new UnbanCompletion(Optional.empty(), null, null));
                    }
                    UUID targetUuid = record.get().uuid();
                    return punishmentService.unbanProfile(targetUuid)
                                    .thenCompose(profileResult -> punishmentService.markActiveBansUnbanned(
                                            targetUuid,
                                            actor,
                                            reason,
                                            profileResult.wasProfileBanned()
                                    )
                                    .thenApply(dbResult -> new UnbanCompletion(record, profileResult, dbResult)));
                })
                .thenAccept(result -> plugin.runSync(() -> {
                    if (result.record().isEmpty()) {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "対象プレイヤーが見つかりません。");
                        return;
                    }
                    if (result.profileResult().wasProfileBanned()) {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "Profile BANを解除しました。");
                    } else {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "Profile BANは既に解除済みでした。");
                    }
                    if (result.dbResult().updatedActiveBans() > 0) {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "DB上の有効BAN "
                                + result.dbResult().updatedActiveBans() + "件を解除済みに更新しました。");
                    } else {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "DB上の有効BANはありませんでした。");
                    }
                    plugin.notifyDiscord(
                            NotificationEventType.UNBAN,
                            unbanDiscord(result.record().get(), sender.getName(), reason, result.profileResult(), result.dbResult())
                    );
                }))
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Failed to unban player: " + throwable.getMessage());
                    plugin.runSync(() -> sender.sendMessage(ReputationBanPlugin.PREFIX + "BAN解除に失敗しました。"));
                    return null;
                });
    }

    private void pardon(CommandSender sender, String[] args) {
        if (!sender.hasPermission("reputationban.admin.score") || !sender.hasPermission("reputationban.admin.ban")) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "権限がありません。");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "使い方: /rep pardon <player> [reason]");
            return;
        }
        String reason = BanAuditMetadata.reasonFromArgs(args, 2);
        CommandActor actor = CommandActor.from(sender);
        resolvePlayer(args[1], true)
                .thenCompose(record -> {
                    if (record.isEmpty()) {
                        return CompletableFuture.completedFuture(new PardonCompletion(Optional.empty(), null, null));
                    }
                    PlayerRecord target = record.get();
                    return punishmentService.unbanProfile(target.uuid())
                            .thenCompose(profileResult -> punishmentService.pardon(
                                            target.uuid(),
                                            target.name(),
                                            reason,
                                            actor,
                                            profileResult.wasProfileBanned()
                                    )
                                    .thenApply(pardonResult -> new PardonCompletion(record, profileResult, pardonResult)));
                })
                .thenAccept(result -> plugin.runSync(() -> {
                    if (result.record().isEmpty()) {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "対象プレイヤーが見つかりません。");
                        return;
                    }
                    sender.sendMessage(ReputationBanPlugin.PREFIX + result.record().get().name() + " をpardonしました。");
                    if (result.profileResult().wasProfileBanned()) {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "Profile BANを解除しました。");
                    } else {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "Profile BANは既に解除済みでした。");
                    }
                    if (result.pardonResult().updatedActiveBans() > 0) {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "DB上の有効BAN "
                                + result.pardonResult().updatedActiveBans() + "件を解除済みに更新しました。");
                    } else {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "DB上の有効BANはありませんでした。");
                    }
                    sender.sendMessage(ReputationBanPlugin.PREFIX + "スコア: " + result.pardonResult().oldScore()
                            + " -> " + result.pardonResult().newScore()
                            + " (" + signed(result.pardonResult().delta()) + ")");
                    plugin.notifyDiscord(
                            NotificationEventType.PARDON,
                            pardonDiscord(result.record().get(), sender.getName(), reason, result.profileResult(), result.pardonResult())
                    );
                }))
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Failed to pardon player: " + throwable.getMessage());
                    plugin.runSync(() -> sender.sendMessage(ReputationBanPlugin.PREFIX + "pardon処理に失敗しました。"));
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
        List<dev.modplugin.reputationban.config.ConfigValidationIssue> issues = plugin.reloadPluginConfig();
        auditService.recordEvent(AuditEvent.create(
                AuditEventType.CONFIG_RELOADED,
                senderUuid(sender),
                sender.getName(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "config reload",
                AuditMetadata.create().put("source", "command").toJson(),
                System.currentTimeMillis()
        ));
        sender.sendMessage(ReputationBanPlugin.PREFIX + "設定を再読み込みしました。");
        if (!issues.isEmpty()) {
            long errors = issues.stream()
                    .filter(issue -> issue.severity() == dev.modplugin.reputationban.config.ConfigValidationIssue.Severity.ERROR)
                    .count();
            long warnings = issues.size() - errors;
            sender.sendMessage(ReputationBanPlugin.PREFIX + "設定警告: " + warnings + "件 / エラー: " + errors + "件。詳細はコンソールを確認してください。");
        }
    }

    private void doctor(CommandSender sender) {
        if (!sender.hasPermission("reputationban.admin.diagnostics")) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "権限がありません。");
            return;
        }
        CommandActor actor = CommandActor.from(sender);
        String version = plugin.getPluginMeta().getVersion();
        String server = Bukkit.getVersion();
        String javaVersion = System.getProperty("java.version", "unknown");
        diagnosticService.run(actor, version, server, javaVersion)
                .thenAccept(report -> plugin.runSync(() -> sendDiagnosticReport(sender, report)))
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Failed to run diagnostics: " + throwable.getMessage());
                    plugin.runSync(() -> sender.sendMessage(ReputationBanPlugin.PREFIX + "診断情報の取得に失敗しました。"));
                    return null;
                });
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

    private static String signed(int value) {
        return value >= 0 ? "+" + value : Integer.toString(value);
    }

    private static String formatNullable(Long epochMillis) {
        return epochMillis == null ? "-" : FORMATTER.format(Instant.ofEpochMilli(epochMillis));
    }

    private static String fallbackDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String yesNo(boolean value) {
        return value ? "はい" : "いいえ";
    }

    private OptionalInt parseAuditLimit(CommandSender sender, String value, int max) {
        if (value == null || value.isBlank()) {
            return OptionalInt.of(Math.min(10, Math.max(1, max)));
        }
        OptionalInt parsed = CommandArgumentParser.parseLimit(value, Math.max(1, max));
        if (parsed.isPresent()) {
            return parsed;
        }
        sender.sendMessage(ReputationBanPlugin.PREFIX + "limit は 1〜" + max + " の数値で指定してください。");
        return OptionalInt.empty();
    }

    private static void sendAuditEvents(CommandSender sender, String label, List<AuditEvent> events) {
        sender.sendMessage(ReputationBanPlugin.PREFIX + "監査ログ " + label + " " + events.size() + "件");
        for (AuditEvent event : events) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "#%d %s actor=%s target=%s report=%s delta=%s %s".formatted(
                    event.id(),
                    event.eventType().databaseValue(),
                    fallbackDash(event.actorName()),
                    fallbackDash(event.targetName()),
                    event.reportId() == null ? "-" : "#" + event.reportId(),
                    event.delta() == null ? "-" : signed(event.delta()),
                    FORMATTER.format(Instant.ofEpochMilli(event.createdAt()))
            ));
        }
    }

    private Void auditFailure(CommandSender sender, Throwable throwable) {
        plugin.getLogger().severe("Failed to process audit command: " + throwable.getMessage());
        plugin.runSync(() -> sender.sendMessage(ReputationBanPlugin.PREFIX + "監査ログ処理に失敗しました。"));
        return null;
    }

    private static UUID senderUuid(CommandSender sender) {
        return sender instanceof Player player ? player.getUniqueId() : null;
    }

    private static void sendMaintenanceResult(
            CommandSender sender,
            String title,
            AuditService.MaintenanceResult result,
            boolean includeBackup
    ) {
        sender.sendMessage(ReputationBanPlugin.PREFIX + title);
        if (includeBackup) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "backup: " + fallbackDash(result.backupFileName()));
        }
        sender.sendMessage(ReputationBanPlugin.PREFIX + "rejected reports: " + result.rejectedReportsDeleted());
        sender.sendMessage(ReputationBanPlugin.PREFIX + "cancelled reports: " + result.cancelledReportsDeleted());
        sender.sendMessage(ReputationBanPlugin.PREFIX + "audit events: " + result.auditEventsDeleted());
        sender.sendMessage(ReputationBanPlugin.PREFIX + "score_history: " + result.scoreHistoryDeleted());
        sender.sendMessage(ReputationBanPlugin.PREFIX + "bans: " + result.bansDeleted());
    }

    private static void sendDiagnosticReport(CommandSender sender, DiagnosticReport report) {
        sender.sendMessage(ReputationBanPlugin.PREFIX + "Doctor");
        sender.sendMessage(ReputationBanPlugin.PREFIX + "version: " + report.version());
        sender.sendMessage(ReputationBanPlugin.PREFIX + "server: " + report.server());
        sender.sendMessage(ReputationBanPlugin.PREFIX + "java: " + report.javaVersion());
        sender.sendMessage(ReputationBanPlugin.PREFIX + "database: " + report.databaseStatus());
        sender.sendMessage(ReputationBanPlugin.PREFIX + "tables: " + report.tablesStatus());
        if (!report.missingTables().isEmpty()) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "missingTables: " + String.join(",", report.missingTables()));
        }
        sender.sendMessage(ReputationBanPlugin.PREFIX + "config: warnings="
                + report.configWarnings() + " errors=" + report.configErrors());
        sender.sendMessage(ReputationBanPlugin.PREFIX + "discord: enabled="
                + report.discordEnabled() + " urlConfigured=" + report.discordUrlConfigured());
        sender.sendMessage(ReputationBanPlugin.PREFIX + "auditExportDirectory: "
                + (report.auditExportDirectorySafe() ? "safe" : "unsafe"));
        sender.sendMessage(ReputationBanPlugin.PREFIX + "retention: " + report.retentionSummary());
        sender.sendMessage(ReputationBanPlugin.PREFIX + "pendingReports: " + report.pendingReports());
        sender.sendMessage(ReputationBanPlugin.PREFIX + "thresholdPendingReports: " + report.thresholdPendingReports());
        sender.sendMessage(ReputationBanPlugin.PREFIX + "activeDbBans: " + report.activeDbBans());
        sender.sendMessage(ReputationBanPlugin.PREFIX + "overall: " + report.overallStatus());
    }

    private String unbanDiscord(
            PlayerRecord target,
            String actorName,
            String reason,
            PunishmentService.ProfileUnbanResult profileResult,
            PunishmentService.UnbanResult dbResult
    ) {
        DiscordWebhookConfig discord = plugin.pluginConfig().discordWebhookConfig();
        StringBuilder message = new StringBuilder();
        message.append("**BAN解除**\n");
        appendPlayer(message, "対象", target.name(), target.uuid(), discord);
        message.append("実行者: ").append(actorName).append('\n');
        if (discord.includeReasons()) {
            message.append("理由: ").append(reason).append('\n');
        }
        message.append("Profile BAN解除: ").append(profileResult.wasProfileBanned()).append('\n');
        message.append("DB有効BAN更新数: ").append(dbResult.updatedActiveBans());
        return message.toString();
    }

    private String pardonDiscord(
            PlayerRecord target,
            String actorName,
            String reason,
            PunishmentService.ProfileUnbanResult profileResult,
            PunishmentService.PardonResult pardonResult
    ) {
        DiscordWebhookConfig discord = plugin.pluginConfig().discordWebhookConfig();
        StringBuilder message = new StringBuilder();
        message.append("**Pardon**\n");
        appendPlayer(message, "対象", target.name(), target.uuid(), discord);
        message.append("実行者: ").append(actorName).append('\n');
        if (discord.includeReasons()) {
            message.append("理由: ").append(reason).append('\n');
        }
        message.append("Profile BAN解除: ").append(profileResult.wasProfileBanned()).append('\n');
        message.append("スコア: ").append(pardonResult.oldScore()).append(" -> ").append(pardonResult.newScore()).append('\n');
        message.append("DB有効BAN更新数: ").append(pardonResult.updatedActiveBans());
        return message.toString();
    }

    private static void appendPlayer(
            StringBuilder message,
            String label,
            String playerName,
            UUID playerUuid,
            DiscordWebhookConfig discord
    ) {
        message.append(label).append(": ").append(playerName);
        if (discord.includePlayerUuids()) {
            message.append(" (").append(playerUuid).append(")");
        }
        message.append('\n');
    }

    private record HistoryResult(Optional<PlayerRecord> record, java.util.List<ScoreService.ScoreHistoryEntry> history) {
    }

    private record AuditListResult(Optional<PlayerRecord> record, List<AuditEvent> events, String label) {
    }

    private record AuditExportResult(Optional<PlayerRecord> record, java.nio.file.Path path) {
    }

    private record MutationResult(Optional<PlayerRecord> record, ScoreService.ScoreChange change, boolean banned) {
    }

    private record BanHistoryResult(Optional<PlayerRecord> record, java.util.List<PunishmentService.BanHistoryEntry> history) {
    }

    private record BanInfoResult(
            Optional<PlayerRecord> record,
            boolean profileBanned,
            PunishmentService.CurrentBanInfo dbInfo
    ) {
    }

    private record UnbanCompletion(
            Optional<PlayerRecord> record,
            PunishmentService.ProfileUnbanResult profileResult,
            PunishmentService.UnbanResult dbResult
    ) {
    }

    private record PardonCompletion(
            Optional<PlayerRecord> record,
            PunishmentService.ProfileUnbanResult profileResult,
            PunishmentService.PardonResult pardonResult
    ) {
    }
}
