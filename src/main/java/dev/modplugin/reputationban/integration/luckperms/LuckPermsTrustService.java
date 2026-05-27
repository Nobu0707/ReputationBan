package dev.modplugin.reputationban.integration.luckperms;

public record LuckPermsTrustService(String primaryGroup, double reporterWeight, boolean bypassGroup) {
    public static LuckPermsTrustService unavailable(double defaultWeight) {
        return new LuckPermsTrustService("", defaultWeight, false);
    }

    public boolean hasPrimaryGroup() {
        return primaryGroup != null && !primaryGroup.isBlank();
    }
}
