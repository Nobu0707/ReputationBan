package dev.modplugin.reputationban.service;

import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.database.DatabaseManager;
import dev.modplugin.reputationban.model.AuditEvent;
import dev.modplugin.reputationban.model.AuditEventType;
import dev.modplugin.reputationban.model.CommandActor;
import dev.modplugin.reputationban.util.AuditMetadata;
import dev.modplugin.reputationban.util.CsvEscaper;
import dev.modplugin.reputationban.util.RetentionPolicy;
import dev.modplugin.reputationban.util.SafePathResolver;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public final class AuditService {
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneId.systemDefault());
    private static final String CSV_HEADER = "id,created_at,event_type,actor_uuid,actor_name,target_uuid,target_name,"
            + "report_id,ban_id,score_history_id,old_score,new_score,delta,reason,metadata";

    private final JavaPlugin plugin;
    private final DatabaseManager database;
    private volatile PluginConfig config;

    public AuditService(JavaPlugin plugin, DatabaseManager database, PluginConfig config) {
        this.plugin = plugin;
        this.database = database;
        this.config = config;
    }

    public void updateConfig(PluginConfig config) {
        this.config = config;
    }

    public CompletableFuture<Void> recordEvent(AuditEvent event) {
        return database.runAsync(connection -> recordEventInTransaction(connection, event))
                .exceptionally(throwable -> {
                    plugin.getLogger().log(Level.WARNING, "Failed to record audit event", throwable);
                    return null;
                });
    }

    public void recordEventInTransaction(Connection connection, AuditEvent event) throws SQLException {
        if (!config.auditEnabled() || event == null) {
            return;
        }
        insertEvent(connection, event);
    }

    public CompletableFuture<List<AuditEvent>> listRecent(int limit) {
        return database.supplyAsync(connection -> list(connection, """
                SELECT *
                FROM audit_events
                ORDER BY created_at DESC
                LIMIT ?
                """, limit));
    }

    public CompletableFuture<List<AuditEvent>> listForTarget(UUID targetUuid, int limit) {
        return database.supplyAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT *
                    FROM audit_events
                    WHERE target_uuid = ?
                    ORDER BY created_at DESC
                    LIMIT ?
                    """)) {
                statement.setString(1, targetUuid.toString());
                statement.setInt(2, limit);
                return readEvents(statement);
            }
        });
    }

    public CompletableFuture<List<AuditEvent>> listForActor(UUID actorUuid, int limit) {
        return database.supplyAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT *
                    FROM audit_events
                    WHERE actor_uuid = ?
                    ORDER BY created_at DESC
                    LIMIT ?
                    """)) {
                statement.setString(1, actorUuid.toString());
                statement.setInt(2, limit);
                return readEvents(statement);
            }
        });
    }

    public CompletableFuture<List<AuditEvent>> listByType(AuditEventType type, int limit) {
        return database.supplyAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT *
                    FROM audit_events
                    WHERE event_type = ?
                    ORDER BY created_at DESC
                    LIMIT ?
                    """)) {
                statement.setString(1, type.databaseValue());
                statement.setInt(2, limit);
                return readEvents(statement);
            }
        });
    }

    public CompletableFuture<Path> exportRecentCsv(int limit) {
        String fileName = "audit-recent-" + FILE_STAMP.format(Instant.now()) + ".csv";
        return database.supplyAsync(connection -> {
            try {
                Path output = exportPath(fileName);
                writeCsv(output, list(connection, """
                        SELECT *
                        FROM audit_events
                        ORDER BY created_at DESC
                        LIMIT ?
                        """, clampedExportLimit(limit)));
                return output;
            } catch (IOException exception) {
                throw new SQLException("Failed to export audit CSV", exception);
            }
        });
    }

    public CompletableFuture<Path> exportTargetCsv(UUID targetUuid, String targetName, int limit) {
        String safeName = targetName == null || targetName.isBlank() ? targetUuid.toString() : targetName.replaceAll("[^A-Za-z0-9_.-]", "_");
        String fileName = "audit-" + safeName + "-" + FILE_STAMP.format(Instant.now()) + ".csv";
        return database.supplyAsync(connection -> {
            try {
                Path output = exportPath(fileName);
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT *
                        FROM audit_events
                        WHERE target_uuid = ?
                        ORDER BY created_at DESC
                        LIMIT ?
                        """)) {
                    statement.setString(1, targetUuid.toString());
                    statement.setInt(2, clampedExportLimit(limit));
                    writeCsv(output, readEvents(statement));
                }
                return output;
            } catch (IOException exception) {
                throw new SQLException("Failed to export audit CSV", exception);
            }
        });
    }

    public CompletableFuture<MaintenanceResult> previewMaintenance(CommandActor actor) {
        long now = System.currentTimeMillis();
        return database.supplyAsync(connection -> {
            MaintenanceResult result = previewCounts(connection, now, null);
            auditServicePreview(connection, actor, result, now);
            return result;
        });
    }

    public CompletableFuture<MaintenanceResult> runMaintenance(CommandActor actor) {
        long now = System.currentTimeMillis();
        return database.supplyAsync(connection -> {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Path backupPath = backupDatabase(connection);
                int rejectedReports = cleanupReports(connection, "rejected", config.retentionRejectedReportsDays(), now);
                int cancelledReports = cleanupReports(connection, "cancelled", config.retentionCancelledReportsDays(), now);
                int scoreHistory = cleanupByCreatedAt(connection, "score_history", config.retentionScoreHistoryDays(), now);
                int bans = cleanupByCreatedAt(connection, "bans", config.retentionBansDays(), now);
                int auditEvents = cleanupByCreatedAt(connection, "audit_events", config.retentionAuditEventsDays(), now);
                MaintenanceResult result = new MaintenanceResult(
                        rejectedReports,
                        cancelledReports,
                        auditEvents,
                        scoreHistory,
                        bans,
                        "backups/" + backupPath.getFileName()
                );
                recordEventInTransaction(connection, AuditEvent.create(
                        AuditEventType.MAINTENANCE_RUN,
                        actor.uuid(),
                        actor.name(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "retention cleanup",
                        result.metadata(),
                        now
                ));
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

    private void auditServicePreview(Connection connection, CommandActor actor, MaintenanceResult result, long now) throws SQLException {
        recordEventInTransaction(connection, AuditEvent.create(
                AuditEventType.MAINTENANCE_PREVIEW,
                actor.uuid(),
                actor.name(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "retention cleanup preview",
                result.metadata(),
                now
        ));
    }

    private static void insertEvent(Connection connection, AuditEvent event) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO audit_events (
                  event_type, actor_uuid, actor_name, target_uuid, target_name, report_id, ban_id,
                  score_history_id, old_score, new_score, delta, reason, metadata, created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            insert.setString(1, event.eventType().databaseValue());
            setUuid(insert, 2, event.actorUuid());
            insert.setString(3, event.actorName());
            setUuid(insert, 4, event.targetUuid());
            insert.setString(5, event.targetName());
            setLong(insert, 6, event.reportId());
            setLong(insert, 7, event.banId());
            setLong(insert, 8, event.scoreHistoryId());
            setInteger(insert, 9, event.oldScore());
            setInteger(insert, 10, event.newScore());
            setInteger(insert, 11, event.delta());
            insert.setString(12, event.reason());
            insert.setString(13, event.metadata());
            insert.setLong(14, event.createdAt());
            insert.executeUpdate();
        }
    }

    private static List<AuditEvent> list(Connection connection, String sql, int limit) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            return readEvents(statement);
        }
    }

    private static List<AuditEvent> readEvents(PreparedStatement statement) throws SQLException {
        List<AuditEvent> events = new ArrayList<>();
        try (ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                events.add(readEvent(result));
            }
        }
        return events;
    }

    private static AuditEvent readEvent(ResultSet result) throws SQLException {
        String actorUuid = result.getString("actor_uuid");
        String targetUuid = result.getString("target_uuid");
        return new AuditEvent(
                result.getLong("id"),
                AuditEventType.parse(result.getString("event_type")),
                actorUuid == null ? null : UUID.fromString(actorUuid),
                result.getString("actor_name"),
                targetUuid == null ? null : UUID.fromString(targetUuid),
                result.getString("target_name"),
                nullableLong(result, "report_id"),
                nullableLong(result, "ban_id"),
                nullableLong(result, "score_history_id"),
                nullableInteger(result, "old_score"),
                nullableInteger(result, "new_score"),
                nullableInteger(result, "delta"),
                result.getString("reason"),
                result.getString("metadata"),
                result.getLong("created_at")
        );
    }

    private Path exportPath(String fileName) throws IOException {
        Path base = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
        Path directory = SafePathResolver.resolveInsideBase(
                base,
                config.auditExportDirectory(),
                SafePathResolver.DEFAULT_EXPORT_DIRECTORY
        );
        if (!SafePathResolver.staysInsideBase(base, config.auditExportDirectory())) {
            plugin.getLogger().warning("Invalid audit export directory configured; using safe default export directory.");
        }
        Files.createDirectories(directory);
        return directory.resolve(fileName).normalize();
    }

    private static void writeCsv(Path output, List<AuditEvent> events) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(CSV_HEADER);
        for (AuditEvent event : events) {
            lines.add(toCsv(event));
        }
        Files.write(output, lines, StandardCharsets.UTF_8);
    }

    private static String toCsv(AuditEvent event) {
        return String.join(",",
                Long.toString(event.id() == null ? 0L : event.id()),
                Long.toString(event.createdAt()),
                CsvEscaper.escape(event.eventType().databaseValue()),
                CsvEscaper.escape(event.actorUuid() == null ? "" : event.actorUuid().toString()),
                CsvEscaper.escape(event.actorName()),
                CsvEscaper.escape(event.targetUuid() == null ? "" : event.targetUuid().toString()),
                CsvEscaper.escape(event.targetName()),
                nullable(event.reportId()),
                nullable(event.banId()),
                nullable(event.scoreHistoryId()),
                nullable(event.oldScore()),
                nullable(event.newScore()),
                nullable(event.delta()),
                CsvEscaper.escape(event.reason()),
                CsvEscaper.escape(event.metadata())
        );
    }

    private int clampedExportLimit(int limit) {
        return Math.max(1, Math.min(limit, config.auditMaxExportLimit()));
    }

    private static int cleanupReports(Connection connection, String status, int retentionDays, long now) throws SQLException {
        Long cutoff = RetentionPolicy.cutoffMillis(retentionDays, now);
        if (cutoff == null) {
            return 0;
        }
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM reports WHERE status = ? AND created_at < ?")) {
            statement.setString(1, status);
            statement.setLong(2, cutoff);
            return statement.executeUpdate();
        }
    }

    private MaintenanceResult previewCounts(Connection connection, long now, String backupFileName) throws SQLException {
        return new MaintenanceResult(
                countReports(connection, "rejected", config.retentionRejectedReportsDays(), now),
                countReports(connection, "cancelled", config.retentionCancelledReportsDays(), now),
                countByCreatedAt(connection, "audit_events", config.retentionAuditEventsDays(), now),
                countByCreatedAt(connection, "score_history", config.retentionScoreHistoryDays(), now),
                countByCreatedAt(connection, "bans", config.retentionBansDays(), now),
                backupFileName
        );
    }

    private static int countReports(Connection connection, String status, int retentionDays, long now) throws SQLException {
        Long cutoff = RetentionPolicy.cutoffMillis(retentionDays, now);
        if (cutoff == null) {
            return 0;
        }
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM reports WHERE status = ? AND created_at < ?")) {
            statement.setString(1, status);
            statement.setLong(2, cutoff);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        }
    }

    private static int countByCreatedAt(Connection connection, String table, int retentionDays, long now) throws SQLException {
        Long cutoff = RetentionPolicy.cutoffMillis(retentionDays, now);
        if (cutoff == null) {
            return 0;
        }
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table + " WHERE created_at < " + cutoff)) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    private static int cleanupByCreatedAt(Connection connection, String table, int retentionDays, long now) throws SQLException {
        Long cutoff = RetentionPolicy.cutoffMillis(retentionDays, now);
        if (cutoff == null) {
            return 0;
        }
        try (Statement statement = connection.createStatement()) {
            return statement.executeUpdate("DELETE FROM " + table + " WHERE created_at < " + cutoff);
        }
    }

    private Path backupDatabase(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA wal_checkpoint(FULL)");
        }
        try {
            Path backups = plugin.getDataFolder().toPath().resolve("backups").toAbsolutePath().normalize();
            Files.createDirectories(backups);
            Path databasePath = database.databasePath();
            String stamp = FILE_STAMP.format(Instant.now());
            Path destination = backups.resolve("reputationban-before-maintenance-" + stamp + ".db").normalize();
            Files.copy(databasePath, destination, StandardCopyOption.REPLACE_EXISTING);
            copyIfExists(Path.of(databasePath.toString() + "-wal"), Path.of(destination.toString() + "-wal"));
            copyIfExists(Path.of(databasePath.toString() + "-shm"), Path.of(destination.toString() + "-shm"));
            return destination;
        } catch (IOException exception) {
            throw new SQLException("Failed to create maintenance backup", exception);
        }
    }

    private static void copyIfExists(Path source, Path destination) throws IOException {
        if (Files.exists(source)) {
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void setUuid(PreparedStatement statement, int index, UUID value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value.toString());
        }
    }

    private static void setLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setLong(index, value);
        }
    }

    private static void setInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private static Long nullableLong(ResultSet result, String column) throws SQLException {
        long value = result.getLong(column);
        return result.wasNull() ? null : value;
    }

    private static Integer nullableInteger(ResultSet result, String column) throws SQLException {
        int value = result.getInt(column);
        return result.wasNull() ? null : value;
    }

    private static String nullable(Number value) {
        return value == null ? "" : value.toString();
    }

    public record MaintenanceResult(
            int rejectedReportsDeleted,
            int cancelledReportsDeleted,
            int auditEventsDeleted,
            int scoreHistoryDeleted,
            int bansDeleted,
            String backupFileName
    ) {
        public String metadata() {
            AuditMetadata metadata = AuditMetadata.create()
                    .put("rejectedReports", rejectedReportsDeleted)
                    .put("cancelledReports", cancelledReportsDeleted)
                    .put("auditEvents", auditEventsDeleted)
                    .put("scoreHistory", scoreHistoryDeleted)
                    .put("bans", bansDeleted);
            if (backupFileName != null && !backupFileName.isBlank()) {
                metadata.put("backup", backupFileName);
            }
            return metadata.toJson();
        }
    }
}
