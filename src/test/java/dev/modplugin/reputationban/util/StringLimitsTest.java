package dev.modplugin.reputationban.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StringLimitsTest {
    @Test
    void truncatesLongValues() {
        assertEquals("abc", StringLimits.truncate("abcdef", 3));
    }

    @Test
    void detectsLengthExceeded() {
        assertTrue(StringLimits.exceeds("abcd", 3));
        assertFalse(StringLimits.exceeds("abc", 3));
        assertFalse(StringLimits.exceeds(null, 3));
    }
}
