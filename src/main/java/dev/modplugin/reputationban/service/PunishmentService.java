package dev.modplugin.reputationban.service;

import dev.modplugin.reputationban.ReputationBanPlugin;
import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.model.AuditEvent;
import dev.modplugin.reputationban.model.AuditEventType;
import dev.modplugin.reputationban.model.CommandActor;
import dev.modplugin.reputationban.model.TargetProtectionResult;
import dev.modplugin.reputationban.notification.DiscordWebhookConfig;
import dev.modplugin.reputationban.notification.NotificationEventType;
import dev.modplugin.reputationban.util.AuditMetadata;
import dev.modplugin.reputationban.util.BanManagementPolicy;
import dev.modplugin.reputationban.util.DurationParser;
import dev.modplugin.reputationban.util.StringLimits;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import io.papermc.paper.ban.BanListType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.entity.Player;

public final class PunishmentService {
    private final ReputationBanPlugin plugin;
    private final dev.modplugin.reputationban.database.DatabaseManager database;
    private final AuditService auditService;
    private final TargetProtectionService targetProtectionService;
    private volatile PluginConfig config;

    public PunishmentService(
            ReputationBanPlugin plugin,
            dev.modplugin.reputationban.database.DatabaseManager database,
            AuditService auditService,
            TargetProtectionService targetProtectionService,
            PluginConfig config
    ) {
        this.plugin = plugin;
        this.database = database;
        this.auditService = auditService;
        this.targetProtectionService = targetProtectionService;
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

        String safeReason = StringLimits.truncate(reason, config.maxAuditReasonLength());
        CompletableFuture<Boolean> alreadyBanned = plugin.supplySync(() -> Bukkit.getOfflinePlayer(targetUuid).isBanned());
        return alreadyBanned.thenCompose(blocked -> {
            if (blocked) {
                return CompletableFuture.completedFuture(false);
            }
            return targetProtectionService.check(targetUuid)
                    .thenCompose(protection -> punishUnprotectedTarget(targetUuid, targetName, safeReason, protection));
        });
    }

    private CompletableFuture<Boolean> punishUnprotectedTarget(
            UUID targetUuid,
            String targetName,
            String reason,
            TargetProtectionResult protection
    ) {
        if (protection.protectedTarget()) {
            return CompletableFuture.completedFuture(false);
        }
        return createBanPlan(targetUuid)
                .thenCompose(plan -> plugin.runSyncFuture(() -> applyBukkitBan(targetUuid, targetName, reason, plan.expiresAt()))
                        .thenCompose(ignored -> recordBan(targetUuid, targetName, reason, plan)
                                .handle((recorded, throwable) -> {
                                    if (throwable != null) {
                                        warnBanRecordFailure(targetUuid, targetName, throwable);
                                        throw new CompletionException(throwable);
                                    }
                                    return true;
                                })));
    }

    private void warnBanRecordFailure(UUID targetUuid, String targetName, Throwable throwable) {
        plugin.getLogger().log(
                java.util.logging.Level.SEVERE,
                "Bukkit Profile BAN was applied but ReputationBan DB record failed for targetUuid="
                        + targetUuid + " targetName=" + targetName,
                throwable
        );
        plugin.runSync(() -> plugin.notifyStaff(
                NotificationEventType.AUTO_BAN,
                "警告: Profile BANは適用済みですがDB記録に失敗しました: " + targetName + " (" + targetUuid + ")",
                "**BAN記録失敗**\n対象: " + targetName + "\nUUID: " + targetUuid
        ));
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
                long banId;
                try (PreparedStatement insert = connection.prepareStatement("""
                        INSERT INTO bans (
                          target_uuid, target_name, reason, ban_type, created_at, expires_at, created_by
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
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
                    try (ResultSet generated = insert.getGeneratedKeys()) {
                        banId = generated.next() ? generated.getLong(1) : -1L;
                    }
                }
                try (PreparedStatement update = connection.prepareStatement("""
                        UPDATE players
                        SET ban_count = ban_count + 1
                        WHERE uuid = ?
                        """)) {
                    update.setString(1, targetUuid.toString());
                    update.executeUpdate();
                }
                int banCount = getBanCount(connection, targetUuid);
                auditService.recordEventInTransaction(connection, AuditEvent.create(
                        AuditEventType.AUTO_BAN,
                        null,
                        "SYSTEM",
                        targetUuid,
                        targetName,
                        null,
                        banId,
                        null,
                        null,
                        null,
                        null,
                        reason,
                        AuditMetadata.create()
                                .put("expiresAt", plan.expiresAt() == null ? "permanent" : plan.expiresAt().toEpochMilli())
                                .put("banCount", banCount)
                                .put("banType", plan.banType())
                                .toJson(),
                        now
                ));

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
        plugin.notifyStaff(
                NotificationEventType.AUTO_BAN,
                "自動BAN: " + targetName + " / 理由: " + reason,
                autoBanDiscord(targetName, targetUuid, reason, expiresAt)
        );
    }

    private String autoBanDiscord(String targetName, UUID targetUuid, String reason, Instant expiresAt) {
        DiscordWebhookConfig discord = config.discordWebhookConfig();
        StringBuilder message = new StringBuilder();
        message.append("**自動BAN**\n");
        message.append("対象: ").append(targetName);
        if (discord.includePlayerUuids()) {
            message.append(" (").append(targetUuid).append(")");
        }
        message.append('\n');
        message.append("期限: ").append(expiresAt == null ? "permanent" : expiresAt);
        if (discord.includeReasons()) {
            message.append('\n').append("理由: ").append(reason);
        }
        return message.toString();
    }

    public CompletableFuture<List<BanHistoryEntry>> banHistory(UUID targetUuid, int limit) {
        return database.supplyAsync(connection -> {
            List<BanHistoryEntry> history = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT id, reason, ban_type, created_at, expires_at, created_by,
                           unbanned_at, unbanned_by, unbanned_by_name, unban_reason
                    FROM bans
                    WHERE target_uuid = ?
                    ORDER BY created_at DESC
                    LIMIT ?
                    """)) {
                statement.setString(1, targetUuid.toString());
                statement.setInt(2, limit);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        history.add(readBanHistoryEntry(result));
                    }
                }
            }
            return history;
        });
    }

    public CompletableFuture<CurrentBanInfo> currentBanInfo(UUID targetUuid) {
        long now = System.currentTimeMillis();
        return database.supplyAsync(connection -> {
            int banCount = getBanCount(connection, targetUuid);
            int activeCount;
            try (PreparedStatement count = connection.prepareStatement("""
                    SELECT COUNT(*)
                    FROM bans
                    WHERE target_uuid = ?
                      AND unbanned_at IS NULL
                      AND (expires_at IS NULL OR expires_at > ?)
                    """)) {
                count.setString(1, targetUuid.toString());
                count.setLong(2, now);
                try (ResultSet result = count.executeQuery()) {
                    activeCount = result.next() ? result.getInt(1) : 0;
                }
            }

            Optional<BanHistoryEntry> latest = Optional.empty();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT id, reason, ban_type, created_at, expires_at, created_by,
                           unbanned_at, unbanned_by, unbanned_by_name, unban_reason
                    FROM bans
                    WHERE target_uuid = ?
                      AND unbanned_at IS NULL
                      AND (expires_at IS NULL OR expires_at > ?)
                    ORDER BY created_at DESC
                    LIMIT 1
                    """)) {
                statement.setString(1, targetUuid.toString());
                statement.setLong(2, now);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        latest = Optional.of(readBanHistoryEntry(result));
                    }
                }
            }
            return new CurrentBanInfo(activeCount, latest, banCount);
        });
    }

    public CompletableFuture<ProfileUnbanResult> unbanProfile(UUID targetUuid) {
        return plugin.supplySync(() -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetUuid);
            ProfileBanList profileBanList = Bukkit.getBanList(BanListType.PROFILE);
            boolean wasBanned = profileBanList.isBanned(offlinePlayer.getPlayerProfile());
            profileBanList.pardon(offlinePlayer.getPlayerProfile());
            return new ProfileUnbanResult(wasBanned);
        });
    }

    public CompletableFuture<UnbanResult> markActiveBansUnbanned(
            UUID targetUuid,
            CommandActor actor,
            String unbanReason,
            boolean profileBanRemoved
    ) {
        long now = System.currentTimeMillis();
        return database.supplyAsync(connection -> {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                int updated = markActiveBansUnbannedInTransaction(connection, targetUuid, actor, unbanReason, now);
                auditService.recordEventInTransaction(connection, AuditEvent.create(
                        AuditEventType.UNBAN,
                        actor.uuid(),
                        actor.name(),
                        targetUuid,
                        playerName(connection, targetUuid),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        unbanReason,
                        AuditMetadata.create()
                                .put("profileBanRemoved", profileBanRemoved)
                                .put("dbActiveBanUpdatedCount", updated)
                                .toJson(),
                        now
                ));
                connection.commit();
                return new UnbanResult(updated);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        });
    }

    public CompletableFuture<PardonResult> pardon(
            UUID targetUuid,
            String targetName,
            String reason,
            CommandActor actor,
            boolean profileBanRemoved
    ) {
        long now = System.currentTimeMillis();
        return database.supplyAsync(connection -> {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                int updatedBans = markActiveBansUnbannedInTransaction(connection, targetUuid, actor, reason, now);
                int oldScore = ScoreService.currentScoreInTransaction(connection, targetUuid, config.initialScore());
                int newScore = BanManagementPolicy.pardonTargetScore(oldScore, config.maxScore());
                int delta = newScore - oldScore;
                long scoreHistoryId;

                try (PreparedStatement update = connection.prepareStatement("""
                        UPDATE players
                        SET score = ?, name = ?, report_banned_until = NULL
                        WHERE uuid = ?
                        """)) {
                    update.setInt(1, newScore);
                    update.setString(2, targetName);
                    update.setString(3, targetUuid.toString());
                    update.executeUpdate();
                }

                try (PreparedStatement insert = connection.prepareStatement("""
                        INSERT INTO score_history (
                          target_uuid, target_name, old_score, new_score, delta, reason, source_type, source_id, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    insert.setString(1, targetUuid.toString());
                    insert.setString(2, targetName);
                    insert.setInt(3, oldScore);
                    insert.setInt(4, newScore);
                    insert.setInt(5, delta);
                    insert.setString(6, reason);
                    insert.setString(7, "pardon");
                    insert.setNull(8, Types.INTEGER);
                    insert.setLong(9, now);
                    insert.executeUpdate();
                    try (ResultSet generated = insert.getGeneratedKeys()) {
                        scoreHistoryId = generated.next() ? generated.getLong(1) : -1L;
                    }
                }
                auditService.recordEventInTransaction(connection, AuditEvent.create(
                        AuditEventType.PARDON,
                        actor.uuid(),
                        actor.name(),
                        targetUuid,
                        targetName,
                        null,
                        null,
                        scoreHistoryId,
                        oldScore,
                        newScore,
                        delta,
                        reason,
                        AuditMetadata.create()
                                .put("profileBanRemoved", profileBanRemoved)
                                .put("dbActiveBanUpdatedCount", updatedBans)
                                .put("reportBanCleared", true)
                                .toJson(),
                        now
                ));

                connection.commit();
                return new PardonResult(updatedBans, oldScore, newScore, delta);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        });
    }

    private static int markActiveBansUnbannedInTransaction(
            Connection connection,
            UUID targetUuid,
            CommandActor actor,
            String unbanReason,
            long now
    ) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement("""
                UPDATE bans
                SET unbanned_at = ?, unbanned_by = ?, unbanned_by_name = ?, unban_reason = ?
                WHERE target_uuid = ?
                  AND unbanned_at IS NULL
                  AND (expires_at IS NULL OR expires_at > ?)
                """)) {
            update.setLong(1, now);
            update.setString(2, actor.databaseActorId());
            update.setString(3, actor.name());
            update.setString(4, unbanReason);
            update.setString(5, targetUuid.toString());
            update.setLong(6, now);
            return update.executeUpdate();
        }
    }

    private static BanHistoryEntry readBanHistoryEntry(ResultSet result) throws SQLException {
        return new BanHistoryEntry(
                result.getLong("id"),
                result.getString("reason"),
                result.getString("ban_type"),
                result.getLong("created_at"),
                nullableLong(result, "expires_at"),
                result.getString("created_by"),
                nullableLong(result, "unbanned_at"),
                result.getString("unbanned_by"),
                result.getString("unbanned_by_name"),
                result.getString("unban_reason")
        );
    }

    private static Long nullableLong(ResultSet result, String column) throws SQLException {
        long value = result.getLong(column);
        return result.wasNull() ? null : value;
    }

    private static String playerName(Connection connection, UUID targetUuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT name FROM players WHERE uuid = ?")) {
            statement.setString(1, targetUuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString("name") : null;
            }
        }
    }

    private record BanPlan(Instant expiresAt, String banType) {
    }

    public record BanHistoryEntry(
            long id,
            String reason,
            String banType,
            long createdAt,
            Long expiresAt,
            String createdBy,
            Long unbannedAt,
            String unbannedBy,
            String unbannedByName,
            String unbanReason
    ) {
    }

    public record CurrentBanInfo(int activeBanCount, Optional<BanHistoryEntry> latestActiveBan, int banCount) {
    }

    public record ProfileUnbanResult(boolean wasProfileBanned) {
    }

    public record UnbanResult(int updatedActiveBans) {
    }

    public record PardonResult(int updatedActiveBans, int oldScore, int newScore, int delta) {
    }
}
