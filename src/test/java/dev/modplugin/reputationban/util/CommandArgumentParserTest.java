package dev.modplugin.reputationban.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class CommandArgumentParserTest {
    @Test
    void parsesAndClampsLimit() {
        assertEquals(OptionalInt.of(10), CommandArgumentParser.parseLimit("10", 50));
        assertEquals(OptionalInt.of(50), CommandArgumentParser.parseLimit("999", 50));
    }

    @Test
    void rejectsNonNumericAndBelowOneLimit() {
        assertTrue(CommandArgumentParser.parseLimit("abc", 50).isEmpty());
        assertTrue(CommandArgumentParser.parseLimit("0", 50).isEmpty());
        assertTrue(CommandArgumentParser.parseLimit("-1", 50).isEmpty());
    }
}
