package net.runelite.client.plugins.microbot.viruzosbot;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class ViruzFFABotHudOverlay extends OverlayPanel
{
	private static final Color ACCENT = new Color(255, 120, 0, 200);
	private static final Color OVERLAY_TINT = new Color(0, 0, 0, 150);

	private final ViruzFFABotScript script;
	private final ViruzFFABotConfig config;

	private BufferedImage bg;
	private boolean bgTried;
	private BufferedImage bgScaled;
	private int bgW;
	private int bgH;

	@Inject
	public ViruzFFABotHudOverlay(ViruzFFABotScript script, ViruzFFABotConfig config)
	{
		this.script = script;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	private BufferedImage background()
	{
		if (bgTried)
		{
			return bg;
		}
		bgTried = true;
		try (InputStream in = ViruzFFABotHudOverlay.class.getResourceAsStream("viruzosbot.png"))
		{
			if (in != null)
			{
				bg = ImageIO.read(in);
				if (bg != null)
				{
					return bg;
				}
			}
		}
		catch (Exception ignored)
		{
		}
		try
		{
			File f = new File(System.getProperty("user.home"), ".microbot/viruzosbot/viruzosbot.png");
			if (f.exists())
			{
				bg = ImageIO.read(f);
			}
		}
		catch (Exception ignored)
		{
		}
		return bg;
	}

	private BufferedImage cover(BufferedImage src, int w, int h)
	{
		if (bgScaled != null && bgW == w && bgH == h)
		{
			return bgScaled;
		}
		double sc = Math.max((double) w / (double) src.getWidth(), (double) h / (double) src.getHeight());
		int sw = (int) Math.ceil(src.getWidth() * sc);
		int sh = (int) Math.ceil(src.getHeight() * sc);
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.drawImage(src, (w - sw) / 2, (h - sh) / 2, sw, sh, null);
		g.dispose();
		bgScaled = out;
		bgW = w;
		bgH = h;
		return out;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		if (!Microbot.isLoggedIn() || script == null)
		{
			return null;
		}

		try
		{
			panelComponent.setPreferredSize(new Dimension(230, 0));
			panelComponent.setBackgroundColor(new Color(0, 0, 0, 0));

			String titleText = "ViruzFFABot";
			if (config != null && config.location() != null)
			{
				titleText = titleText + " - " + config.location().getDisplayName();
			}
			panelComponent.getChildren().add(TitleComponent.builder()
					.text(titleText)
					.color(new Color(255, 120, 0))
					.build());

			panelComponent.getChildren().add(LineComponent.builder()
					.left("Status")
					.right(script.status)
					.build());

			// Use PlayerStats total runtime (backed by totalRuntimeMs)
			String runtime = script.playerStats != null ? script.playerStats.getRuntimeString() : "0:00:00";
			panelComponent.getChildren().add(LineComponent.builder()
					.left("Runtime")
					.right(runtime)
					.build());

			String locationText = "None selected";
			if (config != null && config.location() != null)
			{
				locationText = config.location().getDisplayName();
			}
			panelComponent.getChildren().add(LineComponent.builder()
					.left("Location")
					.right(locationText)
					.build());

			BufferedImage img = background();
			if (img != null)
			{
				BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
				Graphics2D pg = probe.createGraphics();
				Dimension d = panelComponent.render(pg);
				pg.dispose();
				if (d != null && d.width > 0 && d.height > 0)
				{
					Shape oldClip = g.getClip();
					g.setClip(new RoundRectangle2D.Float(0.0f, 0.0f, (float) d.width, (float) d.height, 8.0f, 8.0f));
					g.drawImage(cover(img, d.width, d.height), 0, 0, null);
					g.setColor(OVERLAY_TINT);
					g.fillRect(0, 0, d.width, d.height);
					g.setClip(oldClip);
					g.setColor(ACCENT);
					g.drawRoundRect(0, 0, d.width - 1, d.height - 1, 8, 8);
				}
			}

			return super.render(g);
		}
		catch (Exception e)
		{
			return null;
		}
	}
}
