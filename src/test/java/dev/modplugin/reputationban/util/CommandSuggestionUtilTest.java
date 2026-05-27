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
        assertEquals(List.of("help", "version", "check", "history", "add", "remove", "set"),
                CommandSuggestionUtil.repSubcommands(scoreOnly, ""));

        assertEquals(List.of("version"), CommandSuggestionUtil.repSubcommands(permission -> false, "ver"));

        Predicate<String> banOnly = Set.of("reputationban.admin.ban")::contains;
        assertEquals(List.of("banhistory", "baninfo"),
                CommandSuggestionUtil.repSubcommands(banOnly, "ban"));

        Predicate<String> auditAndMaintenance = Set.of("reputationban.admin.audit", "reputationban.admin.maintenance")::contains;
        assertEquals(List.of("audit"), CommandSuggestionUtil.repSubcommands(auditAndMaintenance, "aud"));
        assertEquals(List.of("maintenance"), CommandSuggestionUtil.repSubcommands(auditAndMaintenance, "main"));

        Predicate<String> diagnostics = Set.of("reputationban.admin.diagnostics")::contains;
        assertEquals(List.of("doctor"), CommandSuggestionUtil.repSubcommands(diagnostics, "doc"));
        assertEquals(List.of("diagnostics"), CommandSuggestionUtil.repSubcommands(diagnostics, "diag"));
        assertEquals(List.of("support"), CommandSuggestionUtil.repSubcommands(diagnostics, "sup"));

        Predicate<String> maintenance = Set.of("reputationban.admin.maintenance")::contains;
        assertEquals(List.of("backup"), CommandSuggestionUtil.repSubcommands(maintenance, "back"));

        Predicate<String> integrations = Set.of("reputationban.admin.integrations")::contains;
        assertEquals(List.of("integrations", "integration"), CommandSuggestionUtil.repSubcommands(integrations, "integ"));
        assertEquals(List.of("test"), CommandSuggestionUtil.repSecondArgumentSuggestions("integrations", List.of(), "te"));
        assertEquals(List.of("test"), CommandSuggestionUtil.repSecondArgumentSuggestions("integration", List.of(), "te"));
    }

    @Test
    void reportsListStatusSuggestionsIncludeAll() {
        assertEquals(List.of("pending"), CommandSuggestionUtil.reportsSecondArgumentSuggestions("list", List.of(), "pen"));
        assertEquals(List.of("threshold_pending"), CommandSuggestionUtil.reportsSecondArgumentSuggestions("list", List.of(), "threshold"));
        assertTrue(CommandSuggestionUtil.reportsSecondArgumentSuggestions("list", List.of(), "").contains("all"));
        assertTrue(CommandSuggestionUtil.reportsSubcommands(true, "evi").contains("evidence"));
        assertEquals(List.of("123"), CommandSuggestionUtil.reportsSecondArgumentSuggestions("evidence", List.of("123"), "1"));
    }

    @Test
    void numericSuggestionsAreScopedBySubcommand() {
        assertEquals(List.of("10"), CommandSuggestionUtil.repThirdArgumentSuggestions("history", "1"));
        assertEquals(List.of("1", "10", "100"), CommandSuggestionUtil.repThirdArgumentSuggestions("add", "1"));
        assertEquals(List.of("10", "20", "50"), CommandSuggestionUtil.reportsThirdArgumentSuggestions("list", ""));
    }

    @Test
    void auditSuggestionsIncludeModesPlayersAndTypes() {
        assertEquals(List.of("recent"), CommandSuggestionUtil.repSecondArgumentSuggestions("audit", List.of("Alex"), "rec"));
        assertEquals(List.of("Alex"), CommandSuggestionUtil.repSecondArgumentSuggestions("audit", List.of("Alex"), "Al"));
        assertTrue(CommandSuggestionUtil.repAuditThirdArgumentSuggestions("type", List.of(), "REPORT").contains("REPORT_CREATED"));
        assertEquals(List.of("run"), CommandSuggestionUtil.repSecondArgumentSuggestions("maintenance", List.of(), "r"));
        assertEquals(List.of("preview"), CommandSuggestionUtil.repSecondArgumentSuggestions("maintenance", List.of(), "pre"));
        assertEquals(List.of("confirm"), CommandSuggestionUtil.repMaintenanceThirdArgumentSuggestions("run", "con"));
        assertEquals(List.of("bundle"), CommandSuggestionUtil.repSecondArgumentSuggestions("support", List.of(), "bun"));
    }
}
