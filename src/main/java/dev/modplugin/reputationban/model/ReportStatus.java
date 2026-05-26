package dev.modplugin.reputationban.model;

import java.util.Locale;

public enum ReportStatus {
    PENDING("pending"),
    AUTO_ACCEPTED("auto_accepted"),
    APPROVED("approved"),
    REJECTED("rejected"),
    CANCELLED("cancelled");

    private final String databaseValue;

    ReportStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public boolean reviewable() {
        return this == PENDING;
    }

    public static ReportStatus parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ReportStatus status : values()) {
            if (status.databaseValue.equals(normalized)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown report status: " + value);
    }

    public static boolean canReview(String value) {
        return parse(value).reviewable();
    }
}
