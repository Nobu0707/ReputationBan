package dev.modplugin.reputationban.integration.placeholderapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlaceholderApiStatusTest {
    @Test
    void labelsActiveDisabledAndUnavailable() {
        assertEquals("active", new PlaceholderApiStatus(
                true, true, true, true, "reputationban", 60, true, "active"
        ).availabilityLabel());
        assertEquals("disabled", new PlaceholderApiStatus(
                false, false, false, false, "reputationban", 60, false, "disabled"
        ).availabilityLabel());
        assertEquals("unavailable", new PlaceholderApiStatus(
                true, false, false, false, "reputationban", 60, false, "PlaceholderAPI not found"
        ).availabilityLabel());
    }

    @Test
    void sanitizesIdentifier() {
        assertEquals("reputationban", PlaceholderApiIntegration.identifier(""));
        assertEquals("reputationban", PlaceholderApiIntegration.identifier("Bad-Identifier"));
        assertEquals("rep_status", PlaceholderApiIntegration.identifier("rep_status"));
    }
}
