package net.runelite.client.plugins.microbot.viruzosbot;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
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
		enabledByDefault = false,
		authors = {"Viruz Scripts"},
		version = ViruzFFABotScript.VERSION,
		minClientVersion = "2.6.30",
		isExternal = true
)
@Slf4j
public class ViruzFFABotPlugin extends Plugin {
	@Inject
	private ViruzFFABotScript script;
	@Inject
	private ViruzFFABotHudOverlay hudOverlay;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private ViruzFFABotConfig config;

	@Provides
	ViruzFFABotConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(ViruzFFABotConfig.class);
	}

	@Override
	protected void startUp() throws Exception {
		script.setConfig(config);
		overlayManager.add(hudOverlay);
		script.run();
	}

	@Override
	protected void shutDown() throws Exception {
		overlayManager.remove(hudOverlay);
		script.shutdown();
	}

	@Subscribe
	public void onChatMessage(ChatMessage event) {
		script.handleChatMessage(event);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (!ViruzFFABotConfig.configGroup.equals(event.getGroup())) {
			return;
		}

		String key = event.getKey();
		if ("resetFatigue".equals(key)) {
			script.resetFatigue();
		} else if ("reloadNsList".equals(key)) {
			script.reloadNsList();
		} else if ("dragMode".equals(key)) {
			script.setDragModeEnabled(Boolean.parseBoolean(event.getNewValue()));
		}
	}
}
