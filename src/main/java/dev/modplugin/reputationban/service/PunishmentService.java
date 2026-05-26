package dev.modplugin.reputationban.service;

import dev.modplugin.reputationban.ReputationBanPlugin;
import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.util.DurationParser;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public final class PunishmentService {
    private final ReputationBanPlugin plugin;
    private final dev.modplugin.reputationban.database.DatabaseManager database;
    private volatile PluginConfig config;

    public PunishmentService(
            ReputationBanPlugin plugin,
            dev.modplugin.reputationban.database.DatabaseManager database,
            PluginConfig config
    ) {
        this.plugin = plugin;
        this.database = database;
        this.config = config;
    }

    public void updateConfig(PluginConfig config) {
        this.config = config;
    }

    public CompletableFuture<Boolean> punishIfNeeded(
            UUID targetUuid,
            String targetName,
            int oldScore,
            int newScore,
            String reason
    ) {
        if (!config.banEnabled() || oldScore <= config.banThreshold() || newScore > config.banThreshold()) {
            return CompletableFuture.completedFuture(false);
        }

        CompletableFuture<Boolean> blockedCheck = new CompletableFuture<>();
        plugin.runSync(() -> {
            Player online = Bukkit.getPlayer(targetUuid);
            if (online != null && (online.hasPermission("reputationban.bypass") || online.isOp())) {
                blockedCheck.complete(true);
                return;
            }

            OfflinePlayer offline = Bukkit.getOfflinePlayer(targetUuid);
            blockedCheck.complete(offline.isBanned() || offline.isOp());
        });

        return blockedCheck.thenCompose(blocked -> {
            if (blocked) {
                return CompletableFuture.completedFuture(false);
            }
            return createBanPlan(targetUuid)
                    .thenCompose(plan -> plugin.runSyncFuture(() -> applyBukkitBan(targetUuid, targetName, reason, plan.expiresAt()))
                            .thenCompose(ignored -> recordBan(targetUuid, targetName, reason, plan))
                            .thenApply(ignored -> true));
        });
    }

    private CompletableFuture<BanPlan> createBanPlan(UUID targetUuid) {
        return database.supplyAsync(connection -> {
            int banCount = getBanCount(connection, targetUuid);
            String durationValue = config.banDurationForCount(banCount);
            Optional<Duration> duration = DurationParser.parseBanDuration(durationValue);
            long now = System.currentTimeMillis();
            Instant expiresAt = duration.map(value -> Instant.ofEpochMilli(now).plus(value)).orElse(null);
            String banType = expiresAt == null ? "permanent" : "temporary";
            return new BanPlan(expiresAt, banType);
        });
    }

    private CompletableFuture<Void> recordBan(UUID targetUuid, String targetName, String reason, BanPlan plan) {
        return database.supplyAsync(connection -> {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                long now = System.currentTimeMillis();
                try (PreparedStatement insert = connection.prepareStatement("""
                        INSERT INTO bans (
                          target_uuid, target_name, reason, ban_type, created_at, expires_at, created_by
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """)) {
                    insert.setString(1, targetUuid.toString());
                    insert.setString(2, targetName);
                    insert.setString(3, reason);
                    insert.setString(4, plan.banType());
                    insert.setLong(5, now);
                    if (plan.expiresAt() == null) {
                        insert.setNull(6, java.sql.Types.INTEGER);
                    } else {
                        insert.setLong(6, plan.expiresAt().toEpochMilli());
                    }
                    insert.setString(7, config.banSource());
                    insert.executeUpdate();
                }
                try (PreparedStatement update = connection.prepareStatement("""
                        UPDATE players
                        SET ban_count = ban_count + 1
                        WHERE uuid = ?
                        """)) {
                    update.setString(1, targetUuid.toString());
                    update.executeUpdate();
                }

                connection.commit();
                return null;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        });
    }

    private static int getBanCount(Connection connection, UUID targetUuid) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement("SELECT ban_count FROM players WHERE uuid = ?")) {
            select.setString(1, targetUuid.toString());
            try (ResultSet result = select.executeQuery()) {
                return result.next() ? result.getInt("ban_count") : 0;
            }
        }
    }

    private void applyBukkitBan(UUID targetUuid, String targetName, String reason, Instant expiresAt) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetUuid);
        Player online = Bukkit.getPlayer(targetUuid);
        if (online != null) {
            online.ban(reason, expiresAt, config.banSource(), true);
        } else {
            offlinePlayer.ban(reason, expiresAt, config.banSource());
        }
        plugin.notifyStaff("自動BAN: " + targetName + " / 理由: " + reason);
    }

    private record BanPlan(Instant expiresAt, String banType) {
    }
}
