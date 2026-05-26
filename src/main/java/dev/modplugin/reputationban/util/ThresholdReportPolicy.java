package dev.modplugin.reputationban.util;

import java.time.Duration;

public final class ThresholdReportPolicy {
    private ThresholdReportPolicy() {
    }

    public static int effectiveRequiredReports(int configuredRequiredReports) {
        return Math.max(1, configuredRequiredReports);
    }

    public static boolean shouldQueueForThreshold(int configuredRequiredReports) {
        return effectiveRequiredReports(configuredRequiredReports) >= 2;
    }

    public static boolean thresholdReached(int requiredReports, int currentUniqueReports) {
        return currentUniqueReports >= effectiveRequiredReports(requiredReports);
    }

    public static long windowCutoff(long now, int reportWindowDays) {
        if (reportWindowDays <= 0) {
            return Long.MIN_VALUE;
        }
        return now - Duration.ofDays(reportWindowDays).toMillis();
    }
}
