package dev.modplugin.reputationban.integration.griefprevention;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class GriefPreventionReflectionAdapterTest {
    @Test
    void unavailableWhenPluginMissing() {
        GriefPreventionReflectionAdapter adapter = new GriefPreventionReflectionAdapter(ignored -> null, () -> true);

        assertFalse(adapter.pluginPresent());
        assertFalse(adapter.apiAvailable());
    }
}
