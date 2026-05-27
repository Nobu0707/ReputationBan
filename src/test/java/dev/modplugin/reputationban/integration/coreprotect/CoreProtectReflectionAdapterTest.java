package dev.modplugin.reputationban.integration.coreprotect;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class CoreProtectReflectionAdapterTest {
    @Test
    void unavailableWhenPluginMissing() {
        CoreProtectReflectionAdapter adapter = new CoreProtectReflectionAdapter(name -> null, () -> true);

        assertTrue(adapter.api(11).isEmpty());
    }

    @Test
    void unavailableWhenApiMissing() {
        CoreProtectReflectionAdapter adapter = new CoreProtectReflectionAdapter(name -> new FakeCoreProtect(null), () -> true);

        assertTrue(adapter.api(11).isEmpty());
    }

    @Test
    void unavailableWhenApiDisabled() {
        CoreProtectReflectionAdapter adapter = new CoreProtectReflectionAdapter(
                name -> new FakeCoreProtect(new FakeApi(false, 11)),
                () -> true
        );

        assertTrue(adapter.api(11).isEmpty());
    }

    @Test
    void unavailableWhenApiVersionTooOld() {
        CoreProtectReflectionAdapter adapter = new CoreProtectReflectionAdapter(
                name -> new FakeCoreProtect(new FakeApi(true, 10)),
                () -> true
        );

        assertTrue(adapter.api(11).isEmpty());
    }

    @Test
    void lookupResultIsLimitedByMaxResults() {
        CoreProtectReflectionAdapter adapter = new CoreProtectReflectionAdapter(name -> null, () -> true);
        FakeLookupApi api = new FakeLookupApi();

        CoreProtectLookupResult result = adapter.performLookup(
                new CoreProtectReflectionAdapter.ApiHandle(api, 11),
                300,
                List.of("target"),
                List.of(0, 1),
                5,
                null,
                1
        );

        assertEquals(1, result.entries().size());
        assertEquals("block-break", result.entries().getFirst().action());
    }

    static final class FakeCoreProtect {
        private final FakeApi api;

        FakeCoreProtect(FakeApi api) {
            this.api = api;
        }

        public FakeApi getAPI() {
            return api;
        }
    }

    static class FakeApi {
        private final boolean enabled;
        private final int apiVersion;

        FakeApi(boolean enabled, int apiVersion) {
            this.enabled = enabled;
            this.apiVersion = apiVersion;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public int APIVersion() {
            return apiVersion;
        }
    }

    static final class FakeLookupApi extends FakeApi {
        FakeLookupApi() {
            super(true, 11);
        }

        public List<String[]> performLookup(
                int lookupSeconds,
                List<String> players,
                Object excluded,
                Object blocks,
                Object actionsExcluded,
                List<Integer> actions,
                int radius,
                Object location
        ) {
            return List.of(new String[]{"one"}, new String[]{"two"});
        }

        public ParsedResult parseResult(String[] result) {
            return new ParsedResult(result[0].equals("one") ? "block-break" : "block-place");
        }
    }

    static final class ParsedResult {
        private final String action;

        ParsedResult(String action) {
            this.action = action;
        }

        public String getActionString() {
            return action;
        }

        public String getPlayer() {
            return "target";
        }

        public int getX() {
            return 1;
        }

        public int getY() {
            return 2;
        }

        public int getZ() {
            return 3;
        }

        public String getType() {
            return "stone";
        }
    }
}
