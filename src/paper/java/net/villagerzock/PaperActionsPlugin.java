package net.villagerzock;

import net.villagerzock.annotations.PluginIdentifier;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginIdentifier("paper-actions")
public final class PaperActionsPlugin extends JavaPlugin {
    public static final Logger LOGGER = LoggerFactory.getLogger("PaperActions");

    @Override
    public void onEnable() {
        ActionManager.getInstance(this)
                .register(DefaultActions.class);
    }
}
