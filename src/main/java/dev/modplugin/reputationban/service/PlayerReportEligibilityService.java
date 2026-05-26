package dev.modplugin.reputationban.service;

import dev.modplugin.reputationban.util.ReportEligibilityPolicy;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

public final class PlayerReportEligibilityService {
    private static final int TICKS_PER_MINUTE = 20 * 60;

    public ReportEligibilityPolicy.EligibilityResult checkPlaytime(Player reporter, int requiredPlaytimeMinutes) {
        int playtimeTicks = reporter.getStatistic(Statistic.PLAY_ONE_MINUTE);
        int playtimeMinutes = Math.max(0, playtimeTicks) / TICKS_PER_MINUTE;
        return ReportEligibilityPolicy.checkPlaytime(playtimeMinutes, requiredPlaytimeMinutes);
    }
}
