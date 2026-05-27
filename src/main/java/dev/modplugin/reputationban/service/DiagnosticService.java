package dev.modplugin.reputationban.service;

import dev.modplugin.reputationban.config.ConfigValidationIssue;
import dev.modplugin.reputationban.config.ConfigValidator;
import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.database.DatabaseManager;
import dev.modplugin.reputationban.model.AuditEvent;
import dev.modplugin.reputationban.model.AuditEventType;
import dev.modplugin.reputationban.model.CommandActor;
import dev.modplugin.reputationban.model.DiagnosticReport;
import dev.modplugin.reputationban.model.DiagnosticStatus;
import dev.modplugin.reputationban.util.AuditMetadata;
import dev.modplugin.reputationban.util.SafePathResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public final class DiagnosticService {
    private static final List<String> REQUIRED_TABLES = List.of(
            "players",
            "reports",
            "score_history",
            "bans",
            "audit_events",
            "report_context"
    );

    private final JavaPlugin plugin;
    private final DatabaseManager database;
    private final AuditService auditService;
    private final Supplier<PluginConfig> configSupplier;

    public DiagnosticService(
            JavaPlugin plugin,
            DatabaseManager database,
            AuditService auditService,
            Supplier<PluginConfig> configSupplier
    ) {
        this.plugin = plugin;
        this.database = database;
        this.auditService = auditService;
        this.configSupplier = configSupplier;
    }

    public CompletableFuture<DiagnosticReport> run(
            CommandActor actor,
            String version,
            String server,
            String javaVersion
    ) {
        PluginConfig config = configSupplier.get();
        List<ConfigValidationIssue> issues = ConfigValidator.validate(config, plugin.getDataFolder().toPath());
        int errors = (int) issues.stream()
                .filter(issue -> issue.severity() == ConfigValidationIssue.Severity.ERROR)
                .count();
        int warnings = issues.size() - errors;
        boolean auditExportSafe = SafePathResolver.staysInsideBase(
                plugin.getDataFolder().toPath().toAbsolutePath().normalize(),
                config.auditExportDirectory()
        );
        String pluginDataFolder = plugin.getDataFolder().toPath().toAbsolutePath().normalize().toString();
        boolean databaseFileExists = Files.exists(database.databasePath());
        boolean backupDirectoryWritable = backupDirectoryWritable(plugin.getDataFolder().toPath());

        return database.supplyAsync(connection -> {
            DiagnosticReport report = databaseReport(
                    connection,
                    config,
                    version,
                    server,
                    javaVersion,
                    pluginDataFolder,
                    warnings,
                    errors,
                    auditExportSafe,
                    databaseFileExists,
                    backupDirectoryWritable
            );
            recordDiagnosticsRun(connection, actor, report);
            return report;
        });
    }

    private DiagnosticReport databaseReport(
            Connection connection,
            PluginConfig config,
            String version,
            String server,
            String javaVersion,
            String pluginDataFolder,
            int warnings,
            int errors,
            boolean auditExportSafe,
            boolean databaseFileExists,
            boolean backupDirectoryWritable
    ) {
        boolean databaseOk = false;
        Set<String> tables = Set.of();
        List<String> missingTables = new ArrayList<>(REQUIRED_TABLES);
        int pendingReports = 0;
        int thresholdPendingReports = 0;
        int activeDbBans = 0;

        try {
            databaseOk = selectOne(connection);
            tables = tableNames(connection);
            Set<String> existingTables = tables;
            missingTables = REQUIRED_TABLES.stream()
                    .filter(table -> !existingTables.contains(table))
                    .toList();
            if (missingTables.isEmpty()) {
                pendingReports = countReportsByStatus(connection, "pending");
                thresholdPendingReports = countReportsByStatus(connection, "threshold_pending");
                activeDbBans = countActiveBans(connection, System.currentTimeMillis());
            }
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "Diagnostic database check failed", exception);
            databaseOk = false;
            missingTables = new ArrayList<>(REQUIRED_TABLES);
        }

        DiagnosticStatus databaseStatus = databaseOk ? DiagnosticStatus.OK : DiagnosticStatus.ERROR;
        DiagnosticStatus tableStatus = databaseOk && missingTables.isEmpty() ? DiagnosticStatus.OK : DiagnosticStatus.ERROR;
        DiagnosticStatus configStatus = errors > 0
                ? DiagnosticStatus.ERROR
                : warnings > 0 ? DiagnosticStatus.WARN : DiagnosticStatus.OK;
        DiagnosticStatus auditExportStatus = auditExportSafe ? DiagnosticStatus.OK : DiagnosticStatus.ERROR;

        return new DiagnosticReport(
                version,
                server,
                javaVersion,
                pluginDataFolder,
                databaseStatus,
                tableStatus,
                configStatus,
                auditExportStatus,
                warnings,
                errors,
                databaseFileExists,
                config.discordWebhookConfig().enabled(),
                config.discordWebhookConfig().hasUsableUrl(),
                auditExportSafe,
                backupDirectoryWritable,
                retentionSummary(config),
                pendingReports,
                thresholdPendingReports,
                activeDbBans,
                missingTables
        );
    }

    private void recordDiagnosticsRun(Connection connection, CommandActor actor, DiagnosticReport report) {
        try {
            auditService.recordEventInTransaction(connection, AuditEvent.create(
                    AuditEventType.DIAGNOSTICS_RUN,
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
                    "diagnostics run",
                    AuditMetadata.create()
                            .put("warnings", report.configWarnings())
                            .put("errors", report.configErrors())
                            .put("databaseOk", report.databaseOk())
                            .put("tablesOk", report.tablesOk())
                            .put("databaseFileExists", report.databaseFileExists())
                            .put("backupDirectoryWritable", report.backupDirectoryWritable())
                            .put("discordEnabled", report.discordEnabled())
                            .put("discordUrlConfigured", report.discordUrlConfigured())
                            .toJson(),
                    System.currentTimeMillis()
            ));
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to record diagnostics audit event", exception);
        }
    }

    private static boolean selectOne(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT 1")) {
            return result.next() && result.getInt(1) == 1;
        }
    }

    private static Set<String> tableNames(Connection connection) throws SQLException {
        Set<String> names = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT name
                FROM sqlite_master
                WHERE type = 'table'
                """);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                names.add(result.getString("name"));
            }
        }
        return names;
    }

    private static int countReportsByStatus(Connection connection, String status) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM reports WHERE status = ?")) {
            statement.setString(1, status);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        }
    }

    private static int countActiveBans(Connection connection, long now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM bans
                WHERE unbanned_at IS NULL
                  AND (expires_at IS NULL OR expires_at > ?)
                """)) {
            statement.setLong(1, now);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        }
    }

    private static String retentionSummary(PluginConfig config) {
        return "audit=%d rejected=%d cancelled=%d score=%d bans=%d".formatted(
                config.retentionAuditEventsDays(),
                config.retentionRejectedReportsDays(),
                config.retentionCancelledReportsDays(),
                config.retentionScoreHistoryDays(),
                config.retentionBansDays()
        );
    }

    private static boolean backupDirectoryWritable(Path dataFolder) {
        try {
            Path backups = dataFolder.resolve("backups").toAbsolutePath().normalize();
            Files.createDirectories(backups);
            return Files.isDirectory(backups) && Files.isWritable(backups);
        } catch (IOException exception) {
            return false;
        }
    }
}
