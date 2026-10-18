package net.runelite.client.plugins.microbot.viruzosbot;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ViruzFFABotOverlay extends OverlayPanel {
	private final ViruzFFABotScript script;

	@Inject
	public ViruzFFABotOverlay(ViruzFFABotScript script) {
		super();
		this.script = script;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics) {
		if (script == null) {
			return super.render(graphics);
		}
		if (script.timer2 == null || script.selectedLocation == null || script.status == null) {
			return super.render(graphics);
		}
		if (script.guiHidden) {
			graphics.setColor(new Color(20, 20, 20, 200));
			graphics.fillRect(script.guiToggleButton.x, script.guiToggleButton.y, script.guiToggleButton.width, script.guiToggleButton.height);
			graphics.setColor(new Color(26, 180, 250));
			graphics.drawRect(script.guiToggleButton.x, script.guiToggleButton.y, script.guiToggleButton.width, script.guiToggleButton.height);
			graphics.setColor(Color.WHITE);
			graphics.setFont(new Font("Arial", Font.BOLD, 14));
			graphics.drawString("▶", script.guiToggleButton.x + 10, script.guiToggleButton.y + 21);
			return super.render(graphics);
		}
		int width = 260;
		int statsPaneX = width + 5;
		int height = 540;

		graphics.setColor(new Color(20, 20, 20, 200));
		graphics.fillRect(0, 0, width, height);
		graphics.setColor(new Color(26, 180, 250));
		graphics.drawRect(0, 0, width - 1, height - 1);

		graphics.setColor(new Color(26, 180, 250, 50));
		graphics.fillRect(0, 0, width, 30);
		graphics.setColor(Color.WHITE);
		graphics.setFont(new Font("Arial", Font.BOLD, 16));
		graphics.drawString("ViruzFFABot", 65, 22);

		graphics.setColor(new Color(40, 40, 40));
		graphics.fillRect(script.hideGuiButton.x, script.hideGuiButton.y, script.hideGuiButton.width, script.hideGuiButton.height);
		graphics.setColor(new Color(26, 180, 250));
		graphics.drawRect(script.hideGuiButton.x, script.hideGuiButton.y, script.hideGuiButton.width, script.hideGuiButton.height);
		graphics.setColor(Color.WHITE);
		graphics.setFont(new Font("Arial", Font.BOLD, 12));
		graphics.drawString("×", script.hideGuiButton.x + 6, script.hideGuiButton.y + 15);

		graphics.setFont(new Font("Arial", Font.PLAIN, 12));
		graphics.drawString("Configure via plugin settings", 10, 75);

		graphics.setColor(new Color(26, 180, 250, 30));
		graphics.fillRect(0, height - 105, width, 105);
		int line = height - 100;
		graphics.setFont(new Font("Consolas", Font.PLAIN, 11));
		graphics.setColor(new Color(200, 220, 255));
		graphics.drawString(fitText(graphics, "Runtime: " + script.timer2.formatTime(), width - 15), 10, line); line += 12;

		long runtime = System.currentTimeMillis() - script.fatigueStart;
		double currentFatigue = script.getFatigueFactor(runtime);
		String fatigueStr = String.format("Fatigue: %.1f/20", currentFatigue);
		if (script.energyBoost > 0.01) {
			fatigueStr += String.format(" (B:%.1f)", script.energyBoost);
		}
		Color fatigueColor = currentFatigue > 12.0 ? new Color(255, 100, 100) :
				currentFatigue > 6.0 ? new Color(255, 200, 80) :
						new Color(150, 210, 255);
		graphics.setColor(fatigueColor);
		if (script.fatigueSpikeEndTime > System.currentTimeMillis()) {
			fatigueStr += " [S]";
		}
		if (script.performanceStreakEndTime > System.currentTimeMillis()) {
			fatigueStr += " [2W]";
		}
		graphics.drawString(fitText(graphics, fatigueStr, width - 15), 10, line); line += 12;

		Color statusColor = script.status.contains("Error") || script.status.contains("AFK") ?
				new Color(255, 120, 120) : new Color(180, 200, 255);
		graphics.setColor(statusColor);
		graphics.drawString(fitText(graphics, "Status: " + script.status, width - 15), 10, line); line += 13;

		graphics.drawString(fitText(graphics, "Actions: " + script.getTotalActions() + " | Session: " + script.getSessionDurationMinutes() + "m", width - 15), 10, line); line += 12;

		graphics.setColor(new Color(180, 140, 255, 50));
		graphics.drawLine(5, line, width - 5, line); line += 5;

		graphics.setFont(new Font("Arial", Font.BOLD, 11));
		graphics.setColor(new Color(180, 140, 255));
		graphics.drawString("AI PROFILES", 10, line); line += 13;

		int profileCount = script.aiAssistant.getPlayerProfiles().size();
		graphics.setFont(new Font("Consolas", Font.PLAIN, 10));
		graphics.setColor(new Color(180, 180, 180));
		graphics.drawString("Tracking: " + profileCount + " players", 10, line); line += 11;

		if (!script.aiAssistant.getPlayerProfiles().isEmpty()) {
			Optional<Map.Entry<String, AIAssistant.PlayerProfile>> topThreat = script.aiAssistant.getPlayerProfiles().entrySet().stream()
					.max(Comparator.comparingDouble(e -> e.getValue().threatDisplay()));
			double threat = topThreat.get().getValue().threatDisplay();
			Color threatColor = threat >= 5.0 ? new Color(255, 70, 70) :
					threat >= 3.0 ? new Color(255, 165, 50) :
							threat >= 1.0 ? new Color(255, 220, 80) :
									new Color(100, 220, 100);
			graphics.setColor(threatColor);
			String topStr = String.format("Top: %s [%.2f]", topThreat.get().getKey(), threat);
			graphics.drawString(fitText(graphics, topStr, width - 15), 10, line);
		} else {
			graphics.setColor(new Color(120, 120, 120));
			graphics.drawString("No data yet", 10, line);
		}

		line += 2;
		script.purgeListButton.y = line;
		script.reEnergizeButton.y = line;
		Point guiMouse = Microbot.getMouse().getMousePosition();
		boolean hovered = guiMouse != null && script.purgeListButton.contains(guiMouse);
		graphics.setColor(hovered ? new Color(60, 55, 70) : new Color(45, 40, 55));
		graphics.fillRoundRect(script.purgeListButton.x, script.purgeListButton.y, script.purgeListButton.width, script.purgeListButton.height, 3, 3);
		graphics.setColor(new Color(180, 140, 255, 150));
		graphics.drawRoundRect(script.purgeListButton.x, script.purgeListButton.y, script.purgeListButton.width, script.purgeListButton.height, 3, 3);
		graphics.setFont(new Font("Consolas", Font.PLAIN, 10));
		graphics.setColor(new Color(200, 200, 220));
		graphics.drawString("Purge List", script.purgeListButton.x + 10, script.purgeListButton.y + 12);
		boolean hoveredRe = guiMouse != null && script.reEnergizeButton.contains(guiMouse);
		graphics.setColor(hoveredRe ? new Color(60, 80, 60) : new Color(35, 50, 35));
		graphics.fillRoundRect(script.reEnergizeButton.x, script.reEnergizeButton.y, script.reEnergizeButton.width, script.reEnergizeButton.height, 3, 3);
		graphics.setColor(new Color(100, 220, 100, 150));
		graphics.drawRoundRect(script.reEnergizeButton.x, script.reEnergizeButton.y, script.reEnergizeButton.width, script.reEnergizeButton.height, 3, 3);
		graphics.setFont(new Font("Consolas", Font.PLAIN, 10));
		graphics.setColor(new Color(200, 220, 200));
		graphics.drawString("ReEnergize", script.reEnergizeButton.x + 6, script.reEnergizeButton.y + 12);
		line += 17;

		graphics.setFont(new Font("Arial", Font.BOLD, 10));
		graphics.setColor(new Color(100, 100, 100));
		graphics.drawString("v" + ViruzFFABotScript.VERSION, width - 28, line);

		int statsWidth = 194;
		int statsPanelY = 20;
		int statsPanelH = 210;
		graphics.setColor(new Color(15, 15, 20, 200));
		graphics.fillRoundRect(statsPaneX - 3, statsPanelY, statsWidth, statsPanelH, 5, 5);
		graphics.setColor(new Color(26, 180, 250, 60));
		graphics.drawRoundRect(statsPaneX - 3, statsPanelY, statsWidth - 1, statsPanelH - 1, 5, 5);
		graphics.setColor(new Color(26, 180, 250, 12));
		graphics.fillRect(statsPaneX - 1, statsPanelY, statsWidth - 4, 18);
		if (script.sessionTracker != null) {
			script.sessionTracker.drawStats(graphics, statsPaneX, statsPanelY + 10, currentFatigue);
		}

		int nttY = statsPanelY + statsPanelH + 8;
		int nttH = 72;
		graphics.setColor(new Color(15, 15, 20, 200));
		graphics.fillRoundRect(statsPaneX - 3, nttY, statsWidth, nttH, 5, 5);
		graphics.setColor(new Color(26, 180, 250, 60));
		graphics.drawRoundRect(statsPaneX - 3, nttY, statsWidth - 1, nttH - 1, 5, 5);
		graphics.setFont(new Font("Arial", Font.BOLD, 11));
		graphics.setColor(new Color(26, 180, 250));
		graphics.drawString("NON-TARGET TRACKER", statsPaneX, nttY + 14);
		graphics.setFont(new Font("Consolas", Font.PLAIN, 10));
		graphics.setColor(new Color(180, 180, 180));
		if (script.nonTargetTracker != null) {
			graphics.drawString("Tracked: " + script.nonTargetTracker.getTrackedCount() + " players", statsPaneX, nttY + 30);
			Map<String, NonTargetTracker.LastSeenEntry> nttEntries = script.nonTargetTracker.getEntries();
			if (!nttEntries.isEmpty()) {
				NonTargetTracker.LastSeenEntry last = null;
				for (NonTargetTracker.LastSeenEntry candidate : nttEntries.values()) {
					if (last == null || candidate.getLastSeenMs() > last.getLastSeenMs()) {
						last = candidate;
					}
				}
				if (last != null) {
					String lastStr = String.format("Last: %s @%s (%d,%d,%d) cb=%d",
							last.getPlayerName(), last.getLocation(),
							last.getX(), last.getY(), last.getZ(),
							last.getCombatLevel());
					int lastMax = statsWidth - 10;
					if (graphics.getFontMetrics().stringWidth(lastStr) > lastMax) {
						lastStr = String.format("Last: %s @%s", last.getPlayerName(), last.getLocation());
					}
					if (graphics.getFontMetrics().stringWidth(lastStr) > lastMax) {
						String lname = last.getPlayerName();
						String lloc = last.getLocation();
						while (lname.length() > 1 && graphics.getFontMetrics().stringWidth("Last: " + lname + "~ @") > lastMax) {
							lname = lname.substring(0, lname.length() - 1);
						}
						while (lloc.length() > 1 && graphics.getFontMetrics().stringWidth("Last: " + lname + "~@" + lloc + "~") > lastMax) {
							lloc = lloc.substring(0, lloc.length() - 1);
						}
						lastStr = "Last: " + lname + "~@" + lloc;
					}
					graphics.drawString(lastStr, statsPaneX, nttY + 44);
				}
			}
		}
		graphics.setColor(new Color(100, 100, 100, 80));
		graphics.drawLine(statsPaneX, nttY + 56, statsPaneX + statsWidth - 10, nttY + 56);
		graphics.setColor(new Color(120, 120, 120));
		graphics.drawString("next save in " + (30000 - (System.currentTimeMillis() % 30000)) / 1000 + "s", statsPaneX + 4, nttY + 66);

		int psY = nttY + nttH + 8;
		int psH = 130;
		graphics.setColor(new Color(15, 15, 20, 200));
		graphics.fillRoundRect(statsPaneX - 3, psY, statsWidth, psH, 5, 5);
		graphics.setColor(new Color(26, 180, 250, 60));
		graphics.drawRoundRect(statsPaneX - 3, psY, statsWidth - 1, psH - 1, 5, 5);
		graphics.setColor(new Color(26, 180, 250, 12));
		graphics.fillRect(statsPaneX - 1, psY, statsWidth - 4, 18);
		graphics.setFont(new Font("Arial", Font.BOLD, 11));
		graphics.setColor(new Color(26, 180, 250));
		graphics.drawString("ALL-TIME STATS", statsPaneX, psY + 14);
		if (script.playerStats != null) {
			graphics.setFont(new Font("Consolas", Font.PLAIN, 10));
			graphics.setColor(new Color(200, 220, 255));
			graphics.drawString("Runtime: " + script.playerStats.getRuntimeString(), statsPaneX, psY + 30);
			graphics.setColor(new Color(180, 200, 255));
			graphics.drawString(fitText(graphics, "Current Runtime: " + script.playerStats.getCurrentRuntimeString(), statsWidth - 10), statsPaneX, psY + 42);
			graphics.setColor(new Color(130, 200, 130));
			graphics.drawString("XP Earned: " + script.playerStats.getTotalXpEarned(), statsPaneX, psY + 54);
			List<Map.Entry<String, Integer>> bullied = script.playerStats.getTopBullied(5);
			if (!bullied.isEmpty()) {
				graphics.setColor(new Color(255, 160, 100));
				int by = psY + 68;
				int maxWidth = statsWidth - 10;
				for (Map.Entry<String, Integer> entry : bullied) {
					String name = entry.getKey();
					String lineStr = name + ": " + entry.getValue();
					if (graphics.getFontMetrics().stringWidth(lineStr) > maxWidth) {
						while (graphics.getFontMetrics().stringWidth(lineStr) > maxWidth && name.length() > 1) {
							name = name.substring(0, name.length() - 1);
							lineStr = name + "~: " + entry.getValue();
						}
					}
					graphics.drawString(lineStr, statsPaneX, by);
					by += 12;
				}
			} else {
				graphics.setColor(new Color(120, 120, 120));
				graphics.drawString("No targets attacked yet", statsPaneX, psY + 68);
			}
		}

		return super.render(graphics);
	}

	private void drawCheckbox(Graphics2D graphics, Rectangle rect, String label, boolean checked) {
		graphics.setColor(new Color(40, 40, 40));
		graphics.fillRect(rect.x, rect.y, rect.width, rect.height);
		graphics.setColor(checked ? new Color(26, 180, 250) : Color.GRAY);
		graphics.drawRect(rect.x, rect.y, rect.width, rect.height);
		if (checked) {
			graphics.setColor(new Color(26, 180, 250));
			graphics.fillRect(rect.x + 3, rect.y + 3, rect.width - 6, rect.height - 6);
		}
		graphics.setColor(Color.WHITE);
		graphics.setFont(new Font("Arial", Font.PLAIN, 12));
		graphics.drawString(label, rect.x + 22, rect.y + 12);
	}

	private String fitText(Graphics2D graphics, String text, int maxWidth) {
		if (graphics.getFontMetrics().stringWidth(text) <= maxWidth) return text;
		while (text.length() > 1 && graphics.getFontMetrics().stringWidth(text + "~") > maxWidth) {
			text = text.substring(0, text.length() - 1);
		}
		return text + "~";
	}

	private void drawComboBox(Graphics2D graphics) {
		graphics.setColor(new Color(40, 40, 40));
		graphics.fillRect(script.locationComboBox.x, script.locationComboBox.y, script.locationComboBox.width, script.locationComboBox.height);
		graphics.setColor(new Color(26, 180, 250));
		graphics.drawRect(script.locationComboBox.x, script.locationComboBox.y, script.locationComboBox.width, script.locationComboBox.height);
		graphics.setColor(Color.WHITE);
		graphics.setFont(new Font("Arial", Font.BOLD, 12));
		graphics.drawString(script.selectedLocation, script.locationComboBox.x + 5, script.locationComboBox.y + 15);
		int arrowX = script.locationComboBox.x + script.locationComboBox.width - 15;
		int arrowY = script.locationComboBox.y + 15;
		graphics.drawString(script.comboBoxExpanded ? "▲" : "▼", arrowX, arrowY);
		if (script.comboBoxExpanded) {
			for (int i = 0; i < script.locationOptions.size(); ++i) {
				int optionY = script.locationComboBox.y + (i + 1) * 20;
				String location = script.locationOptions.get(i);
				graphics.setColor(new Color(30, 30, 30));
				graphics.fillRect(script.locationComboBox.x, optionY, script.locationComboBox.width, 20);
				graphics.setColor(new Color(60, 60, 60));
				graphics.drawRect(script.locationComboBox.x, optionY, script.locationComboBox.width, 20);
				if (location.equals(script.selectedLocation)) {
					graphics.setColor(new Color(26, 180, 250, 100));
					graphics.fillRect(script.locationComboBox.x, optionY, script.locationComboBox.width, 20);
				}
				graphics.setColor(Color.WHITE);
				graphics.setFont(new Font("Arial", Font.PLAIN, 12));
				graphics.drawString(location, script.locationComboBox.x + 5, optionY + 15);
			}
		}
	}
}
