package dev.modplugin.reputationban.model;

import java.util.Objects;

public record ReportCategory(String key, String displayName, int deduction, boolean staffReviewRequired) {
    public ReportCategory {
        key = requireText(key, "key");
        displayName = requireText(displayName, "displayName");
        if (deduction < 0) {
            throw new IllegalArgumentException("deduction must not be negative");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }
}
