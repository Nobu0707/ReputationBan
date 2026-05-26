package dev.modplugin.reputationban.notification;

public final class DiscordWebhookPayloadBuilder {
    public static final int MAX_CONTENT_LENGTH = 1900;
    private static final String TRUNCATED_SUFFIX = "...(truncated)";

    private DiscordWebhookPayloadBuilder() {
    }

    public static String buildContentPayload(String username, String content) {
        String safeUsername = username == null || username.isBlank() ? "ReputationBan" : username.trim();
        String safeContent = truncateContent(content == null ? "" : content);
        return "{\"username\":\"" + JsonEscaper.escape(safeUsername)
                + "\",\"content\":\"" + JsonEscaper.escape(safeContent) + "\"}";
    }

    public static String truncateContent(String content) {
        if (content == null) {
            return "";
        }
        if (content.length() <= MAX_CONTENT_LENGTH) {
            return content;
        }
        return content.substring(0, MAX_CONTENT_LENGTH - TRUNCATED_SUFFIX.length()) + TRUNCATED_SUFFIX;
    }
}
