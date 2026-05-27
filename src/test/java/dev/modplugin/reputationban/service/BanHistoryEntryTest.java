package dev.modplugin.reputationban.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BanHistoryEntryTest {
    @Test
    void unbannedByIdAndDisplayNameAreSeparateFields() {
        PunishmentService.BanHistoryEntry entry = new PunishmentService.BanHistoryEntry(
                1L,
                "reason",
                "temporary",
                1000L,
                2000L,
                "ReputationBan",
                1500L,
                "11111111-1111-1111-1111-111111111111",
                "Alice",
                "誤BAN対応"
        );

        assertEquals("11111111-1111-1111-1111-111111111111", entry.unbannedBy());
        assertEquals("Alice", entry.unbannedByName());
        assertEquals("誤BAN対応", entry.unbanReason());
    }
}
