package net.villagerzock;

import org.bukkit.World;

import java.util.Map;

public interface Action {
    void execute(Map<String, Object> context);
}
