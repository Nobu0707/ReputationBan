package dev.modplugin.reputationban.command;

import dev.modplugin.reputationban.ReputationBanPlugin;
import dev.modplugin.reputationban.service.ReportService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class ReportsCommand implements CommandExecutor {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final ReputationBanPlugin plugin;
    private final ReportService reportService;

    public ReportsCommand(ReputationBanPlugin plugin, ReportService reportService) {
        this.plugin = plugin;
        this.reportService = reportService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("reputationban.admin.reports")) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "権限がありません。");
            return true;
        }
        if (args.length > 0) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "承認・却下機能はPhase 2で実装予定です。");
            return true;
        }

        reportService.recentReports(10)
                .thenAccept(reports -> plugin.runSync(() -> {
                    sender.sendMessage(ReputationBanPlugin.PREFIX + "直近の通報: " + reports.size() + "件");
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
                    plugin.getLogger().severe("Failed to load reports: " + throwable.getMessage());
                    plugin.runSync(() -> sender.sendMessage(ReputationBanPlugin.PREFIX + "通報一覧の取得に失敗しました。"));
                    return null;
                });
        return true;
    }
}
