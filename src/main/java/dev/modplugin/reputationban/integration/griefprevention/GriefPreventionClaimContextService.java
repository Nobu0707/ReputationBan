package dev.modplugin.reputationban.integration.griefprevention;

import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.model.AuditEvent;
import dev.modplugin.reputationban.model.AuditEventType;
import dev.modplugin.reputationban.model.ReportCategory;
import dev.modplugin.reputationban.service.AuditService;
import dev.modplugin.reputationban.service.ReportService;
import dev.modplugin.reputationban.util.AuditMetadata;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public final class GriefPreventionClaimContextService {
    private final JavaPlugin plugin;
    private final GriefPreventionIntegration integration;
    private final ReportService reportService;
    private final AuditService auditService;

    public GriefPreventionClaimContextService(
            JavaPlugin plugin,
            GriefPreventionIntegration integration,
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
            Location location,
            PluginConfig config
    ) {
        PluginConfig.GriefPreventionIntegrationConfig griefConfig = config.griefPreventionIntegration();
        if (!GriefPreventionClaimPolicy.shouldCapture(
                griefConfig.enabled(),
                griefConfig.reportContextEnabled(),
                griefConfig.reportContextCategories(),
                category.key()
        )) {
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                Optional<GriefPreventionClaimSummary> summary = integration.claimSummary(location, griefConfig);
                summary.ifPresent(value -> save(reportId, reporterUuid, reporterName, targetUuid, targetName, category, value));
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.WARNING, "GriefPrevention claim context failed: " + exception.getMessage());
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
            GriefPreventionClaimSummary summary
    ) {
        reportService.saveReportContext(reportId, "griefprevention", summary.summary(), summary.metadata())
                .thenRun(() -> auditService.recordEvent(AuditEvent.create(
                        AuditEventType.GRIEFPREVENTION_CONTEXT_CAPTURED,
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
                        "griefprevention report context",
                        AuditMetadata.create()
                                .put("claimPresent", summary.claimPresent())
                                .put("claimId", summary.claimId())
                                .put("adminClaim", summary.adminClaim())
                                .put("world", summary.world())
                                .put("x", summary.x())
                                .put("y", summary.y())
                                .put("z", summary.z())
                                .put("category", category.key())
                                .toJson(),
                        System.currentTimeMillis()
                )));
    }
}
