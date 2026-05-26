package dev.modplugin.reputationban.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NotificationEventTypeTest {
    @Test
    void exposesConfigKeys() {
        assertEquals("report-created", NotificationEventType.REPORT_CREATED.configKey());
        assertEquals("report-approved", NotificationEventType.REPORT_APPROVED.configKey());
        assertEquals("report-rejected", NotificationEventType.REPORT_REJECTED.configKey());
        assertEquals("score-threshold-crossed", NotificationEventType.SCORE_THRESHOLD_CROSSED.configKey());
        assertEquals("auto-ban", NotificationEventType.AUTO_BAN.configKey());
        assertEquals("unban", NotificationEventType.UNBAN.configKey());
        assertEquals("pardon", NotificationEventType.PARDON.configKey());
        assertEquals("reporter-penalty", NotificationEventType.REPORTER_PENALTY.configKey());
        assertEquals("recovery-summary", NotificationEventType.RECOVERY_SUMMARY.configKey());
    }
}
