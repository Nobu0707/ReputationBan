package dev.modplugin.reputationban.service;

import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.database.DatabaseManager;
import dev.modplugin.reputationban.model.ReportCategory;
import dev.modplugin.reputationban.model.ReportStatus;
import dev.modplugin.reputationban.util.ReporterPenalty;
import dev.modplugin.reputationban.util.ReviewApprovalGate;
import dev.modplugin.reputationban.util.ScoreMath;
import dev.modplugin.reputationban.util.ThresholdReportPolicy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ReportService {
    private static final DateTimeFormatter REPORT_BAN_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final DatabaseManager database;
    private final ScoreService scoreService;
    private volatile PluginConfig config;

    public ReportService(DatabaseManager database, ScoreService scoreService, PluginConfig config) {
        this.database = database;
        this.scoreService = scoreService;
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
            if (ReporterPenalty.isReportBanned(reportBanUntil, now)) {
                return ReportResult.rejected("あなたは現在、通報機能の利用を一時停止されています。解除予定: "
                        + REPORT_BAN_FORMATTER.format(Instant.ofEpochMilli(reportBanUntil)));
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

            if (ThresholdReportPolicy.shouldQueueForThreshold(config.minUniqueReportsBeforeDeduction())) {
                return submitThresholdPendingReportInTransaction(
                        connection,
                        reporterUuid,
                        reporterName,
                        targetUuid,
                        targetName,
                        category,
                        reason,
                        now
                );
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
            ScoreService.ScoreChange change = scoreService.mutateScoreInTransaction(
                    connection,
                    targetUuid,
                    targetName,
                    ScoreService.ScoreMutation.delta(-deduction),
                    "Report #" + reportId + ": " + category.key(),
                    "report",
                    reportId,
                    now
            );
            connection.commit();
            return ReportResult.accepted(
                    reportId,
                    "auto_accepted",
                    deduction,
                    false,
                    change.oldScore(),
                    change.newScore(),
                    change.crossedBanThreshold()
            );
        } catch (SQLException | RuntimeException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private ReportResult submitThresholdPendingReportInTransaction(
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
            int requiredReports = ThresholdReportPolicy.effectiveRequiredReports(config.minUniqueReportsBeforeDeduction());
            long reportId = insertReport(
                    connection,
                    reporterUuid,
                    reporterName,
                    targetUuid,
                    targetName,
                    category,
                    reason,
                    "threshold_pending",
                    0,
                    now
            );

            long windowCutoff = ThresholdReportPolicy.windowCutoff(now, config.reportWindowDays());
            int currentUniqueReports = countUniqueThresholdPendingReporters(
                    connection,
                    targetUuid,
                    category.key(),
                    windowCutoff
            );
            if (!ThresholdReportPolicy.thresholdReached(requiredReports, currentUniqueReports)) {
                connection.commit();
                return ReportResult.thresholdPending(reportId, requiredReports, currentUniqueReports);
            }

            int deduction = category.deduction();
            ScoreService.ScoreChange change = scoreService.mutateScoreInTransaction(
                    connection,
                    targetUuid,
                    targetName,
                    ScoreService.ScoreMutation.delta(-deduction),
                    "Threshold reports reached #" + reportId + ": " + category.key(),
                    "report_threshold",
                    reportId,
                    now
            );
            int acceptedReportCount = autoAcceptThresholdPendingReports(
                    connection,
                    targetUuid,
                    category.key(),
                    deduction,
                    windowCutoff,
                    now,
                    currentUniqueReports,
                    requiredReports
            );
            connection.commit();
            return ReportResult.thresholdReached(
                    reportId,
                    deduction,
                    change.oldScore(),
                    change.newScore(),
                    change.crossedBanThreshold(),
                    requiredReports,
                    currentUniqueReports,
                    acceptedReportCount
            );
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

    public CompletableFuture<java.util.List<ReportSummary>> recentReports(int limit) {
        return listReports("all", limit);
    }

    public CompletableFuture<java.util.List<ReportSummary>> listReports(String status, int limit) {
        return database.supplyAsync(connection -> {
            java.util.List<ReportSummary> reports = new java.util.ArrayList<>();
            boolean all = status == null || "all".equalsIgnoreCase(status);
            String sql = all
                    ? """
                      SELECT id, reporter_name, target_name, category, status, deduction, created_at
                      FROM reports
                      ORDER BY created_at DESC
                      LIMIT ?
                      """
                    : """
                      SELECT id, reporter_name, target_name, category, status, deduction, created_at
                      FROM reports
                      WHERE status = ?
                      ORDER BY created_at DESC
                      LIMIT ?
                      """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                if (all) {
                    statement.setInt(1, limit);
                } else {
                    statement.setString(1, ReportStatus.parse(status).databaseValue());
                    statement.setInt(2, limit);
                }
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

    public CompletableFuture<java.util.Optional<ReportRecord>> getReport(long id) {
        return database.supplyAsync(connection -> loadReport(connection, id));
    }

    public CompletableFuture<ReviewResult> approveReport(
            long id,
            UUID moderatorUuid,
            String moderatorName,
            String note,
            boolean hasBanPermission,
            boolean targetProtected
    ) {
        long now = System.currentTimeMillis();
        return database.supplyAsync(connection -> {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                java.util.Optional<ReportRecord> optionalReport = loadReport(connection, id);
                if (optionalReport.isEmpty()) {
                    connection.rollback();
                    return ReviewResult.failed("通報が見つかりません。");
                }
                ReportRecord report = optionalReport.get();
                if (!ReportStatus.canReview(report.status())) {
                    connection.rollback();
                    return ReviewResult.failed("この通報は既に処理済みです。");
                }
                ReportCategory category = config.category(report.category());
                if (category == null) {
                    connection.rollback();
                    return ReviewResult.failed("設定にカテゴリが見つかりません: " + report.category());
                }
                if (targetProtected) {
                    connection.rollback();
                    return ReviewResult.failed("対象プレイヤーは保護されているため承認できません。");
                }

                int deduction = category.deduction();
                int oldScore = ScoreService.currentScoreInTransaction(connection, report.targetUuid(), config.initialScore());
                int newScore = ScoreMath.clampToMax(oldScore - deduction, config.maxScore());
                if (!ReviewApprovalGate.canApprove(false, oldScore, newScore, config.banThreshold(), hasBanPermission)) {
                    connection.rollback();
                    return ReviewResult.failed("この承認は対象をBANしきい値以下にするため、reputationban.admin.ban 権限が必要です。");
                }
                updateReportReview(connection, id, "approved", deduction, moderatorUuid, moderatorName, note, now);
                ScoreService.ScoreChange change = scoreService.mutateScoreInTransaction(
                        connection,
                        report.targetUuid(),
                        report.targetName(),
                        ScoreService.ScoreMutation.delta(-deduction),
                        "Approved report #" + id + ": " + category.key() + " / " + moderatorName + " / " + normalizeNote(note),
                        "report_review",
                        id,
                        now
                );
                connection.commit();
                return ReviewResult.approved(report, deduction, change);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        });
    }

    public CompletableFuture<ReviewResult> rejectReport(long id, UUID moderatorUuid, String moderatorName, String note) {
        long now = System.currentTimeMillis();
        return database.supplyAsync(connection -> {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                java.util.Optional<ReportRecord> optionalReport = loadReport(connection, id);
                if (optionalReport.isEmpty()) {
                    connection.rollback();
                    return ReviewResult.failed("通報が見つかりません。");
                }
                ReportRecord report = optionalReport.get();
                if (!ReportStatus.canReview(report.status())) {
                    connection.rollback();
                    return ReviewResult.failed("この通報は既に処理済みです。");
                }

                updateReportReview(connection, id, "rejected", report.deduction(), moderatorUuid, moderatorName, note, now);
                FalseReportPenalty penalty = incrementFalseReportPenalty(connection, report.reporterUuid(), now);
                connection.commit();
                return ReviewResult.rejected(report, penalty.falseReportCount(), penalty.reportBannedUntil());
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        });
    }

    private static void updateReportReview(
            Connection connection,
            long id,
            String status,
            int deduction,
            UUID moderatorUuid,
            String moderatorName,
            String note,
            long now
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE reports
                SET status = ?, deduction = ?, reviewed_by = ?, reviewed_at = ?, review_note = ?
                WHERE id = ?
                """)) {
            statement.setString(1, status);
            statement.setInt(2, deduction);
            statement.setString(3, moderatorUuid.getMostSignificantBits() == 0L && moderatorUuid.getLeastSignificantBits() == 0L
                    ? "CONSOLE"
                    : moderatorUuid.toString());
            statement.setLong(4, now);
            statement.setString(5, normalizeNote(note));
            statement.setLong(6, id);
            statement.executeUpdate();
        }
    }

    private FalseReportPenalty incrementFalseReportPenalty(Connection connection, UUID reporterUuid, long now) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement("""
                UPDATE players
                SET false_report_count = false_report_count + 1
                WHERE uuid = ?
                """)) {
            update.setString(1, reporterUuid.toString());
            update.executeUpdate();
        }

        int falseReportCount;
        try (PreparedStatement select = connection.prepareStatement("""
                SELECT false_report_count
                FROM players
                WHERE uuid = ?
                """)) {
            select.setString(1, reporterUuid.toString());
            try (ResultSet result = select.executeQuery()) {
                falseReportCount = result.next() ? result.getInt("false_report_count") : 0;
            }
        }

        Long reportBannedUntil = ReporterPenalty.nextReportBannedUntil(
                config.reporterPenaltyEnabled(),
                falseReportCount,
                config.falseReportThreshold(),
                config.reportBanDays(),
                now
        );
        if (reportBannedUntil != null) {
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE players
                    SET report_banned_until = ?
                    WHERE uuid = ?
                    """)) {
                update.setLong(1, reportBannedUntil);
                update.setString(2, reporterUuid.toString());
                update.executeUpdate();
            }
        }
        return new FalseReportPenalty(falseReportCount, reportBannedUntil);
    }

    private static java.util.Optional<ReportRecord> loadReport(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, reporter_uuid, reporter_name, target_uuid, target_name, category, reason,
                       status, deduction, created_at, reviewed_by, reviewed_at, review_note
                FROM reports
                WHERE id = ?
                """)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return java.util.Optional.empty();
                }
                return java.util.Optional.of(new ReportRecord(
                        result.getLong("id"),
                        UUID.fromString(result.getString("reporter_uuid")),
                        result.getString("reporter_name"),
                        UUID.fromString(result.getString("target_uuid")),
                        result.getString("target_name"),
                        result.getString("category"),
                        result.getString("reason"),
                        result.getString("status"),
                        result.getInt("deduction"),
                        result.getLong("created_at"),
                        result.getString("reviewed_by"),
                        nullableLong(result, "reviewed_at"),
                        result.getString("review_note")
                ));
            }
        }
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

    private static Long nullableLong(ResultSet result, String column) throws SQLException {
        long value = result.getLong(column);
        return result.wasNull() ? null : value;
    }

    private static String normalizeNote(String note) {
        if (note == null || note.isBlank()) {
            return "";
        }
        return note.trim();
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

    private static int countUniqueThresholdPendingReporters(
            Connection connection,
            UUID targetUuid,
            String category,
            long createdAfter
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(DISTINCT reporter_uuid)
                FROM reports
                WHERE target_uuid = ?
                  AND category = ?
                  AND status = 'threshold_pending'
                  AND created_at >= ?
                """)) {
            statement.setString(1, targetUuid.toString());
            statement.setString(2, category);
            statement.setLong(3, createdAfter);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        }
    }

    private static int autoAcceptThresholdPendingReports(
            Connection connection,
            UUID targetUuid,
            String category,
            int deduction,
            long createdAfter,
            long now,
            int currentUniqueReports,
            int requiredReports
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE reports
                SET status = 'auto_accepted',
                    deduction = ?,
                    reviewed_at = ?,
                    review_note = ?
                WHERE target_uuid = ?
                  AND category = ?
                  AND status = 'threshold_pending'
                  AND created_at >= ?
                """)) {
            statement.setInt(1, deduction);
            statement.setLong(2, now);
            statement.setString(3, "threshold reached: " + currentUniqueReports + "/" + requiredReports);
            statement.setString(4, targetUuid.toString());
            statement.setString(5, category);
            statement.setLong(6, createdAfter);
            return statement.executeUpdate();
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
            boolean crossedBanThreshold,
            int thresholdRequired,
            int thresholdCurrent,
            boolean thresholdReached,
            int acceptedReportCount
    ) {
        static ReportResult rejected(String message) {
            return new ReportResult(false, message, -1L, "rejected", 0, false, null, null, false, 0, 0, false, 0);
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
                    crossedBanThreshold,
                    0,
                    0,
                    false,
                    0
            );
        }

        static ReportResult thresholdPending(long reportId, int thresholdRequired, int thresholdCurrent) {
            return new ReportResult(
                    true,
                    "通報を受け付けました。",
                    reportId,
                    "threshold_pending",
                    0,
                    false,
                    null,
                    null,
                    false,
                    thresholdRequired,
                    thresholdCurrent,
                    false,
                    0
            );
        }

        static ReportResult thresholdReached(
                long reportId,
                int deduction,
                int oldScore,
                int newScore,
                boolean crossedBanThreshold,
                int thresholdRequired,
                int thresholdCurrent,
                int acceptedReportCount
        ) {
            return new ReportResult(
                    true,
                    "通報を受け付けました。",
                    reportId,
                    "auto_accepted",
                    deduction,
                    false,
                    oldScore,
                    newScore,
                    crossedBanThreshold,
                    thresholdRequired,
                    thresholdCurrent,
                    true,
                    acceptedReportCount
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

    public record ReportRecord(
            long id,
            UUID reporterUuid,
            String reporterName,
            UUID targetUuid,
            String targetName,
            String category,
            String reason,
            String status,
            int deduction,
            long createdAt,
            String reviewedBy,
            Long reviewedAt,
            String reviewNote
    ) {
    }

    public record ReviewResult(
            boolean success,
            String message,
            ReportRecord report,
            int deduction,
            ScoreService.ScoreChange scoreChange,
            boolean rejected,
            int falseReportCount,
            Long reportBannedUntil
    ) {
        static ReviewResult failed(String message) {
            return new ReviewResult(false, message, null, 0, null, false, 0, null);
        }

        static ReviewResult approved(ReportRecord report, int deduction, ScoreService.ScoreChange scoreChange) {
            return new ReviewResult(true, "通報を承認しました。", report, deduction, scoreChange, false, 0, null);
        }

        static ReviewResult rejected(ReportRecord report, int falseReportCount, Long reportBannedUntil) {
            return new ReviewResult(true, "通報を却下しました。", report, 0, null, true, falseReportCount, reportBannedUntil);
        }
    }

    private record FalseReportPenalty(int falseReportCount, Long reportBannedUntil) {
    }
}
