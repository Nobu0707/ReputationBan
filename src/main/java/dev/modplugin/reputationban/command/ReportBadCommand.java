package dev.modplugin.reputationban.command;

import dev.modplugin.reputationban.ReputationBanPlugin;
import dev.modplugin.reputationban.model.PlayerRecord;
import dev.modplugin.reputationban.model.ReportCategory;
import dev.modplugin.reputationban.service.PlayerDataService;
import dev.modplugin.reputationban.service.PunishmentService;
import dev.modplugin.reputationban.service.ReportService;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ReportBadCommand implements CommandExecutor {
    private final ReputationBanPlugin plugin;
    private final PlayerDataService playerDataService;
    private final ReportService reportService;
    private final PunishmentService punishmentService;

    public ReportBadCommand(
            ReputationBanPlugin plugin,
            PlayerDataService playerDataService,
            ReportService reportService,
            PunishmentService punishmentService
    ) {
        this.plugin = plugin;
        this.playerDataService = playerDataService;
        this.reportService = reportService;
        this.punishmentService = punishmentService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player reporter)) {
            sender.sendMessage(ReputationBanPlugin.PREFIX + "このコマンドはプレイヤーのみ実行できます。");
            return true;
        }
        if (!reporter.hasPermission("reputationban.report")) {
            reporter.sendMessage(ReputationBanPlugin.PREFIX + "権限がありません。");
            return true;
        }
        if (!plugin.pluginConfig().ratingEnabled()) {
            reporter.sendMessage(ReputationBanPlugin.PREFIX + "現在、通報機能は無効です。");
            return true;
        }
        if (args.length < 3) {
            reporter.sendMessage(ReputationBanPlugin.PREFIX + "使い方: /reportbad <player> <category> <reason>");
            return true;
        }

        ReportCategory category = plugin.pluginConfig().category(args[1]);
        if (category == null) {
            reporter.sendMessage(ReputationBanPlugin.PREFIX + "カテゴリが見つかりません: " + args[1]);
            return true;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim();
        if (reason.length() < plugin.pluginConfig().minReasonLength()) {
            reporter.sendMessage(ReputationBanPlugin.PREFIX + "理由は" + plugin.pluginConfig().minReasonLength() + "文字以上で入力してください。");
            return true;
        }

        Player onlineTarget = Bukkit.getPlayerExact(args[0]);
        if (onlineTarget != null && (onlineTarget.hasPermission("reputationban.bypass") || onlineTarget.isOp())) {
            reporter.sendMessage(ReputationBanPlugin.PREFIX + "このプレイヤーは通報対象外です。");
            return true;
        }

        UUID reporterUuid = reporter.getUniqueId();
        String reporterName = reporter.getName();
        resolveTarget(args[0], onlineTarget)
                .thenCompose(target -> handleResolvedTarget(reporter, reporterUuid, reporterName, target, category, reason))
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Failed to process report: " + throwable.getMessage());
                    plugin.runSync(() -> reporter.sendMessage(ReputationBanPlugin.PREFIX + "通報処理に失敗しました。"));
                    return null;
                });
        return true;
    }

    private CompletableFuture<Optional<Target>> resolveTarget(String name, Player onlineTarget) {
        if (onlineTarget != null) {
            UUID targetUuid = onlineTarget.getUniqueId();
            String targetName = onlineTarget.getName();
            return playerDataService.ensurePlayer(targetUuid, targetName)
                    .thenApply(ignored -> Optional.of(new Target(targetUuid, targetName)));
        }
        return playerDataService.findByName(name)
                .thenApply(record -> record.map(value -> new Target(value.uuid(), value.name())));
    }

    private CompletableFuture<Void> handleResolvedTarget(
            Player reporter,
            UUID reporterUuid,
            String reporterName,
            Optional<Target> target,
            ReportCategory category,
            String reason
    ) {
        if (target.isEmpty()) {
            plugin.runSync(() -> reporter.sendMessage(ReputationBanPlugin.PREFIX + "対象プレイヤーが見つかりません。"));
            return CompletableFuture.completedFuture(null);
        }

        Target value = target.get();
        if (value.uuid().equals(reporterUuid)) {
            plugin.runSync(() -> reporter.sendMessage(ReputationBanPlugin.PREFIX + "自分自身は通報できません。"));
            return CompletableFuture.completedFuture(null);
        }

        return isTargetProtected(value.uuid())
                .thenCompose(protectedTarget -> {
                    if (protectedTarget) {
                        plugin.runSync(() -> reporter.sendMessage(ReputationBanPlugin.PREFIX + "このプレイヤーは通報対象外です。"));
                        return CompletableFuture.completedFuture(null);
                    }
                    return submitAllowedReport(reporter, reporterUuid, reporterName, value, category, reason);
                });
    }

    private CompletableFuture<Void> submitAllowedReport(
            Player reporter,
            UUID reporterUuid,
            String reporterName,
            Target value,
            ReportCategory category,
            String reason
    ) {
        return playerDataService.ensurePlayer(reporterUuid, reporterName)
                .thenCompose(ignored -> reportService.submitReport(reporterUuid, reporterName, value.uuid(), value.name(), category, reason))
                .thenCompose(result -> {
                    if (!result.accepted()) {
                        plugin.runSync(() -> reporter.sendMessage(ReputationBanPlugin.PREFIX + result.message()));
                        return CompletableFuture.completedFuture(null);
                    }
                    if (result.staffReviewRequired() || result.deduction() <= 0) {
                        plugin.runSync(() -> {
                            reporter.sendMessage(ReputationBanPlugin.PREFIX + "通報を受け付けました。スタッフ審査待ちです。");
                            plugin.notifyStaff("審査待ち通報 #" + result.reportId() + ": " + reporterName + " -> " + value.name());
                        });
                        return CompletableFuture.completedFuture(null);
                    }

                    CompletableFuture<Boolean> banFuture = result.crossedBanThreshold()
                            ? punishmentService.punishIfNeeded(
                                    value.uuid(),
                                    value.name(),
                                    result.oldScore(),
                                    result.newScore(),
                                    "Reputation score reached " + result.newScore()
                            )
                            : CompletableFuture.completedFuture(false);

                    return banFuture.thenApply(banned -> {
                        plugin.runSync(() -> {
                            reporter.sendMessage(ReputationBanPlugin.PREFIX + "通報を受け付けました。対象スコア: "
                                    + result.oldScore() + " -> " + result.newScore());
                            plugin.notifyStaff("自動承認通報 #" + result.reportId() + ": " + value.name()
                                    + " -" + result.deduction() + " (" + result.newScore() + ")");
                            if (banned) {
                                plugin.notifyStaff(value.name() + " は評判スコアによりBAN処理されました。");
                            }
                        });
                        return null;
                    });
                });
    }

    private CompletableFuture<Boolean> isTargetProtected(UUID targetUuid) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        plugin.runSync(() -> {
            Player online = Bukkit.getPlayer(targetUuid);
            if (online != null) {
                result.complete(online.hasPermission("reputationban.bypass") || online.isOp());
                return;
            }

            OfflinePlayer offline = Bukkit.getOfflinePlayer(targetUuid);
            result.complete(offline.isOp());
        });
        return result;
    }

    private record Target(UUID uuid, String name) {
    }
}
