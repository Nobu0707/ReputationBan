package dev.modplugin.reputationban.integration.griefprevention;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.modplugin.reputationban.config.PluginConfig;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class GriefPreventionIntegrationStatusTest {
    @Test
    void reportsActiveDisabledAndUnavailable() {
        assertTrue(integration(true, true).status(config(true)).active());
        assertFalse(integration(true, true).status(config(false)).active());
        assertEquals("disabled", integration(true, true).status(config(false)).availabilityLabel());
        assertEquals("unavailable", integration(false, false).status(config(true)).availabilityLabel());
        assertEquals("unavailable", integration(true, false).status(config(true)).availabilityLabel());
    }

    private static GriefPreventionIntegration integration(boolean pluginPresent, boolean apiAvailable) {
        return new GriefPreventionIntegration(
                java.util.logging.Logger.getLogger("test"),
                new FakeAdapter(pluginPresent, apiAvailable)
        );
    }

    private static PluginConfig config(boolean enabled) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("integrations.griefprevention.enabled", enabled);
        yaml.set("integrations.griefprevention.report-context.enabled", true);
        yaml.set("integrations.griefprevention.report-context.categories", java.util.List.of("griefing"));
        yaml.set("integrations.griefprevention.report-context.include-claim-owner", false);
        yaml.set("integrations.griefprevention.report-context.include-trust-counts", false);
        yaml.set("integrations.griefprevention.report-context.include-boundaries", true);
        return PluginConfig.load(yaml);
    }

    private static final class FakeAdapter extends GriefPreventionReflectionAdapter {
        private final boolean pluginPresent;
        private final boolean apiAvailable;

        private FakeAdapter(boolean pluginPresent, boolean apiAvailable) {
            super(ignored -> null, () -> true);
            this.pluginPresent = pluginPresent;
            this.apiAvailable = apiAvailable;
        }

        @Override
        public boolean pluginPresent() {
            return pluginPresent;
        }

        @Override
        public boolean apiAvailable() {
            return apiAvailable;
        }
    }
}
