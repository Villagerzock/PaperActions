package net.villagerzock;

import net.md_5.bungee.api.plugin.Plugin;
import net.villagerzock.annotations.PluginIdentifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ActionManager {
    private static final ActionRegistry<Plugin> REGISTRY = new ActionRegistry<>(ActionManager::getPluginId);

    private ActionManager() {
    }

    public static ActionRegistry.PluginActions<Plugin> getInstance(Plugin plugin) {
        return REGISTRY.getInstance(plugin);
    }

    public static void executeAction(String id, Map<String, Object> context) {
        REGISTRY.executeAction(id, context);
    }

    public static String getActionDescription(String id) {
        return REGISTRY.getActionDescription(id);
    }

    public static LinkedHashMap<String, Class<?>> getParameterTypes(String id) {
        return REGISTRY.getParameterTypes(id);
    }

    public static List<String> getAllActions() {
        return REGISTRY.getAllActions();
    }

    private static String getPluginId(Plugin plugin) {
        if (plugin.getClass().isAnnotationPresent(PluginIdentifier.class)) {
            return plugin.getClass().getAnnotation(PluginIdentifier.class).value();
        }
        return plugin.getDescription().getName();
    }
}
