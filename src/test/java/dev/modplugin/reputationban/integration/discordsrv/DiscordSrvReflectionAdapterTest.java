package dev.modplugin.reputationban.integration.discordsrv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DiscordSrvReflectionAdapterTest {
    private static final UUID PLAYER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000123");

    @Test
    void pluginInstanceRouteMakesApiAvailableWithoutApiClass() {
        FakeAccountLinkManager manager = new FakeAccountLinkManager("1234567890");
        DiscordSrvReflectionAdapter adapter = adapter(new FakeDiscordSrvPlugin(manager, new FakeChannel()));

        assertTrue(adapter.pluginPresent());
        assertTrue(adapter.apiAvailable());
    }

    @Test
    void pluginMissingIsUnavailable() {
        DiscordSrvReflectionAdapter adapter = adapter(null);

        assertFalse(adapter.pluginPresent());
        assertFalse(adapter.apiAvailable());
    }

    @Test
    void managerMissingIsUnavailable() {
        DiscordSrvReflectionAdapter adapter = adapter(new FakeDiscordSrvPlugin(null, new FakeChannel()));

        assertFalse(adapter.apiAvailable());
    }

    @Test
    void discordIdReturnsValueFromPluginInstanceManager() {
        DiscordSrvReflectionAdapter adapter = adapter(new FakeDiscordSrvPlugin(
                new FakeAccountLinkManager("discord-123"),
                new FakeChannel()
        ));

        assertEquals(Optional.of("discord-123"), adapter.discordId(PLAYER_UUID));
    }

    @Test
    void discordIdNullReturnsEmpty() {
        DiscordSrvReflectionAdapter adapter = adapter(new FakeDiscordSrvPlugin(
                new FakeAccountLinkManager(null),
                new FakeChannel()
        ));

        assertEquals(Optional.empty(), adapter.discordId(PLAYER_UUID));
    }

    @Test
    void sendMessageReturnsFalseWhenChannelMissing() {
        DiscordSrvReflectionAdapter adapter = adapter(new FakeDiscordSrvPlugin(
                new FakeAccountLinkManager("discord-123"),
                null
        ));

        assertFalse(adapter.sendMessage("staff", "hello"));
    }

    @Test
    void sendMessageQueuesWhenChannelExists() {
        FakeChannel channel = new FakeChannel();
        DiscordSrvReflectionAdapter adapter = adapter(new FakeDiscordSrvPlugin(
                new FakeAccountLinkManager("discord-123"),
                channel
        ));

        assertTrue(adapter.sendMessage("staff", "hello"));
        assertEquals("hello", channel.message);
        assertTrue(channel.action.queued);
    }

    private static DiscordSrvReflectionAdapter adapter(Object plugin) {
        return new DiscordSrvReflectionAdapter(ignored -> plugin, () -> true);
    }

    private static final class FakeDiscordSrvPlugin {
        private final Object manager;
        private final FakeChannel channel;

        private FakeDiscordSrvPlugin(Object manager, FakeChannel channel) {
            this.manager = manager;
            this.channel = channel;
        }

        public Object getAccountLinkManager() {
            return manager;
        }

        public FakeChannel getDestinationTextChannelForGameChannelName(String channelName) {
            return "staff".equals(channelName) ? channel : null;
        }
    }

    private static final class FakeAccountLinkManager {
        private final String discordId;

        private FakeAccountLinkManager(String discordId) {
            this.discordId = discordId;
        }

        public String getDiscordId(UUID playerUuid) {
            return PLAYER_UUID.equals(playerUuid) ? discordId : null;
        }
    }

    private static final class FakeChannel {
        private String message;
        private FakeMessageAction action;

        public FakeMessageAction sendMessage(String message) {
            this.message = message;
            this.action = new FakeMessageAction();
            return action;
        }
    }

    private static final class FakeMessageAction {
        private boolean queued;

        public void queue() {
            queued = true;
        }
    }
}
