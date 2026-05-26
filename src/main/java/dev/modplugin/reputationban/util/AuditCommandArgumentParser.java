package dev.modplugin.reputationban.util;

public final class AuditCommandArgumentParser {
    private AuditCommandArgumentParser() {
    }

    public static AuditCommandArgument parse(String[] args) {
        if (args.length < 2 || "recent".equalsIgnoreCase(args[1])) {
            return new AuditCommandArgument(AuditCommandArgument.Mode.RECENT, "recent");
        }
        if ("type".equalsIgnoreCase(args[1])) {
            return new AuditCommandArgument(AuditCommandArgument.Mode.TYPE, args.length >= 3 ? args[2] : "");
        }
        if ("export".equalsIgnoreCase(args[1])) {
            if (args.length < 3 || "recent".equalsIgnoreCase(args[2])) {
                return new AuditCommandArgument(AuditCommandArgument.Mode.EXPORT_RECENT, "recent");
            }
            return new AuditCommandArgument(AuditCommandArgument.Mode.EXPORT_TARGET, args[2]);
        }
        return new AuditCommandArgument(AuditCommandArgument.Mode.TARGET, args[1]);
    }
}
