package dev.modplugin.reputationban.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScoreMathTest {
    @Test
    void clampsOnlyToMaximumScore() {
        assertEquals(100, ScoreMath.clampToMax(120, 100));
        assertEquals(75, ScoreMath.clampToMax(75, 100));
        assertEquals(-5, ScoreMath.clampToMax(-5, 100));
    }

    @Test
    void detectsFirstDownwardThresholdCrossing() {
        assertTrue(ScoreMath.crossedThresholdDownward(1, 0, 0));
        assertTrue(ScoreMath.crossedThresholdDownward(10, -1, 0));
        assertFalse(ScoreMath.crossedThresholdDownward(0, -5, 0));
        assertFalse(ScoreMath.crossedThresholdDownward(10, 5, 0));
    }
}
