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
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
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

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            java.util.Optional<CoreProtectAPI> api = integration.api(coreProtectConfig.minimumApiVersion());
            if (api.isEmpty()) {
                return;
            }
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> lookupAndSave(
                    api.get(),
                    reportId,
                    reporterUuid,
                    reporterName,
                    targetUuid,
                    targetName,
                    category,
                    radiusLocation,
                    coreProtectConfig
            ));
        }));
    }

    private void lookupAndSave(
            CoreProtectAPI api,
            long reportId,
            UUID reporterUuid,
            String reporterName,
            UUID targetUuid,
            String targetName,
            ReportCategory category,
            Location radiusLocation,
            PluginConfig.CoreProtectIntegrationConfig config
    ) {
        try {
            List<Integer> actions = CoreProtectEvidencePolicy.actionIds(config.includeActions());
            List<String[]> results = api.performLookup(
                    config.lookupSeconds(),
                    List.of(targetName),
                    null,
                    null,
                    null,
                    actions.isEmpty() ? null : actions,
                    config.radius(),
                    config.radius() > 0 ? radiusLocation : null
            );
            CoreProtectEvidenceSummary summary = summarize(api, results, radiusLocation, config, category.key());
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
                                    .toJson(),
                            System.currentTimeMillis()
                    )));
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "CoreProtect lookup failed: " + exception.getMessage());
        }
    }

    private CoreProtectEvidenceSummary summarize(
            CoreProtectAPI api,
            List<String[]> results,
            Location location,
            PluginConfig.CoreProtectIntegrationConfig config,
            String category
    ) {
        int available = results == null ? 0 : results.size();
        int count = CoreProtectEvidencePolicy.clampMaxResults(config.maxResults(), available);
        String world = location.getWorld() == null ? "unknown" : location.getWorld().getName();
        String header = count + " result(s) near " + world + " " + location.getBlockX() + " "
                + location.getBlockY() + " " + location.getBlockZ()
                + " within " + config.radius() + " blocks";
        if (count == 0) {
            return CoreProtectEvidenceSummary.empty(
                    world,
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ(),
                    config.radius(),
                    config.lookupSeconds(),
                    category
            );
        }

        List<String> lines = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            var parsed = api.parseResult(results.get(index));
            lines.add("#" + (index + 1)
                    + " action=" + parsed.getActionString()
                    + " player=" + parsed.getPlayer()
                    + " x=" + parsed.getX()
                    + " y=" + parsed.getY()
                    + " z=" + parsed.getZ()
                    + " type=" + parsed.getType());
        }
        String summary = CoreProtectEvidencePolicy.truncateSummary(header + "\n" + String.join("\n", lines), SUMMARY_MAX_LENGTH);
        return new CoreProtectEvidenceSummary(
                count,
                summary,
                List.copyOf(lines),
                CoreProtectEvidenceSummary.metadata(count, config.lookupSeconds(), config.radius(), category)
        );
    }
}
