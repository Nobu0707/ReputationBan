package dev.modplugin.reputationban.integration.discordsrv;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.modplugin.reputationban.config.PluginConfig;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class DiscordSrvIntegrationStatusTest {
    @Test
    void activeWhenConfiguredPresentAndApiAvailable() {
        DiscordSrvIntegration integration = new DiscordSrvIntegration(Logger.getLogger("test"), new FakeAdapter(true, true));

        DiscordSrvStatus status = integration.detail(config(true));

        assertTrue(status.pluginPresent());
        assertTrue(status.apiAvailable());
        assertTrue(status.active());
    }

    @Test
    void disabledWhenConfiguredOff() {
        DiscordSrvIntegration integration = new DiscordSrvIntegration(Logger.getLogger("test"), new FakeAdapter(true, true));

        DiscordSrvStatus status = integration.detail(config(false));

        assertFalse(status.active());
        assertFalse(integration.status(config(false)).configuredEnabled());
    }

    @Test
    void unavailableWhenPluginMissing() {
        DiscordSrvIntegration integration = new DiscordSrvIntegration(Logger.getLogger("test"), new FakeAdapter(false, false));

        DiscordSrvStatus status = integration.detail(config(true));

        assertFalse(status.pluginPresent());
        assertFalse(status.active());
    }

    private static PluginConfig config(boolean enabled) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("integrations.discordsrv.enabled", enabled);
        yaml.set("integrations.discordsrv.account-link-context.enabled", true);
        yaml.set("integrations.discordsrv.account-link-context.include-discord-ids", false);
        yaml.set("integrations.discordsrv.account-link-context.categories", java.util.List.of("griefing"));
        yaml.set("integrations.discordsrv.notifications.enabled", false);
        yaml.set("integrations.discordsrv.notifications.channel", "staff");
        yaml.set("integrations.discordsrv.notifications.include-reasons", true);
        yaml.set("integrations.discordsrv.notifications.events.report-created", true);
        yaml.set("integrations.discordsrv.notifications.events.auto-ban", true);
        return PluginConfig.load(yaml);
    }

    private static final class FakeAdapter extends DiscordSrvReflectionAdapter {
        private final boolean pluginPresent;
        private final boolean apiAvailable;

        private FakeAdapter(boolean pluginPresent, boolean apiAvailable) {
            super(ignored -> pluginPresent ? new Object() : null, () -> true);
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
