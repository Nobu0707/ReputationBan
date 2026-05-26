package dev.modplugin.reputationban.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JsonEscaperTest {
    @Test
    void escapesJsonSpecialCharacters() {
        assertEquals("\\\"quote\\\" \\\\ slash \\n \\r \\t",
                JsonEscaper.escape("\"quote\" \\ slash \n \r \t"));
    }

    @Test
    void escapesControlCharactersAsUnicode() {
        assertEquals("a\\u0001b", JsonEscaper.escape("a\u0001b"));
    }
}
