package dev.modplugin.reputationban.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AuditCommandArgumentParserTest {
    @Test
    void parsesRecent() {
        assertEquals(AuditCommandArgument.Mode.RECENT, AuditCommandArgumentParser.parse(new String[]{"audit", "recent"}).mode());
    }

    @Test
    void parsesType() {
        AuditCommandArgument parsed = AuditCommandArgumentParser.parse(new String[]{"audit", "type", "AUTO_BAN"});
        assertEquals(AuditCommandArgument.Mode.TYPE, parsed.mode());
        assertEquals("AUTO_BAN", parsed.value());
    }

    @Test
    void parsesExports() {
        assertEquals(AuditCommandArgument.Mode.EXPORT_RECENT,
                AuditCommandArgumentParser.parse(new String[]{"audit", "export", "recent"}).mode());
        assertEquals(AuditCommandArgument.Mode.EXPORT_TARGET,
                AuditCommandArgumentParser.parse(new String[]{"audit", "export", "Alex"}).mode());
    }

    @Test
    void parsesPlayerLikeArgument() {
        AuditCommandArgument parsed = AuditCommandArgumentParser.parse(new String[]{"audit", "Alex"});
        assertEquals(AuditCommandArgument.Mode.TARGET, parsed.mode());
        assertEquals("Alex", parsed.value());
    }
}
