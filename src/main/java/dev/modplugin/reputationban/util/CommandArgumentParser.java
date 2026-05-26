package dev.modplugin.reputationban.util;

import java.util.OptionalInt;

public final class CommandArgumentParser {
    private CommandArgumentParser() {
    }

    public static OptionalInt parseLimit(String value, int max) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1) {
                return OptionalInt.empty();
            }
            return OptionalInt.of(Math.min(max, parsed));
        } catch (NumberFormatException exception) {
            return OptionalInt.empty();
        }
    }
}
