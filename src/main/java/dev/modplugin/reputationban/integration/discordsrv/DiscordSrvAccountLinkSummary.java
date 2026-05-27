package dev.modplugin.reputationban.integration.discordsrv;

import dev.modplugin.reputationban.util.AuditMetadata;

public record DiscordSrvAccountLinkSummary(
        boolean reporterLinked,
        boolean targetLinked,
        boolean includeDiscordIds,
        String reporterDiscordId,
        String targetDiscordId
) {
    public String summary() {
        String visibility = includeDiscordIds ? "visible" : "hidden";
        return "DiscordSRV: reporterLinked=%s targetLinked=%s discordIds=%s".formatted(
                reporterLinked,
                targetLinked,
                visibility
        );
    }

    public String metadata() {
        AuditMetadata metadata = AuditMetadata.create()
                .put("reporterLinked", reporterLinked)
                .put("targetLinked", targetLinked)
                .put("includeDiscordIds", includeDiscordIds);
        if (includeDiscordIds) {
            metadata.put("reporterDiscordId", DiscordSrvPolicy.discordIdDisplay(true, reporterDiscordId));
            metadata.put("targetDiscordId", DiscordSrvPolicy.discordIdDisplay(true, targetDiscordId));
        }
        return metadata.toJson();
    }
}
