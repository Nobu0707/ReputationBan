package dev.modplugin.reputationban.util;

public final class StringLimits {
    private StringLimits() {
    }

    public static boolean exceeds(String value, int maxLength) {
        return value != null && maxLength >= 0 && value.length() > maxLength;
    }

    public static String truncate(String value, int maxLength) {
        if (value == null || maxLength < 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
