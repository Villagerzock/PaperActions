package net.villagerzock;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import net.villagerzock.annotations.PluginIdentifier;

@Plugin(
        id = "paper-actions",
        name = "PaperActions",
        version = "1.0-Beta",
        authors = {"villagerzock"}
)
@PluginIdentifier("paper-actions")
public final class VelocityActionsPlugin {
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        ActionManager.getInstance(this);
    }
}
