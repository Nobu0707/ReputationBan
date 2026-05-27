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
    private static final String SECRET_WORD = "webhook|url|password|token|secret|session(?:id)?|cookie";
    private static final Pattern SECRET_ASSIGNMENT = Pattern.compile(
            "(?i)\\b(" + SECRET_WORD + ")(\\s*[:=]\\s*)\\S+"
    );
    private static final Pattern SECRET_IS_VALUE = Pattern.compile(
            "(?i)\\b(" + SECRET_WORD + ")\\s+is\\s+\\S+"
    );
    private static final Pattern SECRET_FREE_TEXT_VALUE = Pattern.compile(
            "(?i)\\b(" + SECRET_WORD + ")\\s+\\S+"
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
        redacted = SECRET_ASSIGNMENT.matcher(redacted).replaceAll("$1$2" + REDACTED);
        redacted = SECRET_IS_VALUE.matcher(redacted).replaceAll("$1 " + REDACTED);
        redacted = SECRET_FREE_TEXT_VALUE.matcher(redacted).replaceAll("$1 " + REDACTED);
        return redacted;
    }
}
