package net.villagerzock;

import net.villagerzock.annotations.Action;
import net.villagerzock.annotations.Param;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class ActionRegistry<P> {
    private final Map<P, PluginActions<P>> managers = new HashMap<>();
    private final Function<P, String> pluginIdResolver;
    private final Consumer<String> warningLogger;

    public ActionRegistry(Function<P, String> pluginIdResolver) {
        this(pluginIdResolver, ignored -> {
        });
    }

    public ActionRegistry(Function<P, String> pluginIdResolver, Consumer<String> warningLogger) {
        this.pluginIdResolver = Objects.requireNonNull(pluginIdResolver, "pluginIdResolver");
        this.warningLogger = Objects.requireNonNull(warningLogger, "warningLogger");
    }

    public PluginActions<P> getInstance(P plugin) {
        Objects.requireNonNull(plugin, "plugin");
        return managers.computeIfAbsent(plugin, key -> new PluginActions<>(this, key, pluginIdResolver.apply(key)));
    }

    public void executeAction(String id, Map<String, Object> context) {
        ActionId actionId = parseId(id);

        for (PluginActions<P> manager : managers.values()) {
            if (manager.pluginId.equalsIgnoreCase(actionId.pluginId()) && manager.actions.containsKey(actionId.actionName())) {
                manager.actions.get(actionId.actionName()).execute(context);
                return;
            }
        }
    }

    public String getActionDescription(String id) {
        ActionId actionId = parseId(id);

        for (PluginActions<P> manager : managers.values()) {
            if (manager.pluginId.equalsIgnoreCase(actionId.pluginId()) && manager.actions.containsKey(actionId.actionName())) {
                return manager.actions.get(actionId.actionName()).getDescription();
            }
        }
        return "";
    }

    public LinkedHashMap<String, Class<?>> getParameterTypes(String id) {
        ActionId actionId = parseId(id);

        for (PluginActions<P> manager : managers.values()) {
            if (manager.pluginId.equalsIgnoreCase(actionId.pluginId()) && manager.actions.containsKey(actionId.actionName())) {
                return manager.actions.get(actionId.actionName()).getParameterTypes();
            }
        }
        return null;
    }

    public List<String> getAllActions() {
        List<String> result = new ArrayList<>();
        for (PluginActions<P> manager : managers.values()) {
            for (String name : manager.actions.keySet()) {
                result.add("%s:%s".formatted(manager.pluginId, name));
            }
        }
        return result;
    }

    private static ActionId parseId(String id) {
        String[] parts = id.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("'id' needs to have 2 Parts. Example: 'plugin:name'");
        }
        return new ActionId(parts[0], parts[1]);
    }

    private void warn(String message) {
        warningLogger.accept(message);
    }

    private record ActionId(String pluginId, String actionName) {
    }

    public static final class PluginActions<P> {
        private final ActionRegistry<P> registry;
        private final P plugin;
        private final String pluginId;
        private final Map<String, IAction> actions = new HashMap<>();

        private PluginActions(ActionRegistry<P> registry, P plugin, String pluginId) {
            this.registry = registry;
            this.plugin = plugin;
            this.pluginId = Objects.requireNonNull(pluginId, "pluginId");
        }

        public PluginActions<P> register(Class<?> clazz) {
            List<Method> methods = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(value -> value.isAnnotationPresent(Action.class))
                    .toList();

            for (Method method : methods) {
                Action actionMeta = method.getAnnotation(Action.class);
                String name = actionMeta.value();
                if ((method.getModifiers() & Modifier.STATIC) == 0) {
                    throw new IllegalArgumentException("Method '%s' is not static. Methods Annotated with @Action need to be static.".formatted(method.getName()));
                }

                IAction action = new IAction() {
                    @Override
                    public void execute(Map<String, Object> context) {
                        LinkedHashMap<String, Class<?>> paramTypes = getParameterTypes();
                        int paramSize = method.getParameterCount();
                        Object[] params = new Object[paramSize];
                        List<String> keys = new ArrayList<>(paramTypes.keySet());
                        for (int i = 0; i < paramSize; i++) {
                            String key = keys.get(i);
                            List<Annotation> annotations = List.of(method.getParameterAnnotations()[i]);
                            Optional<Param> paramAnnotation = annotations.stream()
                                    .filter(Param.class::isInstance)
                                    .map(Param.class::cast)
                                    .findFirst();

                            if (!context.containsKey(key) && (paramAnnotation.isEmpty() || paramAnnotation.get().required())) {
                                throw new IllegalStateException("Missing argument '%s' in action '%s:%s'".formatted(key, pluginId, name));
                            }

                            Object value = context.get(key);
                            Class<?> paramType = paramTypes.get(key);
                            if (value != null && !paramType.isAssignableFrom(value.getClass())) {
                                throw new IllegalArgumentException("Value of type '%s' cannot be assigned to Parameter of type '%s'".formatted(value.getClass().getCanonicalName(), paramType.getCanonicalName()));
                            }

                            params[i] = value;
                            if (params[i] == null && paramType.isPrimitive()) {
                                switch (paramType.getName()) {
                                    case "boolean" -> params[i] = false;
                                    case "byte" -> params[i] = (byte) 0;
                                    case "short" -> params[i] = (short) 0;
                                    case "int" -> params[i] = 0;
                                    case "long" -> params[i] = 0L;
                                    case "float" -> params[i] = 0.0F;
                                    case "double" -> params[i] = 0.0D;
                                    case "char" -> params[i] = '\0';
                                    default -> throw new IllegalStateException("Unknown primitive type: " + paramType);
                                }
                            }
                        }

                        try {
                            if (actionMeta.deprecated()) {
                                registry.warn("Called Deprecated Action '%s:%s'. This Action is deprecated and might be removed in future versions.".formatted(pluginId, name));
                            }
                            method.invoke(null, params);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public LinkedHashMap<String, Class<?>> getParameterTypes() {
                        LinkedHashMap<String, Class<?>> result = new LinkedHashMap<>();
                        for (Parameter parameter : method.getParameters()) {
                            String key;

                            Param param = parameter.getAnnotation(Param.class);

                            if (param != null && !param.name().isEmpty()) {
                                key = param.name();
                            } else {
                                key = parameter.getName();

                                if (!parameter.isNamePresent()) {
                                    registry.warn(
                                            "Parameter '%s' in action '%s:%s' has no runtime name information. Using the synthetic name '%s' instead. Compile with '-parameters' or annotate the parameter with @Param."
                                                    .formatted(parameter, pluginId, name, key)
                                    );
                                }
                            }

                            result.put(key, parameter.getType());
                        }
                        return result;
                    }

                    @Override
                    public String getDescription() {
                        return actionMeta.description();
                    }
                };

                actions.put(name, action);
            }
            return this;
        }

        public P getPlugin() {
            return plugin;
        }

        public String getPluginId() {
            return pluginId;
        }

        public String getDescription(String id) {
            if (actions.containsKey(id)) {
                return actions.get(id).getDescription();
            }
            return "";
        }

        public LinkedHashMap<String, Class<?>> getParameters(String id) {
            if (actions.containsKey(id)) {
                return actions.get(id).getParameterTypes();
            }
            return null;
        }

        public List<String> getActions() {
            return new ArrayList<>(actions.keySet());
        }
    }
}
