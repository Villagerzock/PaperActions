package net.villagerzock;

import net.villagerzock.annotations.PluginIdentifier;

@PluginIdentifier("paper-actions")
public final class BungeeActionsPlugin extends net.md_5.bungee.api.plugin.Plugin {
    @Override
    public void onEnable() {
        ActionManager.getInstance(this);
    }
}
