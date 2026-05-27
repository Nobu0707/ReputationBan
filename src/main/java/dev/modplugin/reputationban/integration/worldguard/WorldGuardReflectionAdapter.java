package dev.modplugin.reputationban.integration.worldguard;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldGuardReflectionAdapter {
    private static final String WORLD_GUARD_CLASS = "com.sk89q.worldguard.WorldGuard";
    private static final String BUKKIT_ADAPTER_CLASS = "com.sk89q.worldedit.bukkit.BukkitAdapter";

    private final PluginLookup pluginLookup;
    private final BooleanSupplier primaryThread;

    public WorldGuardReflectionAdapter(JavaPlugin plugin) {
        this(ignored -> plugin.getServer().getPluginManager().getPlugin(ignored), Bukkit::isPrimaryThread);
    }

    WorldGuardReflectionAdapter(PluginLookup pluginLookup, BooleanSupplier primaryThread) {
        this.pluginLookup = pluginLookup;
        this.primaryThread = primaryThread;
    }

    public boolean worldGuardPresent() {
        return pluginLookup.plugin("WorldGuard") != null;
    }

    public boolean worldEditPresent() {
        return pluginLookup.plugin("WorldEdit") != null;
    }

    public boolean apiAvailable() {
        ensurePrimaryThread();
        if (!worldGuardPresent() || !worldEditPresent()) {
            return false;
        }
        try {
            Object instance = invokeStatic(Class.forName(WORLD_GUARD_CLASS), "getInstance");
            Object platform = invoke(instance, "getPlatform");
            Object container = invoke(platform, "getRegionContainer");
            Object query = invoke(container, "createQuery");
            Class.forName(BUKKIT_ADAPTER_CLASS);
            return query != null;
        } catch (ClassNotFoundException | RuntimeException exception) {
            return false;
        }
    }

    public Optional<WorldGuardRegionSummary> regionSummary(
            Location location,
            int maxRegions,
            boolean includeOwners,
            boolean includeMembers,
            List<String> includeFlags
    ) {
        ensurePrimaryThread();
        if (location == null || !apiAvailable()) {
            return Optional.empty();
        }
        try {
            Object query = query();
            Object adaptedLocation = adaptLocation(location);
            Object rawSet = invokeByName(query, "getApplicableRegions", adaptedLocation);
            List<WorldGuardRegionEntry> entries = rawSet instanceof Iterable<?> iterable
                    ? entries(iterable, includeOwners, includeMembers, includeFlags)
                    : List.of();
            String world = location.getWorld() == null ? "unknown" : location.getWorld().getName();
            return Optional.of(WorldGuardRegionSummary.create(
                    entries.size(),
                    world,
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ(),
                    maxRegions,
                    entries
            ));
        } catch (ClassNotFoundException | RuntimeException exception) {
            return Optional.empty();
        }
    }

    private static List<WorldGuardRegionEntry> entries(
            Iterable<?> rawRegions,
            boolean includeOwners,
            boolean includeMembers,
            List<String> includeFlags
    ) {
        List<WorldGuardRegionEntry> entries = new ArrayList<>();
        for (Object rawRegion : rawRegions) {
            entries.add(new WorldGuardRegionEntry(
                    stringValue(invoke(rawRegion, "getId")),
                    intValue(invoke(rawRegion, "getPriority")),
                    WorldGuardRegionPolicy.ownerMemberValue(includeOwners, domainCount(invoke(rawRegion, "getOwners"))),
                    WorldGuardRegionPolicy.ownerMemberValue(includeMembers, domainCount(invoke(rawRegion, "getMembers"))),
                    flags(invoke(rawRegion, "getFlags"), includeFlags)
            ));
        }
        return List.copyOf(entries);
    }

    private static Map<String, String> flags(Object rawFlags, List<String> includeFlags) {
        if (!(rawFlags instanceof Map<?, ?> map) || includeFlags == null || includeFlags.isEmpty()) {
            return Map.of();
        }
        List<String> normalized = includeFlags.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .toList();
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String name = stringValue(invoke(entry.getKey(), "getName"));
            if (name.isBlank() || !normalized.contains(name.toLowerCase(Locale.ROOT))) {
                continue;
            }
            values.put(name, stringValue(entry.getValue()));
        }
        return Map.copyOf(values);
    }

    private Object query() throws ClassNotFoundException {
        Object instance = invokeStatic(Class.forName(WORLD_GUARD_CLASS), "getInstance");
        Object platform = invoke(instance, "getPlatform");
        Object container = invoke(platform, "getRegionContainer");
        return invoke(container, "createQuery");
    }

    private static Object adaptLocation(Location location) throws ClassNotFoundException {
        return invokeByName(Class.forName(BUKKIT_ADAPTER_CLASS), "adapt", location);
    }

    private void ensurePrimaryThread() {
        if (!primaryThread.getAsBoolean()) {
            throw new IllegalStateException("WorldGuard region query must run on the main thread");
        }
    }

    private static Object invoke(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | RuntimeException exception) {
            return null;
        }
    }

    private static Object invokeStatic(Class<?> targetClass, String methodName) {
        try {
            Method method = targetClass.getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | RuntimeException exception) {
            return null;
        }
    }

    private static Object invokeByName(Object target, String methodName, Object... parameters) {
        if (target == null) {
            return null;
        }
        Class<?> targetClass = target instanceof Class<?> clazz ? clazz : target.getClass();
        Object receiver = target instanceof Class<?> ? null : target;
        for (Method method : targetClass.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == parameters.length) {
                try {
                    method.setAccessible(true);
                    return method.invoke(receiver, parameters);
                } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
                    return null;
                }
            }
        }
        return null;
    }

    private static int domainCount(Object domain) {
        Object size = invoke(domain, "size");
        if (size instanceof Number number) {
            return number.intValue();
        }
        Object players = invoke(domain, "getPlayers");
        Object uniqueIds = invoke(domain, "getUniqueIds");
        return collectionSize(players) + collectionSize(uniqueIds);
    }

    private static int collectionSize(Object value) {
        return value instanceof java.util.Collection<?> collection ? collection.size() : 0;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private static int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    interface PluginLookup {
        Object plugin(String name);
    }
}
