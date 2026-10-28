package net.runelite.client.plugins.microbot.viruzoscombat;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

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
            panelComponent.setPreferredSize(new Dimension(200, 300));
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Viruz Combat")
                    .color(Color.GREEN)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Status:")
                    .right(script.getStatus() != null ? script.getStatus() : "Idle")
                    .build());
        } catch (Exception ex) {
            // ignore
        }
        return super.render(graphics);
    }
}
