package net.villagerzock;

import java.util.LinkedHashMap;
import java.util.Map;

public interface IAction {
    void execute(Map<String, Object> context);
    LinkedHashMap<String, Class<?>> getParameterTypes();
}
