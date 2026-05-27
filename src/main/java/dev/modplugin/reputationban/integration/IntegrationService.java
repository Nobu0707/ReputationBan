package dev.modplugin.reputationban.integration;

import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.integration.coreprotect.CoreProtectEvidenceService;
import dev.modplugin.reputationban.integration.coreprotect.CoreProtectIntegration;
import dev.modplugin.reputationban.integration.luckperms.LuckPermsIntegration;
import dev.modplugin.reputationban.integration.luckperms.LuckPermsTrustService;
import dev.modplugin.reputationban.model.ReportCategory;
import dev.modplugin.reputationban.service.AuditService;
import dev.modplugin.reputationban.service.ReportService;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class IntegrationService {
    private final JavaPlugin plugin;
    private final Supplier<PluginConfig> configSupplier;
    private final LuckPermsIntegration luckPermsIntegration;
    private final CoreProtectIntegration coreProtectIntegration;
    private final CoreProtectEvidenceService coreProtectEvidenceService;

    public IntegrationService(
            JavaPlugin plugin,
            Supplier<PluginConfig> configSupplier,
            ReportService reportService,
            AuditService auditService
    ) {
        this.plugin = plugin;
        this.configSupplier = configSupplier;
        this.luckPermsIntegration = new LuckPermsIntegration(plugin);
        this.coreProtectIntegration = new CoreProtectIntegration(plugin);
        this.coreProtectEvidenceService = new CoreProtectEvidenceService(plugin, coreProtectIntegration, reportService, auditService);
    }

    public List<IntegrationStatus> statuses() {
        PluginConfig config = configSupplier.get();
        return List.of(
                luckPermsIntegration.status(config),
                coreProtectIntegration.status(config)
        );
    }

    public LuckPermsTrustService luckPermsTrust(UUID playerUuid) {
        return luckPermsIntegration.trustFor(playerUuid, configSupplier.get());
    }

    public boolean isLuckPermsBypassGroup(UUID playerUuid) {
        return luckPermsTrust(playerUuid).bypassGroup();
    }

    public void captureCoreProtectContext(
            long reportId,
            Player reporter,
            UUID targetUuid,
            String targetName,
            ReportCategory category
    ) {
        Location currentLocation = reporter.getLocation();
        String worldName = currentLocation.getWorld() == null ? "unknown" : currentLocation.getWorld().getName();
        int x = currentLocation.getBlockX();
        int y = currentLocation.getBlockY();
        int z = currentLocation.getBlockZ();
        Location location = currentLocation.clone();
        coreProtectEvidenceService.captureAfterReport(
                reportId,
                reporter.getUniqueId(),
                reporter.getName(),
                targetUuid,
                targetName,
                category,
                worldName,
                x,
                y,
                z,
                location,
                configSupplier.get()
        );
    }

    public void logStartupStatuses() {
        for (IntegrationStatus status : statuses()) {
            plugin.getLogger().info(status.startupLine());
        }
    }
}
