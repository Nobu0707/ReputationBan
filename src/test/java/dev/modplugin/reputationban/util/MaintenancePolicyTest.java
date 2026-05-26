package dev.modplugin.reputationban.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.modplugin.reputationban.util.MaintenancePolicy.MaintenanceAction;
import org.junit.jupiter.api.Test;

class MaintenancePolicyTest {
    @Test
    void runAloneRequiresConfirmation() {
        assertEquals(MaintenanceAction.RUN_REQUIRES_CONFIRMATION, MaintenancePolicy.parse(new String[] {"maintenance", "run"}));
    }

    @Test
    void runConfirmExecutes() {
        assertEquals(MaintenanceAction.RUN_CONFIRMED, MaintenancePolicy.parse(new String[] {"maintenance", "run", "confirm"}));
    }

    @Test
    void previewDoesNotDelete() {
        assertEquals(MaintenanceAction.PREVIEW, MaintenancePolicy.parse(new String[] {"maintenance", "preview"}));
    }
}
