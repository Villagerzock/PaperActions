package net.villagerzock;

import org.bukkit.plugin.java.JavaPlugin;

public final class PaperActionsPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getServer().getGlobalRegionScheduler().execute(this, () ->
                getLogger().info("PaperActions enabled with Folia-compatible scheduling."));
    }

    @Override
    public void onDisable() {
        getServer().getAsyncScheduler().cancelTasks(this);
        getServer().getGlobalRegionScheduler().cancelTasks(this);
    }
}
