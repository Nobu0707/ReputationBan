package dev.modplugin.reputationban.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReportStatusTest {
    @Test
    void onlyPendingReportsCanBeReviewed() {
        assertTrue(ReportStatus.canReview("pending"));
        assertFalse(ReportStatus.canReview("auto_accepted"));
        assertFalse(ReportStatus.canReview("approved"));
        assertFalse(ReportStatus.canReview("rejected"));
        assertFalse(ReportStatus.canReview("cancelled"));
    }

    @Test
    void rejectsUnknownStatus() {
        assertThrows(IllegalArgumentException.class, () -> ReportStatus.parse("unknown"));
    }

    @Test
    void exposesValidDatabaseValuesForListCommand() {
        assertTrue(ReportStatus.isDatabaseValue("pending"));
        assertTrue(ReportStatus.isDatabaseValue("approved"));
        assertTrue(ReportStatus.isDatabaseValue("rejected"));
        assertTrue(ReportStatus.isDatabaseValue("auto_accepted"));
        assertTrue(ReportStatus.isDatabaseValue("cancelled"));
        assertFalse(ReportStatus.isDatabaseValue("all"));
        assertEquals(
                java.util.List.of("pending", "auto_accepted", "approved", "rejected", "cancelled"),
                ReportStatus.databaseValues()
        );
    }
}
