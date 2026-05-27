package dev.modplugin.reputationban.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiagnosticReportTest {
    @Test
    void aggregatesOkWarnAndErrorStatuses() {
        assertEquals(DiagnosticStatus.OK, DiagnosticStatus.aggregate(DiagnosticStatus.OK, DiagnosticStatus.OK));
        assertEquals(DiagnosticStatus.WARN, DiagnosticStatus.aggregate(DiagnosticStatus.OK, DiagnosticStatus.WARN));
        assertEquals(DiagnosticStatus.ERROR, DiagnosticStatus.aggregate(DiagnosticStatus.WARN, DiagnosticStatus.ERROR));
    }

    @Test
    void reportOverallStatusReflectsConfigWarningsAndTableErrors() {
        DiagnosticReport warnOnly = report(DiagnosticStatus.OK, DiagnosticStatus.OK, DiagnosticStatus.WARN, DiagnosticStatus.OK);
        assertEquals(DiagnosticStatus.WARN, warnOnly.overallStatus());

        DiagnosticReport tableError = report(DiagnosticStatus.OK, DiagnosticStatus.ERROR, DiagnosticStatus.OK, DiagnosticStatus.OK);
        assertEquals(DiagnosticStatus.ERROR, tableError.overallStatus());
        assertFalse(tableError.tablesOk());
    }

    @Test
    void discordDiagnosticStateOnlyStoresBooleanConfigurationState() {
        DiagnosticReport report = new DiagnosticReport(
                "0.11.0",
                "Paper 26.1.2",
                "25",
                "/server/plugins/ReputationBan",
                DiagnosticStatus.OK,
                DiagnosticStatus.OK,
                DiagnosticStatus.OK,
                DiagnosticStatus.OK,
                0,
                0,
                true,
                true,
                true,
                true,
                true,
                "audit=180 rejected=90 cancelled=90 score=0 bans=0",
                1,
                2,
                3,
                List.of()
        );

        assertEquals(true, report.discordEnabled());
        assertEquals(true, report.discordUrlConfigured());
        assertFalse(Arrays.stream(DiagnosticReport.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals("discordWebhookUrl")));
        assertFalse(Arrays.stream(DiagnosticReport.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals("webhookUrl")));
    }

    private static DiagnosticReport report(
            DiagnosticStatus database,
            DiagnosticStatus tables,
            DiagnosticStatus config,
            DiagnosticStatus auditExport
    ) {
        return new DiagnosticReport(
                "0.11.0",
                "Paper",
                "25",
                "/server/plugins/ReputationBan",
                database,
                tables,
                config,
                auditExport,
                config == DiagnosticStatus.WARN ? 1 : 0,
                config == DiagnosticStatus.ERROR ? 1 : 0,
                true,
                false,
                false,
                auditExport == DiagnosticStatus.OK,
                true,
                "audit=180 rejected=90 cancelled=90 score=0 bans=0",
                0,
                0,
                0,
                tables == DiagnosticStatus.ERROR ? List.of("bans") : List.of()
        );
    }
}
