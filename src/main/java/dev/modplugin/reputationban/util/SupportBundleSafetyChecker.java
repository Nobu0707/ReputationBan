package dev.modplugin.reputationban.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class SupportBundleSafetyChecker {
    private static final Pattern DISCORD_WEBHOOK = Pattern.compile(
            "https://(?:canary\\.|ptb\\.)?discord(?:app)?\\.com/api/webhooks/\\S+",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern URL = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);

    private SupportBundleSafetyChecker() {
    }

    public static boolean isForbiddenEntryName(String name) {
        if (name == null || name.isBlank()) {
            return true;
        }
        String normalized = name.replace('\\', '/').toLowerCase(Locale.ROOT);
        return normalized.contains("..")
                || normalized.startsWith("/")
                || normalized.contains("reputationban.db")
                || normalized.contains("latest.log")
                || normalized.contains("debug.log")
                || normalized.equals("logs")
                || normalized.startsWith("logs/")
                || normalized.contains("/logs/");
    }

    public static boolean containsDiscordWebhook(String text) {
        return text != null && DISCORD_WEBHOOK.matcher(text).find();
    }

    public static boolean containsUrl(String text) {
        return text != null && URL.matcher(text).find();
    }

    public static boolean containsAbsolutePath(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String redacted = PathRedactor.redactAbsolutePaths(text);
        return !redacted.equals(text);
    }
}
