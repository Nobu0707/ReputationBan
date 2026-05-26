package dev.modplugin.reputationban.database;

import dev.modplugin.reputationban.config.PluginConfig;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public final class DatabaseManager implements AutoCloseable {
    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final ExecutorService executor;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "ReputationBan-DB");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void initialize() throws SQLException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new SQLException("Could not create plugin data folder: " + dataFolder);
        }

        File databaseFile = new File(dataFolder, config.databaseFile());
        connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA foreign_keys=ON");
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "SQLite PRAGMA setup failed; continuing with default settings.", exception);
        }
        createTables();
    }

    private void createTables() throws SQLException {
        execute("""
                CREATE TABLE IF NOT EXISTS players (
                  uuid TEXT PRIMARY KEY,
                  name TEXT NOT NULL,
                  score INTEGER NOT NULL,
                  ban_count INTEGER NOT NULL DEFAULT 0,
                  false_report_count INTEGER NOT NULL DEFAULT 0,
                  report_banned_until INTEGER,
                  last_recovery_at INTEGER,
                  first_seen INTEGER,
                  last_seen INTEGER
                )
                """);
        execute("""
                CREATE TABLE IF NOT EXISTS reports (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  reporter_uuid TEXT NOT NULL,
                  reporter_name TEXT NOT NULL,
                  target_uuid TEXT NOT NULL,
                  target_name TEXT NOT NULL,
                  category TEXT NOT NULL,
                  reason TEXT NOT NULL,
                  status TEXT NOT NULL,
                  deduction INTEGER NOT NULL DEFAULT 0,
                  created_at INTEGER NOT NULL,
                  reviewed_by TEXT,
                  reviewed_at INTEGER,
                  review_note TEXT
                )
                """);
        execute("""
                CREATE TABLE IF NOT EXISTS score_history (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  target_uuid TEXT NOT NULL,
                  target_name TEXT NOT NULL,
                  old_score INTEGER NOT NULL,
                  new_score INTEGER NOT NULL,
                  delta INTEGER NOT NULL,
                  reason TEXT NOT NULL,
                  source_type TEXT NOT NULL,
                  source_id INTEGER,
                  created_at INTEGER NOT NULL
                )
                """);
        execute("""
                CREATE TABLE IF NOT EXISTS bans (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  target_uuid TEXT NOT NULL,
                  target_name TEXT NOT NULL,
                  reason TEXT NOT NULL,
                  ban_type TEXT NOT NULL,
                  created_at INTEGER NOT NULL,
                  expires_at INTEGER,
                  created_by TEXT,
                  unbanned_at INTEGER,
                  unbanned_by TEXT,
                  unban_reason TEXT
                )
                """);
        execute("""
                CREATE TABLE IF NOT EXISTS audit_events (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  event_type TEXT NOT NULL,
                  actor_uuid TEXT,
                  actor_name TEXT,
                  target_uuid TEXT,
                  target_name TEXT,
                  report_id INTEGER,
                  ban_id INTEGER,
                  score_history_id INTEGER,
                  old_score INTEGER,
                  new_score INTEGER,
                  delta INTEGER,
                  reason TEXT,
                  metadata TEXT,
                  created_at INTEGER NOT NULL
                )
                """);
        execute("CREATE INDEX IF NOT EXISTS idx_reports_reporter_target_created ON reports(reporter_uuid, target_uuid, created_at)");
        execute("CREATE INDEX IF NOT EXISTS idx_reports_target_created ON reports(target_uuid, created_at)");
        execute("CREATE INDEX IF NOT EXISTS idx_score_history_target_created ON score_history(target_uuid, created_at)");
        execute("CREATE INDEX IF NOT EXISTS idx_audit_events_created ON audit_events(created_at)");
        execute("CREATE INDEX IF NOT EXISTS idx_audit_events_target_created ON audit_events(target_uuid, created_at)");
        execute("CREATE INDEX IF NOT EXISTS idx_audit_events_actor_created ON audit_events(actor_uuid, created_at)");
        execute("CREATE INDEX IF NOT EXISTS idx_audit_events_type_created ON audit_events(event_type, created_at)");
        migratePlayersTable();
        migrateBansTable();
    }

    private void migratePlayersTable() throws SQLException {
        Set<String> columns = tableColumns("players");
        if (!columns.contains("last_recovery_at")) {
            execute("ALTER TABLE players ADD COLUMN last_recovery_at INTEGER");
        }
        if (!columns.contains("first_seen")) {
            execute("ALTER TABLE players ADD COLUMN first_seen INTEGER");
        }
        if (!columns.contains("last_seen")) {
            execute("ALTER TABLE players ADD COLUMN last_seen INTEGER");
        }
    }

    private void migrateBansTable() throws SQLException {
        Set<String> columns = tableColumns("bans");
        if (!columns.contains("unban_reason")) {
            execute("ALTER TABLE bans ADD COLUMN unban_reason TEXT");
        }
    }

    private Set<String> tableColumns(String tableName) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (result.next()) {
                columns.add(result.getString("name"));
            }
        }
        return columns;
    }

    private void execute(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    public <T> CompletableFuture<T> supplyAsync(SqlFunction<Connection, T> operation) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return operation.apply(connection);
            } catch (SQLException exception) {
                throw new DatabaseException(exception);
            }
        }, executor).whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                plugin.getLogger().log(Level.SEVERE, "Database operation failed", throwable);
            }
        });
    }

    public CompletableFuture<Void> runAsync(SqlConsumer<Connection> operation) {
        return supplyAsync(connection -> {
            operation.accept(connection);
            return null;
        });
    }

    public <T> CompletableFuture<T> thenInDatabase(CompletableFuture<T> future, Function<Throwable, T> fallback) {
        return future.exceptionally(fallback);
    }

    @Override
    public void close() {
        executor.shutdown();
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException exception) {
                plugin.getLogger().log(Level.WARNING, "Failed to close database connection", exception);
            }
        }
    }

    @FunctionalInterface
    public interface SqlFunction<T, R> {
        R apply(T value) throws SQLException;
    }

    @FunctionalInterface
    public interface SqlConsumer<T> {
        void accept(T value) throws SQLException;
    }

    public static final class DatabaseException extends RuntimeException {
        public DatabaseException(Throwable cause) {
            super(cause);
        }
    }
}
