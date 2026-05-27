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
import org.bukkit.command.CommandSender;
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

    public List<String> statusLines() {
        PluginConfig config = configSupplier.get();
        PluginConfig.LuckPermsIntegrationConfig luckPermsConfig = config.luckPermsIntegration();
        PluginConfig.CoreProtectIntegrationConfig coreProtectConfig = config.coreProtectIntegration();
        IntegrationStatus luckPerms = luckPermsIntegration.status(config);
        IntegrationStatus coreProtect = coreProtectIntegration.status(config);

        return List.of(
                "LuckPerms:",
                "  configuredEnabled=" + luckPerms.configuredEnabled(),
                "  pluginPresent=" + luckPerms.pluginPresent(),
                "  apiAvailable=" + luckPerms.apiAvailable(),
                "  active=" + luckPerms.active(),
                "  defaultWeight=" + luckPermsConfig.defaultWeight(),
                "  useGroupWeight=" + luckPermsConfig.useGroupWeight(),
                "  applyWeightToDeduction=" + luckPermsConfig.applyWeightToDeduction(),
                "  configuredGroups count=" + luckPermsConfig.groupWeights().size(),
                "  bypassGroups count=" + luckPermsConfig.bypassGroups().size(),
                "",
                "CoreProtect:",
                "  configuredEnabled=" + coreProtect.configuredEnabled(),
                "  pluginPresent=" + coreProtect.pluginPresent(),
                "  apiAvailable=" + coreProtect.apiAvailable(),
                "  apiVersion=" + fallback(coreProtect.apiVersion()),
                "  active=" + coreProtect.active(),
                "  minimumApiVersion=" + coreProtectConfig.minimumApiVersion(),
                "  reportContextEnabled=" + coreProtectConfig.reportContextEnabled(),
                "  lookupSeconds=" + coreProtectConfig.lookupSeconds(),
                "  radius=" + coreProtectConfig.radius(),
                "  maxResults=" + coreProtectConfig.maxResults()
        );
    }

    public IntegrationTestResult test(CommandSender sender) {
        PluginConfig config = configSupplier.get();
        PluginConfig.LuckPermsIntegrationConfig luckPermsConfig = config.luckPermsIntegration();
        PluginConfig.CoreProtectIntegrationConfig coreProtectConfig = config.coreProtectIntegration();
        IntegrationStatus luckPerms = luckPermsIntegration.status(config);
        IntegrationStatus coreProtect = coreProtectIntegration.status(config);
        List<String> lines = new java.util.ArrayList<>();

        lines.add("LuckPerms:");
        lines.add("  pluginPresent=" + luckPerms.pluginPresent());
        lines.add("  apiAvailable=" + luckPerms.apiAvailable());
        if (sender instanceof Player player) {
            LuckPermsTrustService trust = luckPermsTrust(player.getUniqueId());
            lines.add("  primaryGroup=" + fallback(trust.primaryGroup()));
            lines.add("  reporterWeight=" + trust.reporterWeight());
            lines.add("  bypassGroup=" + trust.bypassGroup());
        } else {
            lines.add("  LuckPerms user test: console sender, skipped");
        }

        boolean coreProtectMeetsMinimum = coreProtect.apiAvailable()
                && !coreProtect.apiVersion().isBlank()
                && Integer.parseInt(coreProtect.apiVersion()) >= coreProtectConfig.minimumApiVersion();
        lines.add("");
        lines.add("CoreProtect:");
        lines.add("  pluginPresent=" + coreProtect.pluginPresent());
        lines.add("  apiAvailable=" + coreProtect.apiAvailable());
        lines.add("  apiVersion=" + fallback(coreProtect.apiVersion()));
        lines.add("  minimumApiVersion=" + coreProtectConfig.minimumApiVersion());
        lines.add("  minimumApiVersionMet=" + coreProtectMeetsMinimum);
        lines.add("  reportContextEnabled=" + coreProtectConfig.reportContextEnabled());
        lines.add("  lookupSeconds=" + coreProtectConfig.lookupSeconds());
        lines.add("  radius=" + coreProtectConfig.radius());
        lines.add("  maxResults=" + coreProtectConfig.maxResults());
        lines.add("  lookup=skipped (Phase 17 default)");

        return new IntegrationTestResult(
                List.copyOf(lines),
                luckPerms.active(),
                coreProtect.active(),
                sender instanceof Player ? "player" : "console"
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

    private static String fallback(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    public record IntegrationTestResult(
            List<String> lines,
            boolean luckPermsActive,
            boolean coreProtectActive,
            String senderType
    ) {
    }
}
