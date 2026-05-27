package dev.modplugin.reputationban.util;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Redactor {
    public static final String REDACTED = "<redacted>";
    private static final List<String> SECRET_KEY_PARTS = List.of(
            "webhook",
            "url",
            "password",
            "token",
            "secret",
            "session",
            "cookie"
    );
    private static final Pattern DISCORD_WEBHOOK = Pattern.compile(
            "https://(?:canary\\.|ptb\\.)?discord(?:app)?\\.com/api/webhooks/\\S+",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern URL = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOKEN_ASSIGNMENT = Pattern.compile(
            "(?i)(webhook|url|password|token|secret|session|cookie)(\\s*[:=]\\s*)\\S+"
    );

    private Redactor() {
    }

    public static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(".", "");
        return SECRET_KEY_PARTS.stream().anyMatch(normalized::contains);
    }

    public static String redactValueForKey(String key, String value) {
        if (isSensitiveKey(key)) {
            return REDACTED;
        }
        return redactSecretLikeValue(value);
    }

    public static String redactSecretLikeValue(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String redacted = DISCORD_WEBHOOK.matcher(value).replaceAll(REDACTED);
        redacted = URL.matcher(redacted).replaceAll(REDACTED);
        redacted = TOKEN_ASSIGNMENT.matcher(redacted).replaceAll("$1$2" + REDACTED);
        return redacted;
    }
}
