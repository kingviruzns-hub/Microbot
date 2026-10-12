package net.runelite.client.plugins.microbot.viruzosbot;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.MouseListener;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.event.MouseEvent;

@SuppressWarnings("unused")
@PluginDescriptor(
		name = PluginDescriptor.Default + "Viruz FFA Bot",
		description = "Clanwars FFA automation bot",
		tags = {"microbot", "pvp", "clanwars", "minigame", "viruz"},
		enabledByDefault = false
)
@Slf4j
public class ViruzFFABotPlugin extends Plugin implements MouseListener {
	@Inject
	private ViruzFFABotScript script;
	@Inject
	private ViruzFFABotOverlay overlay;
	@Inject
	private OverlayManager overlayManager;

	@Override
	protected void startUp() throws Exception {
		overlayManager.add(overlay);
		Microbot.mouseManager.registerMouseListener(this);
		script.run();
	}

	@Override
	protected void shutDown() throws Exception {
		overlayManager.remove(overlay);
		Microbot.mouseManager.unregisterMouseListener(this);
		script.shutdown();
	}

	@Subscribe
	public void onChatMessage(ChatMessage event) {
		script.handleChatMessage(event);
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			script.handleMouseClick(e.getPoint());
		}
		return script.isPointInGui(e.getPoint()) ? null : e;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent e) {
		return e;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent e) {
		return e;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent e) {
		return e;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent e) {
		return e;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent e) {
		return e;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent e) {
		return e;
	}
}
