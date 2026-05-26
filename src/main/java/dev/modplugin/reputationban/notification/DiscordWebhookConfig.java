package dev.modplugin.reputationban.notification;

import java.util.EnumMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

public final class DiscordWebhookConfig {
    public static final String DEFAULT_USERNAME = "ReputationBan";
    public static final int DEFAULT_TIMEOUT_SECONDS = 5;
    public static final int DEFAULT_FAILURE_LOG_SECONDS = 60;

    private final boolean enabled;
    private final String url;
    private final String username;
    private final int timeoutSeconds;
    private final int rateLimitFailureLogSeconds;
    private final boolean includeReasons;
    private final boolean includePlayerUuids;
    private final Map<NotificationEventType, Boolean> events;

    public DiscordWebhookConfig(
            boolean enabled,
            String url,
            String username,
            int timeoutSeconds,
            int rateLimitFailureLogSeconds,
            boolean includeReasons,
            boolean includePlayerUuids,
            Map<NotificationEventType, Boolean> events
    ) {
        this.enabled = enabled;
        this.url = url == null ? "" : url.trim();
        this.username = username == null || username.isBlank() ? DEFAULT_USERNAME : username.trim();
        this.timeoutSeconds = clamp(timeoutSeconds, 1, 30);
        this.rateLimitFailureLogSeconds = Math.max(1, rateLimitFailureLogSeconds);
        this.includeReasons = includeReasons;
        this.includePlayerUuids = includePlayerUuids;
        this.events = new EnumMap<>(defaultEvents());
        if (events != null) {
            this.events.putAll(events);
        }
    }

    public static DiscordWebhookConfig defaults() {
        return new DiscordWebhookConfig(
                false,
                "",
                DEFAULT_USERNAME,
                DEFAULT_TIMEOUT_SECONDS,
                DEFAULT_FAILURE_LOG_SECONDS,
                true,
                false,
                defaultEvents()
        );
    }

    public static DiscordWebhookConfig legacyBoolean(boolean enabled) {
        return new DiscordWebhookConfig(
                enabled,
                "",
                DEFAULT_USERNAME,
                DEFAULT_TIMEOUT_SECONDS,
                DEFAULT_FAILURE_LOG_SECONDS,
                true,
                false,
                defaultEvents()
        );
    }

    public static DiscordWebhookConfig fromConfiguration(ConfigurationSection root) {
        if (root == null) {
            return defaults();
        }
        if (!root.isConfigurationSection("discord-webhook")) {
            return legacyBoolean(root.getBoolean("discord-webhook", false));
        }
        ConfigurationSection section = root.getConfigurationSection("discord-webhook");
        if (section == null) {
            return defaults();
        }
        Map<NotificationEventType, Boolean> loadedEvents = defaultEvents();
        ConfigurationSection eventSection = section.getConfigurationSection("events");
        if (eventSection != null) {
            for (NotificationEventType type : NotificationEventType.values()) {
                loadedEvents.put(type, eventSection.getBoolean(type.configKey(), loadedEvents.get(type)));
            }
        }
        return new DiscordWebhookConfig(
                section.getBoolean("enabled", false),
                section.getString("url", ""),
                section.getString("username", DEFAULT_USERNAME),
                section.getInt("timeout-seconds", DEFAULT_TIMEOUT_SECONDS),
                section.getInt("rate-limit-failure-log-seconds", DEFAULT_FAILURE_LOG_SECONDS),
                section.getBoolean("include-reasons", true),
                section.getBoolean("include-player-uuids", false),
                loadedEvents
        );
    }

    public DiscordWebhookConfig withEvent(NotificationEventType type, boolean eventEnabled) {
        Map<NotificationEventType, Boolean> updated = new EnumMap<>(events);
        updated.put(type, eventEnabled);
        return new DiscordWebhookConfig(
                enabled,
                url,
                username,
                timeoutSeconds,
                rateLimitFailureLogSeconds,
                includeReasons,
                includePlayerUuids,
                updated
        );
    }

    public boolean enabled() {
        return enabled;
    }

    public String url() {
        return url;
    }

    public String username() {
        return username;
    }

    public int timeoutSeconds() {
        return timeoutSeconds;
    }

    public int rateLimitFailureLogSeconds() {
        return rateLimitFailureLogSeconds;
    }

    public boolean includeReasons() {
        return includeReasons;
    }

    public boolean includePlayerUuids() {
        return includePlayerUuids;
    }

    public boolean hasUsableUrl() {
        return !url.isBlank();
    }

    public boolean eventEnabled(NotificationEventType type) {
        return type != null && events.getOrDefault(type, false);
    }

    private static Map<NotificationEventType, Boolean> defaultEvents() {
        Map<NotificationEventType, Boolean> defaults = new EnumMap<>(NotificationEventType.class);
        for (NotificationEventType type : NotificationEventType.values()) {
            defaults.put(type, type != NotificationEventType.RECOVERY_SUMMARY);
        }
        return defaults;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
