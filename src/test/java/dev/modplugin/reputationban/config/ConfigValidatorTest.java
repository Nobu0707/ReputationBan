package dev.modplugin.reputationban.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.modplugin.reputationban.config.ConfigValidationIssue.Severity;
import java.nio.file.Path;
import java.util.List;
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
                auditExportDirectory
        );
    }
}
