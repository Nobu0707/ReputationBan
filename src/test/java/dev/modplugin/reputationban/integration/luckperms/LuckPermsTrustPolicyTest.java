package dev.modplugin.reputationban.integration.luckperms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LuckPermsTrustPolicyTest {
    @Test
    void calculatesDefaultAndGroupWeights() {
        Map<String, Double> weights = Map.of("trusted", 1.2D, "moderator", 2.0D);

        assertEquals(1.0D, LuckPermsTrustPolicy.weightForGroup(true, 1.0D, weights, "default"));
        assertEquals(1.2D, LuckPermsTrustPolicy.weightForGroup(true, 1.0D, weights, "trusted"));
        assertEquals(2.0D, LuckPermsTrustPolicy.weightForGroup(true, 1.0D, weights, "MODERATOR"));
        assertEquals(1.0D, LuckPermsTrustPolicy.weightForGroup(true, 1.0D, weights, "unknown"));
        assertEquals(1.0D, LuckPermsTrustPolicy.weightForGroup(false, 1.0D, weights, "trusted"));
    }

    @Test
    void detectsBypassGroupsCaseInsensitively() {
        assertTrue(LuckPermsTrustPolicy.isBypassGroup(Set.of("admin", "owner"), "Admin"));
        assertFalse(LuckPermsTrustPolicy.isBypassGroup(Set.of("admin", "owner"), "trusted"));
    }
}
