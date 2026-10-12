package net.runelite.client.plugins.microbot.viruzosbot;

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
		try {
			script.onPaint(graphics);
		} catch (Exception ex) {
			// never let a paint error disable the overlay
		}
		return null;
	}
}
