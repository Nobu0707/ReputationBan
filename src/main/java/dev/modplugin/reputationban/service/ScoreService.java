package dev.modplugin.reputationban.service;

import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.database.DatabaseManager;
import dev.modplugin.reputationban.model.AuditEvent;
import dev.modplugin.reputationban.model.AuditEventType;
import dev.modplugin.reputationban.util.AuditMetadata;
import dev.modplugin.reputationban.util.ScoreMath;
import dev.modplugin.reputationban.util.ScoreRecoveryPolicy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ScoreService {
    private final DatabaseManager database;
    private final AuditService auditService;
    private volatile PluginConfig config;

    public ScoreService(DatabaseManager database, AuditService auditService, PluginConfig config) {
        this.database = database;
        this.auditService = auditService;
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
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                ScoreChange change = mutateScoreInTransaction(
                        connection,
                        targetUuid,
                        targetName,
                        ScoreMutation.delta(delta),
                        reason,
                        sourceType,
                        sourceId,
                        System.currentTimeMillis()
                );
                connection.commit();
                return change;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        });
    }

    public CompletableFuture<ScoreChange> setScore(
            UUID targetUuid,
            String targetName,
            int score,
            String reason,
            String sourceType,
            Long sourceId
    ) {
        return database.supplyAsync(connection -> {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                ScoreChange change = mutateScoreInTransaction(
                        connection,
                        targetUuid,
                        targetName,
                        ScoreMutation.set(score),
                        reason,
                        sourceType,
                        sourceId,
                        System.currentTimeMillis()
                );
                connection.commit();
                return change;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        });
    }

    public ScoreChange mutateScoreInTransaction(
            Connection connection,
            UUID targetUuid,
            String targetName,
            ScoreMutation mutation,
            String reason,
            String sourceType,
            Long sourceId,
            long now
    ) throws SQLException {
        int oldScore = getCurrentScore(connection, targetUuid);
        int requestedScore = mutation.type() == ScoreMutation.Type.DELTA ? oldScore + mutation.value() : mutation.value();
        int newScore = ScoreMath.clampToMax(requestedScore, config.maxScore());
        int delta = newScore - oldScore;

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

        long scoreHistoryId;
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
            insert.setString(7, sourceType);
            if (sourceId == null) {
                insert.setNull(8, Types.INTEGER);
            } else {
                insert.setLong(8, sourceId);
            }
            insert.setLong(9, now);
            insert.executeUpdate();
            try (ResultSet generated = insert.getGeneratedKeys()) {
                scoreHistoryId = generated.next() ? generated.getLong(1) : -1L;
            }
        }

        return new ScoreChange(
                targetUuid,
                targetName,
                oldScore,
                newScore,
                delta,
                scoreHistoryId,
                ScoreMath.crossedThresholdDownward(oldScore, newScore, config.banThreshold())
        );
    }

    public CompletableFuture<List<ScoreHistoryEntry>> history(UUID targetUuid, int limit) {
        return database.supplyAsync(connection -> {
            List<ScoreHistoryEntry> history = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT old_score, new_score, delta, reason, source_type, source_id, created_at
                    FROM score_history
                    WHERE target_uuid = ?
                    ORDER BY created_at DESC
                    LIMIT ?
                    """)) {
                statement.setString(1, targetUuid.toString());
                statement.setInt(2, limit);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        Long sourceId = nullableLong(result, "source_id");
                        history.add(new ScoreHistoryEntry(
                                result.getInt("old_score"),
                                result.getInt("new_score"),
                                result.getInt("delta"),
                                result.getString("reason"),
                                result.getString("source_type"),
                                sourceId,
                                result.getLong("created_at")
                        ));
                    }
                }
            }
            return history;
        });
    }

    public CompletableFuture<RecoveryRunResult> runRecovery() {
        PluginConfig currentConfig = config;
        if (!currentConfig.scoreRecoveryEnabled() || currentConfig.recoveryPointsPerDay() <= 0) {
            return CompletableFuture.completedFuture(new RecoveryRunResult(0, 0));
        }
        long now = System.currentTimeMillis();
        return database.supplyAsync(connection -> {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                RecoveryRunResult result = runRecoveryInTransaction(connection, currentConfig, now);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        });
    }

    private RecoveryRunResult runRecoveryInTransaction(Connection connection, PluginConfig currentConfig, long now) throws SQLException {
        List<RecoveryCandidate> candidates = new ArrayList<>();
        int recovered = 0;
        try (PreparedStatement select = connection.prepareStatement("""
                SELECT uuid, name, score, last_recovery_at
                FROM players
                WHERE score < ?
                """)) {
            select.setInt(1, currentConfig.recoveryMaxScore());
            try (ResultSet result = select.executeQuery()) {
                while (result.next()) {
                    candidates.add(new RecoveryCandidate(
                            UUID.fromString(result.getString("uuid")),
                            result.getString("name"),
                            result.getInt("score"),
                            nullableLong(result, "last_recovery_at")
                    ));
                }
            }
        }
        for (RecoveryCandidate candidate : candidates) {
            if (ScoreRecoveryPolicy.recentlyRecovered(candidate.lastRecoveryAt(), now)) {
                continue;
            }
            Long lastValidReportAt = lastValidReportAt(connection, candidate.uuid());
            if (!ScoreRecoveryPolicy.hasEnoughNoReportTime(
                    lastValidReportAt,
                    currentConfig.recoveryNoReportDaysRequired(),
                    now
            )) {
                continue;
            }

            int newScore = ScoreRecoveryPolicy.recoveredScore(
                    candidate.score(),
                    currentConfig.recoveryPointsPerDay(),
                    currentConfig.recoveryMaxScore()
            );
            int delta = newScore - candidate.score();
            if (delta <= 0) {
                continue;
            }
            updateRecoveredPlayer(connection, candidate.uuid(), candidate.name(), candidate.score(), newScore, delta, now);
            auditService.recordEventInTransaction(connection, AuditEvent.create(
                    AuditEventType.SCORE_RECOVERED,
                    null,
                    "SYSTEM",
                    candidate.uuid(),
                    candidate.name(),
                    null,
                    null,
                    null,
                    candidate.score(),
                    newScore,
                    delta,
                    "recovery",
                    AuditMetadata.create().put("source", "score-recovery").toJson(),
                    now
            ));
            recovered++;
        }
        return new RecoveryRunResult(candidates.size(), recovered);
    }

    private static Long lastValidReportAt(Connection connection, UUID targetUuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT MAX(created_at)
                FROM reports
                WHERE target_uuid = ? AND status IN ('approved', 'auto_accepted')
                """)) {
            statement.setString(1, targetUuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                long value = result.getLong(1);
                return result.wasNull() ? null : value;
            }
        }
    }

    private static void updateRecoveredPlayer(
            Connection connection,
            UUID targetUuid,
            String targetName,
            int oldScore,
            int newScore,
            int delta,
            long now
    ) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement("""
                UPDATE players
                SET score = ?, last_recovery_at = ?
                WHERE uuid = ?
                """)) {
            update.setInt(1, newScore);
            update.setLong(2, now);
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
            insert.setString(6, "問題なし期間によるスコア回復");
            insert.setString(7, "recovery");
            insert.setNull(8, Types.INTEGER);
            insert.setLong(9, now);
            insert.executeUpdate();
        }
    }

    private int getCurrentScore(Connection connection, UUID targetUuid) throws SQLException {
        return currentScoreInTransaction(connection, targetUuid, config.initialScore());
    }

    public static int currentScoreInTransaction(Connection connection, UUID targetUuid, int fallbackScore) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement("SELECT score FROM players WHERE uuid = ?")) {
            select.setString(1, targetUuid.toString());
            try (ResultSet result = select.executeQuery()) {
                return result.next() ? result.getInt("score") : fallbackScore;
            }
        }
    }

    private static Long nullableLong(ResultSet result, String column) throws SQLException {
        long value = result.getLong(column);
        return result.wasNull() ? null : value;
    }

    public record ScoreMutation(Type type, int value) {
        public static ScoreMutation delta(int delta) {
            return new ScoreMutation(Type.DELTA, delta);
        }

        public static ScoreMutation set(int score) {
            return new ScoreMutation(Type.SET, score);
        }

        public enum Type {
            DELTA,
            SET
        }
    }

    public record ScoreChange(
            UUID targetUuid,
            String targetName,
            int oldScore,
            int newScore,
            int delta,
            long scoreHistoryId,
            boolean crossedBanThreshold
    ) {
    }

    public record ScoreHistoryEntry(
            int oldScore,
            int newScore,
            int delta,
            String reason,
            String sourceType,
            Long sourceId,
            long createdAt
    ) {
    }

    public record RecoveryRunResult(int checkedPlayers, int recoveredPlayers) {
    }

    private record RecoveryCandidate(UUID uuid, String name, int score, Long lastRecoveryAt) {
    }
}
