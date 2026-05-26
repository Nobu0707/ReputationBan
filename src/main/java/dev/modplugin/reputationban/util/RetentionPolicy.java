package dev.modplugin.reputationban.util;

import java.time.Duration;

public final class RetentionPolicy {
    private RetentionPolicy() {
    }

    public static boolean cleanupEnabled(int retentionDays) {
        return retentionDays > 0;
    }

    public static Long cutoffMillis(int retentionDays, long now) {
        if (!cleanupEnabled(retentionDays)) {
            return null;
        }
        return now - Duration.ofDays(retentionDays).toMillis();
    }
}
