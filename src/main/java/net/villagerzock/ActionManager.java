package net.villagerzock;

import net.villagerzock.annotations.Action;
import net.villagerzock.annotations.Param;
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
                throw new IllegalArgumentException("Method '%s' is static. Methods Annotated with @Action need to be static.");
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
                            throw new IllegalStateException("Missing argument '%s' in action '%s:%s'".formatted(key,name,plugin.getPluginMeta().getName()));
                        }
                        if (!paramTypes.get(key).isAssignableFrom(context.get(key).getClass())){
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
            if (entry.getKey().getPluginMeta().getName().equalsIgnoreCase(parts[0]) && entry.getValue().actions.containsKey(parts[1])){
                entry.getValue().actions.get(parts[1]).execute(context);
                return;
            }
        }
    }
}
