package dev.modplugin.reputationban.integration.worldguard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.modplugin.reputationban.config.PluginConfig;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class WorldGuardIntegrationStatusTest {
    @Test
    void reportsActiveDisabledAndUnavailable() {
        assertTrue(integration(true, true, true).status(config(true)).active());
        assertFalse(integration(true, true, true).status(config(false)).active());
        assertEquals("disabled", integration(true, true, true).status(config(false)).availabilityLabel());
        assertEquals("unavailable", integration(false, true, false).status(config(true)).availabilityLabel());
        assertEquals("unavailable", integration(true, false, false).status(config(true)).availabilityLabel());
    }

    private static WorldGuardIntegration integration(boolean worldGuardPresent, boolean worldEditPresent, boolean apiAvailable) {
        return new WorldGuardIntegration(
                java.util.logging.Logger.getLogger("test"),
                new FakeAdapter(worldGuardPresent, worldEditPresent, apiAvailable)
        );
    }

    private static PluginConfig config(boolean enabled) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("integrations.worldguard.enabled", enabled);
        yaml.set("integrations.worldguard.report-context.enabled", true);
        yaml.set("integrations.worldguard.report-context.categories", java.util.List.of("griefing"));
        yaml.set("integrations.worldguard.report-context.max-regions", 10);
        return PluginConfig.load(yaml);
    }

    private static final class FakeAdapter extends WorldGuardReflectionAdapter {
        private final boolean worldGuardPresent;
        private final boolean worldEditPresent;
        private final boolean apiAvailable;

        private FakeAdapter(boolean worldGuardPresent, boolean worldEditPresent, boolean apiAvailable) {
            super(ignored -> null, () -> true);
            this.worldGuardPresent = worldGuardPresent;
            this.worldEditPresent = worldEditPresent;
            this.apiAvailable = apiAvailable;
        }

        @Override
        public boolean worldGuardPresent() {
            return worldGuardPresent;
        }

        @Override
        public boolean worldEditPresent() {
            return worldEditPresent;
        }

        @Override
        public boolean apiAvailable() {
            return apiAvailable;
        }
    }
}
