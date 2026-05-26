package dev.modplugin.reputationban.listener;

import dev.modplugin.reputationban.service.PlayerDataService;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerJoinListener implements Listener {
    private final PlayerDataService playerDataService;
    private final Logger logger;

    public PlayerJoinListener(PlayerDataService playerDataService, Logger logger) {
        this.playerDataService = playerDataService;
        this.logger = logger;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerDataService.ensurePlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName())
                .exceptionally(throwable -> {
                    logger.log(Level.SEVERE, "Failed to ensure player record", throwable);
                    return null;
                });
    }
}
