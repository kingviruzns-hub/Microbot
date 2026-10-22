package net.runelite.client.plugins.microbot.viruzosbot;

import net.runelite.api.Client;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ViruzFFABotHudOverlay extends Overlay
{
	private static final int WIDTH = 260;
	private static final int HEIGHT = 560;
	private static final int PADDING = 10;
	private static final int PANEL_X = 7;
	private static final int PANEL_WIDTH = WIDTH - PANEL_X * 2;
	private static final int ROW_HEIGHT = 13;

	private static final Color ACCENT = new Color(26, 180, 250);
	private static final Color ACCENT_SOFT = new Color(26, 180, 250, 60);
	private static final Color BACKGROUND = new Color(20, 20, 20, 210);
	private static final Color PANEL_BACKGROUND = new Color(15, 15, 20, 210);
	private static final Color TEXT = new Color(200, 220, 255);
	private static final Color MUTED = new Color(130, 130, 130);
	private static final Color SECTION = new Color(180, 140, 255);
	private static final Color DANGER = new Color(255, 120, 120);
	private static final Color WARNING = new Color(255, 200, 80);
	private static final Color GOOD = new Color(120, 220, 120);
	private static final Color THREAT_HIGH = new Color(255, 70, 70);
	private static final Color THREAT_MEDIUM = new Color(255, 165, 50);

	private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 16);
	private static final Font SECTION_FONT = new Font("Arial", Font.BOLD, 11);
	private static final Font BODY_FONT = new Font("Consolas", Font.PLAIN, 11);
	private static final Font SMALL_FONT = new Font("Consolas", Font.PLAIN, 10);

	@Inject
	private Client client;

	private final ViruzFFABotScript script;

	@Inject
	public ViruzFFABotHudOverlay(ViruzFFABotScript script)
	{
		this.script = script;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!Microbot.isLoggedIn() || client == null || client.getLocalPlayer() == null)
		{
			return null;
		}

		if (script == null || script.timer2 == null || script.selectedLocation == null || script.status == null)
		{
			return null;
		}

		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			graphics.setColor(BACKGROUND);
			graphics.fillRoundRect(0, 0, WIDTH, HEIGHT, 6, 6);
			graphics.setColor(ACCENT);
			graphics.drawRoundRect(0, 0, WIDTH - 1, HEIGHT - 1, 6, 6);

			graphics.setColor(new Color(26, 180, 250, 45));
			graphics.fillRoundRect(0, 0, WIDTH, 30, 6, 6);
			graphics.setFont(TITLE_FONT);
			graphics.setColor(Color.WHITE);
			graphics.drawString("ViruzFFABot", PADDING, 21);
			drawVersion(graphics, "v" + ViruzFFABotScript.VERSION);

			long runtime = System.currentTimeMillis() - script.fatigueStart;
			double fatigue = script.getFatigueFactor(runtime);

			int y = 47;
			graphics.setFont(BODY_FONT);
			graphics.setColor(statusColor(script.status));
			drawFit(graphics, "Status: " + script.status, PADDING, y, WIDTH - PADDING * 2);
			y += ROW_HEIGHT;

			graphics.setColor(TEXT);
			drawFit(graphics, "Location: " + script.selectedLocation, PADDING, y, WIDTH - PADDING * 2);
			y += ROW_HEIGHT;

			drawFit(graphics, "Runtime: " + script.timer2.formatTime(), PADDING, y, WIDTH - PADDING * 2);
			y += ROW_HEIGHT;

			drawFit(graphics, "Actions: " + script.getTotalActions() + " | Session: " + script.getSessionDurationMinutes() + "m", PADDING, y, WIDTH - PADDING * 2);
			y += ROW_HEIGHT;

			drawFatigue(graphics, y, fatigue);
			y += 20;

			drawSeparator(graphics, y);
			y += 14;

			drawSectionHeader(graphics, y, "AI PROFILES");
			y += 14;

			graphics.setFont(SMALL_FONT);
			graphics.setColor(new Color(180, 180, 180));
			Map<String, AIAssistant.PlayerProfile> profiles = script.aiAssistant.getPlayerProfiles();
			drawFit(graphics, "Tracking: " + profiles.size() + " players", PADDING, y, WIDTH - PADDING * 2);
			y += 13;

			Optional<Map.Entry<String, AIAssistant.PlayerProfile>> topThreat = profiles.entrySet().stream()
					.max(Comparator.comparingDouble(entry -> entry.getValue().threatDisplay()));
			if (topThreat.isPresent())
			{
				double threat = topThreat.get().getValue().threatDisplay();
				graphics.setColor(threatColor(threat));
				drawFit(graphics, String.format("Top: %s [%.2f]", topThreat.get().getKey(), threat), PADDING, y, WIDTH - PADDING * 2);
			}
			else
			{
				graphics.setColor(MUTED);
				graphics.drawString("No data yet", PADDING, y);
			}

			y += 10;
			drawSessionPanel(graphics, y, fatigue);
			y += 194;

			drawNonTargetPanel(graphics, y);
			y += 70;

			drawAllTimePanel(graphics, y);

			return new Dimension(WIDTH, HEIGHT);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	private void drawVersion(Graphics2D graphics, String version)
	{
		graphics.setFont(SMALL_FONT);
		graphics.setColor(MUTED);
		graphics.drawString(version, WIDTH - PADDING - graphics.getFontMetrics().stringWidth(version), 20);
	}

	private void drawFatigue(Graphics2D graphics, int y, double fatigue)
	{
		String fatigueStr = String.format("Fatigue: %.1f/20", fatigue);
		if (script.energyBoost > 0.01)
		{
			fatigueStr += String.format(" (B:%.1f)", script.energyBoost);
		}
		if (script.fatigueSpikeEndTime > System.currentTimeMillis())
		{
			fatigueStr += " [S]";
		}
		if (script.performanceStreakEndTime > System.currentTimeMillis())
		{
			fatigueStr += " [2W]";
		}

		Color fatigueColor = fatigue > 12.0 ? DANGER : fatigue > 6.0 ? WARNING : new Color(150, 210, 255);
		graphics.setFont(BODY_FONT);
		graphics.setColor(fatigueColor);

		int barWidth = 54;
		int maxTextWidth = WIDTH - PADDING * 2 - barWidth - 8;
		drawFit(graphics, fatigueStr, PADDING, y, maxTextWidth);
		drawBar(graphics, WIDTH - PADDING - barWidth, y - 8, barWidth, 5, fatigue / 20.0, fatigueColor);
	}

	private void drawSessionPanel(Graphics2D graphics, int y, double fatigue)
	{
		drawPanel(graphics, PANEL_X, y, PANEL_WIDTH, 186);
		if (script.sessionTracker != null)
		{
			script.sessionTracker.drawStats(graphics, PADDING, y + 6, fatigue);
		}
	}

	private void drawNonTargetPanel(Graphics2D graphics, int y)
	{
		drawPanel(graphics, PANEL_X, y, PANEL_WIDTH, 62);
		drawSectionHeader(graphics, y + 14, "NON-TARGET TRACKER");

		graphics.setFont(SMALL_FONT);
		graphics.setColor(new Color(180, 180, 180));

		int line = y + 28;
		if (script.nonTargetTracker != null)
		{
			Map<String, NonTargetTracker.LastSeenEntry> entries = script.nonTargetTracker.getEntries();
			graphics.drawString("Tracked: " + script.nonTargetTracker.getTrackedCount() + " players", PADDING, line);
			line += 13;

			Optional<NonTargetTracker.LastSeenEntry> last = entries.values().stream()
					.max(Comparator.comparingLong(NonTargetTracker.LastSeenEntry::getLastSeenMs));
			if (last.isPresent())
			{
				NonTargetTracker.LastSeenEntry entry = last.get();
				String lastStr = String.format("Last: %s @%s (%d,%d,%d) cb=%d",
						entry.getPlayerName(), entry.getLocation(),
						entry.getX(), entry.getY(), entry.getZ(), entry.getCombatLevel());
				graphics.setColor(TEXT);
				drawFit(graphics, lastStr, PADDING, line, PANEL_WIDTH - 6);
			}
			else
			{
				graphics.setColor(MUTED);
				graphics.drawString("No players tracked", PADDING, line);
			}
		}
	}

	private void drawAllTimePanel(Graphics2D graphics, int y)
	{
		drawPanel(graphics, PANEL_X, y, PANEL_WIDTH, 118);
		drawSectionHeader(graphics, y + 14, "ALL-TIME STATS");

		graphics.setFont(SMALL_FONT);
		int line = y + 28;

		if (script.playerStats != null)
		{
			graphics.setColor(TEXT);
			graphics.drawString("Runtime: " + script.playerStats.getRuntimeString(), PADDING, line);
			line += 13;

			graphics.setColor(new Color(180, 200, 255));
			drawFit(graphics, "Current Runtime: " + script.playerStats.getCurrentRuntimeString(), PADDING, line, PANEL_WIDTH - 6);
			line += 13;

			graphics.setColor(GOOD);
			graphics.drawString("XP Earned: " + script.playerStats.getTotalXpEarned(), PADDING, line);
			line += 13;

			List<Map.Entry<String, Integer>> bullied = script.playerStats.getTopBullied(3);
			if (bullied.isEmpty())
			{
				graphics.setColor(MUTED);
				graphics.drawString("No targets attacked yet", PADDING, line);
			}
			else
			{
				graphics.setColor(new Color(255, 160, 100));
				for (Map.Entry<String, Integer> entry : bullied)
				{
					drawFit(graphics, entry.getKey() + ": " + entry.getValue(), PADDING, line, PANEL_WIDTH - 6);
					line += 12;
				}
			}
		}
	}

	private void drawPanel(Graphics2D graphics, int x, int y, int width, int height)
	{
		graphics.setColor(PANEL_BACKGROUND);
		graphics.fillRoundRect(x, y, width, height, 5, 5);
		graphics.setColor(ACCENT_SOFT);
		graphics.drawRoundRect(x, y, width - 1, height - 1, 5, 5);
	}

	private void drawSectionHeader(Graphics2D graphics, int y, String text)
	{
		graphics.setFont(SECTION_FONT);
		graphics.setColor(SECTION);
		graphics.drawString(text, PADDING, y);
	}

	private void drawSeparator(Graphics2D graphics, int y)
	{
		graphics.setColor(new Color(26, 180, 250, 45));
		graphics.drawLine(PADDING, y, WIDTH - PADDING, y);
	}

	private void drawBar(Graphics2D graphics, int x, int y, int width, int height, double percent, Color color)
	{
		graphics.setColor(new Color(25, 25, 30));
		graphics.fillRoundRect(x, y, width, height, 2, 2);

		int fillWidth = (int) Math.round(width * Math.max(0.0, Math.min(1.0, percent)));
		if (fillWidth > 0)
		{
			graphics.setColor(color);
			graphics.fillRoundRect(x, y, fillWidth, height, 2, 2);
		}

		graphics.setColor(new Color(60, 60, 65, 120));
		graphics.drawRoundRect(x, y, width, height, 2, 2);
	}

	private void drawFit(Graphics2D graphics, String text, int x, int y, int maxWidth)
	{
		graphics.drawString(fitText(graphics, text, maxWidth), x, y);
	}

	private String fitText(Graphics2D graphics, String text, int maxWidth)
	{
		if (graphics.getFontMetrics().stringWidth(text) <= maxWidth)
		{
			return text;
		}
		while (text.length() > 1 && graphics.getFontMetrics().stringWidth(text + "~") > maxWidth)
		{
			text = text.substring(0, text.length() - 1);
		}
		return text + "~";
	}

	private Color statusColor(String status)
	{
		return status.contains("Error") || status.contains("AFK") ? DANGER : new Color(180, 200, 255);
	}

	private Color threatColor(double threat)
	{
		if (threat >= 5.0)
		{
			return THREAT_HIGH;
		}
		if (threat >= 3.0)
		{
			return THREAT_MEDIUM;
		}
		if (threat >= 1.0)
		{
			return new Color(255, 220, 80);
		}
		return GOOD;
	}
}
