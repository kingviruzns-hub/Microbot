package net.runelite.client.plugins.microbot.viruzoscombat;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLiteDebug;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.concurrent.TimeUnit;

@Slf4j
public class ViruzCombatScript extends Script {
    private static final String VERSION = "1.0.0";

    public static void main(String[] args) throws Exception {
        RuneLiteDebug.pluginsToDebug.add(ViruzCombatPlugin.class);
        RuneLiteDebug.main(args);
    }

    @Setter
    private ViruzCombatConfig config;
    
    @Getter
    private String status = "Idle";

    public ViruzCombatScript() {
    }

    public boolean run() {
        return run(config);
    }

    public boolean run(ViruzCombatConfig config) {
        this.config = config;
        if (mainScheduledFuture != null && !mainScheduledFuture.isCancelled()) {
            mainScheduledFuture.cancel(true);
        }
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) {
                    status = "Not logged in";
                    return;
                }
                if (!super.run()) {
                    return;
                }

                status = "Running";

                if (Rs2Player.isInCombat()) {
                    status = "In Combat";
                    return;
                }

                if (Rs2Inventory.isFull()) {
                    status = "Inventory Full";
                    return;
                }

                status = "Idle";
            } catch (Exception ex) {
                log.error("Error in ViruzCombat script", ex);
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        status = "Idle";
    }
}
