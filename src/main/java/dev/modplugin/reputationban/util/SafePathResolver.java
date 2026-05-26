package dev.modplugin.reputationban.util;

import java.nio.file.Path;

public final class SafePathResolver {
    public static final String DEFAULT_EXPORT_DIRECTORY = "exports";

    private SafePathResolver() {
    }

    public static Path resolveInsideBase(Path baseDirectory, String configuredPath, String fallbackRelativePath) {
        Path base = baseDirectory.toAbsolutePath().normalize();
        String value = configuredPath == null || configuredPath.isBlank() ? fallbackRelativePath : configuredPath;
        Path raw = Path.of(value);
        Path fallback = base.resolve(fallbackRelativePath).normalize();
        if (raw.isAbsolute()) {
            return fallback;
        }
        Path candidate = base.resolve(raw).normalize();
        return candidate.startsWith(base) ? candidate : fallback;
    }

    public static boolean staysInsideBase(Path baseDirectory, String configuredPath) {
        Path base = baseDirectory.toAbsolutePath().normalize();
        if (configuredPath == null || configuredPath.isBlank()) {
            return true;
        }
        Path raw = Path.of(configuredPath);
        return !raw.isAbsolute() && base.resolve(raw).normalize().startsWith(base);
    }
}
