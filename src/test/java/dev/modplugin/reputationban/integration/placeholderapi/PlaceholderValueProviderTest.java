package dev.modplugin.reputationban.integration.placeholderapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.modplugin.reputationban.config.PluginConfig;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class PlaceholderValueProviderTest {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    @Test
    void returnsValuesFromCacheOnly() {
        UUID uuid = UUID.randomUUID();
        long now = System.currentTimeMillis();
        PlayerReputationSummary summary = new PlayerReputationSummary(
                uuid,
                "Alex",
                40,
                100,
                3,
                4,
                now + 60_000L,
                now,
                now
        );
        PlaceholderValueProvider provider = new PlaceholderValueProvider(
                id -> id.equals(uuid) ? Optional.of(summary) : Optional.empty(),
                () -> config(),
                "0.20.0"
        );

        Map<String, String> expected = Map.of(
                "score", "40",
                "max_score", "100",
                "score_percent", "40",
                "status", "watch",
                "ban_count", "3",
                "false_report_count", "4",
                "report_banned", "true",
                "last_seen", FORMATTER.format(Instant.ofEpochMilli(now)),
                "version", "0.20.0"
        );
        expected.forEach((key, value) -> assertEquals(value, provider.value(uuid, key)));
        assertEquals(FORMATTER.format(Instant.ofEpochMilli(now + 60_000L)), provider.value(uuid, "report_banned_until"));
    }

    @Test
    void returnsUnknownFallbackForMissingCacheNullPlayerAndUnknownParam() {
        UUID uuid = UUID.randomUUID();
        PlaceholderValueProvider provider = new PlaceholderValueProvider(
                id -> Optional.empty(),
                () -> config(),
                "0.20.0"
        );

        assertEquals("-", provider.value(uuid, "score"));
        assertEquals("-", provider.value((UUID) null, "score"));
        assertEquals("-", provider.value(uuid, "missing"));
        assertEquals("0.20.0", provider.value((UUID) null, "version"));
    }

    private static PluginConfig config() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("max-score", 100);
        config.set("score-thresholds.warning", 70);
        config.set("score-thresholds.watch", 50);
        config.set("score-thresholds.restricted", 30);
        config.set("score-thresholds.final-warning", 10);
        config.set("score-thresholds.ban", 0);
        config.set("integrations.placeholderapi.show-unknown-as", "-");
        return PluginConfig.load(config);
    }
}
