package net.runelite.client.plugins.microbot.viruzoscombat;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.client.RuneLiteDebug;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.npc.Rs2NpcCache;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.api.tileitem.models.Rs2TileItemModel;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;

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
    
    @Getter
    private long startTime = 0;
    
    @Getter
    private int kills = 0;

    public ViruzCombatScript() {
    }

    public boolean run() {
        return run(config);
    }

    public long getRuntime() {
        if (startTime == 0) return 0;
        return System.currentTimeMillis() - startTime;
    }

    public int getFoodCount() {
        return Rs2Inventory.count(item -> item != null && 
            (item.getName().toLowerCase().contains("shark") || 
             item.getName().toLowerCase().contains("lobster") ||
             item.getName().toLowerCase().contains("tuna") ||
             item.getName().toLowerCase().contains("salmon") ||
             item.getName().toLowerCase().contains("meat") ||
             item.getName().toLowerCase().contains("food")));
    }

    public boolean run(ViruzCombatConfig config) {
        this.config = config;
        startTime = System.currentTimeMillis();
        kills = 0;
        
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

                if (Rs2Player.getHealthPercentage() < 50) {
                    if (getFoodCount() > 0) {
                        status = "Eating";
                        Rs2Inventory.interact(item -> item != null && 
                            (item.getName().toLowerCase().contains("shark") || 
                             item.getName().toLowerCase().contains("lobster") ||
                             item.getName().toLowerCase().contains("tuna") ||
                             item.getName().toLowerCase().contains("salmon") ||
                             item.getName().toLowerCase().contains("meat") ||
                             item.getName().toLowerCase().contains("food")), "Eat");
                        return;
                    } else {
                        status = "Low Health - No Food";
                        return;
                    }
                }

                if (config != null && config.usePrayer() && Rs2Player.getHealthPercentage() < 70) {
                    if (!Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_MELEE)) {
                        Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, true);
                    }
                }

                if (config != null && config.lootItems() && !Rs2Inventory.isFull()) {
                    Rs2TileItemModel bones = Microbot.getRs2TileItemCache().getStream()
                        .filter(item -> item.getName() != null && item.getName().toLowerCase().contains("bones"))
                        .findFirst()
                        .orElse(null);
                    if (bones != null) {
                        status = "Looting";
                        Microbot.getClientThread().runOnClientThreadOptional(() -> {
                            if (bones.getTileItem() != null) {
                                bones.getTileItem().interact("Take");
                            }
                        });
                        sleep(600);
                        return;
                    }
                    Rs2TileItemModel coins = Microbot.getRs2TileItemCache().getStream()
                        .filter(item -> item.getName() != null && item.getName().equals("Coins"))
                        .findFirst()
                        .orElse(null);
                    if (coins != null) {
                        status = "Looting";
                        Microbot.getClientThread().runOnClientThreadOptional(() -> {
                            if (coins.getTileItem() != null) {
                                coins.getTileItem().interact("Take");
                            }
                        });
                        sleep(600);
                        return;
                    }
                }

                if (Rs2Player.isInCombat()) {
                    status = "In Combat";
                    return;
                }

                if (Rs2Inventory.isFull()) {
                    status = "Inventory Full";
                    return;
                }

                Rs2NpcModel target = null;
                if (config != null && config.attackNpc() != null && !config.attackNpc().isEmpty()) {
                    target = Microbot.getRs2NpcCache().getStream()
                        .filter(n -> n.getName() != null && n.getName().toLowerCase().contains(config.attackNpc().toLowerCase()))
                        .findFirst()
                        .orElse(null);
                }
                if (target == null) {
                    target = Microbot.getRs2NpcCache().getStream()
                        .filter(n -> n != null && !n.isDead())
                        .filter(n -> {
                            NPC npc = n.getNpc();
                            if (npc == null) return false;
                            if (npc.isDead()) return false;
                            if (npc.getInteracting() != null) return false;
                            return npc.getCombatLevel() > 0;
                        })
                        .findFirst()
                        .orElse(null);
                }

                if (target != null && target.getNpc() != null) {
                    final NPC finalTarget = target.getNpc();
                    status = "Attacking " + target.getName();
                    Microbot.getClientThread().runOnClientThreadOptional(() -> {
                        finalTarget.interact("Attack");
                    });
                    sleepUntil(() -> Rs2Player.isInCombat(), 3000);
                    return;
                }

                status = "Looking for targets";
            } catch (Exception ex) {
                log.error("Error in ViruzCombat script", ex);
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown() {
        if (config != null && config.usePrayer()) {
            Rs2Prayer.disableAllPrayers();
        }
        super.shutdown();
        status = "Idle";
        startTime = 0;
    }
}
