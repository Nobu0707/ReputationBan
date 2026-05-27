package dev.modplugin.reputationban.listener;

import dev.modplugin.reputationban.integration.placeholderapi.PlaceholderCacheService;
import dev.modplugin.reputationban.service.PlayerDataService;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerJoinListener implements Listener {
    private final PlayerDataService playerDataService;
    private final PlaceholderCacheService placeholderCacheService;
    private final Logger logger;

    public PlayerJoinListener(
            PlayerDataService playerDataService,
            PlaceholderCacheService placeholderCacheService,
            Logger logger
    ) {
        this.playerDataService = playerDataService;
        this.placeholderCacheService = placeholderCacheService;
        this.logger = logger;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        java.util.UUID uuid = event.getPlayer().getUniqueId();
        String name = event.getPlayer().getName();
        playerDataService.ensurePlayer(uuid, name)
                .thenRun(() -> placeholderCacheService.refreshPlayer(uuid, name))
                .exceptionally(throwable -> {
                    logger.log(Level.SEVERE, "Failed to ensure player record", throwable);
                    return null;
                });
    }
}
