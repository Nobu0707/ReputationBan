package dev.modplugin.reputationban.util;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

public final class DurationParser {
    private DurationParser() {
    }

    public static Optional<Duration> parseBanDuration(String input) {
        String value = normalize(input);
        if ("permanent".equals(value) || "perm".equals(value)) {
            return Optional.empty();
        }
        return Optional.of(parseFinite(value));
    }

    public static Duration parseFinite(String input) {
        String value = normalize(input);
        if (value.length() < 2) {
            throw new IllegalArgumentException("Duration must include a number and unit");
        }

        char unit = value.charAt(value.length() - 1);
        long amount;
        try {
            amount = Long.parseLong(value.substring(0, value.length() - 1));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid duration amount: " + input, exception);
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Duration amount must be positive");
        }

        return switch (unit) {
            case 'd' -> Duration.ofDays(amount);
            case 'h' -> Duration.ofHours(amount);
            case 'm' -> Duration.ofMinutes(amount);
            case 's' -> Duration.ofSeconds(amount);
            default -> throw new IllegalArgumentException("Unsupported duration unit: " + unit);
        };
    }

    private static String normalize(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Duration must not be null");
        }
        String value = input.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Duration must not be blank");
        }
        return value;
    }
}
