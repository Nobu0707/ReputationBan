package dev.modplugin.reputationban.integration.placeholderapi;

import java.util.List;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ReputationBanPlaceholderExpansion extends PlaceholderExpansion {
    private final JavaPlugin plugin;
    private final String identifier;
    private final PlaceholderValueProvider valueProvider;

    public ReputationBanPlaceholderExpansion(
            JavaPlugin plugin,
            String identifier,
            PlaceholderValueProvider valueProvider
    ) {
        this.plugin = plugin;
        this.identifier = identifier;
        this.valueProvider = valueProvider;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String getAuthor() {
        List<String> authors = plugin.getPluginMeta().getAuthors();
        return authors == null || authors.isEmpty() ? "MOD_PLUGIN" : String.join(",", authors);
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        return valueProvider.value(player, params);
    }
}
