package dev.modplugin.reputationban.service;

import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.database.DatabaseManager;
import dev.modplugin.reputationban.model.PlayerRecord;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PlayerDataService {
    private final DatabaseManager database;
    private volatile PluginConfig config;

    public PlayerDataService(DatabaseManager database, PluginConfig config) {
        this.database = database;
        this.config = config;
    }

    public void updateConfig(PluginConfig config) {
        this.config = config;
    }

    public CompletableFuture<Void> ensurePlayer(UUID uuid, String name) {
        long now = System.currentTimeMillis();
        return database.runAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO players (uuid, name, score, ban_count, false_report_count, first_seen, last_seen)
                    VALUES (?, ?, ?, 0, 0, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET name = excluded.name, last_seen = excluded.last_seen
                    """)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, name);
                statement.setInt(3, config.initialScore());
                statement.setLong(4, now);
                statement.setLong(5, now);
                statement.executeUpdate();
            }
        });
    }

    public CompletableFuture<Optional<PlayerRecord>> getPlayerRecord(UUID uuid) {
        return database.supplyAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT uuid, name, score, ban_count, false_report_count, report_banned_until, first_seen, last_seen
                    FROM players
                    WHERE uuid = ?
                    """)) {
                statement.setString(1, uuid.toString());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(readRecord(result));
                }
            }
        });
    }

    public CompletableFuture<Optional<PlayerRecord>> findByName(String name) {
        return database.supplyAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT uuid, name, score, ban_count, false_report_count, report_banned_until, first_seen, last_seen
                    FROM players
                    WHERE lower(name) = lower(?)
                    ORDER BY last_seen DESC
                    LIMIT 1
                    """)) {
                statement.setString(1, name);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(readRecord(result));
                }
            }
        });
    }

    public CompletableFuture<Integer> getScore(UUID uuid) {
        return getPlayerRecord(uuid).thenApply(record -> record.map(PlayerRecord::score).orElse(config.initialScore()));
    }

    public CompletableFuture<Integer> incrementBanCount(UUID uuid) {
        return database.supplyAsync(connection -> {
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE players
                    SET ban_count = ban_count + 1
                    WHERE uuid = ?
                    """)) {
                update.setString(1, uuid.toString());
                update.executeUpdate();
            }
            try (PreparedStatement select = connection.prepareStatement("SELECT ban_count FROM players WHERE uuid = ?")) {
                select.setString(1, uuid.toString());
                try (ResultSet result = select.executeQuery()) {
                    return result.next() ? result.getInt("ban_count") : 0;
                }
            }
        });
    }

    private static PlayerRecord readRecord(ResultSet result) throws SQLException {
        Long reportBannedUntil = nullableLong(result, "report_banned_until");
        return new PlayerRecord(
                UUID.fromString(result.getString("uuid")),
                result.getString("name"),
                result.getInt("score"),
                result.getInt("ban_count"),
                result.getInt("false_report_count"),
                reportBannedUntil,
                nullableLong(result, "first_seen"),
                nullableLong(result, "last_seen")
        );
    }

    private static Long nullableLong(ResultSet result, String column) throws SQLException {
        long value = result.getLong(column);
        return result.wasNull() ? null : value;
    }
}
