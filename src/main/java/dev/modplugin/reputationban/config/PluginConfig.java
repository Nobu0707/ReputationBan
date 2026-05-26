package dev.modplugin.reputationban.config;

import dev.modplugin.reputationban.model.ReportCategory;
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
    private final String databaseFile;
    private final Map<String, ReportCategory> categories;

    private PluginConfig(FileConfiguration config) {
        initialScore = config.getInt("initial-score", 100);
        maxScore = config.getInt("max-score", 100);
        ratingEnabled = config.getBoolean("rating.enabled", true);
        defaultDeduction = config.getInt("rating.default-deduction", 10);
        minReasonLength = config.getInt("rating.min-reason-length", 5);
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

    public String databaseFile() {
        return databaseFile;
    }

    public Map<String, ReportCategory> categories() {
        return categories;
    }
}
