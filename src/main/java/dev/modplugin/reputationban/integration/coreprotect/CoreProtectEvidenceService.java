package dev.modplugin.reputationban.integration.coreprotect;

import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.model.AuditEvent;
import dev.modplugin.reputationban.model.AuditEventType;
import dev.modplugin.reputationban.model.ReportCategory;
import dev.modplugin.reputationban.service.AuditService;
import dev.modplugin.reputationban.service.ReportService;
import dev.modplugin.reputationban.util.AuditMetadata;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public final class CoreProtectEvidenceService {
    private static final int SUMMARY_MAX_LENGTH = 1000;

    private final JavaPlugin plugin;
    private final CoreProtectIntegration integration;
    private final ReportService reportService;
    private final AuditService auditService;

    public CoreProtectEvidenceService(
            JavaPlugin plugin,
            CoreProtectIntegration integration,
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
            String worldName,
            int x,
            int y,
            int z,
            Location radiusLocation,
            PluginConfig config
    ) {
        PluginConfig.CoreProtectIntegrationConfig coreProtectConfig = config.coreProtectIntegration();
        if (!CoreProtectEvidencePolicy.shouldCapture(
                coreProtectConfig.enabled(),
                coreProtectConfig.reportContextEnabled(),
                coreProtectConfig.reportContextCategories(),
                category.key()
        )) {
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            java.util.Optional<CoreProtectReflectionAdapter.ApiHandle> api = integration.api(coreProtectConfig.minimumApiVersion());
            if (api.isEmpty()) {
                return;
            }
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> lookupAndSave(
                    api.get(),
                    reportId,
                    reporterUuid,
                    reporterName,
                    targetUuid,
                    targetName,
                    category,
                    worldName,
                    x,
                    y,
                    z,
                    radiusLocation,
                    coreProtectConfig
            ));
        });
    }

    private void lookupAndSave(
            CoreProtectReflectionAdapter.ApiHandle api,
            long reportId,
            UUID reporterUuid,
            String reporterName,
            UUID targetUuid,
            String targetName,
            ReportCategory category,
            String worldName,
            int x,
            int y,
            int z,
            Location radiusLocation,
            PluginConfig.CoreProtectIntegrationConfig config
    ) {
        try {
            List<Integer> actions = CoreProtectEvidencePolicy.actionIds(config.includeActions());
            CoreProtectLookupResult lookupResult = integration.performLookup(
                    api,
                    config.lookupSeconds(),
                    List.of(targetName),
                    actions,
                    config.radius(),
                    radiusLocation,
                    config.maxResults()
            );
            CoreProtectEvidenceSummary summary = summarize(lookupResult, worldName, x, y, z, config, category.key());
            if (summary.resultCount() <= 0) {
                return;
            }
            reportService.saveReportContext(reportId, "coreprotect", summary.summary(), summary.metadata())
                    .thenRun(() -> auditService.recordEvent(AuditEvent.create(
                            AuditEventType.COREPROTECT_CONTEXT_CAPTURED,
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
                            "coreprotect report context",
                            AuditMetadata.create()
                                    .put("resultCount", summary.resultCount())
                                    .put("category", category.key())
                                    .put("radius", config.radius())
                                    .put("lookupSeconds", config.lookupSeconds())
                                    .put("world", worldName)
                                    .put("x", x)
                                    .put("y", y)
                                    .put("z", z)
                                    .put("apiVersion", api.apiVersion())
                                    .toJson(),
                            System.currentTimeMillis()
                    )));
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "CoreProtect lookup failed: " + exception.getMessage());
        }
    }

    private CoreProtectEvidenceSummary summarize(
            CoreProtectLookupResult lookupResult,
            String world,
            int x,
            int y,
            int z,
            PluginConfig.CoreProtectIntegrationConfig config,
            String category
    ) {
        List<CoreProtectLookupEntry> entries = lookupResult.entries();
        int count = CoreProtectEvidencePolicy.clampMaxResults(config.maxResults(), entries.size());
        int resultCount = Math.max(lookupResult.totalResults(), entries.size());
        String header = "CoreProtect: 周辺ログ " + resultCount + "件 world=" + world
                + " x=" + x
                + " y=" + y
                + " z=" + z
                + " radius=" + config.radius();
        if (count == 0) {
            return new CoreProtectEvidenceSummary(
                    resultCount,
                    header,
                    List.of(),
                    CoreProtectEvidenceSummary.metadata(
                            resultCount,
                            config.lookupSeconds(),
                            config.radius(),
                            category,
                            world,
                            x,
                            y,
                            z,
                            lookupResult.apiVersion()
                    )
            );
        }

        List<String> lines = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            CoreProtectLookupEntry parsed = entries.get(index);
            lines.add("#" + (index + 1)
                    + " action=" + parsed.action()
                    + " player=" + parsed.player()
                    + " x=" + parsed.x()
                    + " y=" + parsed.y()
                    + " z=" + parsed.z()
                    + " type=" + parsed.type());
        }
        String summary = CoreProtectEvidencePolicy.truncateSummary(header + "\n" + String.join("\n", lines), SUMMARY_MAX_LENGTH);
        return new CoreProtectEvidenceSummary(
                resultCount,
                summary,
                List.copyOf(lines),
                CoreProtectEvidenceSummary.metadata(
                        resultCount,
                        config.lookupSeconds(),
                        config.radius(),
                        category,
                        world,
                        x,
                        y,
                        z,
                        lookupResult.apiVersion()
                )
        );
    }
}
