package dev.modplugin.reputationban.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IntegrationStatusTest {
    @Test
    void labelsActiveDisabledAndUnavailable() {
        assertEquals("active", new IntegrationStatus(
                ExternalIntegrationType.LUCKPERMS, true, true, true, "", true, "active"
        ).availabilityLabel());
        assertEquals("disabled", IntegrationStatus.disabled(ExternalIntegrationType.COREPROTECT).availabilityLabel());
        assertEquals("unavailable", new IntegrationStatus(
                ExternalIntegrationType.COREPROTECT, true, false, false, "", false, "plugin not found"
        ).availabilityLabel());
        assertEquals("unavailable", new IntegrationStatus(
                ExternalIntegrationType.WORLDGUARD, true, false, false, "", false, "WorldGuard not found"
        ).availabilityLabel());
    }

    @Test
    void compactLineIncludesSafetyBooleans() {
        String line = new IntegrationStatus(
                ExternalIntegrationType.LUCKPERMS, true, true, true, "", true, "active"
        ).compactLine();

        assertTrue(line.contains("LuckPerms"));
        assertTrue(line.contains("active=true"));
        assertTrue(line.contains("pluginPresent=true"));
        assertTrue(line.contains("apiAvailable=true"));
    }
}
