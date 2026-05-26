package dev.modplugin.reputationban.config;

import dev.modplugin.reputationban.model.ReportCategory;
import dev.modplugin.reputationban.notification.DiscordWebhookConfig;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
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
    private final String databaseFile;
    private final Map<String, ReportCategory> categories;

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
        databaseFile = config.getString("database.file", "reputationban.db");
        categories = loadCategories(config);
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

    public String databaseFile() {
        return databaseFile;
    }

    public Map<String, ReportCategory> categories() {
        return categories;
    }
}
