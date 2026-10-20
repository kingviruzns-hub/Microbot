package net.runelite.client.plugins.microbot.viruzosbot;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;

@SuppressWarnings("unused")
@PluginDescriptor(
		name = PluginDescriptor.Default + "Viruz FFA Bot",
		description = "Clanwars FFA automation bot",
		tags = {"microbot", "pvp", "clanwars", "minigame", "viruz"},
		enabledByDefault = false
)
@Slf4j
public class ViruzFFABotPlugin extends Plugin {
	@Inject
	private ViruzFFABotScript script;
	@Inject
	private ViruzFFABotOverlay overlay;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private ViruzFFABotConfig config;
	private ViruzBotControl botControl;

	@Provides
	ViruzFFABotConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(ViruzFFABotConfig.class);
	}

	@Override
	protected void startUp() throws Exception {
		script.setConfig(config);
		overlayManager.add(overlay);
		botControl = new ViruzBotControl(script);
		botControl.start();
		script.run();
	}

	@Override
	protected void shutDown() throws Exception {
		overlayManager.remove(overlay);
		if (botControl != null) {
			botControl.shutdown();
			botControl = null;
		}
		script.shutdown();
	}

	@Subscribe
	public void onChatMessage(ChatMessage event) {
		script.handleChatMessage(event);
	}
}
