package dev.modplugin.reputationban.integration.placeholderapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PlaceholderStatusPolicyTest {
    private static final Map<String, Integer> THRESHOLDS = Map.of(
            "warning", 70,
            "watch", 50,
            "restricted", 30,
            "final-warning", 10,
            "ban", 0
    );

    @Test
    void resolvesStableStatusValuesFromScore() {
        assertEquals("normal", PlaceholderStatusPolicy.status(100, THRESHOLDS));
        assertEquals("warning", PlaceholderStatusPolicy.status(70, THRESHOLDS));
        assertEquals("watch", PlaceholderStatusPolicy.status(50, THRESHOLDS));
        assertEquals("restricted", PlaceholderStatusPolicy.status(30, THRESHOLDS));
        assertEquals("final-warning", PlaceholderStatusPolicy.status(10, THRESHOLDS));
        assertEquals("banned-threshold", PlaceholderStatusPolicy.status(0, THRESHOLDS));
    }
}
