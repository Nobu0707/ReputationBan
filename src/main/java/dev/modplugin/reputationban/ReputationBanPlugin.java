package dev.modplugin.reputationban;

import dev.modplugin.reputationban.command.RepCommand;
import dev.modplugin.reputationban.command.RepTabCompleter;
import dev.modplugin.reputationban.command.ReportBadCommand;
import dev.modplugin.reputationban.command.ReportBadTabCompleter;
import dev.modplugin.reputationban.command.ReportsCommand;
import dev.modplugin.reputationban.command.ReportsTabCompleter;
import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.database.DatabaseManager;
import dev.modplugin.reputationban.listener.PlayerJoinListener;
import dev.modplugin.reputationban.model.ScoreThresholdCrossing;
import dev.modplugin.reputationban.notification.DiscordWebhookConfig;
import dev.modplugin.reputationban.notification.NotificationEventType;
import dev.modplugin.reputationban.notification.NotificationService;
import dev.modplugin.reputationban.service.PlayerDataService;
import dev.modplugin.reputationban.service.PunishmentService;
import dev.modplugin.reputationban.service.ReportService;
import dev.modplugin.reputationban.service.ScoreService;
import dev.modplugin.reputationban.util.ScoreThresholdPolicy;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ReputationBanPlugin extends JavaPlugin {
    public static final String PREFIX = "[ReputationBan] ";

    private PluginConfig pluginConfig;
    private DatabaseManager databaseManager;
    private PlayerDataService playerDataService;
    private ScoreService scoreService;
    private ReportService reportService;
    private PunishmentService punishmentService;
    private NotificationService notificationService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        pluginConfig = PluginConfig.load(getConfig());
        notificationService = new NotificationService(this, this::pluginConfig);
        databaseManager = new DatabaseManager(this, pluginConfig);
        try {
            databaseManager.initialize();
        } catch (SQLException exception) {
            getLogger().log(Level.SEVERE, "Failed to initialize ReputationBan database", exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        playerDataService = new PlayerDataService(databaseManager, pluginConfig);
        scoreService = new ScoreService(databaseManager, pluginConfig);
        reportService = new ReportService(databaseManager, scoreService, pluginConfig);
        punishmentService = new PunishmentService(this, databaseManager, pluginConfig);

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(playerDataService, getLogger()), this);
        registerCommand("rep", new RepCommand(this, playerDataService, scoreService, punishmentService), new RepTabCompleter());
        registerCommand(
                "reportbad",
                new ReportBadCommand(this, playerDataService, reportService, punishmentService),
                new ReportBadTabCompleter(this::pluginConfig)
        );
        registerCommand("reports", new ReportsCommand(this, reportService, punishmentService), new ReportsTabCompleter());
        startScoreRecoveryTask();
        getLogger().info("ReputationBan v0.7.0 enabled.");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    public void reloadPluginConfig() {
        reloadConfig();
        pluginConfig = PluginConfig.load(getConfig());
        playerDataService.updateConfig(pluginConfig);
        scoreService.updateConfig(pluginConfig);
        reportService.updateConfig(pluginConfig);
        punishmentService.updateConfig(pluginConfig);
    }

    public PluginConfig pluginConfig() {
        return pluginConfig;
    }

    public void runSync(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
            return;
        }
        Bukkit.getScheduler().runTask(this, task);
    }

    public CompletableFuture<Void> runSyncFuture(Runnable task) {
        return supplySync(() -> {
            task.run();
            return null;
        });
    }

    public <T> CompletableFuture<T> supplySync(Supplier<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Runnable wrapped = () -> {
            try {
                future.complete(task.get());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        };
        if (Bukkit.isPrimaryThread()) {
            wrapped.run();
        } else {
            Bukkit.getScheduler().runTask(this, wrapped);
        }
        return future;
    }

    public void notifyStaff(String message) {
        notificationService.notifyStaff(message);
    }

    public void notifyStaff(NotificationEventType type, String message) {
        notificationService.notifyStaff(type, message);
    }

    public void notifyStaff(NotificationEventType type, String staffMessage, String discordContent) {
        notificationService.notifyStaff(type, staffMessage, discordContent);
    }

    public void notifyDiscord(NotificationEventType type, String discordContent) {
        notificationService.notifyDiscord(type, discordContent);
    }

    public void notifyScoreThresholdCrossings(
            java.util.UUID targetUuid,
            String targetName,
            int oldScore,
            int newScore,
            String context
    ) {
        for (ScoreThresholdCrossing crossing : ScoreThresholdPolicy.crossedDownward(
                oldScore,
                newScore,
                pluginConfig.scoreThresholds()
        )) {
            notifyStaff(
                    NotificationEventType.SCORE_THRESHOLD_CROSSED,
                    "スコアしきい値到達: " + targetName + " / " + crossing.key()
                            + " (" + crossing.threshold() + ") / " + oldScore + " -> " + newScore,
                    scoreThresholdDiscord(targetUuid, targetName, oldScore, newScore, crossing, context)
            );
        }
    }

    private String scoreThresholdDiscord(
            java.util.UUID targetUuid,
            String targetName,
            int oldScore,
            int newScore,
            ScoreThresholdCrossing crossing,
            String context
    ) {
        DiscordWebhookConfig discord = pluginConfig.discordWebhookConfig();
        StringBuilder message = new StringBuilder();
        message.append("**スコアしきい値到達**\n");
        message.append("対象: ").append(targetName);
        if (discord.includePlayerUuids()) {
            message.append(" (").append(targetUuid).append(")");
        }
        message.append('\n');
        message.append("しきい値: ").append(crossing.key()).append(" (").append(crossing.threshold()).append(")\n");
        message.append("スコア: ").append(oldScore).append(" -> ").append(newScore);
        if (context != null && !context.isBlank()) {
            message.append('\n').append("理由: ").append(context);
        }
        return message.toString();
    }

    private void registerCommand(
            String name,
            org.bukkit.command.CommandExecutor executor,
            org.bukkit.command.TabCompleter tabCompleter
    ) {
        PluginCommand command = Objects.requireNonNull(getCommand(name), "Missing command in plugin.yml: " + name);
        command.setExecutor(executor);
        command.setTabCompleter(tabCompleter);
    }

    private void startScoreRecoveryTask() {
        long initialDelayTicks = 5L * 60L * 20L;
        long periodTicks = 24L * 60L * 60L * 20L;
        Bukkit.getScheduler().runTaskTimer(this, () -> scoreService.runRecovery()
                .thenAccept(result -> {
                    if (result.recoveredPlayers() > 0) {
                        String message = "Score recovery completed: "
                                + result.recoveredPlayers() + "/" + result.checkedPlayers() + " players recovered.";
                        getLogger().info(message);
                        notifyDiscord(NotificationEventType.RECOVERY_SUMMARY, """
                                **スコア回復**
                                対象人数: %d / チェック対象: %d
                                """.formatted(result.recoveredPlayers(), result.checkedPlayers()).trim());
                    }
                })
                .exceptionally(throwable -> {
                    getLogger().log(Level.SEVERE, "Score recovery failed", throwable);
                    return null;
                }), initialDelayTicks, periodTicks);
    }
}
