package dev.modplugin.reputationban.integration.placeholderapi;

import dev.modplugin.reputationban.config.PluginConfig;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.bukkit.entity.Player;

public final class PlaceholderValueProvider {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    private static final List<String> KEYS = List.of(
            "score",
            "max_score",
            "score_percent",
            "status",
            "ban_count",
            "false_report_count",
            "report_banned",
            "report_banned_until",
            "last_seen",
            "version"
    );

    private final Function<UUID, Optional<PlayerReputationSummary>> cacheLookup;
    private final Supplier<PluginConfig> configSupplier;
    private final String pluginVersion;

    public PlaceholderValueProvider(
            PlaceholderCacheService cacheService,
            Supplier<PluginConfig> configSupplier,
            String pluginVersion
    ) {
        this(cacheService::cached, configSupplier, pluginVersion);
    }

    public PlaceholderValueProvider(
            Function<UUID, Optional<PlayerReputationSummary>> cacheLookup,
            Supplier<PluginConfig> configSupplier,
            String pluginVersion
    ) {
        this.cacheLookup = cacheLookup;
        this.configSupplier = configSupplier;
        this.pluginVersion = pluginVersion;
    }

    public String value(Player player, String params) {
        if (player == null) {
            return value((UUID) null, params);
        }
        return value(player.getUniqueId(), params);
    }

    public String value(UUID playerUuid, String params) {
        String key = normalize(params);
        if ("version".equals(key)) {
            return pluginVersion;
        }
        String unknown = unknown();
        if (playerUuid == null) {
            return unknown;
        }
        PlayerReputationSummary summary = cacheLookup.apply(playerUuid).orElse(null);
        if (summary == null) {
            return unknown;
        }
        return switch (key) {
            case "score" -> Integer.toString(summary.score());
            case "max_score" -> Integer.toString(summary.maxScore());
            case "score_percent" -> Integer.toString(summary.scorePercent());
            case "status" -> PlaceholderStatusPolicy.status(summary.score(), configSupplier.get().scoreThresholds());
            case "ban_count" -> Integer.toString(summary.banCount());
            case "false_report_count" -> Integer.toString(summary.falseReportCount());
            case "report_banned" -> Boolean.toString(summary.reportBanned(System.currentTimeMillis()));
            case "report_banned_until" -> summary.reportBanned(System.currentTimeMillis())
                    && summary.reportBannedUntil() != null
                    ? FORMATTER.format(Instant.ofEpochMilli(summary.reportBannedUntil()))
                    : unknown;
            case "last_seen" -> summary.lastSeen() == null ? unknown : FORMATTER.format(Instant.ofEpochMilli(summary.lastSeen()));
            default -> unknown;
        };
    }

    public List<String> placeholderKeys() {
        return KEYS;
    }

    private String unknown() {
        String value = configSupplier.get().placeholderApiIntegration().showUnknownAs();
        return value == null ? "-" : value;
    }

    private static String normalize(String params) {
        return params == null ? "" : params.trim().toLowerCase(Locale.ROOT);
    }
}
