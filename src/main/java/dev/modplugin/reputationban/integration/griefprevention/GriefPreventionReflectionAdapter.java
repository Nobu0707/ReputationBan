package dev.modplugin.reputationban.integration.griefprevention;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public class GriefPreventionReflectionAdapter {
    private static final List<String> API_CLASSES = List.of(
            "me.ryanhamshire.GriefPrevention.GriefPrevention",
            "me.ryanhamshire.griefprevention.GriefPrevention"
    );

    private final PluginLookup pluginLookup;
    private final BooleanSupplier primaryThread;

    public GriefPreventionReflectionAdapter(JavaPlugin plugin) {
        this(ignored -> plugin.getServer().getPluginManager().getPlugin(ignored), Bukkit::isPrimaryThread);
    }

    GriefPreventionReflectionAdapter(PluginLookup pluginLookup, BooleanSupplier primaryThread) {
        this.pluginLookup = pluginLookup;
        this.primaryThread = primaryThread;
    }

    public boolean pluginPresent() {
        return pluginLookup.plugin("GriefPrevention") != null;
    }

    public boolean apiAvailable() {
        ensurePrimaryThread();
        if (!pluginPresent()) {
            return false;
        }
        try {
            Object api = apiInstance();
            Object store = field(api, "dataStore");
            return store != null && getClaimAtMethod(store) != null;
        } catch (ClassNotFoundException | RuntimeException exception) {
            return false;
        }
    }

    public Optional<GriefPreventionClaimSummary> claimSummary(
            Location location,
            boolean includeClaimOwner,
            boolean includeTrustCounts,
            boolean includeBoundaries
    ) {
        ensurePrimaryThread();
        if (location == null || !apiAvailable()) {
            return Optional.empty();
        }
        String world = location.getWorld() == null ? "unknown" : location.getWorld().getName();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        try {
            Object store = field(apiInstance(), "dataStore");
            Method method = getClaimAtMethod(store);
            Object rawClaim = method == null ? null : method.invoke(store, location, false, null);
            if (rawClaim == null) {
                return Optional.of(GriefPreventionClaimSummary.absent(world, x, y, z));
            }
            return Optional.of(GriefPreventionClaimSummary.present(
                    world,
                    x,
                    y,
                    z,
                    entry(rawClaim, includeClaimOwner, includeTrustCounts, includeBoundaries)
            ));
        } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            return Optional.empty();
        }
    }

    private static GriefPreventionClaimEntry entry(
            Object rawClaim,
            boolean includeClaimOwner,
            boolean includeTrustCounts,
            boolean includeBoundaries
    ) {
        String claimId = firstNonBlank(
                stringValue(invoke(rawClaim, "getID")),
                stringValue(field(rawClaim, "id"))
        );
        boolean adminClaim = booleanValue(invoke(rawClaim, "isAdminClaim")) || booleanValue(field(rawClaim, "adminClaim"));
        String owner = GriefPreventionClaimPolicy.ownerValue(includeClaimOwner, firstNonBlank(
                stringValue(invoke(rawClaim, "getOwnerName")),
                stringValue(field(rawClaim, "ownerID"))
        ));
        String bounds = GriefPreventionClaimPolicy.boundsValue(
                includeBoundaries,
                bounds(field(rawClaim, "lesserBoundaryCorner"), field(rawClaim, "greaterBoundaryCorner"))
        );
        String trustCounts = GriefPreventionClaimPolicy.trustCountsValue(
                includeTrustCounts,
                collectionSize(field(rawClaim, "builders")),
                collectionSize(field(rawClaim, "containers")),
                collectionSize(field(rawClaim, "accessors")),
                collectionSize(field(rawClaim, "managers"))
        );
        return new GriefPreventionClaimEntry(
                claimId.isBlank() ? "unknown" : claimId,
                adminClaim,
                owner,
                bounds,
                trustCounts
        );
    }

    private Object apiInstance() throws ClassNotFoundException {
        for (String className : API_CLASSES) {
            try {
                Class<?> clazz = Class.forName(className);
                Object instance = field(clazz, "instance");
                if (instance != null) {
                    return instance;
                }
                instance = invokeStatic(clazz, "getInstance");
                if (instance != null) {
                    return instance;
                }
            } catch (ClassNotFoundException exception) {
                // Try the next known package spelling.
            }
        }
        throw new ClassNotFoundException("GriefPrevention API class not found");
    }

    private static Method getClaimAtMethod(Object store) {
        if (store == null) {
            return null;
        }
        for (Method method : store.getClass().getMethods()) {
            if (!"getClaimAt".equals(method.getName()) || method.getParameterCount() != 3) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (Location.class.isAssignableFrom(parameterTypes[0])
                    && (boolean.class.equals(parameterTypes[1]) || Boolean.class.equals(parameterTypes[1]))) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private void ensurePrimaryThread() {
        if (!primaryThread.getAsBoolean()) {
            throw new IllegalStateException("GriefPrevention claim query must run on the main thread");
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

    private static Object field(Object target, String fieldName) {
        if (target == null) {
            return null;
        }
        Class<?> targetClass = target instanceof Class<?> clazz ? clazz : target.getClass();
        Object receiver = target instanceof Class<?> ? null : target;
        try {
            Field field = targetClass.getField(fieldName);
            field.setAccessible(true);
            return field.get(receiver);
        } catch (IllegalAccessException | NoSuchFieldException | RuntimeException exception) {
            try {
                Field declared = targetClass.getDeclaredField(fieldName);
                declared.setAccessible(true);
                return declared.get(receiver);
            } catch (IllegalAccessException | NoSuchFieldException | RuntimeException ignored) {
                return null;
            }
        }
    }

    private static String bounds(Object lesser, Object greater) {
        String left = corner(lesser);
        String right = corner(greater);
        if (left.isBlank() || right.isBlank()) {
            return "unknown";
        }
        return left + "-" + right;
    }

    private static String corner(Object value) {
        if (value instanceof Location location) {
            return "(%d,%d,%d)".formatted(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }
        Integer x = coordinate(value, "getBlockX", "x");
        Integer y = coordinate(value, "getBlockY", "y");
        Integer z = coordinate(value, "getBlockZ", "z");
        if (x == null || y == null || z == null) {
            return "";
        }
        return "(%d,%d,%d)".formatted(x, y, z);
    }

    private static Integer coordinate(Object value, String methodName, String fieldName) {
        Object methodValue = invoke(value, methodName);
        if (methodValue instanceof Number number) {
            return number.intValue();
        }
        Object fieldValue = field(value, fieldName);
        return fieldValue instanceof Number number ? number.intValue() : null;
    }

    private static int collectionSize(Object value) {
        return value instanceof Collection<?> collection ? collection.size() : 0;
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean bool && bool;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? (second == null ? "" : second) : first;
    }

    interface PluginLookup {
        Object plugin(String name);
    }
}
