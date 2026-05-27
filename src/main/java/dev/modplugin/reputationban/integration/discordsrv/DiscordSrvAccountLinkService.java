package dev.modplugin.reputationban.integration.discordsrv;

import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.model.AuditEvent;
import dev.modplugin.reputationban.model.AuditEventType;
import dev.modplugin.reputationban.model.ReportCategory;
import dev.modplugin.reputationban.service.AuditService;
import dev.modplugin.reputationban.service.ReportService;
import dev.modplugin.reputationban.util.AuditMetadata;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public final class DiscordSrvAccountLinkService {
    private final JavaPlugin plugin;
    private final DiscordSrvIntegration integration;
    private final ReportService reportService;
    private final AuditService auditService;

    public DiscordSrvAccountLinkService(
            JavaPlugin plugin,
            DiscordSrvIntegration integration,
            ReportService reportService,
            AuditService auditService
    ) {
        this.plugin = plugin;
        this.integration = integration;
        this.reportService = reportService;
        this.auditService = auditService;
    }

    public void captureAfterReport(
            long reportId,
            UUID reporterUuid,
            String reporterName,
            UUID targetUuid,
            String targetName,
            ReportCategory category,
            PluginConfig config
    ) {
        PluginConfig.DiscordSrvIntegrationConfig discordConfig = config.discordSrvIntegration();
        if (!DiscordSrvPolicy.shouldCapture(
                discordConfig.enabled(),
                discordConfig.accountLinkContextEnabled(),
                discordConfig.accountLinkContextCategories(),
                category.key()
        )) {
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                DiscordSrvStatus status = integration.detail(config);
                if (!status.active() || !status.accountLinkAvailable()) {
                    return;
                }
                String reporterDiscordId = integration.discordId(reporterUuid).orElse("");
                String targetDiscordId = integration.discordId(targetUuid).orElse("");
                DiscordSrvAccountLinkSummary summary = new DiscordSrvAccountLinkSummary(
                        DiscordSrvPolicy.linked(reporterDiscordId),
                        DiscordSrvPolicy.linked(targetDiscordId),
                        discordConfig.includeDiscordIds(),
                        reporterDiscordId,
                        targetDiscordId
                );
                save(reportId, reporterUuid, reporterName, targetUuid, targetName, category, summary);
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.WARNING, "DiscordSRV account link context failed: " + exception.getMessage());
            }
        });
    }

    private void save(
            long reportId,
            UUID reporterUuid,
            String reporterName,
            UUID targetUuid,
            String targetName,
            ReportCategory category,
            DiscordSrvAccountLinkSummary summary
    ) {
        reportService.saveReportContext(reportId, "discordsrv", summary.summary(), summary.metadata())
                .thenRun(() -> auditService.recordEvent(AuditEvent.create(
                        AuditEventType.DISCORDSRV_CONTEXT_CAPTURED,
                        reporterUuid,
                        reporterName,
                        targetUuid,
                        targetName,
                        reportId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "discordsrv report context",
                        AuditMetadata.create()
                                .put("reporterLinked", summary.reporterLinked())
                                .put("targetLinked", summary.targetLinked())
                                .put("includeDiscordIds", summary.includeDiscordIds())
                                .put("category", category.key())
                                .toJson(),
                        System.currentTimeMillis()
                )));
    }
}
