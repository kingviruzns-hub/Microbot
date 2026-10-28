package net.runelite.client.plugins.microbot.viruzoscombat;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;

import javax.inject.Inject;
import java.awt.*;

public class ViruzCombatOverlay extends OverlayPanel {
    private final ViruzCombatScript script;

    @Inject
    public ViruzCombatOverlay(ViruzCombatScript script) {
        this.script = script;
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(220, 300));
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Viruz Combat")
                    .color(Color.GREEN)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Status:")
                    .right(script.getStatus() != null ? script.getStatus() : "Idle")
                    .build());

            if (script.getRuntime() > 0) {
                long seconds = script.getRuntime() / 1000;
                long hours = seconds / 3600;
                long minutes = (seconds % 3600) / 60;
                long secs = seconds % 60;
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Runtime:")
                        .right(String.format("%02d:%02d:%02d", hours, minutes, secs))
                        .build());
            }

            if (script.getKills() >= 0) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Kills:")
                        .right(String.valueOf(script.getKills()))
                        .build());
            }

            if (script.getFoodCount() >= 0) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Food:")
                        .right(String.valueOf(script.getFoodCount()))
                        .build());
            }
        } catch (Exception ex) {
            // ignore
        }
        return super.render(graphics);
    }
}
