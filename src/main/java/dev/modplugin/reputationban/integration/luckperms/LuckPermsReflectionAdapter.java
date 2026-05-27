package dev.modplugin.reputationban.integration.luckperms;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class LuckPermsReflectionAdapter {
    private static final String LUCKPERMS_API_CLASS = "net.luckperms.api.LuckPerms";

    private final ClassLoader classLoader;
    private final String apiClassName;
    private final PluginPresenceLookup pluginPresenceLookup;
    private final ServiceRegistrationLookup serviceRegistrationLookup;
    private final BooleanSupplier primaryThread;

    public LuckPermsReflectionAdapter(JavaPlugin plugin) {
        this(
                plugin.getClass().getClassLoader(),
                LUCKPERMS_API_CLASS,
                name -> plugin.getServer().getPluginManager().getPlugin(name) != null,
                serviceClass -> Bukkit.getServicesManager().getRegistration(serviceClass),
                Bukkit::isPrimaryThread
        );
    }

    LuckPermsReflectionAdapter(
            ClassLoader classLoader,
            String apiClassName,
            PluginPresenceLookup pluginPresenceLookup,
            ServiceRegistrationLookup serviceRegistrationLookup,
            BooleanSupplier primaryThread
    ) {
        this.classLoader = classLoader;
        this.apiClassName = apiClassName;
        this.pluginPresenceLookup = pluginPresenceLookup;
        this.serviceRegistrationLookup = serviceRegistrationLookup;
        this.primaryThread = primaryThread;
    }

    public boolean pluginPresent() {
        return pluginPresenceLookup.isPresent("LuckPerms");
    }

    public boolean apiAvailable() {
        return api().isPresent();
    }

    public Optional<String> primaryGroup(UUID playerUuid) {
        Optional<Object> api = api();
        if (api.isEmpty()) {
            return Optional.empty();
        }
        Object userManager = invoke(api.get(), "getUserManager");
        if (userManager == null) {
            return Optional.empty();
        }
        Object user = invoke(userManager, "getUser", UUID.class, playerUuid);
        if (user == null) {
            return Optional.empty();
        }
        Object primaryGroup = invoke(user, "getPrimaryGroup");
        if (!(primaryGroup instanceof String value) || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    private Optional<Object> api() {
        ensurePrimaryThread();
        Class<?> apiClass = optionalClass(apiClassName);
        if (apiClass == null) {
            return Optional.empty();
        }
        Object registration = serviceRegistrationLookup.registration(apiClass);
        if (registration == null) {
            return Optional.empty();
        }
        Object provider = invoke(registration, "getProvider");
        return provider == null ? Optional.empty() : Optional.of(provider);
    }

    private void ensurePrimaryThread() {
        if (!primaryThread.getAsBoolean()) {
            throw new IllegalStateException("LuckPerms service lookup must run on the main thread");
        }
    }

    private Class<?> optionalClass(String className) {
        try {
            if (LUCKPERMS_API_CLASS.equals(className)) {
                return Class.forName("net.luckperms.api.LuckPerms", false, classLoader);
            }
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException | LinkageError error) {
            return null;
        }
    }

    private static Object invoke(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | RuntimeException exception) {
            return null;
        }
    }

    private static Object invoke(Object target, String methodName, Class<?> parameterType, Object parameter) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterType);
            method.setAccessible(true);
            return method.invoke(target, parameter);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | RuntimeException exception) {
            return null;
        }
    }

    interface PluginPresenceLookup {
        boolean isPresent(String name);
    }

    interface ServiceRegistrationLookup {
        Object registration(Class<?> serviceClass);
    }
}
