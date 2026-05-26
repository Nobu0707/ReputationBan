package dev.modplugin.reputationban.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Proxy;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class CommandActorTest {
    @Test
    void playerActorKeepsUuidAndNameSeparate() {
        UUID uuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Player player = proxy(Player.class, uuid, "Alice");

        CommandActor actor = CommandActor.from(player);

        assertEquals(uuid, actor.uuid());
        assertEquals("Alice", actor.name());
        assertEquals(uuid.toString(), actor.databaseActorId());
    }

    @Test
    void consoleActorUsesNullUuidAndConsoleName() {
        CommandSender console = proxy(CommandSender.class, null, "Server");

        CommandActor actor = CommandActor.from(console);

        assertNull(actor.uuid());
        assertEquals("CONSOLE", actor.name());
        assertEquals("CONSOLE", actor.databaseActorId());
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, UUID uuid, String name) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (ignored, method, args) -> switch (method.getName()) {
            case "getUniqueId" -> uuid;
            case "getName" -> name;
            case "toString" -> name;
            case "hashCode" -> 1;
            case "equals" -> ignored == args[0];
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }
}
