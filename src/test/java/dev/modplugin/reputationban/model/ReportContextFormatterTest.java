package dev.modplugin.reputationban.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ReportContextFormatterTest {
    @Test
    void formatsDiscordSrvProvider() {
        ReportContext context = new ReportContext(
                1,
                10,
                "discordsrv",
                "DiscordSRV: reporterLinked=true targetLinked=false discordIds=hidden",
                "{}",
                100
        );

        List<String> lines = ReportContextFormatter.formatEvidence(List.of(context));

        assertTrue(lines.contains("DiscordSRV:"));
        assertTrue(lines.stream().anyMatch(line -> line.contains("reporterLinked=true")));
    }
}
