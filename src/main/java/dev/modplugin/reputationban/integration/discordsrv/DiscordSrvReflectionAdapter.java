package dev.modplugin.reputationban.integration.discordsrv;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class DiscordSrvReflectionAdapter {
    private static final String API_CLASS = "github.scarsz.discordsrv.DiscordSRV";

    private final PluginLookup pluginLookup;
    private final BooleanSupplier primaryThread;

    public DiscordSrvReflectionAdapter(JavaPlugin plugin) {
        this(ignored -> plugin.getServer().getPluginManager().getPlugin("DiscordSRV"), Bukkit::isPrimaryThread);
    }

    DiscordSrvReflectionAdapter(PluginLookup pluginLookup, BooleanSupplier primaryThread) {
        this.pluginLookup = pluginLookup;
        this.primaryThread = primaryThread;
    }

    public boolean pluginPresent() {
        return pluginLookup.plugin("DiscordSRV") != null;
    }

    public boolean apiAvailable() {
        if (!pluginPresent()) {
            return false;
        }
        try {
            Object manager = accountLinkManager();
            return manager != null && getDiscordIdMethod(manager) != null;
        } catch (ClassNotFoundException | RuntimeException exception) {
            return false;
        }
    }

    public boolean accountLinkAvailable() {
        return apiAvailable();
    }

    public Optional<String> discordId(UUID playerUuid) {
        ensurePrimaryThread();
        if (playerUuid == null || !apiAvailable()) {
            return Optional.empty();
        }
        try {
            Object manager = accountLinkManager();
            Method method = getDiscordIdMethod(manager);
            Object value = method == null ? null : method.invoke(manager, playerUuid);
            if (value == null || value.toString().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(value.toString());
        } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            return Optional.empty();
        }
    }

    public boolean sendMessage(String channelName, String message) {
        ensurePrimaryThread();
        if (channelName == null || channelName.isBlank() || message == null || message.isBlank()) {
            return false;
        }
        try {
            Object plugin = channelResolverPlugin();
            Object channel = invoke(plugin, "getDestinationTextChannelForGameChannelName", String.class, channelName);
            if (channel == null) {
                return false;
            }
            Object action = invoke(channel, "sendMessage", String.class, message);
            if (action == null) {
                return false;
            }
            invoke(action, "queue");
            return true;
        } catch (ClassNotFoundException | RuntimeException exception) {
            return false;
        }
    }

    private Object accountLinkManager() throws ClassNotFoundException {
        Object plugin = pluginLookup.plugin("DiscordSRV");
        Object manager = null;
        if (hasMethod(plugin, "getAccountLinkManager")) {
            manager = invoke(plugin, "getAccountLinkManager");
        }
        if (manager != null) {
            return manager;
        }
        Object fallbackPlugin = staticPluginInstance();
        return invoke(fallbackPlugin, "getAccountLinkManager");
    }

    private Object channelResolverPlugin() throws ClassNotFoundException {
        Object plugin = pluginLookup.plugin("DiscordSRV");
        if (hasMethod(plugin, "getDestinationTextChannelForGameChannelName", String.class)) {
            return plugin;
        }
        return staticPluginInstance();
    }

    private Object staticPluginInstance() throws ClassNotFoundException {
        Class<?> clazz = Class.forName(API_CLASS);
        Object plugin = invokeStatic(clazz, "getPlugin");
        if (plugin == null) {
            throw new IllegalStateException("DiscordSRV plugin instance unavailable");
        }
        return plugin;
    }

    private static boolean hasMethod(Object target, String methodName, Class<?>... parameterTypes) {
        if (target == null) {
            return false;
        }
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return true;
        } catch (NoSuchMethodException | RuntimeException exception) {
            return false;
        }
    }

    private static Method getDiscordIdMethod(Object manager) {
        if (manager == null) {
            return null;
        }
        for (Method method : manager.getClass().getMethods()) {
            if (!"getDiscordId".equals(method.getName()) || method.getParameterCount() != 1) {
                continue;
            }
            if (UUID.class.isAssignableFrom(method.getParameterTypes()[0])) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private void ensurePrimaryThread() {
        if (!primaryThread.getAsBoolean()) {
            throw new IllegalStateException("DiscordSRV reflection calls must run on the main thread");
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

    private static Object invoke(Object target, String methodName, Class<?> parameterType, Object argument) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName, parameterType);
            method.setAccessible(true);
            return method.invoke(target, argument);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | RuntimeException exception) {
            return null;
        }
    }

    interface PluginLookup {
        Object plugin(String name);
    }
}
