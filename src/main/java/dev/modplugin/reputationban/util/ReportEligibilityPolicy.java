package dev.modplugin.reputationban.util;

import java.time.Duration;

public final class ReportEligibilityPolicy {
    private ReportEligibilityPolicy() {
    }

    public static EligibilityResult checkPlaytime(int currentPlaytimeMinutes, int requiredPlaytimeMinutes) {
        if (requiredPlaytimeMinutes <= 0 || currentPlaytimeMinutes >= requiredPlaytimeMinutes) {
            return EligibilityResult.pass();
        }
        return EligibilityResult.rejected("通報するには、累計プレイ時間が%d分以上必要です。\n現在のプレイ時間: %d分"
                .formatted(requiredPlaytimeMinutes, Math.max(0, currentPlaytimeMinutes)));
    }

    public static EligibilityResult checkAccountAge(Long firstSeen, int requiredAccountAgeDays, long now) {
        if (requiredAccountAgeDays <= 0) {
            return EligibilityResult.pass();
        }
        if (firstSeen == null) {
            return EligibilityResult.rejected("通報するには、サーバー初参加日時の記録が必要です。再参加後に再試行してください。");
        }
        long requiredMillis = Duration.ofDays(requiredAccountAgeDays).toMillis();
        if (now - firstSeen >= requiredMillis) {
            return EligibilityResult.pass();
        }
        return EligibilityResult.rejected("通報するには、サーバー初参加から%d日以上経過している必要があります。"
                .formatted(requiredAccountAgeDays));
    }

    public record EligibilityResult(boolean allowed, String message) {
        public static EligibilityResult pass() {
            return new EligibilityResult(true, "");
        }

        public static EligibilityResult rejected(String message) {
            return new EligibilityResult(false, message);
        }
    }
}
