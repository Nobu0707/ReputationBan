package dev.modplugin.reputationban.command;

import dev.modplugin.reputationban.ReputationBanPlugin;
import dev.modplugin.reputationban.model.ReportStatus;
import dev.modplugin.reputationban.model.ReportContext;
import dev.modplugin.reputationban.model.ReportContextFormatter;
import dev.modplugin.reputationban.notification.DiscordWebhookConfig;
import dev.modplugin.reputationban.notification.NotificationEventType;
import dev.modplugin.reputationban.service.PunishmentService;
import dev.modplugin.reputationban.service.ReportService;
import dev.modplugin.reputationban.util.CommandArgumentParser;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ReportsCommand implements CommandExecutor {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final UUID CONSOLE_UUID = new UUID(0L, 0L);

    private final ReputationBanPlugin plugin;
    private final ReportService reportService;
    private final PunishmentService punishmentService;

    public ReportsCommand(ReputationBanPlugin plugin, ReportService reportService, PunishmentService punishmentService) {
        this.plugin = plugin;
        this.reportService = reportService;
        this.punishmentService = punishmentService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("reputationban.admin.reports")) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "権限がありません。");
            return true;
        }
        if (args.length == 0 || "list".equalsIgnoreCase(args[0])) {
            listReports(sender, args);
            return true;
        }
        if ("help".equalsIgnoreCase(args[0])) {
            sendHelp(sender);
            return true;
        }
        if ("view".equalsIgnoreCase(args[0])) {
            viewReport(sender, args);
            return true;
        }
        if ("evidence".equalsIgnoreCase(args[0])) {
            viewEvidence(sender, args);
            return true;
        }
        if ("approve".equalsIgnoreCase(args[0])) {
            reviewReport(sender, args, true);
            return true;
        }
        if ("reject".equalsIgnoreCase(args[0])) {
            reviewReport(sender, args, false);
            return true;
        }

        sender.sendMessage(ReputationBanPlugin.PREFIX + "不明なサブコマンドです。/reports help を確認してください。");
        return true;
    }

    private static void sendHelp(CommandSender sender) {
        sender.sendMessage(ReputationBanPlugin.PREFIX + "/reports list [status] [limit] - 通報一覧");
        sender.sendMessage(ReputationBanPlugin.PREFIX + "/reports view <id> - 通報詳細");
        sender.sendMessage(ReputationBanPlugin.PREFIX + "/reports evidence <id> - 通報に紐づく連携証拠を表示");
        sender.sendMessage(ReputationBanPlugin.PREFIX + "/reports approve <id> [note] - 通報承認");
        sender.sendMessage(ReputationBanPlugin.PREFIX + "/reports reject <id> [note] - 通報却下");
    }

    private void listReports(CommandSender sender, String[] args) {
        String status = args.length >= 2 ? args[1] : "pending";
        if (!"all".equalsIgnoreCase(status) && !ReportStatus.isDatabaseValue(status)) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "使い方: /reports list [pending|threshold_pending|approved|rejected|auto_accepted|cancelled|all] [limit]");
            return;
        }
        OptionalInt parsedLimit = args.length >= 3 ? CommandArgumentParser.parseLimit(args[2], 50) : OptionalInt.of(10);
        if (parsedLimit.isEmpty()) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "limit は 1〜50 の数値で指定してください。");
            return;
        }
        int limit = parsedLimit.getAsInt();
        reportService.listReports(status, limit)
                .thenAccept(reports -> plugin.runSync(() -> {
                    sender.sendMessage(ReputationBanPlugin.PREFIX + "通報一覧 [" + status + "] " + reports.size() + "件");
                    for (ReportService.ReportSummary report : reports) {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "#%d [%s] %s -> %s / %s / -%d / %s".formatted(
                                report.id(),
                                report.status(),
                                report.reporterName(),
                                report.targetName(),
                                report.category(),
                                report.deduction(),
                                FORMATTER.format(Instant.ofEpochMilli(report.createdAt()))
                        ));
                    }
                }))
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Failed to list reports: " + throwable.getMessage());
                    plugin.runSync(() -> sender.sendMessage(ReputationBanPlugin.PREFIX + "通報一覧の取得に失敗しました。"));
                    return null;
                });
    }

    private void viewReport(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "使い方: /reports view <id>");
            return;
        }
        Long id = parseId(sender, args[1]);
        if (id == null) {
            return;
        }

        reportService.getReport(id)
                .thenAccept(report -> plugin.runSync(() -> {
                    if (report.isEmpty()) {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "通報が見つかりません。");
                        return;
                    }
                    sendReportDetails(sender, report.get());
                }))
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Failed to view report: " + throwable.getMessage());
                    plugin.runSync(() -> sender.sendMessage(ReputationBanPlugin.PREFIX + "通報詳細の取得に失敗しました。"));
                    return null;
                });
    }

    private void viewEvidence(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "使い方: /reports evidence <id>");
            return;
        }
        Long id = parseId(sender, args[1]);
        if (id == null) {
            return;
        }

        reportService.getReport(id)
                .thenAccept(report -> plugin.runSync(() -> {
                    if (report.isEmpty()) {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + "通報が見つかりません。");
                        return;
                    }
                    sender.sendMessage(ReputationBanPlugin.PREFIX + "Report #" + report.get().id() + " evidence");
                    for (String line : ReportContextFormatter.formatEvidence(report.get().contexts())) {
                        sender.sendMessage(ReputationBanPlugin.PREFIX + line);
                    }
                }))
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Failed to view report evidence: " + throwable.getMessage());
                    plugin.runSync(() -> sender.sendMessage(ReputationBanPlugin.PREFIX + "連携証拠の取得に失敗しました。"));
                    return null;
                });
    }

    private void reviewReport(CommandSender sender, String[] args, boolean approve) {
        if (args.length < 2) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "使い方: /reports " + (approve ? "approve" : "reject") + " <id> [note]");
            return;
        }
        Long id = parseId(sender, args[1]);
        if (id == null) {
            return;
        }
        String note = args.length >= 3 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "";
        UUID moderatorUuid = sender instanceof Player player ? player.getUniqueId() : CONSOLE_UUID;
        String moderatorName = sender.getName();
        boolean hasBanPermission = sender.hasPermission("reputationban.admin.ban");

        CompletableFuture<ReportService.ReviewResult> review = approve
                ? approveReportSafely(id, moderatorUuid, moderatorName, note, hasBanPermission)
                : reportService.rejectReport(id, moderatorUuid, moderatorName, note);

        review.thenCompose(result -> {
                    if (!result.success() || result.rejected() || result.scoreChange() == null || !result.scoreChange().crossedBanThreshold()) {
                        return java.util.concurrent.CompletableFuture.completedFuture(
                                new ReviewCompletion(result, false, moderatorName, note)
                        );
                    }
                    return punishmentService.punishIfNeeded(
                            result.scoreChange().targetUuid(),
                            result.scoreChange().targetName(),
                            result.scoreChange().oldScore(),
                            result.scoreChange().newScore(),
                            "Approved report #" + id + " reached " + result.scoreChange().newScore()
                    ).thenApply(banned -> new ReviewCompletion(result, banned, moderatorName, note));
                })
                .thenAccept(completion -> plugin.runSync(() -> sendReviewResult(sender, completion)))
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Failed to review report: " + throwable.getMessage());
                    plugin.runSync(() -> sender.sendMessage(ReputationBanPlugin.PREFIX + "通報審査に失敗しました。"));
                    return null;
                });
    }

    private CompletableFuture<ReportService.ReviewResult> approveReportSafely(
            long id,
            UUID moderatorUuid,
            String moderatorName,
            String note,
            boolean hasBanPermission
    ) {
        return reportService.getReport(id)
                .thenCompose(report -> {
                    if (report.isEmpty()) {
                        return reportService.approveReport(id, moderatorUuid, moderatorName, note, hasBanPermission, false);
                    }
                    return plugin.supplySync(() -> isTargetProtected(report.get().targetUuid()))
                            .thenCompose(targetProtected -> reportService.approveReport(
                                    id,
                                    moderatorUuid,
                                    moderatorName,
                                    note,
                                    hasBanPermission,
                                    targetProtected
                            ));
                });
    }

    private void sendReviewResult(CommandSender sender, ReviewCompletion completion) {
        ReportService.ReviewResult result = completion.result();
        if (!result.success()) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + result.message());
            return;
        }
        if (result.rejected()) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "通報 #" + result.report().id() + " を却下しました。");
            sender.sendMessage(ReputationBanPlugin.PREFIX + "通報者の虚偽通報回数: " + result.falseReportCount());
            plugin.notifyDiscord(
                    NotificationEventType.REPORT_REJECTED,
                    rejectedDiscord(result, completion.moderatorName(), completion.note())
            );
            if (result.reportBannedUntil() != null) {
                sender.sendMessage(ReputationBanPlugin.PREFIX + "通報者は一時的に通報停止されました。解除予定: "
                        + FORMATTER.format(Instant.ofEpochMilli(result.reportBannedUntil())));
                plugin.notifyDiscord(
                        NotificationEventType.REPORTER_PENALTY,
                        reporterPenaltyDiscord(result)
                );
            }
            return;
        }
        sender.sendMessage(ReputationBanPlugin.PREFIX + "通報 #" + result.report().id() + " を承認しました。対象スコア: "
                + result.scoreChange().oldScore() + " -> " + result.scoreChange().newScore());
        plugin.notifyDiscord(
                NotificationEventType.REPORT_APPROVED,
                approvedDiscord(result, completion.moderatorName(), completion.note())
        );
        plugin.notifyScoreThresholdCrossings(
                result.scoreChange().targetUuid(),
                result.scoreChange().targetName(),
                result.scoreChange().oldScore(),
                result.scoreChange().newScore(),
                "通報承認 #" + result.report().id()
        );
        plugin.integrationService().refreshPlaceholderCache(
                result.scoreChange().targetUuid(),
                result.scoreChange().targetName()
        );
        if (completion.banned()) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "対象プレイヤーは評判スコアによりBAN処理されました。");
        }
    }

    private String approvedDiscord(ReportService.ReviewResult result, String moderatorName, String note) {
        DiscordWebhookConfig discord = plugin.pluginConfig().discordWebhookConfig();
        StringBuilder message = new StringBuilder();
        message.append("**通報承認**\n");
        message.append("Report ID: #").append(result.report().id()).append('\n');
        appendPlayer(message, "対象", result.report().targetName(), result.report().targetUuid(), discord);
        message.append("スコア: ").append(result.scoreChange().oldScore())
                .append(" -> ").append(result.scoreChange().newScore()).append('\n');
        message.append("審査者: ").append(moderatorName);
        appendNote(message, note, discord);
        return message.toString();
    }

    private String rejectedDiscord(ReportService.ReviewResult result, String moderatorName, String note) {
        DiscordWebhookConfig discord = plugin.pluginConfig().discordWebhookConfig();
        StringBuilder message = new StringBuilder();
        message.append("**通報却下**\n");
        message.append("Report ID: #").append(result.report().id()).append('\n');
        appendPlayer(message, "通報者", result.report().reporterName(), result.report().reporterUuid(), discord);
        appendPlayer(message, "対象", result.report().targetName(), result.report().targetUuid(), discord);
        message.append("審査者: ").append(moderatorName).append('\n');
        message.append("虚偽通報回数: ").append(result.falseReportCount());
        appendNote(message, note, discord);
        return message.toString();
    }

    private String reporterPenaltyDiscord(ReportService.ReviewResult result) {
        DiscordWebhookConfig discord = plugin.pluginConfig().discordWebhookConfig();
        StringBuilder message = new StringBuilder();
        message.append("**通報者ペナルティ**\n");
        appendPlayer(message, "通報者", result.report().reporterName(), result.report().reporterUuid(), discord);
        message.append("虚偽通報回数: ").append(result.falseReportCount()).append('\n');
        message.append("解除予定: ").append(FORMATTER.format(Instant.ofEpochMilli(result.reportBannedUntil())));
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

    private static void appendNote(StringBuilder message, String note, DiscordWebhookConfig discord) {
        if (discord.includeReasons() && note != null && !note.isBlank()) {
            message.append('\n').append("メモ: ").append(note.trim());
        }
    }

    private static void sendReportDetails(CommandSender sender, ReportService.ReportRecord report) {
        sender.sendMessage(ReputationBanPlugin.PREFIX + "通報 #" + report.id() + " [" + report.status() + "]");
        sender.sendMessage(ReputationBanPlugin.PREFIX + "通報者: " + report.reporterName() + " / 対象: " + report.targetName());
        sender.sendMessage(ReputationBanPlugin.PREFIX + "カテゴリ: " + report.category() + " / 減点: " + report.deduction());
        sender.sendMessage(ReputationBanPlugin.PREFIX + "理由: " + report.reason());
        sender.sendMessage(ReputationBanPlugin.PREFIX + "作成日時: " + FORMATTER.format(Instant.ofEpochMilli(report.createdAt())));
        if (report.reviewedBy() != null) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "審査者: " + report.reviewedBy());
        }
        if (report.reviewedAt() != null) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "審査日時: " + FORMATTER.format(Instant.ofEpochMilli(report.reviewedAt())));
        }
        if (report.reviewNote() != null && !report.reviewNote().isBlank()) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "メモ: " + report.reviewNote());
        }
        if (!report.contexts().isEmpty()) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "連携情報:");
            for (ReportContext context : report.contexts().stream().limit(5).toList()) {
                sender.sendMessage(ReputationBanPlugin.PREFIX + "- " + providerLabel(context.provider()) + ": " + firstLine(context.summary()));
            }
        }
    }

    private static String providerLabel(String provider) {
        if ("luckperms".equalsIgnoreCase(provider)) {
            return "LuckPerms";
        }
        if ("coreprotect".equalsIgnoreCase(provider)) {
            return "CoreProtect";
        }
        if ("worldguard".equalsIgnoreCase(provider)) {
            return "WorldGuard";
        }
        if ("griefprevention".equalsIgnoreCase(provider)) {
            return "GriefPrevention";
        }
        return provider;
    }

    private static String firstLine(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String[] lines = value.split("\\R", 2);
        return lines[0];
    }

    private static Long parseId(CommandSender sender, String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "IDは数値で指定してください。");
            return null;
        }
    }

    private boolean isTargetProtected(UUID targetUuid) {
        Player online = Bukkit.getPlayer(targetUuid);
        if (online != null) {
            return online.hasPermission("reputationban.bypass")
                    || online.isOp()
                    || plugin.integrationService().isLuckPermsBypassGroup(targetUuid);
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(targetUuid);
        return offline.isOp() || plugin.integrationService().isLuckPermsBypassGroup(targetUuid);
    }

    private record ReviewCompletion(ReportService.ReviewResult result, boolean banned, String moderatorName, String note) {
    }
}
