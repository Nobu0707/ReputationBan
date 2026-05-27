package dev.modplugin.reputationban.model;

import java.util.List;

public record DiagnosticReport(
        String version,
        String server,
        String javaVersion,
        DiagnosticStatus databaseStatus,
        DiagnosticStatus tablesStatus,
        DiagnosticStatus configStatus,
        DiagnosticStatus auditExportDirectoryStatus,
        int configWarnings,
        int configErrors,
        boolean discordEnabled,
        boolean discordUrlConfigured,
        boolean auditExportDirectorySafe,
        String retentionSummary,
        int pendingReports,
        int thresholdPendingReports,
        int activeDbBans,
        List<String> missingTables
) {
    public DiagnosticStatus overallStatus() {
        return DiagnosticStatus.aggregate(
                databaseStatus,
                tablesStatus,
                configStatus,
                auditExportDirectoryStatus
        );
    }

    public boolean databaseOk() {
        return databaseStatus == DiagnosticStatus.OK;
    }

    public boolean tablesOk() {
        return tablesStatus == DiagnosticStatus.OK;
    }
}
