package dev.modplugin.reputationban.notification;

public enum NotificationEventType {
    REPORT_CREATED("report-created"),
    REPORT_APPROVED("report-approved"),
    REPORT_REJECTED("report-rejected"),
    SCORE_THRESHOLD_CROSSED("score-threshold-crossed"),
    AUTO_BAN("auto-ban"),
    UNBAN("unban"),
    PARDON("pardon"),
    REPORTER_PENALTY("reporter-penalty"),
    RECOVERY_SUMMARY("recovery-summary");

    private final String configKey;

    NotificationEventType(String configKey) {
        this.configKey = configKey;
    }

    public String configKey() {
        return configKey;
    }
}
