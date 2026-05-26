package dev.modplugin.reputationban.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SafePathResolverTest {
    private static final Path BASE = Path.of("/tmp/ReputationBan").toAbsolutePath().normalize();

    @Test
    void allowsSimpleExportsDirectory() {
        assertEquals(BASE.resolve("exports"), SafePathResolver.resolveInsideBase(BASE, "exports", "exports"));
    }

    @Test
    void allowsNestedRelativeDirectory() {
        assertEquals(BASE.resolve("nested/exports"), SafePathResolver.resolveInsideBase(BASE, "nested/exports", "exports"));
    }

    @Test
    void fallsBackForParentTraversal() {
        assertEquals(BASE.resolve("exports"), SafePathResolver.resolveInsideBase(BASE, "../outside", "exports"));
    }

    @Test
    void fallsBackForAbsolutePath() {
        assertEquals(BASE.resolve("exports"), SafePathResolver.resolveInsideBase(BASE, "/tmp/outside", "exports"));
    }
}
