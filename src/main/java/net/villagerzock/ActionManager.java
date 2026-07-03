package net.villagerzock;

import net.villagerzock.annotations.Action;
import net.villagerzock.annotations.Param;
import net.villagerzock.annotations.PluginIdentifier;
import org.bukkit.plugin.Plugin;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;

import static net.villagerzock.PaperActionsPlugin.LOGGER;

public class ActionManager {
    private static final Map<Plugin, ActionManager> MANAGERS = new HashMap<>();

    private final Map<String, IAction> actions = new HashMap<>();
    private final Plugin plugin;

    public ActionManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public ActionManager register(Class<?> clazz){
        List<Method> methods = Arrays.stream(clazz.getDeclaredMethods()).filter(value -> value.isAnnotationPresent(Action.class)).toList();
        for (Method method : methods){
            Action actionMeta = method.getAnnotation(Action.class);
            String name = actionMeta.value();
            if ((method.getModifiers() & Modifier.STATIC) == 0) {
                throw new IllegalArgumentException("Method '%s' is not static. Methods Annotated with @Action need to be static.");
            }

            IAction action = new IAction() {
                @Override
                public void execute(Map<String, Object> context) {
                    LinkedHashMap<String, Class<?>> paramTypes = getParameterTypes();
                    int paramSize = method.getParameterCount();
                    Object[] params = new Object[paramSize];
                    List<String> keys = new ArrayList<>(paramTypes.keySet());
                    for (int i = 0; i < paramSize; i++){
                        String key = keys.get(i);
                        List<Annotation> annotations = List.of(method.getParameterAnnotations()[i]);
                        Optional<Param> paramAnnotation = annotations.stream()
                                .filter(Param.class::isInstance)
                                .map(Param.class::cast)
                                .findFirst();

                        if (!context.containsKey(key) && (paramAnnotation.isEmpty() || paramAnnotation.get().required())){
                            throw new IllegalStateException("Missing argument '%s' in action '%s:%s'".formatted(key,getPluginId(plugin),name));
                        }
                        if (context.get(key) != null || !paramTypes.get(key).isAssignableFrom(context.get(key).getClass())){
                            throw new IllegalArgumentException("Value of type '%s' cannot be assigned to Parameter of type '%s'".formatted(context.get(key).getClass().getCanonicalName(), paramTypes.get(key).getCanonicalName()));
                        }
                        params[i] = context.get(key);
                        if (params[i] == null && paramTypes.get(key).isPrimitive()) {
                            switch (paramTypes.get(key).getName()) {
                                case "boolean" -> params[i] = false;
                                case "byte" -> params[i] = (byte) 0;
                                case "short" -> params[i] = (short) 0;
                                case "int" -> params[i] = 0;
                                case "long" -> params[i] = 0L;
                                case "float" -> params[i] = 0.0F;
                                case "double" -> params[i] = 0.0D;
                                case "char" -> params[i] = '\0';
                                default -> throw new IllegalStateException("Unknown primitive type: " + paramTypes.get(key));
                            }
                        }
                    }

                    try {
                        if (actionMeta.deprecated()){
                            LOGGER.warn("Called Deprecated Action '%s'. This Action is deprecated and might be removed in future versions.");
                        }
                        method.invoke(null, params);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public LinkedHashMap<String, Class<?>> getParameterTypes() {
                    LinkedHashMap<String, Class<?>> result = new LinkedHashMap<>();
                    for (Parameter parameter : method.getParameters()){
                        String key;

                        Param param = parameter.getAnnotation(Param.class);

                        if (param != null && !param.name().isEmpty()) {
                            key = param.name();
                        }else {
                            key = parameter.getName();

                            if (!parameter.isNamePresent()) {
                                LOGGER.warn(
                                        "Parameter '{}' in action '{}:{}' has no runtime name information. "
                                                + "Using the synthetic name '{}' instead. "
                                                + "Compile with '-parameters' or annotate the parameter with @Param.",
                                        parameter,
                                        plugin.getPluginMeta().getName(),
                                        name,
                                        key
                                );
                            }
                        }

                        result.put(key,parameter.getType());
                    }
                    return result;
                }

                @Override
                public String getDescription() {
                    return actionMeta.description();
                }
            };

            actions.put(name,action);
        }
        return this;
    }

    public static ActionManager getInstance(Plugin plugin){
        return MANAGERS.computeIfAbsent(plugin, (key)->new ActionManager(plugin));
    }

    public static void executeAction(String id, Map<String, Object> context){
        String[] parts = id.split(":");
        if (parts.length != 2){
            throw new IllegalArgumentException("'id' needs to have 2 Parts. Example: 'plugin:name'");
        }

        for (Map.Entry<Plugin, ActionManager> entry : MANAGERS.entrySet()){
            if (getPluginId(entry.getKey()).equalsIgnoreCase(parts[0]) && entry.getValue().actions.containsKey(parts[1])){
                entry.getValue().actions.get(parts[1]).execute(context);
                return;
            }
        }
    }

    public static String getActionDescription(String id){
        String[] parts = id.split(":");
        if (parts.length != 2){
            throw new IllegalArgumentException("'id' needs to have 2 Parts. Example: 'plugin:name'");
        }

        for (Map.Entry<Plugin, ActionManager> entry : MANAGERS.entrySet()){
            if (getPluginId(entry.getKey()).equalsIgnoreCase(parts[0]) && entry.getValue().actions.containsKey(parts[1])){
                return entry.getValue().actions.get(parts[1]).getDescription();
            }
        }
        return "";
    }

    public String getDescription(String id) {
        if (actions.containsKey(id)){
            return actions.get(id).getDescription();
        }
        return "";
    }

    public static LinkedHashMap<String, Class<?>> getParameterTypes(String id){
        String[] parts = id.split(":");
        if (parts.length != 2){
            throw new IllegalArgumentException("'id' needs to have 2 Parts. Example: 'plugin:name'");
        }

        for (Map.Entry<Plugin, ActionManager> entry : MANAGERS.entrySet()){
            if (getPluginId(entry.getKey()).equalsIgnoreCase(parts[0]) && entry.getValue().actions.containsKey(parts[1])){
                return entry.getValue().actions.get(parts[1]).getParameterTypes();
            }
        }
        return null;
    }

    public LinkedHashMap<String, Class<?>> getParameters(String id) {
        if (actions.containsKey(id)){
            return actions.get(id).getParameterTypes();
        }
        return null;
    }

    public static List<String> getAllActions(){
        List<String> result = new ArrayList<>();
        for (Map.Entry<Plugin, ActionManager> entry : MANAGERS.entrySet()){
            for (String name : entry.getValue().actions.keySet()) {
                result.add("%s:%s".formatted(getPluginId(entry.getKey()),name));
            }
        }
        return result;
    }

    public List<String> getActions() {
        return new ArrayList<>(actions.keySet());
    }

    private static String getPluginId(Plugin plugin){
        if (plugin.getClass().isAnnotationPresent(PluginIdentifier.class)){
            return plugin.getClass().getAnnotation(PluginIdentifier.class).value();
        }
        return plugin.getName();
    }
}
