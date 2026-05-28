package dev.modplugin.reputationban.config;

import dev.modplugin.reputationban.config.ConfigValidationIssue.Severity;
import dev.modplugin.reputationban.util.SafePathResolver;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ConfigValidator {
    private ConfigValidator() {
    }

    public static List<ConfigValidationIssue> validate(PluginConfig config, Path dataFolder) {
        return validate(ConfigValidationInput.from(config), dataFolder);
    }

    public static List<ConfigValidationIssue> validate(ConfigValidationInput config, Path dataFolder) {
        List<ConfigValidationIssue> issues = new ArrayList<>();
        require(config.initialScore() > 0, issues, "initial-score", "initial-score must be greater than 0");
        require(config.maxScore() > 0, issues, "max-score", "max-score must be greater than 0");
        require(config.initialScore() <= config.maxScore(), issues, "initial-score", "initial-score must be less than or equal to max-score");
        require(config.minReasonLength() >= 0, issues, "rating.min-reason-length", "rating.min-reason-length must be 0 or greater");
        require(config.minUniqueReportsBeforeDeduction() >= 1, issues, "rating.min-unique-reports-before-deduction",
                "rating.min-unique-reports-before-deduction must be at least 1");
        require(config.reportWindowDays() >= 1, issues, "rating.report-window-days", "rating.report-window-days must be at least 1");
        require(config.globalReportSeconds() >= 0, issues, "cooldowns.global-report-seconds", "cooldowns.global-report-seconds must be 0 or greater");
        require(config.sameTargetCooldownDays() >= 0, issues, "cooldowns.same-target-cooldown-days", "cooldowns.same-target-cooldown-days must be 0 or greater");
        require(config.maxReportsPerDay() >= 1, issues, "cooldowns.max-reports-per-day", "cooldowns.max-reports-per-day must be at least 1");
        require(config.maxReportsPerWeek() >= 1, issues, "cooldowns.max-reports-per-week", "cooldowns.max-reports-per-week must be at least 1");
        require(config.minPlaytimeMinutes() >= 0, issues, "report-requirements.min-playtime-minutes",
                "report-requirements.min-playtime-minutes must be 0 or greater");
        require(config.minAccountAgeDays() >= 0, issues, "report-requirements.min-account-age-days",
                "report-requirements.min-account-age-days must be 0 or greater");
        require(config.banThreshold() <= config.maxScore(), issues, "ban.threshold", "ban.threshold must be less than or equal to max-score");
        require(config.recoveryPointsPerDay() >= 0, issues, "score-recovery.points-per-day", "score-recovery.points-per-day must be 0 or greater");
        require(config.recoveryMaxScore() > 0, issues, "score-recovery.max-score", "score-recovery.max-score must be greater than 0");
        require(config.recoveryNoReportDaysRequired() >= 0, issues, "score-recovery.no-report-days-required",
                "score-recovery.no-report-days-required must be 0 or greater");
        require(config.auditMaxDisplayLimit() >= 1, issues, "audit.max-display-limit", "audit.max-display-limit must be at least 1");
        require(config.auditMaxExportLimit() >= 1, issues, "audit.max-export-limit", "audit.max-export-limit must be at least 1");
        retention(config.retentionAuditEventsDays(), issues, "retention.audit-events-days");
        retention(config.retentionRejectedReportsDays(), issues, "retention.rejected-reports-days");
        retention(config.retentionCancelledReportsDays(), issues, "retention.cancelled-reports-days");
        retention(config.retentionScoreHistoryDays(), issues, "retention.score-history-days");
        retention(config.retentionBansDays(), issues, "retention.bans-days");
        require(config.maxReportReasonLength() >= config.minReasonLength(), issues, "limits.max-report-reason-length",
                "limits.max-report-reason-length must be greater than or equal to rating.min-reason-length");
        require(config.maxReviewNoteLength() >= 0, issues, "limits.max-review-note-length",
                "limits.max-review-note-length must be 0 or greater");
        require(config.maxAuditReasonLength() >= 0, issues, "limits.max-audit-reason-length",
                "limits.max-audit-reason-length must be 0 or greater");
        require(config.maxContextSummaryLength() >= 0, issues, "limits.max-context-summary-length",
                "limits.max-context-summary-length must be 0 or greater");
        require(config.luckPermsDefaultWeight() > 0.0D, issues, "integrations.luckperms.default-weight",
                "integrations.luckperms.default-weight must be greater than 0");
        require(config.luckPermsOfflineLookupTimeoutMillis() >= 1, issues, "integrations.luckperms.offline-lookup.timeout-millis",
                "integrations.luckperms.offline-lookup.timeout-millis must be at least 1");
        for (java.util.Map.Entry<String, Double> entry : config.luckPermsGroupWeights().entrySet()) {
            require(entry.getValue() != null && entry.getValue() > 0.0D, issues,
                    "integrations.luckperms.group-weights." + entry.getKey(),
                    "integrations.luckperms.group-weights values must be greater than 0");
        }
        require(config.coreProtectMinimumApiVersion() >= 1, issues, "integrations.coreprotect.minimum-api-version",
                "integrations.coreprotect.minimum-api-version must be at least 1");
        require(config.coreProtectLookupSeconds() >= 1, issues, "integrations.coreprotect.report-context.lookup-seconds",
                "integrations.coreprotect.report-context.lookup-seconds must be at least 1");
        require(config.coreProtectRadius() >= 0, issues, "integrations.coreprotect.report-context.radius",
                "integrations.coreprotect.report-context.radius must be 0 or greater");
        require(config.coreProtectMaxResults() >= 0, issues, "integrations.coreprotect.report-context.max-results",
                "integrations.coreprotect.report-context.max-results must be 0 or greater");
        require(config.worldGuardMaxRegions() >= 0, issues, "integrations.worldguard.report-context.max-regions",
                "integrations.worldguard.report-context.max-regions must be 0 or greater");
        if (config.worldGuardReportContextCategories().isEmpty()) {
            issues.add(new ConfigValidationIssue(Severity.WARNING, "integrations.worldguard.report-context.categories",
                    "integrations.worldguard.report-context.categories is empty; WorldGuard report context will not be captured"));
        }
        if (config.griefPreventionReportContextCategories().isEmpty()) {
            issues.add(new ConfigValidationIssue(Severity.WARNING, "integrations.griefprevention.report-context.categories",
                    "integrations.griefprevention.report-context.categories is empty; GriefPrevention report context will not be captured"));
        }
        if (config.discordSrvAccountLinkContextCategories().isEmpty()) {
            issues.add(new ConfigValidationIssue(Severity.WARNING, "integrations.discordsrv.account-link-context.categories",
                    "integrations.discordsrv.account-link-context.categories is empty; DiscordSRV account link context will not be captured"));
        }
        require(config.placeholderApiCacheRefreshSeconds() >= 0, issues, "integrations.placeholderapi.cache-refresh-seconds",
                "integrations.placeholderapi.cache-refresh-seconds must be 0 or greater");
        if (config.placeholderApiIdentifier() == null || config.placeholderApiIdentifier().isBlank()) {
            issues.add(new ConfigValidationIssue(Severity.WARNING, "integrations.placeholderapi.identifier",
                    "integrations.placeholderapi.identifier is empty; reputationban will be used"));
        } else if (!config.placeholderApiIdentifier().matches("[a-z0-9_]+")) {
            issues.add(new ConfigValidationIssue(Severity.WARNING, "integrations.placeholderapi.identifier",
                    "integrations.placeholderapi.identifier should match [a-z0-9_]+; reputationban will be used"));
        }
        if (config.discordWebhookTimeoutSeconds() < 1 || config.discordWebhookTimeoutSeconds() > 30) {
            issues.add(new ConfigValidationIssue(Severity.WARNING, "notify.discord-webhook.timeout-seconds",
                    "notify.discord-webhook.timeout-seconds should be between 1 and 30"));
        }
        if (!SafePathResolver.staysInsideBase(dataFolder, config.auditExportDirectory())) {
            issues.add(new ConfigValidationIssue(Severity.ERROR, "audit.export-directory",
                    "audit.export-directory must stay inside the plugin data folder"));
        }
        return List.copyOf(issues);
    }

    private static void require(boolean valid, List<ConfigValidationIssue> issues, String path, String message) {
        if (!valid) {
            issues.add(new ConfigValidationIssue(Severity.ERROR, path, message));
        }
    }

    private static void retention(int value, List<ConfigValidationIssue> issues, String path) {
        require(value >= 0, issues, path, path + " must be 0 or greater");
    }
}
