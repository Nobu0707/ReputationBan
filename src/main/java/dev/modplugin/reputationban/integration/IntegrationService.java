package dev.modplugin.reputationban.integration;

import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.integration.coreprotect.CoreProtectEvidenceService;
import dev.modplugin.reputationban.integration.coreprotect.CoreProtectIntegration;
import dev.modplugin.reputationban.integration.discordsrv.DiscordSrvAccountLinkService;
import dev.modplugin.reputationban.integration.discordsrv.DiscordSrvIntegration;
import dev.modplugin.reputationban.integration.discordsrv.DiscordSrvPolicy;
import dev.modplugin.reputationban.integration.discordsrv.DiscordSrvStatus;
import dev.modplugin.reputationban.integration.griefprevention.GriefPreventionClaimContextService;
import dev.modplugin.reputationban.integration.griefprevention.GriefPreventionClaimSummary;
import dev.modplugin.reputationban.integration.griefprevention.GriefPreventionIntegration;
import dev.modplugin.reputationban.integration.griefprevention.GriefPreventionStatus;
import dev.modplugin.reputationban.integration.luckperms.LuckPermsIntegration;
import dev.modplugin.reputationban.integration.luckperms.LuckPermsTrustService;
import dev.modplugin.reputationban.integration.placeholderapi.PlaceholderApiIntegration;
import dev.modplugin.reputationban.integration.placeholderapi.PlaceholderApiStatus;
import dev.modplugin.reputationban.integration.placeholderapi.PlaceholderCacheService;
import dev.modplugin.reputationban.integration.placeholderapi.PlaceholderValueProvider;
import dev.modplugin.reputationban.integration.worldguard.WorldGuardIntegration;
import dev.modplugin.reputationban.integration.worldguard.WorldGuardRegionContextService;
import dev.modplugin.reputationban.integration.worldguard.WorldGuardRegionSummary;
import dev.modplugin.reputationban.integration.worldguard.WorldGuardStatus;
import dev.modplugin.reputationban.model.ReportCategory;
import dev.modplugin.reputationban.service.AuditService;
import dev.modplugin.reputationban.service.PlayerDataService;
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
    private final WorldGuardIntegration worldGuardIntegration;
    private final GriefPreventionIntegration griefPreventionIntegration;
    private final PlaceholderCacheService placeholderCacheService;
    private final PlaceholderApiIntegration placeholderApiIntegration;
    private final DiscordSrvIntegration discordSrvIntegration;
    private final CoreProtectEvidenceService coreProtectEvidenceService;
    private final WorldGuardRegionContextService worldGuardRegionContextService;
    private final GriefPreventionClaimContextService griefPreventionClaimContextService;
    private final DiscordSrvAccountLinkService discordSrvAccountLinkService;

    public IntegrationService(
            JavaPlugin plugin,
            Supplier<PluginConfig> configSupplier,
            PlayerDataService playerDataService,
            ReportService reportService,
            AuditService auditService
    ) {
        this.plugin = plugin;
        this.configSupplier = configSupplier;
        this.luckPermsIntegration = new LuckPermsIntegration(plugin);
        this.coreProtectIntegration = new CoreProtectIntegration(plugin);
        this.worldGuardIntegration = new WorldGuardIntegration(plugin);
        this.griefPreventionIntegration = new GriefPreventionIntegration(plugin);
        this.placeholderCacheService = new PlaceholderCacheService(plugin, playerDataService, configSupplier);
        PlaceholderValueProvider placeholderValueProvider = new PlaceholderValueProvider(
                placeholderCacheService,
                configSupplier,
                plugin.getPluginMeta().getVersion()
        );
        this.placeholderApiIntegration = new PlaceholderApiIntegration(plugin, configSupplier, placeholderValueProvider);
        this.discordSrvIntegration = new DiscordSrvIntegration(plugin);
        this.coreProtectEvidenceService = new CoreProtectEvidenceService(plugin, coreProtectIntegration, reportService, auditService);
        this.worldGuardRegionContextService = new WorldGuardRegionContextService(
                plugin,
                worldGuardIntegration,
                reportService,
                auditService
        );
        this.griefPreventionClaimContextService = new GriefPreventionClaimContextService(
                plugin,
                griefPreventionIntegration,
                reportService,
                auditService
        );
        this.discordSrvAccountLinkService = new DiscordSrvAccountLinkService(
                plugin,
                discordSrvIntegration,
                reportService,
                auditService
        );
    }

    public List<IntegrationStatus> statuses() {
        PluginConfig config = configSupplier.get();
        return List.of(
                luckPermsIntegration.status(config),
                coreProtectIntegration.status(config),
                worldGuardIntegration.status(config),
                griefPreventionIntegration.status(config),
                placeholderApiStatus(),
                discordSrvIntegration.status(config)
        );
    }

    public List<String> statusLines() {
        PluginConfig config = configSupplier.get();
        PluginConfig.LuckPermsIntegrationConfig luckPermsConfig = config.luckPermsIntegration();
        PluginConfig.CoreProtectIntegrationConfig coreProtectConfig = config.coreProtectIntegration();
        PluginConfig.WorldGuardIntegrationConfig worldGuardConfig = config.worldGuardIntegration();
        PluginConfig.GriefPreventionIntegrationConfig griefConfig = config.griefPreventionIntegration();
        PluginConfig.PlaceholderApiIntegrationConfig placeholderConfig = config.placeholderApiIntegration();
        PluginConfig.DiscordSrvIntegrationConfig discordConfig = config.discordSrvIntegration();
        IntegrationStatus luckPerms = luckPermsIntegration.status(config);
        IntegrationStatus coreProtect = coreProtectIntegration.status(config);
        WorldGuardStatus worldGuard = worldGuardIntegration.detail(config);
        GriefPreventionStatus griefPrevention = griefPreventionIntegration.detail(config);
        PlaceholderApiStatus placeholderApi = placeholderApiIntegration.status();
        DiscordSrvStatus discordSrv = discordSrvIntegration.detail(config);

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
                "  maxResults=" + coreProtectConfig.maxResults(),
                "",
                "WorldGuard:",
                "  configuredEnabled=" + worldGuard.configuredEnabled(),
                "  worldEditPresent=" + worldGuard.worldEditPresent(),
                "  worldGuardPresent=" + worldGuard.worldGuardPresent(),
                "  apiAvailable=" + worldGuard.apiAvailable(),
                "  active=" + worldGuard.active(),
                "  reportContextEnabled=" + worldGuard.reportContextEnabled(),
                "  maxRegions=" + worldGuardConfig.maxRegions(),
                "",
                "GriefPrevention:",
                "  configuredEnabled=" + griefPrevention.configuredEnabled(),
                "  pluginPresent=" + griefPrevention.pluginPresent(),
                "  apiAvailable=" + griefPrevention.apiAvailable(),
                "  active=" + griefPrevention.active(),
                "  reportContextEnabled=" + griefPrevention.reportContextEnabled(),
                "  includeClaimOwner=" + griefConfig.includeClaimOwner(),
                "  includeTrustCounts=" + griefConfig.includeTrustCounts(),
                "  includeBoundaries=" + griefConfig.includeBoundaries(),
                "",
                "PlaceholderAPI:",
                "  configuredEnabled=" + placeholderApi.configuredEnabled(),
                "  pluginPresent=" + placeholderApi.pluginPresent(),
                "  apiAvailable=" + placeholderApi.apiAvailable(),
                "  active=" + placeholderApi.active(),
                "  identifier=" + placeholderApi.identifier(),
                "  cacheRefreshSeconds=" + placeholderConfig.cacheRefreshSeconds(),
                "",
                "DiscordSRV:",
                "  configuredEnabled=" + discordSrv.configuredEnabled(),
                "  pluginPresent=" + discordSrv.pluginPresent(),
                "  apiAvailable=" + discordSrv.apiAvailable(),
                "  active=" + discordSrv.active(),
                "  accountLinkContextEnabled=" + discordSrv.accountLinkContextEnabled(),
                "  includeDiscordIds=" + discordSrv.includeDiscordIds(),
                "  notificationsEnabled=" + discordSrv.notificationsEnabled(),
                "  notificationChannel=" + fallback(discordConfig.notificationChannel())
        );
    }

    public IntegrationTestResult test(CommandSender sender) {
        PluginConfig config = configSupplier.get();
        PluginConfig.LuckPermsIntegrationConfig luckPermsConfig = config.luckPermsIntegration();
        PluginConfig.CoreProtectIntegrationConfig coreProtectConfig = config.coreProtectIntegration();
        PluginConfig.WorldGuardIntegrationConfig worldGuardConfig = config.worldGuardIntegration();
        PluginConfig.GriefPreventionIntegrationConfig griefConfig = config.griefPreventionIntegration();
        PluginConfig.DiscordSrvIntegrationConfig discordConfig = config.discordSrvIntegration();
        IntegrationStatus luckPerms = luckPermsIntegration.status(config);
        IntegrationStatus coreProtect = coreProtectIntegration.status(config);
        WorldGuardStatus worldGuard = worldGuardIntegration.detail(config);
        GriefPreventionStatus griefPrevention = griefPreventionIntegration.detail(config);
        PlaceholderApiStatus placeholderApi = placeholderApiIntegration.status();
        DiscordSrvStatus discordSrv = discordSrvIntegration.detail(config);
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

        lines.add("");
        lines.add("WorldGuard:");
        lines.add("  pluginPresent=" + worldGuard.worldGuardPresent());
        lines.add("  worldEditPresent=" + worldGuard.worldEditPresent());
        lines.add("  apiAvailable=" + worldGuard.apiAvailable());
        lines.add("  active=" + worldGuard.active());
        if (sender instanceof Player player && worldGuard.active()) {
            WorldGuardRegionSummary summary = worldGuardIntegration.regionSummary(
                    player.getLocation(),
                    worldGuardConfig
            ).orElse(null);
            if (summary == null) {
                lines.add("  currentRegions=-");
                lines.add("  regionCount=0");
            } else {
                lines.add("  currentRegions=" + summary.regions().stream()
                        .map(entry -> entry.id())
                        .filter(value -> value != null && !value.isBlank())
                        .reduce((left, right) -> left + "," + right)
                        .orElse("-"));
                lines.add("  regionCount=" + summary.regionCount());
            }
        } else if (sender instanceof Player) {
            lines.add("  currentRegions=-");
            lines.add("  regionCount=0");
        } else {
            lines.add("  WorldGuard region test: console sender, skipped");
        }

        lines.add("");
        lines.add("GriefPrevention:");
        lines.add("  pluginPresent=" + griefPrevention.pluginPresent());
        lines.add("  apiAvailable=" + griefPrevention.apiAvailable());
        lines.add("  active=" + griefPrevention.active());
        if (sender instanceof Player player && griefPrevention.active()) {
            GriefPreventionClaimSummary summary = griefPreventionIntegration.claimSummary(
                    player.getLocation(),
                    griefConfig
            ).orElse(null);
            if (summary == null) {
                lines.add("  currentClaimPresent=false");
                lines.add("  adminClaim=false");
                lines.add("  claimId=-");
            } else {
                lines.add("  currentClaimPresent=" + summary.claimPresent());
                lines.add("  adminClaim=" + summary.adminClaim());
                lines.add("  claimId=" + fallback(summary.claimId()));
            }
        } else if (sender instanceof Player) {
            lines.add("  currentClaimPresent=false");
            lines.add("  adminClaim=false");
            lines.add("  claimId=-");
        } else {
            lines.add("  GriefPrevention claim test: console sender, skipped");
        }

        lines.add("");
        lines.add("PlaceholderAPI:");
        lines.add("  pluginPresent=" + placeholderApi.pluginPresent());
        lines.add("  active=" + placeholderApi.active());
        lines.add("  identifier=" + placeholderApi.identifier());
        lines.add("  registered=" + placeholderApi.registered());
        if (sender instanceof Player) {
            lines.add("  sample score placeholder=" + placeholderApiIntegration.sampleFor(sender));
        } else {
            lines.add("  sample version placeholder=" + placeholderApiIntegration.sampleFor(sender));
        }

        lines.add("");
        lines.add("DiscordSRV:");
        lines.add("  pluginPresent=" + discordSrv.pluginPresent());
        lines.add("  apiAvailable=" + discordSrv.apiAvailable());
        lines.add("  active=" + discordSrv.active());
        lines.add("  accountLinkAvailable=" + discordSrv.accountLinkAvailable());
        if (sender instanceof Player player && discordSrv.active() && discordSrv.accountLinkAvailable()) {
            String discordId = discordSrvIntegration.discordId(player.getUniqueId()).orElse("");
            lines.add("  senderLinked=" + DiscordSrvPolicy.linked(discordId));
            lines.add("  discordId=" + DiscordSrvPolicy.discordIdDisplay(discordConfig.includeDiscordIds(), discordId));
        } else if (sender instanceof Player) {
            lines.add("  senderLinked=false");
            lines.add("  discordId=hidden");
        } else {
            lines.add("  DiscordSRV account link test: console sender, skipped");
        }
        lines.add("  notificationsEnabled=" + discordSrv.notificationsEnabled());

        return new IntegrationTestResult(
                List.copyOf(lines),
                luckPerms.active(),
                coreProtect.active(),
                worldGuard.active(),
                griefPrevention.active(),
                placeholderApi.active(),
                discordSrv.active(),
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

    public void captureWorldGuardContext(
            long reportId,
            Player reporter,
            UUID targetUuid,
            String targetName,
            ReportCategory category
    ) {
        Location location = reporter.getLocation().clone();
        worldGuardRegionContextService.captureAfterReport(
                reportId,
                reporter.getUniqueId(),
                reporter.getName(),
                targetUuid,
                targetName,
                category,
                location,
                configSupplier.get()
        );
    }

    public void captureGriefPreventionContext(
            long reportId,
            Player reporter,
            UUID targetUuid,
            String targetName,
            ReportCategory category
    ) {
        Location location = reporter.getLocation().clone();
        griefPreventionClaimContextService.captureAfterReport(
                reportId,
                reporter.getUniqueId(),
                reporter.getName(),
                targetUuid,
                targetName,
                category,
                location,
                configSupplier.get()
        );
    }

    public void captureDiscordSrvContext(
            long reportId,
            Player reporter,
            UUID targetUuid,
            String targetName,
            ReportCategory category
    ) {
        discordSrvAccountLinkService.captureAfterReport(
                reportId,
                reporter.getUniqueId(),
                reporter.getName(),
                targetUuid,
                targetName,
                category,
                configSupplier.get()
        );
    }

    public void logStartupStatuses() {
        for (IntegrationStatus status : statuses()) {
            plugin.getLogger().info(status.startupLine());
        }
    }

    public void start() {
        placeholderCacheService.start();
        placeholderApiIntegration.register();
    }

    public void reload() {
        placeholderCacheService.start();
        placeholderApiIntegration.register();
    }

    public void shutdown() {
        placeholderApiIntegration.unregister();
        placeholderCacheService.stop();
    }

    public PlaceholderCacheService placeholderCacheService() {
        return placeholderCacheService;
    }

    public PlaceholderApiStatus placeholderApiStatusDetail() {
        return placeholderApiIntegration.status();
    }

    public DiscordSrvStatus discordSrvStatusDetail() {
        return discordSrvIntegration.detail(configSupplier.get());
    }

    public List<String> placeholderExamples() {
        return placeholderApiIntegration.placeholderExamples();
    }

    public void refreshPlaceholderCache(UUID uuid, String name) {
        placeholderCacheService.refreshPlayer(uuid, name);
    }

    public void refreshOnlinePlaceholderCache() {
        placeholderCacheService.refreshOnlinePlayers();
    }

    private IntegrationStatus placeholderApiStatus() {
        PlaceholderApiStatus detail = placeholderApiIntegration.status();
        return new IntegrationStatus(
                ExternalIntegrationType.PLACEHOLDERAPI,
                detail.configuredEnabled(),
                detail.pluginPresent(),
                detail.apiAvailable(),
                "",
                detail.active(),
                detail.message()
        );
    }

    private static String fallback(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    public record IntegrationTestResult(
            List<String> lines,
            boolean luckPermsActive,
            boolean coreProtectActive,
            boolean worldGuardActive,
            boolean griefPreventionActive,
            boolean placeholderApiActive,
            boolean discordSrvActive,
            String senderType
    ) {
    }
}
