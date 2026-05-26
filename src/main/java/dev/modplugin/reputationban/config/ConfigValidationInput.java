package dev.modplugin.reputationban.config;

public record ConfigValidationInput(
        int initialScore,
        int maxScore,
        int minReasonLength,
        int minUniqueReportsBeforeDeduction,
        int reportWindowDays,
        int globalReportSeconds,
        int sameTargetCooldownDays,
        int maxReportsPerDay,
        int maxReportsPerWeek,
        int minPlaytimeMinutes,
        int minAccountAgeDays,
        int banThreshold,
        int recoveryPointsPerDay,
        int recoveryMaxScore,
        int recoveryNoReportDaysRequired,
        int auditMaxDisplayLimit,
        int auditMaxExportLimit,
        int retentionAuditEventsDays,
        int retentionRejectedReportsDays,
        int retentionCancelledReportsDays,
        int retentionScoreHistoryDays,
        int retentionBansDays,
        int discordWebhookTimeoutSeconds,
        String auditExportDirectory
) {
    public static ConfigValidationInput from(PluginConfig config) {
        return new ConfigValidationInput(
                config.initialScore(),
                config.maxScore(),
                config.minReasonLength(),
                config.minUniqueReportsBeforeDeduction(),
                config.reportWindowDays(),
                config.globalReportSeconds(),
                config.sameTargetCooldownDays(),
                config.maxReportsPerDay(),
                config.maxReportsPerWeek(),
                config.minPlaytimeMinutes(),
                config.minAccountAgeDays(),
                config.banThreshold(),
                config.recoveryPointsPerDay(),
                config.recoveryMaxScore(),
                config.recoveryNoReportDaysRequired(),
                config.auditMaxDisplayLimit(),
                config.auditMaxExportLimit(),
                config.retentionAuditEventsDays(),
                config.retentionRejectedReportsDays(),
                config.retentionCancelledReportsDays(),
                config.retentionScoreHistoryDays(),
                config.retentionBansDays(),
                config.discordWebhookConfig().timeoutSeconds(),
                config.auditExportDirectory()
        );
    }
}
