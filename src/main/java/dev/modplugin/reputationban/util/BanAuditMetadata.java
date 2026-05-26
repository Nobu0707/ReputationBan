package dev.modplugin.reputationban.util;

import java.util.Arrays;
import java.util.UUID;

public final class BanAuditMetadata {
    public static final String CONSOLE_ACTOR = "CONSOLE";
    public static final String DEFAULT_REASON = "理由未指定";

    private BanAuditMetadata() {
    }

    public static String actorId(UUID actorUuid) {
        return actorUuid == null ? CONSOLE_ACTOR : actorUuid.toString();
    }

    public static String reasonFromArgs(String[] args, int startIndex) {
        if (args.length <= startIndex) {
            return DEFAULT_REASON;
        }
        String reason = String.join(" ", Arrays.copyOfRange(args, startIndex, args.length)).trim();
        return reason.isEmpty() ? DEFAULT_REASON : reason;
    }
}
