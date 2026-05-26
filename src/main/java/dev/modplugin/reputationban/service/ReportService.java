package dev.modplugin.reputationban.service;

import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.database.DatabaseManager;
import dev.modplugin.reputationban.model.ReportCategory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ReportService {
    private final DatabaseManager database;
    private volatile PluginConfig config;

    public ReportService(DatabaseManager database, PluginConfig config) {
        this.database = database;
        this.config = config;
    }

    public void updateConfig(PluginConfig config) {
        this.config = config;
    }

    public CompletableFuture<ReportResult> submitReport(
            UUID reporterUuid,
            String reporterName,
            UUID targetUuid,
            String targetName,
            ReportCategory category,
            String reason
    ) {
        long now = System.currentTimeMillis();
        long globalCutoff = now - Duration.ofSeconds(config.globalReportSeconds()).toMillis();
        long sameTargetCutoff = now - Duration.ofDays(config.sameTargetCooldownDays()).toMillis();
        long dayCutoff = now - Duration.ofDays(1).toMillis();
        long weekCutoff = now - Duration.ofDays(7).toMillis();

        return database.supplyAsync(connection -> {
            Long reportBanUntil = getReportBannedUntil(connection, reporterUuid);
            if (reportBanUntil != null && reportBanUntil > now) {
                return ReportResult.rejected("現在、通報機能の利用が一時停止されています。");
            }
            if (countReports(connection, reporterUuid, null, globalCutoff) > 0) {
                return ReportResult.rejected("通報クールダウン中です。しばらく待ってから再試行してください。");
            }
            if (countReports(connection, reporterUuid, targetUuid, sameTargetCutoff) > 0) {
                return ReportResult.rejected("同じプレイヤーへの再通報はまだできません。");
            }
            if (countReports(connection, reporterUuid, null, dayCutoff) >= config.maxReportsPerDay()) {
                return ReportResult.rejected("1日の通報上限に達しています。");
            }
            if (countReports(connection, reporterUuid, null, weekCutoff) >= config.maxReportsPerWeek()) {
                return ReportResult.rejected("1週間の通報上限に達しています。");
            }

            if (category.staffReviewRequired()) {
                long reportId = insertReport(
                        connection,
                        reporterUuid,
                        reporterName,
                        targetUuid,
                        targetName,
                        category,
                        reason,
                        "pending",
                        0,
                        now
                );
                return ReportResult.accepted(reportId, "pending", 0, true, null, null, false);
            }

            return submitAutoAcceptedReportInTransaction(
                    connection,
                    reporterUuid,
                    reporterName,
                    targetUuid,
                    targetName,
                    category,
                    reason,
                    now
            );
        });
    }

    private ReportResult submitAutoAcceptedReportInTransaction(
            Connection connection,
            UUID reporterUuid,
            String reporterName,
            UUID targetUuid,
            String targetName,
            ReportCategory category,
            String reason,
            long now
    ) throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            int deduction = category.deduction();
            long reportId = insertReport(
                    connection,
                    reporterUuid,
                    reporterName,
                    targetUuid,
                    targetName,
                    category,
                    reason,
                    "auto_accepted",
                    deduction,
                    now
            );
            int oldScore = getCurrentScore(connection, targetUuid);
            int newScore = Math.min(config.maxScore(), oldScore - deduction);
            updateScore(connection, targetUuid, targetName, newScore);
            insertScoreHistory(
                    connection,
                    targetUuid,
                    targetName,
                    oldScore,
                    newScore,
                    -deduction,
                    "Report #" + reportId + ": " + category.key(),
                    reportId,
                    now
            );
            connection.commit();
            boolean crossedThreshold = oldScore > config.banThreshold() && newScore <= config.banThreshold();
            return ReportResult.accepted(reportId, "auto_accepted", deduction, false, oldScore, newScore, crossedThreshold);
        } catch (SQLException | RuntimeException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private static long insertReport(
            Connection connection,
            UUID reporterUuid,
            String reporterName,
            UUID targetUuid,
            String targetName,
            ReportCategory category,
            String reason,
            String status,
            int deduction,
            long now
    ) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO reports (
                  reporter_uuid, reporter_name, target_uuid, target_name, category,
                  reason, status, deduction, created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            insert.setString(1, reporterUuid.toString());
            insert.setString(2, reporterName);
            insert.setString(3, targetUuid.toString());
            insert.setString(4, targetName);
            insert.setString(5, category.key());
            insert.setString(6, reason);
            insert.setString(7, status);
            insert.setInt(8, deduction);
            insert.setLong(9, now);
            insert.executeUpdate();
            try (ResultSet generated = insert.getGeneratedKeys()) {
                return generated.next() ? generated.getLong(1) : -1L;
            }
        }
    }

    private int getCurrentScore(Connection connection, UUID targetUuid) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement("SELECT score FROM players WHERE uuid = ?")) {
            select.setString(1, targetUuid.toString());
            try (ResultSet result = select.executeQuery()) {
                return result.next() ? result.getInt("score") : config.initialScore();
            }
        }
    }

    private static void updateScore(Connection connection, UUID targetUuid, String targetName, int newScore) throws SQLException {
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
    }

    private static void insertScoreHistory(
            Connection connection,
            UUID targetUuid,
            String targetName,
            int oldScore,
            int newScore,
            int delta,
            String reason,
            long sourceId,
            long now
    ) throws SQLException {
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
            insert.setString(7, "report");
            insert.setLong(8, sourceId);
            insert.setLong(9, now);
            insert.executeUpdate();
        }
    }

    public CompletableFuture<java.util.List<ReportSummary>> recentReports(int limit) {
        return database.supplyAsync(connection -> {
            java.util.List<ReportSummary> reports = new java.util.ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT id, reporter_name, target_name, category, status, deduction, created_at
                    FROM reports
                    ORDER BY created_at DESC
                    LIMIT ?
                    """)) {
                statement.setInt(1, limit);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        reports.add(new ReportSummary(
                                result.getLong("id"),
                                result.getString("reporter_name"),
                                result.getString("target_name"),
                                result.getString("category"),
                                result.getString("status"),
                                result.getInt("deduction"),
                                result.getLong("created_at")
                        ));
                    }
                }
            }
            return reports;
        });
    }

    private static Long getReportBannedUntil(java.sql.Connection connection, UUID reporterUuid) throws java.sql.SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT report_banned_until
                FROM players
                WHERE uuid = ?
                """)) {
            statement.setString(1, reporterUuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                long value = result.getLong("report_banned_until");
                return result.wasNull() ? null : value;
            }
        }
    }

    private static int countReports(
            java.sql.Connection connection,
            UUID reporterUuid,
            UUID targetUuid,
            long createdAfter
    ) throws java.sql.SQLException {
        String sql = targetUuid == null
                ? "SELECT COUNT(*) FROM reports WHERE reporter_uuid = ? AND created_at >= ?"
                : "SELECT COUNT(*) FROM reports WHERE reporter_uuid = ? AND target_uuid = ? AND created_at >= ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, reporterUuid.toString());
            if (targetUuid == null) {
                statement.setLong(2, createdAfter);
            } else {
                statement.setString(2, targetUuid.toString());
                statement.setLong(3, createdAfter);
            }
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        }
    }

    public record ReportResult(
            boolean accepted,
            String message,
            long reportId,
            String status,
            int deduction,
            boolean staffReviewRequired,
            Integer oldScore,
            Integer newScore,
            boolean crossedBanThreshold
    ) {
        static ReportResult rejected(String message) {
            return new ReportResult(false, message, -1L, "rejected", 0, false, null, null, false);
        }

        static ReportResult accepted(
                long reportId,
                String status,
                int deduction,
                boolean staffReviewRequired,
                Integer oldScore,
                Integer newScore,
                boolean crossedBanThreshold
        ) {
            return new ReportResult(
                    true,
                    "通報を受け付けました。",
                    reportId,
                    status,
                    deduction,
                    staffReviewRequired,
                    oldScore,
                    newScore,
                    crossedBanThreshold
            );
        }
    }

    public record ReportSummary(
            long id,
            String reporterName,
            String targetName,
            String category,
            String status,
            int deduction,
            long createdAt
    ) {
    }
}
