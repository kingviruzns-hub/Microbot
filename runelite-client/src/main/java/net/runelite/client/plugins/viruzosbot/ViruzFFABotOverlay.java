package net.runelite.client.plugins.viruzosbot;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;

public class ViruzFFABotOverlay extends Overlay {
	private final ViruzFFABotScript script;

	@Inject
	public ViruzFFABotOverlay(ViruzFFABotScript script) {
		this.script = script;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics) {
		if (!Microbot.isLoggedIn()) {
			return null;
		}
		script.onPaint(graphics);
		return null;
	}
}
