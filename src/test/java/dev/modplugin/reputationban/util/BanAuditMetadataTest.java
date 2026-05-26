package dev.modplugin.reputationban.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class BanAuditMetadataTest {
    @Test
    void actorIdUsesUuidOrConsole() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        assertEquals("123e4567-e89b-12d3-a456-426614174000", BanAuditMetadata.actorId(uuid));
        assertEquals("CONSOLE", BanAuditMetadata.actorId(null));
    }

    @Test
    void reasonIsSeparatedFromActorAndDefaultsWhenMissing() {
        assertEquals("誤BAN対応", BanAuditMetadata.reasonFromArgs(new String[] {"unban", "Steve", "誤BAN対応"}, 2));
        assertEquals("理由未指定", BanAuditMetadata.reasonFromArgs(new String[] {"unban", "Steve"}, 2));
        assertEquals("理由未指定", BanAuditMetadata.reasonFromArgs(new String[] {"unban", "Steve", " "}, 2));
    }
}
