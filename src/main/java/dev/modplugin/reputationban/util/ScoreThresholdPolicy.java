package dev.modplugin.reputationban.util;

import dev.modplugin.reputationban.model.ScoreThresholdCrossing;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ScoreThresholdPolicy {
    private ScoreThresholdPolicy() {
    }

    public static List<ScoreThresholdCrossing> crossedDownward(
            int oldScore,
            int newScore,
            Map<String, Integer> thresholds
    ) {
        if (newScore >= oldScore) {
            return List.of();
        }
        List<ScoreThresholdCrossing> crossings = new ArrayList<>();
        for (Map.Entry<String, Integer> threshold : thresholds.entrySet()) {
            if ("ban".equals(threshold.getKey())) {
                continue;
            }
            if (ScoreMath.crossedThresholdDownward(oldScore, newScore, threshold.getValue())) {
                crossings.add(new ScoreThresholdCrossing(threshold.getKey(), threshold.getValue()));
            }
        }
        return List.copyOf(crossings);
    }
}
