package dev.modplugin.reputationban.model;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum AuditEventType {
    REPORT_CREATED,
    REPORT_THRESHOLD_REACHED,
    REPORT_APPROVED,
    REPORT_REJECTED,
    SCORE_CHANGED_ADMIN,
    SCORE_RECOVERED,
    AUTO_BAN,
    UNBAN,
    PARDON,
    REPORTER_PENALTY,
    CONFIG_RELOADED,
    MAINTENANCE_PREVIEW,
    MAINTENANCE_RUN,
    DIAGNOSTICS_RUN;

    public String databaseValue() {
        return name();
    }

    public static AuditEventType parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("event type must not be null");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        for (AuditEventType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown audit event type: " + value);
    }

    public static boolean isValid(String value) {
        try {
            parse(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public static List<String> databaseValues() {
        return Arrays.stream(values()).map(AuditEventType::databaseValue).toList();
    }
}
