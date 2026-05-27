package dev.modplugin.reputationban.integration.worldguard;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class WorldGuardReflectionAdapterTest {
    @Test
    void unavailableWhenPluginsAreMissing() {
        WorldGuardReflectionAdapter adapter = new WorldGuardReflectionAdapter(ignored -> null, () -> true);

        assertFalse(adapter.worldEditPresent());
        assertFalse(adapter.worldGuardPresent());
        assertFalse(adapter.apiAvailable());
    }

    @Test
    void apiUnavailableWhenClassesAreMissing() {
        WorldGuardReflectionAdapter adapter = new WorldGuardReflectionAdapter(name -> new Object(), () -> true);

        assertFalse(adapter.apiAvailable());
    }

    @Test
    void requiresPrimaryThread() {
        WorldGuardReflectionAdapter adapter = new WorldGuardReflectionAdapter(ignored -> null, () -> false);

        assertThrows(IllegalStateException.class, adapter::apiAvailable);
    }
}
