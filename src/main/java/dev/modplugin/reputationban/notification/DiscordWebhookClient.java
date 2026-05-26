package dev.modplugin.reputationban.notification;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.logging.Logger;

public final class DiscordWebhookClient {
    private final HttpClient httpClient;
    private final Logger logger;
    private boolean emptyUrlWarningLogged;
    private long lastFailureLogAt;
    private String lastFailureKind = "";

    public DiscordWebhookClient(Logger logger) {
        this(HttpClient.newHttpClient(), logger);
    }

    DiscordWebhookClient(HttpClient httpClient, Logger logger) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void send(NotificationEventType type, DiscordWebhookConfig config, String content) {
        if (config == null || !config.enabled() || !config.eventEnabled(type)) {
            return;
        }
        if (!config.hasUsableUrl()) {
            if (!emptyUrlWarningLogged) {
                emptyUrlWarningLogged = true;
                logger.warning("Discord webhook is enabled but URL is empty. Discord notifications are disabled until configured.");
            }
            return;
        }

        String payload = DiscordWebhookPayloadBuilder.buildContentPayload(config.username(), content);
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(config.url()))
                    .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
        } catch (RuntimeException exception) {
            logFailure(config, exception.getClass().getName());
            return;
        }

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        logFailure(config, throwable.getClass().getName());
                        return;
                    }
                    int status = response.statusCode();
                    if (status < 200 || status >= 300) {
                        logFailure(config, "status=" + status);
                    }
                });
    }

    private synchronized void logFailure(DiscordWebhookConfig config, String failureKind) {
        long now = System.currentTimeMillis();
        long rateLimitMillis = Duration.ofSeconds(config.rateLimitFailureLogSeconds()).toMillis();
        if (failureKind.equals(lastFailureKind) && now - lastFailureLogAt < rateLimitMillis) {
            return;
        }
        lastFailureKind = failureKind;
        lastFailureLogAt = now;
        logger.warning("Discord webhook delivery failed: " + failureKind);
    }
}
