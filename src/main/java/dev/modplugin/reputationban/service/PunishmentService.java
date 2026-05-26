package dev.modplugin.reputationban.service;

import dev.modplugin.reputationban.ReputationBanPlugin;
import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.model.PlayerRecord;
import dev.modplugin.reputationban.util.DurationParser;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public final class PunishmentService {
    private final ReputationBanPlugin plugin;
    private final dev.modplugin.reputationban.database.DatabaseManager database;
    private final PlayerDataService playerDataService;
    private volatile PluginConfig config;

    public PunishmentService(
            ReputationBanPlugin plugin,
            dev.modplugin.reputationban.database.DatabaseManager database,
            PlayerDataService playerDataService,
            PluginConfig config
    ) {
        this.plugin = plugin;
        this.database = database;
        this.playerDataService = playerDataService;
        this.config = config;
    }

    public void updateConfig(PluginConfig config) {
        this.config = config;
    }

    public CompletableFuture<Boolean> punishIfNeeded(UUID targetUuid, String targetName, int score, String reason) {
        if (!config.banEnabled() || score > config.banThreshold()) {
            return CompletableFuture.completedFuture(false);
        }

        CompletableFuture<Boolean> bypassCheck = new CompletableFuture<>();
        plugin.runSync(() -> {
            Player online = Bukkit.getPlayer(targetUuid);
            bypassCheck.complete(online != null && online.hasPermission("reputationban.bypass"));
        });

        return bypassCheck.thenCompose(bypassed -> {
            if (bypassed) {
                return CompletableFuture.completedFuture(false);
            }
            return playerDataService.getPlayerRecord(targetUuid).thenCompose(record -> {
                if (record.isEmpty()) {
                    return CompletableFuture.completedFuture(false);
                }
                return recordBan(record.get(), reason).thenApply(expiresAt -> {
                    plugin.runSync(() -> applyBukkitBan(targetUuid, targetName, reason, expiresAt));
                    return true;
                });
            });
        });
    }

    private CompletableFuture<Long> recordBan(PlayerRecord record, String reason) {
        String durationValue = config.banDurationForCount(record.banCount());
        Optional<Duration> duration = DurationParser.parseBanDuration(durationValue);
        long now = System.currentTimeMillis();
        Long expiresAt = duration.map(value -> now + value.toMillis()).orElse(null);
        String banType = expiresAt == null ? "permanent" : "temporary";

        return database.supplyAsync(connection -> {
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO bans (
                      target_uuid, target_name, reason, ban_type, created_at, expires_at, created_by
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """)) {
                insert.setString(1, record.uuid().toString());
                insert.setString(2, record.name());
                insert.setString(3, reason);
                insert.setString(4, banType);
                insert.setLong(5, now);
                if (expiresAt == null) {
                    insert.setNull(6, java.sql.Types.INTEGER);
                } else {
                    insert.setLong(6, expiresAt);
                }
                insert.setString(7, config.banSource());
                insert.executeUpdate();
            }
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE players
                    SET ban_count = ban_count + 1
                    WHERE uuid = ?
                    """)) {
                update.setString(1, record.uuid().toString());
                update.executeUpdate();
            }
            return expiresAt;
        });
    }

    @SuppressWarnings("deprecation")
    private void applyBukkitBan(UUID targetUuid, String targetName, String reason, Long expiresAt) {
        Date expiration = expiresAt == null ? null : Date.from(Instant.ofEpochMilli(expiresAt));
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetUuid);
        Bukkit.getBanList(BanList.Type.NAME).addBan(targetName, reason, expiration, config.banSource());
        if (offlinePlayer.getName() != null && !offlinePlayer.getName().equalsIgnoreCase(targetName)) {
            Bukkit.getBanList(BanList.Type.NAME).addBan(offlinePlayer.getName(), reason, expiration, config.banSource());
        }

        Player online = Bukkit.getPlayer(targetUuid);
        if (online != null) {
            online.kickPlayer("[ReputationBan] 評判スコアがしきい値を下回ったためBANされました。");
        }
        plugin.notifyStaff("自動BAN: " + targetName + " / 理由: " + reason);
    }
}
