package dev.modplugin.reputationban.command;

import dev.modplugin.reputationban.ReputationBanPlugin;
import dev.modplugin.reputationban.service.PunishmentService;
import dev.modplugin.reputationban.service.ReportService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;
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
        if ("view".equalsIgnoreCase(args[0])) {
            viewReport(sender, args);
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

        sender.sendMessage(ReputationBanPlugin.PREFIX + "使い方: /reports list [pending|approved|rejected|auto_accepted|all] [limit]");
        sender.sendMessage(ReputationBanPlugin.PREFIX + "使い方: /reports view <id>, /reports approve <id> [note], /reports reject <id> [note]");
        return true;
    }

    private void listReports(CommandSender sender, String[] args) {
        String status = args.length >= 2 ? args[1] : "pending";
        int limit = parseLimit(args.length >= 3 ? args[2] : "10", 10);
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

        java.util.concurrent.CompletableFuture<ReportService.ReviewResult> review = approve
                ? reportService.approveReport(id, moderatorUuid, moderatorName, note)
                : reportService.rejectReport(id, moderatorUuid, moderatorName, note);

        review.thenCompose(result -> {
                    if (!result.success() || result.rejected() || result.scoreChange() == null || !result.scoreChange().crossedBanThreshold()) {
                        return java.util.concurrent.CompletableFuture.completedFuture(new ReviewCompletion(result, false));
                    }
                    return punishmentService.punishIfNeeded(
                            result.scoreChange().targetUuid(),
                            result.scoreChange().targetName(),
                            result.scoreChange().oldScore(),
                            result.scoreChange().newScore(),
                            "Approved report #" + id + " reached " + result.scoreChange().newScore()
                    ).thenApply(banned -> new ReviewCompletion(result, banned));
                })
                .thenAccept(completion -> plugin.runSync(() -> sendReviewResult(sender, completion)))
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Failed to review report: " + throwable.getMessage());
                    plugin.runSync(() -> sender.sendMessage(ReputationBanPlugin.PREFIX + "通報審査に失敗しました。"));
                    return null;
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
            return;
        }
        sender.sendMessage(ReputationBanPlugin.PREFIX + "通報 #" + result.report().id() + " を承認しました。対象スコア: "
                + result.scoreChange().oldScore() + " -> " + result.scoreChange().newScore());
        if (completion.banned()) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "対象プレイヤーは評判スコアによりBAN処理されました。");
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
    }

    private static int parseLimit(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value);
            return Math.max(1, Math.min(50, parsed));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static Long parseId(CommandSender sender, String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "IDは数値で指定してください。");
            return null;
        }
    }

    private record ReviewCompletion(ReportService.ReviewResult result, boolean banned) {
    }
}
