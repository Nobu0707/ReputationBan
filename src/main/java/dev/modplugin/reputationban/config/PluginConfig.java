package dev.modplugin.reputationban.config;

import dev.modplugin.reputationban.model.ReportCategory;
import dev.modplugin.reputationban.notification.DiscordWebhookConfig;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public final class PluginConfig {
    private final int initialScore;
    private final int maxScore;
    private final boolean ratingEnabled;
    private final int defaultDeduction;
    private final int minReasonLength;
    private final int minUniqueReportsBeforeDeduction;
    private final int reportWindowDays;
    private final int minPlaytimeMinutes;
    private final int minAccountAgeDays;
    private final Map<String, Integer> scoreThresholds;
    private final int globalReportSeconds;
    private final int sameTargetCooldownDays;
    private final int maxReportsPerDay;
    private final int maxReportsPerWeek;
    private final int banThreshold;
    private final boolean banEnabled;
    private final String banSource;
    private final String firstBanDuration;
    private final String secondBanDuration;
    private final String thirdBanDuration;
    private final String fourthBanDuration;
    private final boolean notifyConsole;
    private final boolean notifyInGameStaff;
    private final String staffPermission;
    private final DiscordWebhookConfig discordWebhookConfig;
    private final boolean reporterPenaltyEnabled;
    private final int falseReportThreshold;
    private final int reportBanDays;
    private final boolean scoreRecoveryEnabled;
    private final int recoveryPointsPerDay;
    private final int recoveryMaxScore;
    private final int recoveryNoReportDaysRequired;
    private final boolean auditEnabled;
    private final String auditExportDirectory;
    private final int auditMaxDisplayLimit;
    private final int auditMaxExportLimit;
    private final int retentionAuditEventsDays;
    private final int retentionRejectedReportsDays;
    private final int retentionCancelledReportsDays;
    private final int retentionScoreHistoryDays;
    private final int retentionBansDays;
    private final int maxReportReasonLength;
    private final int maxReviewNoteLength;
    private final int maxAuditReasonLength;
    private final int maxContextSummaryLength;
    private final String databaseFile;
    private final Map<String, ReportCategory> categories;
    private final LuckPermsIntegrationConfig luckPermsIntegration;
    private final CoreProtectIntegrationConfig coreProtectIntegration;
    private final WorldGuardIntegrationConfig worldGuardIntegration;
    private final GriefPreventionIntegrationConfig griefPreventionIntegration;
    private final PlaceholderApiIntegrationConfig placeholderApiIntegration;
    private final DiscordSrvIntegrationConfig discordSrvIntegration;

    private PluginConfig(FileConfiguration config) {
        initialScore = config.getInt("initial-score", 100);
        maxScore = config.getInt("max-score", 100);
        ratingEnabled = config.getBoolean("rating.enabled", true);
        defaultDeduction = config.getInt("rating.default-deduction", 10);
        minReasonLength = config.getInt("rating.min-reason-length", 5);
        minUniqueReportsBeforeDeduction = config.getInt("rating.min-unique-reports-before-deduction", 1);
        reportWindowDays = config.getInt("rating.report-window-days", 7);
        minPlaytimeMinutes = config.getInt("report-requirements.min-playtime-minutes", 60);
        minAccountAgeDays = config.getInt("report-requirements.min-account-age-days", 1);
        scoreThresholds = loadScoreThresholds(config);
        globalReportSeconds = config.getInt("cooldowns.global-report-seconds", 300);
        sameTargetCooldownDays = config.getInt("cooldowns.same-target-cooldown-days", 14);
        maxReportsPerDay = config.getInt("cooldowns.max-reports-per-day", 5);
        maxReportsPerWeek = config.getInt("cooldowns.max-reports-per-week", 15);
        banThreshold = config.getInt("ban.threshold", 0);
        banEnabled = config.getBoolean("ban.enabled", true);
        banSource = config.getString("ban.source", "ReputationBan");
        firstBanDuration = config.getString("ban.durations.first", "1d");
        secondBanDuration = config.getString("ban.durations.second", "7d");
        thirdBanDuration = config.getString("ban.durations.third", "30d");
        fourthBanDuration = config.getString("ban.durations.fourth", "permanent");
        notifyConsole = config.getBoolean("notify.console", true);
        notifyInGameStaff = config.getBoolean("notify.in-game-staff", true);
        staffPermission = config.getString("notify.staff-permission", "reputationban.notify");
        discordWebhookConfig = DiscordWebhookConfig.fromConfiguration(config.getConfigurationSection("notify"));
        reporterPenaltyEnabled = config.getBoolean("reporter-penalty.enabled", true);
        falseReportThreshold = config.getInt("reporter-penalty.false-report-threshold", 5);
        reportBanDays = config.getInt("reporter-penalty.report-ban-days", 7);
        scoreRecoveryEnabled = config.getBoolean("score-recovery.enabled", true);
        recoveryPointsPerDay = config.getInt("score-recovery.points-per-day", 2);
        recoveryMaxScore = config.getInt("score-recovery.max-score", maxScore);
        recoveryNoReportDaysRequired = config.getInt("score-recovery.no-report-days-required", 7);
        auditEnabled = config.getBoolean("audit.enabled", true);
        auditExportDirectory = config.getString("audit.export-directory", "exports");
        auditMaxDisplayLimit = config.getInt("audit.max-display-limit", 50);
        auditMaxExportLimit = config.getInt("audit.max-export-limit", 1000);
        retentionAuditEventsDays = config.getInt("retention.audit-events-days", 180);
        retentionRejectedReportsDays = config.getInt("retention.rejected-reports-days", 90);
        retentionCancelledReportsDays = config.getInt("retention.cancelled-reports-days", 90);
        retentionScoreHistoryDays = config.getInt("retention.score-history-days", 0);
        retentionBansDays = config.getInt("retention.bans-days", 0);
        maxReportReasonLength = config.getInt("limits.max-report-reason-length", 300);
        maxReviewNoteLength = config.getInt("limits.max-review-note-length", 300);
        maxAuditReasonLength = config.getInt("limits.max-audit-reason-length", 500);
        maxContextSummaryLength = config.getInt("limits.max-context-summary-length", 1000);
        databaseFile = config.getString("database.file", "reputationban.db");
        categories = loadCategories(config);
        luckPermsIntegration = loadLuckPermsIntegration(config);
        coreProtectIntegration = loadCoreProtectIntegration(config);
        worldGuardIntegration = loadWorldGuardIntegration(config);
        griefPreventionIntegration = loadGriefPreventionIntegration(config);
        placeholderApiIntegration = loadPlaceholderApiIntegration(config);
        discordSrvIntegration = loadDiscordSrvIntegration(config);
    }

    public static PluginConfig load(FileConfiguration config) {
        return new PluginConfig(config);
    }

    private static Map<String, ReportCategory> loadCategories(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("categories");
        if (section == null) {
            return Map.of();
        }
        Map<String, ReportCategory> loaded = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            String path = "categories." + key + ".";
            String normalized = key.toLowerCase(Locale.ROOT);
            String displayName = config.getString(path + "display-name", key);
            int deduction = config.getInt(path + "deduction", config.getInt("rating.default-deduction", 10));
            boolean reviewRequired = config.getBoolean(path + "staff-review-required", false);
            loaded.put(normalized, new ReportCategory(normalized, displayName, deduction, reviewRequired));
        }
        return Collections.unmodifiableMap(loaded);
    }

    private static Map<String, Integer> loadScoreThresholds(FileConfiguration config) {
        Map<String, Integer> thresholds = new LinkedHashMap<>();
        thresholds.put("warning", config.getInt("score-thresholds.warning", 70));
        thresholds.put("watch", config.getInt("score-thresholds.watch", 50));
        thresholds.put("restricted", config.getInt("score-thresholds.restricted", 30));
        thresholds.put("final-warning", config.getInt("score-thresholds.final-warning", 10));
        thresholds.put("ban", config.getInt("score-thresholds.ban", 0));
        return Collections.unmodifiableMap(thresholds);
    }

    private static LuckPermsIntegrationConfig loadLuckPermsIntegration(FileConfiguration config) {
        Map<String, Double> weights = new LinkedHashMap<>();
        ConfigurationSection section = config.getConfigurationSection("integrations.luckperms.group-weights");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                weights.put(key.toLowerCase(Locale.ROOT), section.getDouble(key, 1.0D));
            }
        }
        Set<String> bypassGroups = new HashSet<>();
        for (String group : config.getStringList("integrations.luckperms.bypass-groups")) {
            if (group != null && !group.isBlank()) {
                bypassGroups.add(group.toLowerCase(Locale.ROOT));
            }
        }
        return new LuckPermsIntegrationConfig(
                config.getBoolean("integrations.luckperms.enabled", true),
                config.getBoolean("integrations.luckperms.use-group-weight", true),
                config.getBoolean("integrations.luckperms.apply-weight-to-deduction", false),
                config.getDouble("integrations.luckperms.default-weight", 1.0D),
                Collections.unmodifiableMap(weights),
                Set.copyOf(bypassGroups),
                config.getBoolean("integrations.luckperms.offline-lookup.enabled", true),
                config.getInt("integrations.luckperms.offline-lookup.timeout-millis", 1500),
                config.getBoolean("integrations.luckperms.offline-lookup.fail-closed-for-bypass", true)
        );
    }

    private static CoreProtectIntegrationConfig loadCoreProtectIntegration(FileConfiguration config) {
        return new CoreProtectIntegrationConfig(
                config.getBoolean("integrations.coreprotect.enabled", true),
                config.getInt("integrations.coreprotect.minimum-api-version", 11),
                config.getBoolean("integrations.coreprotect.report-context.enabled", true),
                List.copyOf(config.getStringList("integrations.coreprotect.report-context.categories").stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(value -> value.toLowerCase(Locale.ROOT))
                        .toList()),
                config.getInt("integrations.coreprotect.report-context.lookup-seconds", 3600),
                config.getInt("integrations.coreprotect.report-context.radius", 20),
                config.getInt("integrations.coreprotect.report-context.max-results", 10),
                List.copyOf(config.getStringList("integrations.coreprotect.report-context.include-actions").stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(value -> value.toLowerCase(Locale.ROOT))
                        .toList())
        );
    }

    private static WorldGuardIntegrationConfig loadWorldGuardIntegration(FileConfiguration config) {
        return new WorldGuardIntegrationConfig(
                config.getBoolean("integrations.worldguard.enabled", true),
                config.getBoolean("integrations.worldguard.report-context.enabled", true),
                List.copyOf(config.getStringList("integrations.worldguard.report-context.categories").stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(value -> value.toLowerCase(Locale.ROOT))
                        .toList()),
                config.getInt("integrations.worldguard.report-context.max-regions", 10),
                config.getBoolean("integrations.worldguard.report-context.include-region-owners", false),
                config.getBoolean("integrations.worldguard.report-context.include-region-members", false),
                List.copyOf(config.getStringList("integrations.worldguard.report-context.include-flags").stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(value -> value.toLowerCase(Locale.ROOT))
                        .toList())
        );
    }

    private static GriefPreventionIntegrationConfig loadGriefPreventionIntegration(FileConfiguration config) {
        return new GriefPreventionIntegrationConfig(
                config.getBoolean("integrations.griefprevention.enabled", true),
                config.getBoolean("integrations.griefprevention.report-context.enabled", true),
                List.copyOf(config.getStringList("integrations.griefprevention.report-context.categories").stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(value -> value.toLowerCase(Locale.ROOT))
                        .toList()),
                config.getBoolean("integrations.griefprevention.report-context.include-claim-owner", false),
                config.getBoolean("integrations.griefprevention.report-context.include-trust-counts", false),
                config.getBoolean("integrations.griefprevention.report-context.include-boundaries", true)
        );
    }

    private static PlaceholderApiIntegrationConfig loadPlaceholderApiIntegration(FileConfiguration config) {
        return new PlaceholderApiIntegrationConfig(
                config.getBoolean("integrations.placeholderapi.enabled", true),
                config.getString("integrations.placeholderapi.identifier", "reputationban"),
                config.getInt("integrations.placeholderapi.cache-refresh-seconds", 60),
                config.getString("integrations.placeholderapi.show-unknown-as", "-")
        );
    }

    private static DiscordSrvIntegrationConfig loadDiscordSrvIntegration(FileConfiguration config) {
        Map<String, Boolean> events = new LinkedHashMap<>();
        ConfigurationSection section = config.getConfigurationSection("integrations.discordsrv.notifications.events");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                events.put(key.toLowerCase(Locale.ROOT), section.getBoolean(key, false));
            }
        }
        return new DiscordSrvIntegrationConfig(
                config.getBoolean("integrations.discordsrv.enabled", true),
                config.getBoolean("integrations.discordsrv.account-link-context.enabled", true),
                config.getBoolean("integrations.discordsrv.account-link-context.include-discord-ids", false),
                List.copyOf(config.getStringList("integrations.discordsrv.account-link-context.categories").stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(value -> value.toLowerCase(Locale.ROOT))
                        .toList()),
                config.getBoolean("integrations.discordsrv.notifications.enabled", false),
                config.getString("integrations.discordsrv.notifications.channel", "staff"),
                config.getBoolean("integrations.discordsrv.notifications.include-reasons", true),
                Collections.unmodifiableMap(events)
        );
    }

    public ReportCategory category(String key) {
        if (key == null) {
            return null;
        }
        return categories.get(key.toLowerCase(Locale.ROOT));
    }

    public int initialScore() {
        return initialScore;
    }

    public int maxScore() {
        return maxScore;
    }

    public boolean ratingEnabled() {
        return ratingEnabled;
    }

    public int defaultDeduction() {
        return defaultDeduction;
    }

    public int minReasonLength() {
        return minReasonLength;
    }

    public int minUniqueReportsBeforeDeduction() {
        return minUniqueReportsBeforeDeduction;
    }

    public int reportWindowDays() {
        return reportWindowDays;
    }

    public int minPlaytimeMinutes() {
        return minPlaytimeMinutes;
    }

    public int minAccountAgeDays() {
        return minAccountAgeDays;
    }

    public Map<String, Integer> scoreThresholds() {
        return scoreThresholds;
    }

    public int globalReportSeconds() {
        return globalReportSeconds;
    }

    public int sameTargetCooldownDays() {
        return sameTargetCooldownDays;
    }

    public int maxReportsPerDay() {
        return maxReportsPerDay;
    }

    public int maxReportsPerWeek() {
        return maxReportsPerWeek;
    }

    public int banThreshold() {
        return banThreshold;
    }

    public boolean banEnabled() {
        return banEnabled;
    }

    public String banSource() {
        return banSource;
    }

    public String banDurationForCount(int currentBanCount) {
        if (currentBanCount <= 0) {
            return firstBanDuration;
        }
        if (currentBanCount == 1) {
            return secondBanDuration;
        }
        if (currentBanCount == 2) {
            return thirdBanDuration;
        }
        return fourthBanDuration;
    }

    public boolean notifyConsole() {
        return notifyConsole;
    }

    public boolean notifyInGameStaff() {
        return notifyInGameStaff;
    }

    public String staffPermission() {
        return staffPermission;
    }

    public DiscordWebhookConfig discordWebhookConfig() {
        return discordWebhookConfig;
    }

    public boolean reporterPenaltyEnabled() {
        return reporterPenaltyEnabled;
    }

    public int falseReportThreshold() {
        return falseReportThreshold;
    }

    public int reportBanDays() {
        return reportBanDays;
    }

    public boolean scoreRecoveryEnabled() {
        return scoreRecoveryEnabled;
    }

    public int recoveryPointsPerDay() {
        return recoveryPointsPerDay;
    }

    public int recoveryMaxScore() {
        return recoveryMaxScore;
    }

    public int recoveryNoReportDaysRequired() {
        return recoveryNoReportDaysRequired;
    }

    public boolean auditEnabled() {
        return auditEnabled;
    }

    public String auditExportDirectory() {
        return auditExportDirectory;
    }

    public int auditMaxDisplayLimit() {
        return auditMaxDisplayLimit;
    }

    public int auditMaxExportLimit() {
        return auditMaxExportLimit;
    }

    public int retentionAuditEventsDays() {
        return retentionAuditEventsDays;
    }

    public int retentionRejectedReportsDays() {
        return retentionRejectedReportsDays;
    }

    public int retentionCancelledReportsDays() {
        return retentionCancelledReportsDays;
    }

    public int retentionScoreHistoryDays() {
        return retentionScoreHistoryDays;
    }

    public int retentionBansDays() {
        return retentionBansDays;
    }

    public int maxReportReasonLength() {
        return maxReportReasonLength;
    }

    public int maxReviewNoteLength() {
        return maxReviewNoteLength;
    }

    public int maxAuditReasonLength() {
        return maxAuditReasonLength;
    }

    public int maxContextSummaryLength() {
        return maxContextSummaryLength;
    }

    public String databaseFile() {
        return databaseFile;
    }

    public Map<String, ReportCategory> categories() {
        return categories;
    }

    public LuckPermsIntegrationConfig luckPermsIntegration() {
        return luckPermsIntegration;
    }

    public CoreProtectIntegrationConfig coreProtectIntegration() {
        return coreProtectIntegration;
    }

    public WorldGuardIntegrationConfig worldGuardIntegration() {
        return worldGuardIntegration;
    }

    public GriefPreventionIntegrationConfig griefPreventionIntegration() {
        return griefPreventionIntegration;
    }

    public PlaceholderApiIntegrationConfig placeholderApiIntegration() {
        return placeholderApiIntegration;
    }

    public DiscordSrvIntegrationConfig discordSrvIntegration() {
        return discordSrvIntegration;
    }

    public record LuckPermsIntegrationConfig(
            boolean enabled,
            boolean useGroupWeight,
            boolean applyWeightToDeduction,
            double defaultWeight,
            Map<String, Double> groupWeights,
            Set<String> bypassGroups,
            boolean offlineLookupEnabled,
            int offlineLookupTimeoutMillis,
            boolean offlineLookupFailClosedForBypass
    ) {
    }

    public record CoreProtectIntegrationConfig(
            boolean enabled,
            int minimumApiVersion,
            boolean reportContextEnabled,
            List<String> reportContextCategories,
            int lookupSeconds,
            int radius,
            int maxResults,
            List<String> includeActions
    ) {
    }

    public record WorldGuardIntegrationConfig(
            boolean enabled,
            boolean reportContextEnabled,
            List<String> reportContextCategories,
            int maxRegions,
            boolean includeRegionOwners,
            boolean includeRegionMembers,
            List<String> includeFlags
    ) {
    }

    public record GriefPreventionIntegrationConfig(
            boolean enabled,
            boolean reportContextEnabled,
            List<String> reportContextCategories,
            boolean includeClaimOwner,
            boolean includeTrustCounts,
            boolean includeBoundaries
    ) {
    }

    public record PlaceholderApiIntegrationConfig(
            boolean enabled,
            String identifier,
            int cacheRefreshSeconds,
            String showUnknownAs
    ) {
    }

    public record DiscordSrvIntegrationConfig(
            boolean enabled,
            boolean accountLinkContextEnabled,
            boolean includeDiscordIds,
            List<String> accountLinkContextCategories,
            boolean notificationsEnabled,
            String notificationChannel,
            boolean notificationIncludeReasons,
            Map<String, Boolean> notificationEvents
    ) {
    }
}
