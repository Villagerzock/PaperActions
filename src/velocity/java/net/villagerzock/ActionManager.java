package net.villagerzock;

import com.velocitypowered.api.plugin.Plugin;
import net.villagerzock.annotations.PluginIdentifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ActionManager {
    private static final ActionRegistry<Object> REGISTRY = new ActionRegistry<>(ActionManager::getPluginId);

    private ActionManager() {
    }

    public static ActionRegistry.PluginActions<Object> getInstance(Object plugin) {
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

    private static String getPluginId(Object plugin) {
        Class<?> pluginClass = plugin.getClass();
        if (pluginClass.isAnnotationPresent(PluginIdentifier.class)) {
            return pluginClass.getAnnotation(PluginIdentifier.class).value();
        }
        if (pluginClass.isAnnotationPresent(Plugin.class)) {
            return pluginClass.getAnnotation(Plugin.class).id();
        }
        return pluginClass.getName();
    }
}
