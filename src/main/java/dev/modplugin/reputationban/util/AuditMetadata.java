package dev.modplugin.reputationban.util;

import dev.modplugin.reputationban.notification.JsonEscaper;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class AuditMetadata {
    private static final java.util.List<String> SECRET_KEYS = java.util.List.of(
            "webhook", "cookie", "sessionid", "password", "hash", "token", "secret", "url"
    );

    private final Map<String, String> values = new LinkedHashMap<>();

    private AuditMetadata() {
    }

    public static AuditMetadata create() {
        return new AuditMetadata();
    }

    public AuditMetadata put(String key, Object value) {
        if (key == null || key.isBlank() || value == null || isSecretLike(key)) {
            return this;
        }
        values.put(key, String.valueOf(value));
        return this;
    }

    public String toJson() {
        if (values.isEmpty()) {
            return "";
        }
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append('"').append(JsonEscaper.escape(entry.getKey())).append("\":");
            json.append('"').append(JsonEscaper.escape(entry.getValue())).append('"');
        }
        json.append('}');
        return json.toString();
    }

    public static boolean isSecretLike(String key) {
        String normalized = key.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        return SECRET_KEYS.stream().anyMatch(normalized::contains);
    }
}
