package dev.modplugin.reputationban.integration.coreprotect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public final class CoreProtectReflectionAdapter {
    private final PluginLookup pluginLookup;
    private final BooleanSupplier primaryThread;

    public CoreProtectReflectionAdapter(JavaPlugin plugin) {
        this(ignored -> plugin.getServer().getPluginManager().getPlugin("CoreProtect"), Bukkit::isPrimaryThread);
    }

    CoreProtectReflectionAdapter(PluginLookup pluginLookup, BooleanSupplier primaryThread) {
        this.pluginLookup = pluginLookup;
        this.primaryThread = primaryThread;
    }

    public boolean pluginPresent() {
        return pluginLookup.plugin("CoreProtect") != null;
    }

    public Optional<ApiHandle> api(int minimumApiVersion) {
        ensurePrimaryThread();
        Object plugin = pluginLookup.plugin("CoreProtect");
        if (plugin == null) {
            return Optional.empty();
        }
        Object api = invoke(plugin, "getAPI");
        if (api == null || !isEnabled(api)) {
            return Optional.empty();
        }
        int apiVersion = apiVersion(api);
        if (apiVersion < minimumApiVersion) {
            return Optional.empty();
        }
        return Optional.of(new ApiHandle(api, apiVersion));
    }

    public Optional<Integer> apiVersion(int minimumApiVersion) {
        return api(minimumApiVersion).map(ApiHandle::apiVersion);
    }

    public CoreProtectLookupResult performLookup(
            ApiHandle apiHandle,
            int lookupSeconds,
            List<String> players,
            List<Integer> actions,
            int radius,
            Location location,
            int maxResults
    ) {
        Object rawResults = invokeByName(
                apiHandle.api(),
                "performLookup",
                lookupSeconds,
                players,
                null,
                null,
                null,
                actions == null || actions.isEmpty() ? null : actions,
                radius,
                radius > 0 ? location : null
        );
        if (!(rawResults instanceof List<?> results) || results.isEmpty()) {
            return CoreProtectLookupResult.empty(apiHandle.apiVersion());
        }
        int count = CoreProtectEvidencePolicy.clampMaxResults(maxResults, results.size());
        List<CoreProtectLookupEntry> entries = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            Object result = results.get(index);
            if (!(result instanceof String[] rawResult)) {
                continue;
            }
            Object parsed = invokeByName(apiHandle.api(), "parseResult", (Object) rawResult);
            if (parsed != null) {
                entries.add(toEntry(parsed));
            }
        }
        return new CoreProtectLookupResult(apiHandle.apiVersion(), results.size(), List.copyOf(entries));
    }

    private static CoreProtectLookupEntry toEntry(Object parsed) {
        return new CoreProtectLookupEntry(
                stringValue(invoke(parsed, "getActionString")),
                stringValue(invoke(parsed, "getPlayer")),
                intValue(invoke(parsed, "getX")),
                intValue(invoke(parsed, "getY")),
                intValue(invoke(parsed, "getZ")),
                stringValue(invoke(parsed, "getType"))
        );
    }

    private void ensurePrimaryThread() {
        if (!primaryThread.getAsBoolean()) {
            throw new IllegalStateException("CoreProtect plugin lookup must run on the main thread");
        }
    }

    private static boolean isEnabled(Object api) {
        Object enabled = invoke(api, "isEnabled");
        return enabled instanceof Boolean value && value;
    }

    private static int apiVersion(Object api) {
        Object version = invoke(api, "APIVersion");
        return version instanceof Number number ? number.intValue() : -1;
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

    private static Object invokeByName(Object target, String methodName, Object... parameters) {
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == parameters.length) {
                try {
                    method.setAccessible(true);
                    return method.invoke(target, parameters);
                } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
                    return null;
                }
            }
        }
        return null;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private static int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    public record ApiHandle(Object api, int apiVersion) {
    }

    interface PluginLookup {
        Object plugin(String name);
    }
}
