package dev.modplugin.reputationban.integration.placeholderapi;

import dev.modplugin.reputationban.config.PluginConfig;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlaceholderApiIntegration {
    private static final String PLUGIN_NAME = "PlaceholderAPI";
    private final JavaPlugin plugin;
    private final Supplier<PluginConfig> configSupplier;
    private final PlaceholderValueProvider valueProvider;
    private Object expansion;
    private boolean registered;
    private String lastMessage = "not registered";

    public PlaceholderApiIntegration(
            JavaPlugin plugin,
            Supplier<PluginConfig> configSupplier,
            PlaceholderValueProvider valueProvider
    ) {
        this.plugin = plugin;
        this.configSupplier = configSupplier;
        this.valueProvider = valueProvider;
    }

    public void register() {
        unregister();
        PluginConfig.PlaceholderApiIntegrationConfig config = configSupplier.get().placeholderApiIntegration();
        if (!config.enabled()) {
            lastMessage = "disabled";
            return;
        }
        if (!pluginPresent()) {
            lastMessage = "PlaceholderAPI not found";
            return;
        }
        try {
            String identifier = identifier(config.identifier());
            Class<?> expansionType = Class.forName(
                    "dev.modplugin.reputationban.integration.placeholderapi.ReputationBanPlaceholderExpansion",
                    true,
                    plugin.getClass().getClassLoader()
            );
            Constructor<?> constructor = expansionType.getConstructor(
                    JavaPlugin.class,
                    String.class,
                    PlaceholderValueProvider.class
            );
            expansion = constructor.newInstance(plugin, identifier, valueProvider);
            Method registerMethod = expansionType.getMethod("register");
            Object result = registerMethod.invoke(expansion);
            registered = Boolean.TRUE.equals(result);
            lastMessage = registered ? "active" : "register returned false";
        } catch (ReflectiveOperationException | LinkageError exception) {
            registered = false;
            expansion = null;
            lastMessage = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            plugin.getLogger().log(Level.WARNING, "PlaceholderAPI expansion registration failed", exception);
        }
    }

    public void unregister() {
        if (expansion == null) {
            registered = false;
            return;
        }
        try {
            Method unregisterMethod = expansion.getClass().getMethod("unregister");
            unregisterMethod.invoke(expansion);
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().log(Level.WARNING, "PlaceholderAPI expansion unregister failed", exception);
        } finally {
            expansion = null;
            registered = false;
        }
    }

    public PlaceholderApiStatus status() {
        PluginConfig.PlaceholderApiIntegrationConfig config = configSupplier.get().placeholderApiIntegration();
        boolean present = pluginPresent();
        boolean apiAvailable = present && apiAvailable();
        boolean active = config.enabled() && present && apiAvailable && registered;
        return new PlaceholderApiStatus(
                config.enabled(),
                present,
                apiAvailable,
                active,
                identifier(config.identifier()),
                config.cacheRefreshSeconds(),
                registered,
                message(config.enabled(), present, apiAvailable)
        );
    }

    public List<String> placeholderExamples() {
        String identifier = status().identifier();
        return valueProvider.placeholderKeys().stream()
                .map(key -> "%" + identifier + "_" + key + "%")
                .toList();
    }

    public String sampleFor(org.bukkit.command.CommandSender sender) {
        if (sender instanceof org.bukkit.entity.Player player) {
            return valueProvider.value(player, "score");
        }
        return valueProvider.value((java.util.UUID) null, "version");
    }

    public boolean registered() {
        return registered;
    }

    public static String identifier(String configured) {
        if (configured == null || configured.isBlank()) {
            return "reputationban";
        }
        String normalized = configured.toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9_]+")) {
            return "reputationban";
        }
        return normalized;
    }

    private boolean pluginPresent() {
        return plugin.getServer().getPluginManager().getPlugin(PLUGIN_NAME) != null;
    }

    private boolean apiAvailable() {
        return pluginPresent();
    }

    private String message(boolean configuredEnabled, boolean pluginPresent, boolean apiAvailable) {
        if (!configuredEnabled) {
            return "disabled";
        }
        if (!pluginPresent) {
            return "PlaceholderAPI not found";
        }
        if (!apiAvailable) {
            return "api unavailable";
        }
        return lastMessage;
    }
}
