package dev.modplugin.reputationban.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class CommandSuggestionUtilTest {
    @Test
    void filtersSuggestionsByPrefixCaseInsensitively() {
        assertEquals(List.of("Steve", "staff"), CommandSuggestionUtil.filterByPrefix(List.of("Alex", "Steve", "staff"), "st"));
    }

    @Test
    void reportCategoriesUsePrefixFiltering() {
        assertEquals(List.of("griefing"), CommandSuggestionUtil.filterByPrefix(List.of("griefing", "spam", "scam"), "g"));
    }

    @Test
    void repSubcommandsRespectPermissions() {
        Predicate<String> scoreOnly = Set.of("reputationban.admin.score")::contains;
        assertEquals(List.of("help", "check", "history", "add", "remove", "set"),
                CommandSuggestionUtil.repSubcommands(scoreOnly, ""));

        Predicate<String> banOnly = Set.of("reputationban.admin.ban")::contains;
        assertEquals(List.of("banhistory", "baninfo"),
                CommandSuggestionUtil.repSubcommands(banOnly, "ban"));
    }

    @Test
    void reportsListStatusSuggestionsIncludeAll() {
        assertEquals(List.of("pending"), CommandSuggestionUtil.reportsSecondArgumentSuggestions("list", List.of(), "pen"));
        assertEquals(List.of("threshold_pending"), CommandSuggestionUtil.reportsSecondArgumentSuggestions("list", List.of(), "threshold"));
        assertTrue(CommandSuggestionUtil.reportsSecondArgumentSuggestions("list", List.of(), "").contains("all"));
    }

    @Test
    void numericSuggestionsAreScopedBySubcommand() {
        assertEquals(List.of("10"), CommandSuggestionUtil.repThirdArgumentSuggestions("history", "1"));
        assertEquals(List.of("1", "10", "100"), CommandSuggestionUtil.repThirdArgumentSuggestions("add", "1"));
        assertEquals(List.of("10", "20", "50"), CommandSuggestionUtil.reportsThirdArgumentSuggestions("list", ""));
    }
}
