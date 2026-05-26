package dev.modplugin.reputationban.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.modplugin.reputationban.model.ScoreThresholdCrossing;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ScoreThresholdPolicyTest {
    private static final Map<String, Integer> THRESHOLDS = thresholds();

    @Test
    void detectsWarningCrossing() {
        assertEquals(
                List.of(new ScoreThresholdCrossing("warning", 70)),
                ScoreThresholdPolicy.crossedDownward(72, 68, THRESHOLDS)
        );
    }

    @Test
    void detectsMultipleCrossings() {
        assertEquals(
                List.of(new ScoreThresholdCrossing("watch", 50), new ScoreThresholdCrossing("restricted", 30)),
                ScoreThresholdPolicy.crossedDownward(52, 29, THRESHOLDS)
        );
    }

    @Test
    void ignoresNoCrossingAndUpwardChanges() {
        assertEquals(List.of(), ScoreThresholdPolicy.crossedDownward(20, 15, THRESHOLDS));
        assertEquals(List.of(), ScoreThresholdPolicy.crossedDownward(5, 15, THRESHOLDS));
    }

    private static Map<String, Integer> thresholds() {
        Map<String, Integer> thresholds = new LinkedHashMap<>();
        thresholds.put("warning", 70);
        thresholds.put("watch", 50);
        thresholds.put("restricted", 30);
        thresholds.put("final-warning", 10);
        thresholds.put("ban", 0);
        return thresholds;
    }
}
