package dev.modplugin.reputationban.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CsvEscaperTest {
    @Test
    void leavesNormalTextUnquoted() {
        assertEquals("normal", CsvEscaper.escape("normal"));
    }

    @Test
    void quotesComma() {
        assertEquals("\"a,b\"", CsvEscaper.escape("a,b"));
    }

    @Test
    void doublesQuotes() {
        assertEquals("\"a\"\"b\"", CsvEscaper.escape("a\"b"));
    }

    @Test
    void quotesNewlineAndCrLf() {
        assertEquals("\"a\nb\"", CsvEscaper.escape("a\nb"));
        assertEquals("\"a\r\nb\"", CsvEscaper.escape("a\r\nb"));
    }
}
