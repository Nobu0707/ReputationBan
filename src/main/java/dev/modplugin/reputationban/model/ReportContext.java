package dev.modplugin.reputationban.model;

import dev.modplugin.reputationban.util.Redactor;
import java.util.Locale;

public record ReportContext(
        long id,
        long reportId,
        String provider,
        String summary,
        String metadata,
        long createdAt
) {
    public ReportContext {
        provider = provider == null ? "" : provider.toLowerCase(Locale.ROOT);
        summary = Redactor.redactSecretLikeValue(summary == null ? "" : summary);
        metadata = Redactor.redactSecretLikeValue(metadata == null ? "" : metadata);
    }
}
