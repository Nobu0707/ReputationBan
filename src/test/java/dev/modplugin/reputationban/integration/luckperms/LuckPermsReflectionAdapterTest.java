package dev.modplugin.reputationban.integration.luckperms;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LuckPermsReflectionAdapterTest {
    @Test
    void unavailableWhenLuckPermsClassMissing() {
        LuckPermsReflectionAdapter adapter = new LuckPermsReflectionAdapter(
                getClass().getClassLoader(),
                "missing.luckperms.Api",
                name -> false,
                serviceClass -> new FakeRegistration(new FakeLuckPermsApi(new FakeUserManager(new FakeUser("admin")))),
                () -> true
        );

        assertFalse(adapter.apiAvailable());
        assertTrue(adapter.primaryGroup(UUID.randomUUID()).isEmpty());
    }

    @Test
    void unavailableWhenProviderMissing() {
        LuckPermsReflectionAdapter adapter = adapter(serviceClass -> null);

        assertFalse(adapter.apiAvailable());
    }

    @Test
    void defaultWeightWhenUserMissing() {
        LuckPermsReflectionAdapter adapter = adapter(serviceClass ->
                new FakeRegistration(new FakeLuckPermsApi(new FakeUserManager(null))));

        Optional<String> primaryGroup = adapter.primaryGroup(UUID.randomUUID());

        assertTrue(primaryGroup.isEmpty());
    }

    private LuckPermsReflectionAdapter adapter(LuckPermsReflectionAdapter.ServiceRegistrationLookup lookup) {
        return new LuckPermsReflectionAdapter(
                getClass().getClassLoader(),
                FakeLuckPermsApi.class.getName(),
                name -> true,
                lookup,
                () -> true
        );
    }

    static final class FakeRegistration {
        private final Object provider;

        FakeRegistration(Object provider) {
            this.provider = provider;
        }

        public Object getProvider() {
            return provider;
        }
    }

    static final class FakeLuckPermsApi {
        private final FakeUserManager userManager;

        FakeLuckPermsApi(FakeUserManager userManager) {
            this.userManager = userManager;
        }

        public FakeUserManager getUserManager() {
            return userManager;
        }
    }

    static final class FakeUserManager {
        private final FakeUser user;

        FakeUserManager(FakeUser user) {
            this.user = user;
        }

        public FakeUser getUser(UUID ignored) {
            return user;
        }
    }

    static final class FakeUser {
        private final String primaryGroup;

        FakeUser(String primaryGroup) {
            this.primaryGroup = primaryGroup;
        }

        public String getPrimaryGroup() {
            return primaryGroup;
        }
    }
}
