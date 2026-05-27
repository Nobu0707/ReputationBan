package dev.modplugin.reputationban.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.modplugin.reputationban.config.ConfigValidationIssue.Severity;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfigValidatorTest {
    private static final Path DATA_FOLDER = Path.of("/tmp/ReputationBan");

    @Test
    void acceptsNormalConfig() {
        assertTrue(ConfigValidator.validate(valid(), DATA_FOLDER).isEmpty());
    }

    @Test
    void detectsInitialScoreAboveMaxScore() {
        List<ConfigValidationIssue> issues = ConfigValidator.validate(valid(150, 100, 180, "exports"), DATA_FOLDER);

        assertTrue(issues.stream().anyMatch(issue -> issue.severity() == Severity.ERROR && issue.path().equals("initial-score")));
    }

    @Test
    void detectsInvalidRetention() {
        List<ConfigValidationIssue> issues = ConfigValidator.validate(valid(100, 100, -1, "exports"), DATA_FOLDER);

        assertTrue(issues.stream().anyMatch(issue -> issue.path().equals("retention.audit-events-days")));
    }

    @Test
    void detectsInvalidAuditExportPath() {
        List<ConfigValidationIssue> issues = ConfigValidator.validate(valid(100, 100, 180, "../outside"), DATA_FOLDER);

        assertEquals(1, issues.stream().filter(issue -> issue.path().equals("audit.export-directory")).count());
    }

    @Test
    void detectsInvalidIntegrationValues() {
        ConfigValidationInput config = new ConfigValidationInput(
                100, 100, 5, 1, 7, 300, 14, 5, 15, 60, 1, 0, 2, 100, 7, 50, 1000,
                180, 90, 90, 0, 0, 5, "exports", 0.0D, Map.of("trusted", -1.0D), 0, 0, -1, -1,
                -1, List.of(), List.of(), List.of(), "Bad-Identifier", -1
        );

        List<ConfigValidationIssue> issues = ConfigValidator.validate(config, DATA_FOLDER);

        assertTrue(issues.stream().anyMatch(issue -> issue.path().equals("integrations.luckperms.default-weight")));
        assertTrue(issues.stream().anyMatch(issue -> issue.path().equals("integrations.luckperms.group-weights.trusted")));
        assertTrue(issues.stream().anyMatch(issue -> issue.path().equals("integrations.coreprotect.minimum-api-version")));
        assertTrue(issues.stream().anyMatch(issue -> issue.path().equals("integrations.coreprotect.report-context.lookup-seconds")));
        assertTrue(issues.stream().anyMatch(issue -> issue.path().equals("integrations.coreprotect.report-context.radius")));
        assertTrue(issues.stream().anyMatch(issue -> issue.path().equals("integrations.coreprotect.report-context.max-results")));
        assertTrue(issues.stream().anyMatch(issue -> issue.path().equals("integrations.worldguard.report-context.max-regions")));
        assertTrue(issues.stream().anyMatch(issue -> issue.severity() == Severity.WARNING
                && issue.path().equals("integrations.worldguard.report-context.categories")));
        assertTrue(issues.stream().anyMatch(issue -> issue.severity() == Severity.WARNING
                && issue.path().equals("integrations.griefprevention.report-context.categories")));
        assertTrue(issues.stream().anyMatch(issue -> issue.severity() == Severity.WARNING
                && issue.path().equals("integrations.discordsrv.account-link-context.categories")));
        assertTrue(issues.stream().anyMatch(issue -> issue.path().equals("integrations.placeholderapi.cache-refresh-seconds")));
        assertTrue(issues.stream().anyMatch(issue -> issue.severity() == Severity.WARNING
                && issue.path().equals("integrations.placeholderapi.identifier")));
    }

    private static ConfigValidationInput valid() {
        return valid(100, 100, 180, "exports");
    }

    private static ConfigValidationInput valid(int initialScore, int maxScore, int retentionAuditEventsDays, String auditExportDirectory) {
        return new ConfigValidationInput(
                initialScore,
                maxScore,
                5,
                1,
                7,
                300,
                14,
                5,
                15,
                60,
                1,
                0,
                2,
                100,
                7,
                50,
                1000,
                retentionAuditEventsDays,
                90,
                90,
                0,
                0,
                5,
                auditExportDirectory,
                1.0D,
                Map.of("default", 1.0D),
                11,
                3600,
                20,
                10,
                10,
                List.of("griefing", "harassment"),
                List.of("griefing", "harassment", "scam"),
                List.of("griefing", "harassment", "scam", "abusive_chat"),
                "reputationban",
                60
        );
    }
}
