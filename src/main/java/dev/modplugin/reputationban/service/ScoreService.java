package dev.modplugin.reputationban.service;

import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.database.DatabaseManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ScoreService {
    private final DatabaseManager database;
    private volatile PluginConfig config;

    public ScoreService(DatabaseManager database, PluginConfig config) {
        this.database = database;
        this.config = config;
    }

    public void updateConfig(PluginConfig config) {
        this.config = config;
    }

    public CompletableFuture<ScoreChange> applyDelta(
            UUID targetUuid,
            String targetName,
            int delta,
            String reason,
            String sourceType,
            Long sourceId
    ) {
        return database.supplyAsync(connection -> {
            int oldScore;
            try (PreparedStatement select = connection.prepareStatement("SELECT score FROM players WHERE uuid = ?")) {
                select.setString(1, targetUuid.toString());
                try (ResultSet result = select.executeQuery()) {
                    oldScore = result.next() ? result.getInt("score") : config.initialScore();
                }
            }

            int newScore = Math.min(config.maxScore(), oldScore + delta);
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE players
                    SET score = ?, name = ?
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
                    """)) {
                insert.setString(1, targetUuid.toString());
                insert.setString(2, targetName);
                insert.setInt(3, oldScore);
                insert.setInt(4, newScore);
                insert.setInt(5, delta);
                insert.setString(6, reason);
                insert.setString(7, sourceType);
                if (sourceId == null) {
                    insert.setNull(8, java.sql.Types.INTEGER);
                } else {
                    insert.setLong(8, sourceId);
                }
                insert.setLong(9, System.currentTimeMillis());
                insert.executeUpdate();
            }
            return new ScoreChange(targetUuid, targetName, oldScore, newScore, delta);
        });
    }

    public record ScoreChange(UUID targetUuid, String targetName, int oldScore, int newScore, int delta) {
    }
}
