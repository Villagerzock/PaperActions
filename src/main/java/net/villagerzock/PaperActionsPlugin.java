package net.villagerzock;

import net.villagerzock.annotations.Action;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PaperActionsPlugin extends JavaPlugin {
    public static final Logger LOGGER = LoggerFactory.getLogger("PaperActions");

    @Override
    public void onEnable() {
        ActionManager.getInstance(this)
                .register(PaperActionsPlugin.class);
    }
}
