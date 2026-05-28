package dev.modplugin.reputationban.model;

public record TargetProtectionResult(boolean protectedTarget, String reason, boolean lookupFailed) {
    public static TargetProtectionResult unprotected() {
        return new TargetProtectionResult(false, "", false);
    }

    public static TargetProtectionResult protectedBy(String reason) {
        return new TargetProtectionResult(true, reason, false);
    }

    public static TargetProtectionResult protectedByLookupFailure(String reason) {
        return new TargetProtectionResult(true, reason, true);
    }
}
