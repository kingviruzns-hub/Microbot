package net.runelite.client.plugins.microbot.viruzoscombat;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Viruz Combat",
        description = "Trains Combat - Converted from DreamBot to Microbot",
        tags = {"combat", "microbot", "viruz"},
        enabledByDefault = false
)
@Slf4j
@SuppressWarnings("unused")
public class ViruzCombatPlugin extends Plugin {
    @Inject
    private ViruzCombatScript script;
    @Inject
    private ViruzCombatOverlay overlay;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ViruzCombatConfig config;

    @Provides
    @SuppressWarnings("unused")
    ViruzCombatConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ViruzCombatConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        if (overlay != null) {
            overlayManager.add(overlay);
        }
        if (script != null && config != null) {
            script.setConfig(config);
            script.run(config);
        }
    }

    @Override
    protected void shutDown() {
        if (overlay != null) {
            overlayManager.remove(overlay);
        }
        if (script != null) {
            script.shutdown();
        }
    }

    @Subscribe
    public void onConfigChanged(final ConfigChanged event) {
        if (ViruzCombatConfig.configGroup.equals(event.getGroup())) {
            // No-op
        }
    }
}
