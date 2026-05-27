package dev.modplugin.reputationban.integration.discordsrv;

import dev.modplugin.reputationban.notification.NotificationEventType;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DiscordSrvPolicy {
    private static final int MAX_MESSAGE_LENGTH = 1800;

    private DiscordSrvPolicy() {
    }

    public static boolean shouldCapture(boolean enabled, boolean contextEnabled, List<String> categories, String category) {
        if (!enabled || !contextEnabled || category == null) {
            return false;
        }
        return categories.stream().anyMatch(value -> value.equalsIgnoreCase(category));
    }

    public static boolean linked(String discordId) {
        return discordId != null && !discordId.isBlank();
    }

    public static String discordIdDisplay(boolean includeDiscordIds, String discordId) {
        if (!includeDiscordIds) {
            return "hidden";
        }
        return linked(discordId) ? discordId : "-";
    }

    public static boolean shouldNotify(
            boolean enabled,
            boolean active,
            Map<String, Boolean> events,
            NotificationEventType type
    ) {
        if (!enabled || !active || type == null) {
            return false;
        }
        return events.getOrDefault(type.configKey().toLowerCase(Locale.ROOT), false);
    }

    public static String sanitizeMessage(String message, boolean includeReasons) {
        String value = message == null ? "" : message.strip();
        if (!includeReasons) {
            value = value.lines()
                    .filter(line -> !line.stripLeading().startsWith("理由:"))
                    .filter(line -> !line.stripLeading().startsWith("メモ:"))
                    .filter(line -> !line.stripLeading().startsWith("審査メモ:"))
                    .reduce((left, right) -> left + "\n" + right)
                    .orElse("");
        }
        return truncate(value, MAX_MESSAGE_LENGTH);
    }

    public static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (maxLength < 4 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}
