package dev.modplugin.reputationban.service;

import dev.modplugin.reputationban.ReputationBanPlugin;
import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.model.TargetProtectionResult;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public final class TargetProtectionService {
    private final ReputationBanPlugin plugin;
    private volatile PluginConfig config;

    public TargetProtectionService(ReputationBanPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void updateConfig(PluginConfig config) {
        this.config = config;
    }

    public CompletableFuture<TargetProtectionResult> check(UUID targetUuid) {
        return plugin.supplySync(() -> localProtection(targetUuid))
                .thenCompose(result -> result.protectedTarget()
                        ? CompletableFuture.completedFuture(result)
                        : luckPermsOfflineProtection(targetUuid));
    }

    private TargetProtectionResult localProtection(UUID targetUuid) {
        Player online = Bukkit.getPlayer(targetUuid);
        if (online != null) {
            if (online.hasPermission("reputationban.bypass")) {
                return TargetProtectionResult.protectedBy("permission");
            }
            if (online.isOp()) {
                return TargetProtectionResult.protectedBy("operator");
            }
            if (plugin.integrationService().isLuckPermsBypassGroup(targetUuid)) {
                return TargetProtectionResult.protectedBy("luckperms-bypass-group");
            }
            return TargetProtectionResult.unprotected();
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(targetUuid);
        if (offline.isOp()) {
            return TargetProtectionResult.protectedBy("offline-operator");
        }
        if (!config.luckPermsIntegration().offlineLookupEnabled()
                && plugin.integrationService().isLuckPermsBypassGroup(targetUuid)) {
            return TargetProtectionResult.protectedBy("luckperms-bypass-group");
        }
        return TargetProtectionResult.unprotected();
    }

    private CompletableFuture<TargetProtectionResult> luckPermsOfflineProtection(UUID targetUuid) {
        PluginConfig.LuckPermsIntegrationConfig luckPerms = config.luckPermsIntegration();
        if (!luckPerms.enabled() || luckPerms.bypassGroups().isEmpty() || !luckPerms.offlineLookupEnabled()) {
            return CompletableFuture.completedFuture(TargetProtectionResult.unprotected());
        }

        CompletableFuture<Boolean> lookup = plugin.supplySync(() -> plugin.integrationService().loadLuckPermsBypassGroup(targetUuid))
                .thenCompose(future -> future)
                .orTimeout(luckPerms.offlineLookupTimeoutMillis(), TimeUnit.MILLISECONDS);

        return lookup.handle((bypass, throwable) -> {
            if (throwable != null) {
                plugin.getLogger().warning("LuckPerms offline bypass lookup failed for " + targetUuid + ": " + throwable.getMessage());
                if (config.luckPermsIntegration().offlineLookupFailClosedForBypass()) {
                    return TargetProtectionResult.protectedByLookupFailure("luckperms-lookup-failed");
                }
                return TargetProtectionResult.unprotected();
            }
            return Boolean.TRUE.equals(bypass)
                    ? TargetProtectionResult.protectedBy("luckperms-bypass-group")
                    : TargetProtectionResult.unprotected();
        });
    }
}
