package dev.modplugin.reputationban.util;

public record AuditCommandArgument(Mode mode, String value) {
    public enum Mode {
        RECENT,
        TYPE,
        EXPORT_RECENT,
        EXPORT_TARGET,
        TARGET
    }
}
