package dev.modplugin.reputationban.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuditEventTypeTest {
    @Test
    void parsesNamesCaseInsensitively() {
        assertEquals(AuditEventType.REPORT_CREATED, AuditEventType.parse("report_created"));
        assertEquals(AuditEventType.REPORT_THRESHOLD_REACHED, AuditEventType.parse("report-threshold-reached"));
        assertEquals("AUTO_BAN", AuditEventType.AUTO_BAN.databaseValue());
    }

    @Test
    void exposesDatabaseValues() {
        assertTrue(AuditEventType.databaseValues().contains("MAINTENANCE_RUN"));
        assertTrue(AuditEventType.databaseValues().contains("MAINTENANCE_PREVIEW"));
        assertTrue(AuditEventType.databaseValues().contains("DIAGNOSTICS_RUN"));
        assertTrue(AuditEventType.databaseValues().contains("DB_BACKUP_CREATED"));
        assertTrue(AuditEventType.databaseValues().contains("SUPPORT_BUNDLE_CREATED"));
        assertEquals(AuditEventType.MAINTENANCE_PREVIEW, AuditEventType.parse("maintenance-preview"));
        assertEquals(AuditEventType.DIAGNOSTICS_RUN, AuditEventType.parse("diagnostics-run"));
        assertEquals(AuditEventType.DB_BACKUP_CREATED, AuditEventType.parse("db-backup-created"));
        assertEquals(AuditEventType.SUPPORT_BUNDLE_CREATED, AuditEventType.parse("support-bundle-created"));
    }

    @Test
    void rejectsUnknownType() {
        assertThrows(IllegalArgumentException.class, () -> AuditEventType.parse("unknown"));
    }
}
