package dev.modplugin.reputationban.integration.griefprevention;

import dev.modplugin.reputationban.util.AuditMetadata;

public record GriefPreventionClaimSummary(
        boolean claimPresent,
        String world,
        int x,
        int y,
        int z,
        String claimId,
        boolean adminClaim,
        String owner,
        String bounds,
        String trustCounts,
        String summary,
        String metadata
) {
    private static final int SUMMARY_MAX_LENGTH = 1000;

    public static GriefPreventionClaimSummary absent(String world, int x, int y, int z) {
        String summary = "GriefPrevention: claimPresent=false world=%s x=%d y=%d z=%d".formatted(world, x, y, z);
        return new GriefPreventionClaimSummary(
                false,
                world,
                x,
                y,
                z,
                "-",
                false,
                "hidden",
                "-",
                "hidden",
                summary,
                metadata(false, "-", false, world, x, y, z, "hidden", "-", "hidden")
        );
    }

    public static GriefPreventionClaimSummary present(
            String world,
            int x,
            int y,
            int z,
            GriefPreventionClaimEntry entry
    ) {
        StringBuilder summary = new StringBuilder("GriefPrevention: claimPresent=true")
                .append(" world=").append(world)
                .append(" x=").append(x)
                .append(" y=").append(y)
                .append(" z=").append(z)
                .append(" claimId=").append(fallback(entry.claimId()))
                .append(" adminClaim=").append(entry.adminClaim())
                .append(" owner=").append(fallback(entry.owner()));
        if (entry.bounds() != null && !entry.bounds().isBlank() && !"-".equals(entry.bounds())) {
            summary.append(" bounds=").append(entry.bounds());
        }
        if (entry.trustCounts() != null && !entry.trustCounts().isBlank() && !"hidden".equals(entry.trustCounts())) {
            summary.append(" trustCounts=").append(entry.trustCounts());
        }
        return new GriefPreventionClaimSummary(
                true,
                world,
                x,
                y,
                z,
                fallback(entry.claimId()),
                entry.adminClaim(),
                fallback(entry.owner()),
                fallback(entry.bounds()),
                fallback(entry.trustCounts()),
                GriefPreventionClaimPolicy.truncateSummary(summary.toString(), SUMMARY_MAX_LENGTH),
                metadata(true, entry.claimId(), entry.adminClaim(), world, x, y, z, entry.owner(), entry.bounds(), entry.trustCounts())
        );
    }

    private static String metadata(
            boolean claimPresent,
            String claimId,
            boolean adminClaim,
            String world,
            int x,
            int y,
            int z,
            String owner,
            String bounds,
            String trustCounts
    ) {
        return AuditMetadata.create()
                .put("claimPresent", claimPresent)
                .put("claimId", fallback(claimId))
                .put("adminClaim", adminClaim)
                .put("world", world)
                .put("x", x)
                .put("y", y)
                .put("z", z)
                .put("owner", fallback(owner))
                .put("bounds", fallback(bounds))
                .put("trustCounts", fallback(trustCounts))
                .toJson();
    }

    private static String fallback(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
