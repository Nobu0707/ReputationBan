package dev.modplugin.reputationban;

import dev.modplugin.reputationban.command.RepCommand;
import dev.modplugin.reputationban.command.ReportBadCommand;
import dev.modplugin.reputationban.command.ReportsCommand;
import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.database.DatabaseManager;
import dev.modplugin.reputationban.listener.PlayerJoinListener;
import dev.modplugin.reputationban.service.PlayerDataService;
import dev.modplugin.reputationban.service.PunishmentService;
import dev.modplugin.reputationban.service.ReportService;
import dev.modplugin.reputationban.service.ScoreService;
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

    @Override
    public void onEnable() {
        saveDefaultConfig();
        pluginConfig = PluginConfig.load(getConfig());
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
        registerCommand("rep", new RepCommand(this, playerDataService, scoreService, punishmentService));
        registerCommand("reportbad", new ReportBadCommand(this, playerDataService, reportService, punishmentService));
        registerCommand("reports", new ReportsCommand(this, reportService, punishmentService));
        startScoreRecoveryTask();
        getLogger().info("ReputationBan v0.4.0 enabled.");
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
        if (pluginConfig.notifyConsole()) {
            getLogger().info(message);
        }
        if (!pluginConfig.notifyInGameStaff()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(pluginConfig.staffPermission())) {
                player.sendMessage(PREFIX + message);
            }
        }
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = Objects.requireNonNull(getCommand(name), "Missing command in plugin.yml: " + name);
        command.setExecutor(executor);
    }

    private void startScoreRecoveryTask() {
        long initialDelayTicks = 5L * 60L * 20L;
        long periodTicks = 24L * 60L * 60L * 20L;
        Bukkit.getScheduler().runTaskTimer(this, () -> scoreService.runRecovery()
                .thenAccept(result -> {
                    if (result.recoveredPlayers() > 0) {
                        getLogger().info("Score recovery completed: "
                                + result.recoveredPlayers() + "/" + result.checkedPlayers() + " players recovered.");
                    }
                })
                .exceptionally(throwable -> {
                    getLogger().log(Level.SEVERE, "Score recovery failed", throwable);
                    return null;
                }), initialDelayTicks, periodTicks);
    }
}
