package dev.modplugin.reputationban.integration.worldguard;

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

public final class WorldGuardRegionContextService {
    private final JavaPlugin plugin;
    private final WorldGuardIntegration integration;
    private final ReportService reportService;
    private final AuditService auditService;

    public WorldGuardRegionContextService(
            JavaPlugin plugin,
            WorldGuardIntegration integration,
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
        PluginConfig.WorldGuardIntegrationConfig worldGuardConfig = config.worldGuardIntegration();
        if (!WorldGuardRegionPolicy.shouldCapture(
                worldGuardConfig.enabled(),
                worldGuardConfig.reportContextEnabled(),
                worldGuardConfig.reportContextCategories(),
                category.key()
        )) {
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                Optional<WorldGuardRegionSummary> summary = integration.regionSummary(location, worldGuardConfig);
                summary.ifPresent(value -> save(reportId, reporterUuid, reporterName, targetUuid, targetName, category, value));
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.WARNING, "WorldGuard region context failed: " + exception.getMessage());
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
            WorldGuardRegionSummary summary
    ) {
        reportService.saveReportContext(reportId, "worldguard", summary.summary(), summary.metadata())
                .thenRun(() -> auditService.recordEvent(AuditEvent.create(
                        AuditEventType.WORLDGUARD_CONTEXT_CAPTURED,
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
                        "worldguard report context",
                        AuditMetadata.create()
                                .put("regionCount", summary.regionCount())
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
