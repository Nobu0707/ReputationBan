package dev.modplugin.reputationban.service;

import dev.modplugin.reputationban.config.ConfigValidationIssue;
import dev.modplugin.reputationban.config.ConfigValidator;
import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.database.DatabaseManager;
import dev.modplugin.reputationban.model.AuditEvent;
import dev.modplugin.reputationban.model.AuditEventType;
import dev.modplugin.reputationban.model.CommandActor;
import dev.modplugin.reputationban.model.DiagnosticStatus;
import dev.modplugin.reputationban.model.SupportBundleResult;
import dev.modplugin.reputationban.util.AuditMetadata;
import dev.modplugin.reputationban.util.ConfigRedactor;
import dev.modplugin.reputationban.util.PathRedactor;
import dev.modplugin.reputationban.util.SafePathResolver;
import dev.modplugin.reputationban.util.SupportBundleSafetyChecker;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.bukkit.plugin.java.JavaPlugin;

public final class SupportBundleService {
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneId.systemDefault());
    private static final List<String> REQUIRED_TABLES = List.of(
            "players",
            "reports",
            "score_history",
            "bans",
            "audit_events"
    );
    static final String SHARING_README = """
            このバンドルにはDBとサーバーログは含まれていません。
            共有不要な絶対パスや個人パスは最小化しています。
            共有前にconfig-redacted.ymlを確認してください。
            Webhook URLやトークンなどの秘密情報が含まれていないか確認してください。
            """;

    private final JavaPlugin plugin;
    private final DatabaseManager database;
    private final AuditService auditService;
    private final Supplier<PluginConfig> configSupplier;

    public SupportBundleService(
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

    public CompletableFuture<SupportBundleResult> createBundle(
            CommandActor actor,
            String version,
            String server,
            String javaVersion
    ) {
        PluginConfig config = configSupplier.get();
        Path dataFolder = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
        Path configPath = dataFolder.resolve("config.yml").normalize();
        String pluginYml = readPluginResource("plugin.yml");
        String changelogExcerpt = "";
        String stamp = FILE_STAMP.format(Instant.now());
        String fileName = "reputationban-support-" + stamp + ".zip";

        return database.supplyAsync(connection -> {
            try {
                Path supportDirectory = SafePathResolver.resolveInsideBase(dataFolder, "support", "support");
                Files.createDirectories(supportDirectory);
                Path bundle = supportDirectory.resolve(fileName).normalize();
                if (!bundle.startsWith(supportDirectory)) {
                    throw new IOException("Support bundle path escaped support directory");
                }
                BundleSnapshot snapshot = snapshot(connection, config, dataFolder, configPath, version, server, javaVersion);
                writeBundle(bundle, snapshot.toPayload(pluginYml, changelogExcerpt));
                auditService.recordEventInTransaction(connection, AuditEvent.create(
                        AuditEventType.SUPPORT_BUNDLE_CREATED,
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
                        "support bundle created",
                        AuditMetadata.create().put("file", fileName).toJson(),
                        System.currentTimeMillis()
                ));
                return new SupportBundleResult(fileName, "support/" + fileName);
            } catch (IOException exception) {
                throw new SQLException("Failed to create support bundle", exception);
            }
        });
    }

    static void writeBundle(Path zipPath, SupportBundlePayload payload) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipPath), StandardCharsets.UTF_8)) {
            addText(zip, "meta.txt", payload.meta());
            addText(zip, "doctor.txt", payload.doctor());
            addText(zip, "counts.txt", payload.counts());
            addText(zip, "config-redacted.yml", payload.configRedacted());
            addText(zip, "README-SHARING.txt", payload.readmeSharing());
            if (payload.pluginYml() != null && !payload.pluginYml().isBlank()) {
                addText(zip, "plugin.yml", payload.pluginYml());
            }
            if (payload.changelogExcerpt() != null && !payload.changelogExcerpt().isBlank()) {
                addText(zip, "changelog-excerpt.txt", payload.changelogExcerpt());
            }
        }
    }

    private BundleSnapshot snapshot(
            Connection connection,
            PluginConfig config,
            Path dataFolder,
            Path configPath,
            String version,
            String server,
            String javaVersion
    ) throws SQLException, IOException {
        Map<String, Integer> counts = new LinkedHashMap<>();
        boolean databaseOk = selectOne(connection);
        Set<String> tables = tableNames(connection);
        List<String> missingTables = REQUIRED_TABLES.stream()
                .filter(table -> !tables.contains(table))
                .toList();
        for (String table : REQUIRED_TABLES) {
            counts.put(table, tables.contains(table) ? countRows(connection, table) : 0);
        }
        int pendingReports = tables.contains("reports") ? countReportsByStatus(connection, "pending") : 0;
        int thresholdPendingReports = tables.contains("reports") ? countReportsByStatus(connection, "threshold_pending") : 0;
        int activeDbBans = tables.contains("bans") ? countActiveBans(connection, System.currentTimeMillis()) : 0;
        List<ConfigValidationIssue> issues = ConfigValidator.validate(config, dataFolder);
        int errors = (int) issues.stream()
                .filter(issue -> issue.severity() == ConfigValidationIssue.Severity.ERROR)
                .count();
        int warnings = issues.size() - errors;
        boolean auditExportSafe = SafePathResolver.staysInsideBase(dataFolder, config.auditExportDirectory());
        boolean backupDirectoryWritable = backupDirectoryWritable(dataFolder);
        String configRedacted = Files.exists(configPath)
                ? ConfigRedactor.redactYaml(Files.readString(configPath, StandardCharsets.UTF_8))
                : "# config.yml was not found\n";

        return new BundleSnapshot(
                version,
                server,
                javaVersion,
                PathRedactor.pluginDataFolderForSharing(),
                databaseOk,
                missingTables.isEmpty(),
                warnings,
                errors,
                config.discordWebhookConfig().enabled(),
                config.discordWebhookConfig().hasUsableUrl(),
                auditExportSafe,
                backupDirectoryWritable,
                pendingReports,
                thresholdPendingReports,
                activeDbBans,
                missingTables,
                counts,
                configRedacted
        );
    }

    private String readPluginResource(String name) {
        try (InputStream input = plugin.getResource(name)) {
            if (input == null) {
                return "";
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.FINE, "Failed to read plugin resource " + name, exception);
            return "";
        }
    }

    private static void addText(ZipOutputStream zip, String name, String content) throws IOException {
        if (SupportBundleSafetyChecker.isForbiddenEntryName(name)) {
            throw new IOException("Unsafe support bundle entry: " + name);
        }
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static boolean selectOne(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT 1")) {
            return result.next() && result.getInt(1) == 1;
        }
    }

    private static Set<String> tableNames(Connection connection) throws SQLException {
        java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>();
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

    private static int countRows(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return result.next() ? result.getInt(1) : 0;
        }
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

    private static boolean backupDirectoryWritable(Path dataFolder) {
        try {
            Path backups = dataFolder.resolve("backups").toAbsolutePath().normalize();
            Files.createDirectories(backups);
            return Files.isDirectory(backups) && Files.isWritable(backups);
        } catch (IOException exception) {
            return false;
        }
    }

    record SupportBundlePayload(
            String meta,
            String doctor,
            String counts,
            String configRedacted,
            String readmeSharing,
            String pluginYml,
            String changelogExcerpt
    ) {
    }

    private record BundleSnapshot(
            String version,
            String server,
            String javaVersion,
            String pluginDataFolder,
            boolean databaseOk,
            boolean tablesOk,
            int configWarnings,
            int configErrors,
            boolean discordEnabled,
            boolean discordUrlConfigured,
            boolean auditExportDirectorySafe,
            boolean backupDirectoryWritable,
            int pendingReports,
            int thresholdPendingReports,
            int activeDbBans,
            List<String> missingTables,
            Map<String, Integer> counts,
            String configRedacted
    ) {
        SupportBundlePayload toPayload(String pluginYml, String changelogExcerpt) {
            return new SupportBundlePayload(meta(), doctor(), countsText(), configRedacted, SHARING_README, pluginYml, changelogExcerpt);
        }

        private String meta() {
            return """
                    ReputationBan Support Bundle
                    generatedAt=%s
                    version=%s
                    server=%s
                    java=%s
                    pluginDataFolder=%s
                    """.formatted(Instant.now(), version, server, javaVersion, pluginDataFolder);
        }

        private String doctor() {
            DiagnosticStatus databaseStatus = databaseOk ? DiagnosticStatus.OK : DiagnosticStatus.ERROR;
            DiagnosticStatus tablesStatus = tablesOk ? DiagnosticStatus.OK : DiagnosticStatus.ERROR;
            DiagnosticStatus configStatus = configErrors > 0
                    ? DiagnosticStatus.ERROR
                    : configWarnings > 0 ? DiagnosticStatus.WARN : DiagnosticStatus.OK;
            return """
                    version=%s
                    server=%s
                    java=%s
                    pluginDataFolder=%s
                    database status=%s
                    tables status=%s
                    config status=%s
                    config warnings=%d
                    config errors=%d
                    discord enabled=%s
                    discord URL configured=%s
                    audit export directory safe=%s
                    backup directory writable=%s
                    pending reports count=%d
                    threshold_pending reports count=%d
                    active DB bans count=%d
                    missing tables=%s
                    """.formatted(
                    version,
                    server,
                    javaVersion,
                    pluginDataFolder,
                    databaseStatus,
                    tablesStatus,
                    configStatus,
                    configWarnings,
                    configErrors,
                    discordEnabled,
                    discordUrlConfigured,
                    auditExportDirectorySafe,
                    backupDirectoryWritable,
                    pendingReports,
                    thresholdPendingReports,
                    activeDbBans,
                    missingTables.isEmpty() ? "-" : String.join(",", missingTables)
            );
        }

        private String countsText() {
            StringBuilder text = new StringBuilder();
            counts.forEach((table, count) -> text.append(table).append('=').append(count).append('\n'));
            return text.toString();
        }
    }
}
