package dev.modplugin.reputationban.integration.placeholderapi;

import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.model.PlayerRecord;
import dev.modplugin.reputationban.service.PlayerDataService;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class PlaceholderCacheService {
    private final JavaPlugin plugin;
    private final PlayerDataService playerDataService;
    private final Supplier<PluginConfig> configSupplier;
    private final ConcurrentMap<UUID, PlayerReputationSummary> cache = new ConcurrentHashMap<>();
    private BukkitTask refreshTask;

    public PlaceholderCacheService(
            JavaPlugin plugin,
            PlayerDataService playerDataService,
            Supplier<PluginConfig> configSupplier
    ) {
        this.plugin = plugin;
        this.playerDataService = playerDataService;
        this.configSupplier = configSupplier;
    }

    public void start() {
        stop();
        refreshOnlinePlayers();
        int refreshSeconds = configSupplier.get().placeholderApiIntegration().cacheRefreshSeconds();
        if (refreshSeconds <= 0) {
            return;
        }
        long periodTicks = Math.max(1L, refreshSeconds) * 20L;
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshOnlinePlayers, periodTicks, periodTicks);
    }

    public void stop() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    public Optional<PlayerReputationSummary> cached(UUID uuid) {
        return Optional.ofNullable(cache.get(uuid));
    }

    public void refreshPlayer(UUID uuid, String name) {
        playerDataService.getPlayerRecord(uuid)
                .thenAccept(record -> updateCache(uuid, name, record))
                .exceptionally(throwable -> {
                    plugin.getLogger().log(Level.WARNING, "Failed to refresh PlaceholderAPI cache for " + name, throwable);
                    return null;
                });
    }

    public void refreshOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshPlayer(player.getUniqueId(), player.getName());
        }
    }

    private void updateCache(UUID uuid, String name, Optional<PlayerRecord> record) {
        long now = System.currentTimeMillis();
        if (record.isPresent()) {
            cache.put(uuid, PlayerReputationSummary.fromRecord(record.get(), configSupplier.get().maxScore(), now));
            return;
        }
        cache.put(uuid, new PlayerReputationSummary(
                uuid,
                name,
                configSupplier.get().initialScore(),
                configSupplier.get().maxScore(),
                0,
                0,
                null,
                null,
                now
        ));
    }
}
