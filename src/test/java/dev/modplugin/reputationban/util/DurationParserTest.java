package dev.modplugin.reputationban.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class DurationParserTest {
    @Test
    void parsesDayAndHourDurations() {
        assertEquals(Duration.ofDays(1), DurationParser.parseBanDuration("1d").orElseThrow());
        assertEquals(Duration.ofDays(7), DurationParser.parseBanDuration("7d").orElseThrow());
        assertEquals(Duration.ofDays(30), DurationParser.parseBanDuration("30d").orElseThrow());
        assertEquals(Duration.ofHours(12), DurationParser.parseBanDuration("12h").orElseThrow());
    }

    @Test
    void parsesPermanentAsEmptyOptional() {
        assertTrue(DurationParser.parseBanDuration("permanent").isEmpty());
    }

    @Test
    void rejectsInvalidDuration() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parseBanDuration("later"));
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parseBanDuration("0d"));
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parseBanDuration(""));
    }
}
