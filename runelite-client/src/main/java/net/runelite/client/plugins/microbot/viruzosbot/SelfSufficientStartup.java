package net.runelite.client.plugins.microbot.viruzosbot;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Actor;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.api.tileitem.models.Rs2TileItemModel;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.magic.Rs2CombatSpells;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelfSufficientStartup {
    private static final Logger log = LoggerFactory.getLogger(SelfSufficientStartup.class);

    private static final int DUKE_NPC_ID = 815;

    private static final int AIR_TALISMAN_ID = 1438;

    private static final int AIR_TALISMAN_NOTED = 1439;

    private static final int ONBOARDING_TIP_GROUP = 664;

    private static final int ONBOARDING_TIP_CLOSE_CHILD = 29;

    private static final int BANK_TUTORIAL_GROUP = 12;

    private static final int BANK_TUTORIAL_CONTAINER_CHILD = 108;

    private static final int SCREENHIGHLIGHT_CLOSE_CHILD = 29;

    private static final WorldPoint DUKE_TILE = new WorldPoint(3209, 3225, 1);

    private static final WorldPoint DROP_TILE = new WorldPoint(3210, 3225, 1);

    private static final WorldPoint STAND_TILE = new WorldPoint(3209, 3224, 1);

    private static final String[] GEAR_NAMES = new String[]{"Blue partyhat", "Staff of air", "Blue wizard robe", "Blue skirt", "Yellow gloves", "Yellow boots", "Team-19 cape", "Wooden shield", "Amulet of power"};

    private static final int[] GEAR_IDS = new int[]{1042, 1381, 577, 1011, 2922, 2914, 4351, 1171, 1731};

    private static final int[] GEAR_BUY_PRICE = new int[]{5000, 1500, 1000, 500, 1000, 1000, 1000, 100, 3000};

    private static final int MIND_RUNE_ID = 558;

    private static final int WATER_RUNE_ID = 555;

    private static final String MIND_RUNE_NAME = "Mind rune";

    private static final String WATER_RUNE_NAME = "Water rune";

    private static final int MIND_RUNE_TARGET = 6000;

    private static final int ENDGAME_MIND_RUNE_TARGET = 10000;

    private static final int ENDGAME_COIN_BUFFER = 2000;

    private static final int WATER_RUNE_TARGET = 3000;

    private static final int MIND_RUNE_BUY_PRICE = 4;

    private static final int WATER_RUNE_BUY_PRICE = 5;

    private static final int RUNE_MAX_BUMP = 3;

    private static final long RUNE_FILL_WAIT_MS = 20000L;

    private static final int GEAR_MAX_BUMP = 2;

    private static final long GEAR_FILL_WAIT_MS = 20000L;

    private static final int MAGIC_TARGET = 40;

    private static final int DEFENCE_TARGET = 40;

    private static final int TOTAL_LEVEL_TARGET = 100;

    private static final int WATER_STRIKE_MAGIC_LEVEL = 5;

    private static final int DEFENSIVE_CASTING_ON = 1;

    private static final int TALISMAN_SELL_PRICE = 130;

    private static final WorldPoint CLAN_WARS_TILE = new WorldPoint(3150, 3635, 0);

    private static final WorldPoint LOBBY_TILE = new WorldPoint(3133, 3630, 0);

    private static final WorldArea LOBBY_AREA = new WorldArea(3128, 3625, 11, 11, 0);

    private static final int CLAN_WARS_ARRIVAL_DISTANCE = 40;

    private static final String CLAN_WARS_MINIGAME = "Clan Wars";

    private static final int GROUPING_BUTTON_COMPONENT_ID = 46333957; // 707.5

    private static final int DROPDOWN_BUTTON_COMPONENT_ID = 4980760; // 76.24

    private static final int DROPDOWN_SELECTED_SPRITE_ID = 773;

    private static final int MINIGAME_LIST = 4980758; // 76.22

    private static final int SELECTED_MINIGAME = 4980747; // 76.11

    private static final int MINIGAME_TELEPORT_BUTTON = 4980768; // 76.32

    private static final Object[] GROUPING_SELECTED_OP = new Object[]{489, 0, 0};

    private static final int[] MINIGAME_TELEPORT_GRAPHICS = new int[]{800, 802, 803, 804};

    static int TALISMAN_BATCHES = 4;

    static final int RESTOCK_BATCHES = 3;

    private static final int[] KEEP_ITEM_IDS = new int[]{
            MIND_RUNE_ID, WATER_RUNE_ID,
            1042, 1381, 577, 1011, 2922, 2914, 4351, 1171, 1731,
            995
    };

    private static final WorldPoint LUMBRIDGE = new WorldPoint(3222, 3218, 0);

    private static final WorldPoint WIZARD_TOWER_BOTTOM = new WorldPoint(3105, 3160, 0);

    private static final WorldPoint WIZARD_TOWER_TOP = new WorldPoint(3109, 3162, 2);

    private static final int WIZARD_TOWER_MIN_X = 3104;

    private static final int WIZARD_TOWER_MAX_X = 3115;

    private static final int WIZARD_TOWER_MIN_Y = 3156;

    private static final int WIZARD_TOWER_MAX_Y = 3168;

    private static final String LESSER_DEMON_NAME = "Lesser demon";

    private int batchesThisRun = 3;

    private boolean restockFresh = false;

    public boolean restock = false;

    public boolean farm = false;

    public boolean endgame = false;

    public enum Phase {
        BANK_ITEMS, GO_TO_DUKE, FARM_TALISMANS, BANK_NOTE_BATCH, WALK_TO_GE, SELL_TALISMANS, BUY_GEAR, BUY_RUNES, EQUIP_GEAR, TRAIN_TELEPORT, TRAIN_WALK, TRAIN, FINALIZE, TELEPORT_CLAN_WARS, WALK_TO_LOBBY, DONE
    }

    public Phase phase = Phase.BANK_ITEMS;

    public int talismanBatch = 1;

    private int gearIndex = 0;

    private int groundDropCount = 0;

    private int dropsSinceLastRefresh = 0;

    private int clanWarsTeleportTries = 0;

    private int lobbyWalkTries = 0;

    private final Map<Integer, Integer> runePrice = new HashMap<>();

    private final Map<Integer, Long> runeOfferAt = new HashMap<>();

    private final Map<Integer, Boolean> runeOfferPlaced = new HashMap<>();

    private final Map<String, Integer> gearPriceOverride = new HashMap<>();

    private final Map<String, Long> gearOfferAt = new HashMap<>();

    private final Map<String, Boolean> gearOfferPlaced = new HashMap<>();

    private final Set<String> gearGaveUp = new HashSet<>();

    private int batchTarget() {
        if (this.restock || this.farm || this.endgame) {
            int fit = Rs2Inventory.emptySlotCount() + Rs2Inventory.count(1438);
            return Math.max(1, Math.min(28, fit));
        }
        return (this.talismanBatch == 1) ? 28 : 27;
    }

    private int coins() {
        return Rs2Inventory.itemQuantity("Coins");
    }

    private int magicLvl() {
        return Rs2Player.getRealSkillLevel(Skill.MAGIC);
    }

    private int defenceLvl() {
        return Rs2Player.getRealSkillLevel(Skill.DEFENCE);
    }

    private int mindRunes() {
        return qtyOf(MIND_RUNE_ID, MIND_RUNE_NAME);
    }

    private int waterRunes() {
        return qtyOf(WATER_RUNE_ID, WATER_RUNE_NAME);
    }

    private boolean hasBattleRunes() {
        return mindRunes() > 0;
    }

    private int qtyOf(int id, String name) {
        int q = Rs2Inventory.itemQuantity(name);
        Rs2ItemModel e = Rs2Equipment.get(id);
        if (e != null)
            q += e.getQuantity();
        return q;
    }

    public void reset() {
        this.phase = Phase.BANK_ITEMS;
        this.talismanBatch = 1;
        this.gearIndex = 0;
        this.groundDropCount = 0;
        this.dropsSinceLastRefresh = 0;
        this.restock = false;
        this.farm = false;
        this.endgame = false;
        this.finalizeTries = 0;
        this.batchesThisRun = TALISMAN_BATCHES;
        this.sellTries = 0;
        this.tpTries = 0;
        this.clanWarsTeleportTries = 0;
        this.lobbyWalkTries = 0;
        this.lastTp = 0L;
        this.noRunesSince = 0L;
        this.autoCastRejectLevel = -1;
        resetMarketState();
    }

    public void beginRestock() {
        this.restock = true;
        this.farm = false;
        this.endgame = false;
        this.restockFresh = true;
        this.batchesThisRun = RESTOCK_BATCHES;
        this.talismanBatch = 1;
        this.gearIndex = 0;
        resetMarketState();
        detectAndSetPhase();
    }

    public void beginFarm() {
        this.farm = true;
        this.restock = false;
        this.endgame = false;
        restartFarmCycle();
        log.info("[SS] FARM mode started - endless talisman batches");
    }

    public void stopFarm() {
        if (!this.farm)
            return;
        this.farm = false;
        this.phase = Phase.DONE;
        log.info("[SS] FARM mode stopped");
    }

    private void restartFarmCycle() {
        this.talismanBatch = 1;
        this.batchesThisRun = TALISMAN_BATCHES;
        this.gearIndex = 0;
        this.groundDropCount = 0;
        this.dropsSinceLastRefresh = 0;
        this.sellTries = 0;
        resetMarketState();
        this.phase = Phase.GO_TO_DUKE;
        log.info("[SS] FARM cycle restarted - moving to GO_TO_DUKE");
    }

    private void resetMarketState() {
        this.runePrice.clear();
        this.runeOfferAt.clear();
        this.runeOfferPlaced.clear();
        this.gearPriceOverride.clear();
        this.gearOfferAt.clear();
        this.gearOfferPlaced.clear();
        this.gearGaveUp.clear();
    }

    public void detectAndSetPhase() {
        int talismans = Rs2Inventory.count(1438);
        int noted = Rs2Inventory.itemQuantity(1439);
        int coins = coins();
        boolean allGearOwned = allGearOwnedOrEquipped();
        boolean allGearEquipped = allGearEquipped();
        boolean battleRunes = hasBattleRunes();
        if (!this.restock)
            this.restock = (allGearOwned && !battleRunes);
        this.batchesThisRun = this.restock ? RESTOCK_BATCHES : TALISMAN_BATCHES;
        boolean fresh = this.restockFresh;
        this.restockFresh = false;
        log.info("[SS] talismans={} noted={} coins={} allGearOwned={} allGearEquipped={} battleRunes={} restock={}",
                talismans, noted, coins, allGearOwned, allGearEquipped, battleRunes, this.restock);
        if (fresh) {
            if (noted > 0) {
                this.phase = Phase.WALK_TO_GE;
            } else if (talismans >= batchTarget()) {
                this.phase = (this.talismanBatch < this.batchesThisRun) ? Phase.BANK_NOTE_BATCH : Phase.WALK_TO_GE;
            } else if (talismans > 0) {
                this.phase = Phase.FARM_TALISMANS;
            } else {
                this.phase = Phase.GO_TO_DUKE;
            }
        } else if (allGearEquipped) {
            this.phase = (!this.restock && trainNeeded()) ? trainEntryPhase() : Phase.DONE;
        } else if (allGearOwned && battleRunes) {
            this.phase = Phase.EQUIP_GEAR;
        } else if (allGearOwned && coins >= 40) {
            this.phase = Phase.BUY_RUNES;
        } else if (!allGearOwned && coins > 0) {
            this.phase = Phase.BUY_GEAR;
        } else if (noted > 0) {
            this.phase = Phase.WALK_TO_GE;
        } else if (talismans >= batchTarget()) {
            this.phase = (this.talismanBatch < this.batchesThisRun) ? Phase.BANK_NOTE_BATCH : Phase.WALK_TO_GE;
        } else if (talismans > 0) {
            this.phase = Phase.FARM_TALISMANS;
        } else {
            this.phase = this.restock ? Phase.GO_TO_DUKE : Phase.BANK_ITEMS;
        }
        log.info("[SS] Phase set to {} (batch {}/{})", this.phase, this.talismanBatch, this.batchesThisRun);
        this.groundDropCount = 0;
        this.dropsSinceLastRefresh = 0;
    }

    private boolean ownsOrWears(String name) {
        return (Rs2Inventory.hasItem(name) || Rs2Equipment.isWearing(name));
    }

    private List<String> kit() {
        return new ArrayList<>(Arrays.asList(GEAR_NAMES));
    }

    private int priceOf(String name) {
        for (int i = 0; i < GEAR_NAMES.length; i++) {
            if (GEAR_NAMES[i].equals(name))
                return GEAR_BUY_PRICE[i];
        }
        return 1000;
    }

    private int gearPrice(String name) {
        return this.gearPriceOverride.getOrDefault(name, priceOf(name));
    }

    private boolean allGearOwnedOrEquipped() {
        for (String name : kit()) {
            if (!ownsOrWears(name))
                return false;
        }
        return true;
    }

    private boolean allGearEquipped() {
        for (String name : kit()) {
            if (!Rs2Equipment.isWearing(name))
                return false;
        }
        return true;
    }

    private void dismissOnboardingTipIfPresent() {
        Widget closeButton = Rs2Widget.getWidget(ONBOARDING_TIP_GROUP, ONBOARDING_TIP_CLOSE_CHILD);
        if (closeButton != null) {
            log.info("[SS] Dismissing onboarding tip popup");
            Rs2Widget.clickWidget(closeButton);
            Global.sleep(300, 500);
        }
    }

    private Widget findBankTutorialCloseButton() {
        return Microbot.getClientThread().runOnClientThreadOptional(() -> {
            Widget container = Rs2Widget.getWidget(BANK_TUTORIAL_GROUP, BANK_TUTORIAL_CONTAINER_CHILD);
            if (container == null || container.isHidden())
                return null;
            Widget closeContainer = Rs2Widget.getWidget(ONBOARDING_TIP_GROUP, SCREENHIGHLIGHT_CLOSE_CHILD);
            if (closeContainer == null || closeContainer.isHidden())
                return null;
            Widget[] children = closeContainer.getDynamicChildren();
            if (children != null)
                for (Widget child : children) {
                    if (child != null && !child.isHidden())
                        return child;
                }
            return closeContainer;
        }).orElse(null);
    }

    private void dismissBankTutorialIfPresent() {
        Widget closeButton = findBankTutorialCloseButton();
        if (closeButton == null)
            return;
        log.info("[SS] Dismissing first-time bank tutorial popup");
        Rs2Widget.clickWidget(closeButton);
        boolean closed = Global.sleepUntil(() -> (findBankTutorialCloseButton() == null), 2000);
        if (!closed) {
            Widget fallback = Rs2Widget.getWidget(ONBOARDING_TIP_GROUP, ONBOARDING_TIP_CLOSE_CHILD);
            if (fallback != null && !fallback.isHidden()) {
                log.info("[SS] Retrying bank tutorial dismiss on container widget");
                Rs2Widget.clickWidget(fallback);
                Global.sleepUntil(() -> (findBankTutorialCloseButton() == null), 2000);
            }
        }
        Global.sleep(300, 500);
    }

    public void tick() {
        dismissOnboardingTipIfPresent();
        dismissBankTutorialIfPresent();
        switch (this.phase) {
            case BANK_ITEMS:
                doBankItems();
                break;
            case GO_TO_DUKE:
                doGoToDuke();
                break;
            case FARM_TALISMANS:
                doFarmTalismans();
                break;
            case BANK_NOTE_BATCH:
                doBankNoteBatch();
                break;
            case WALK_TO_GE:
                doWalkToGe();
                break;
            case SELL_TALISMANS:
                doSellTalismans();
                break;
            case BUY_GEAR:
                doBuyGear();
                break;
            case BUY_RUNES:
                doBuyRunes();
                break;
            case EQUIP_GEAR:
                doEquipGear();
                break;
            case TRAIN_TELEPORT:
                doTrainTeleport();
                break;
            case TRAIN_WALK:
                doTrainWalk();
                break;
            case TRAIN:
                doTrain();
                break;
            case FINALIZE:
                doFinalize();
                break;
            case TELEPORT_CLAN_WARS:
                doTeleportClanWars();
                break;
            case WALK_TO_LOBBY:
                doWalkToLobby();
                break;
        }
    }

    private void doBankItems() {
        log.info("[SS] BANK_ITEMS: attempting to open bank");
        if (Rs2Equipment.isWearing("Wooden shield")) {
            Rs2Equipment.unEquip("Wooden shield");
            Global.sleepUntil(() -> !Rs2Equipment.isWearing("Wooden shield"), 2500);
        }
        if (!Rs2Bank.isOpen()) {
            Rs2Bank.walkToBankAndUseBank();
            Global.sleepUntil(Rs2Bank::isOpen, 5000);
            if (!Rs2Bank.isOpen())
                return;
        }
        dismissBankTutorialIfPresent();
        log.info("[SS] Bank open, depositing everything except talismans");
        Rs2Bank.depositAllExcept(1438, 1439);
        Global.sleep(400, 700);
        log.info("[SS] Deposit done, closing bank");
        Rs2Bank.closeBank();
        Global.sleepUntil(() -> !Rs2Bank.isOpen(), 3000);
        this.phase = Phase.GO_TO_DUKE;
        log.info("[SS] BANK_ITEMS complete, moving to GO_TO_DUKE");
    }

    private void doGoToDuke() {
        if (DUKE_TILE.distanceTo(Rs2Player.getWorldLocation()) <= 3) {
            this.phase = Phase.FARM_TALISMANS;
            log.info("[SS] GO_TO_DUKE complete, moving to FARM_TALISMANS");
            return;
        }
        Rs2Walker.walkTo(DUKE_TILE);
    }

    private long lastDukeDialogueEnd = 0L;

    private void doFarmTalismans() {
        int target = batchTarget();
        int invCount = Rs2Inventory.count(1438);
        if (invCount + this.groundDropCount >= target && this.groundDropCount > 0) {
            log.info("[SS] FARM_TALISMANS: batch coverable (inv={} + ground={} >= target={}), final pickup", invCount, this.groundDropCount, target);
            pickupAllDropped(true);
            int newInvCount = Rs2Inventory.count(1438);
            if (newInvCount >= target) {
                this.phase = (this.talismanBatch < this.batchesThisRun) ? Phase.BANK_NOTE_BATCH : Phase.WALK_TO_GE;
                log.info("[SS] FARM_TALISMANS: batch fully held ({} in inv), moving on", newInvCount);
            } else {
                log.info("[SS] FARM_TALISMANS: final pickup partial (inv={} groundLeft={}), will retry next tick", newInvCount, this.groundDropCount);
            }
            return;
        }
        if (invCount + this.groundDropCount >= target && invCount >= target) {
            this.phase = (this.talismanBatch < this.batchesThisRun) ? Phase.BANK_NOTE_BATCH : Phase.WALK_TO_GE;
            log.info("[SS] FARM_TALISMANS: inventory already holds full batch ({}), moving on", invCount);
            return;
        }
        if (this.dropsSinceLastRefresh >= 4) {
            log.info("[SS] FARM_TALISMANS: refreshing {} ground talisman(s) (total on ground: {})", this.dropsSinceLastRefresh, this.groundDropCount);
            pickupAllDropped(false);
            return;
        }
        if (Rs2Inventory.hasItem(1438)) {
            Rs2Walker.walkTo(DROP_TILE);
            Global.sleepUntil(() -> DROP_TILE.equals(Rs2Player.getWorldLocation()), 3000);
            int before = Rs2Inventory.count(1438);
            Rs2Inventory.drop(1438);
            boolean dropped = Global.sleepUntil(() -> (Rs2Inventory.count(1438) < before), 2000);
            if (dropped) {
                this.groundDropCount++;
                this.dropsSinceLastRefresh++;
                log.info("[SS] FARM_TALISMANS: dropped 1, groundDropCount={} dropsSinceLastRefresh={}", this.groundDropCount, this.dropsSinceLastRefresh);
            }
            return;
        }
        if (System.currentTimeMillis() - this.lastDukeDialogueEnd < 4000L && !Rs2Dialogue.isInDialogue()) {
            Global.sleepUntil(() -> Rs2Inventory.hasItem(1438), 3000);
            return;
        }
        if (!Rs2Dialogue.isInDialogue()) {
            Rs2NpcModel duke = Microbot.getRs2NpcCache().query().withId(DUKE_NPC_ID).nearest();
            if (duke == null) {
                log.warn("[SS] FARM_TALISMANS: Duke not found");
                Global.sleep(300, 500);
                return;
            }
            duke.click("Talk-to");
            Global.sleepUntil(Rs2Dialogue::isInDialogue, 6000);
            if (!Rs2Dialogue.isInDialogue())
                return;
        }
        int guard = 0;
        long lastProgress = System.currentTimeMillis();
        while (guard++ < 60 && !Rs2Inventory.hasItem(1438)) {
            if (!Rs2Dialogue.isInDialogue())
                if (!Global.sleepUntil(Rs2Dialogue::isInDialogue, 900))
                    break;
            if (Rs2Dialogue.hasSelectAnOption()) {
                if (!Rs2Dialogue.clickOption("rune") && !Rs2Dialogue.getDialogueOptions().isEmpty())
                    Rs2Dialogue.keyPressForDialogueOption(1);
            } else if (Rs2Dialogue.hasContinue()) {
                Rs2Dialogue.clickContinue();
            }
            Global.sleep(700, 1000);
            if (System.currentTimeMillis() - lastProgress > 30000L)
                break;
        }
        this.lastDukeDialogueEnd = System.currentTimeMillis();
        Global.sleepUntil(() -> Rs2Inventory.hasItem(1438), 3500);
    }

    private void pickupAllDropped(boolean keepInInventory) {
        if (this.groundDropCount <= 0)
            return;
        int expected = this.groundDropCount;
        log.info("[SS] Picking up {} dropped talisman(s), keep={}", expected, keepInInventory);
        Rs2Walker.walkTo(STAND_TILE);
        Global.sleepUntil(() -> STAND_TILE.equals(Rs2Player.getWorldLocation()), 3000);
        int picked = 0;
        int guard = 0;
        int maxAttempts = expected + 5;
        while (picked < expected && guard++ < maxAttempts) {
            if (Rs2Inventory.isFull()) {
                log.warn("[SS] Inventory full during pickup, {} still on ground", expected - picked);
                break;
            }
            int before = Rs2Inventory.count(1438);
            Rs2TileItemModel talisman = Microbot.getRs2TileItemCache().query()
                    .withName("Air talisman")
                    .nearest(5);
            boolean interacted = talisman != null && talisman.click("Take");
            if (!interacted) {
                log.warn("[SS] No ground talisman found to loot (expected {} more), retrying", expected - picked);
                Global.sleep(300, 500);
                continue;
            }
            boolean gotOne = Global.sleepUntil(() -> (Rs2Inventory.count(1438) > before), 2000);
            if (gotOne) {
                picked++;
                continue;
            }
            log.warn("[SS] Loot interaction fired but inventory count didn't increase, retrying");
            Global.sleep(300, 500);
        }
        this.groundDropCount -= picked;
        this.dropsSinceLastRefresh = 0;
        log.info("[SS] Pickup pass: picked={} remainingOnGround={} inv={}", picked, this.groundDropCount, Rs2Inventory.count(1438));
        if (!keepInInventory && picked > 0) {
            Rs2Walker.walkTo(DROP_TILE);
            Global.sleepUntil(() -> DROP_TILE.equals(Rs2Player.getWorldLocation()), 3000);
            int redropped = 0;
            while (redropped < picked && Rs2Inventory.hasItem(1438)) {
                int before = Rs2Inventory.count(1438);
                Rs2Inventory.drop(1438);
                boolean dropped = Global.sleepUntil(() -> (Rs2Inventory.count(1438) < before), 2000);
                if (!dropped)
                    break;
                redropped++;
            }
            this.groundDropCount += redropped;
            log.info("[SS] Refresh redrop: redropped={} groundDropCount={}", redropped, this.groundDropCount);
        }
    }

    private void doBankNoteBatch() {
        if (!Rs2Bank.isOpen()) {
            Rs2Bank.walkToBankAndUseBank();
            Global.sleepUntil(Rs2Bank::isOpen, 5000);
            if (!Rs2Bank.isOpen())
                return;
        }
        dismissBankTutorialIfPresent();
        boolean noted = false;
        for (int attempt = 1; attempt <= 5 && !noted; attempt++) {
            if (!Rs2Bank.isOpen()) {
                Rs2Bank.walkToBankAndUseBank();
                Global.sleepUntil(Rs2Bank::isOpen, 5000);
                if (!Rs2Bank.isOpen())
                    continue;
            }
            Rs2Bank.setWithdrawAsNote();
            Global.sleep(600, 900);
            if (Rs2Inventory.hasItem(1438)) {
                if (attempt % 2 == 1) {
                    Rs2Bank.depositAll(1438);
                } else {
                    Rs2Bank.depositAll("Air talisman");
                }
                Global.sleepUntil(() -> !Rs2Inventory.hasItem(1438), 3000);
            }
            if (!Rs2Inventory.hasItem(1438)) {
                Global.sleep(300, 500);
                Rs2Bank.withdrawAll(1438);
                Global.sleepUntil(() -> Rs2Inventory.hasItem(1439), 3000);
            }
            noted = (!Rs2Inventory.hasItem(1438) && Rs2Inventory.hasItem(1439));
            log.info("[SS] BANK_NOTE_BATCH attempt {}: unnoted={} noted={} ok={}", attempt, Rs2Inventory.count(1438), Rs2Inventory.itemQuantity(1439), noted);
        }
        if (!noted) {
            log.warn("[SS] BANK_NOTE_BATCH failed - retrying next tick");
            return;
        }
        Rs2Bank.closeBank();
        Global.sleepUntil(() -> !Rs2Bank.isOpen(), 3000);
        if (this.talismanBatch < this.batchesThisRun) {
            this.talismanBatch++;
            this.groundDropCount = 0;
            this.dropsSinceLastRefresh = 0;
            this.phase = Phase.GO_TO_DUKE;
            log.info("[SS] BANK_NOTE_BATCH complete, starting batch {}/{}, moving to GO_TO_DUKE", this.talismanBatch, this.batchesThisRun);
        } else {
            this.phase = Phase.WALK_TO_GE;
            log.info("[SS] BANK_NOTE_BATCH complete, moving to WALK_TO_GE");
        }
    }

    private void doWalkToGe() {
        if (Rs2GrandExchange.walkToGrandExchange()) {
            this.phase = Phase.SELL_TALISMANS;
            log.info("[SS] WALK_TO_GE complete, moving to SELL_TALISMANS");
        }
    }

    private int sellTries = 0;

    private int openTalismanSellPrice() {
        return Microbot.getClientThread().runOnClientThreadOptional(() -> {
            GrandExchangeOffer[] offers = Microbot.getClient().getGrandExchangeOffers();
            if (offers == null)
                return -1;
            for (GrandExchangeOffer o : offers) {
                if (o != null && o.getItemId() == AIR_TALISMAN_ID) {
                    GrandExchangeOfferState st = o.getState();
                    if (st == GrandExchangeOfferState.SELLING || st == GrandExchangeOfferState.SOLD || st == GrandExchangeOfferState.CANCELLED_SELL)
                        return (int) o.getPrice();
                }
            }
            return -1;
        }).orElse(-1);
    }

    private boolean talismanSellStillOpen() {
        return Microbot.getClientThread().runOnClientThreadOptional(() -> {
            GrandExchangeOffer[] offers = Microbot.getClient().getGrandExchangeOffers();
            if (offers == null)
                return false;
            for (GrandExchangeOffer o : offers) {
                if (o != null && o.getItemId() == AIR_TALISMAN_ID && o.getState() == GrandExchangeOfferState.SELLING)
                    return true;
            }
            return false;
        }).orElse(false);
    }

    private void doSellTalismans() {
        if (!Rs2GrandExchange.isOpen()) {
            log.info("[SS] SELL_TALISMANS: GE not open (player at {}), attempting to open", Rs2Player.getWorldLocation());
            boolean opened = Rs2GrandExchange.openExchange();
            boolean becameOpen = Global.sleepUntil(Rs2GrandExchange::isOpen, 5000);
            log.info("[SS] SELL_TALISMANS: openExchange() returned {}, isOpen now {}", opened, becameOpen);
            return;
        }
        if (Rs2Inventory.hasItem(AIR_TALISMAN_NOTED) || Rs2Inventory.hasItem(AIR_TALISMAN_ID)) {
            int qty = Rs2Inventory.itemQuantity(AIR_TALISMAN_NOTED) + Rs2Inventory.itemQuantity(AIR_TALISMAN_ID);
            if (qty <= 0)
                qty = 1;
            log.info("[SS] SELL_TALISMANS: selling {} Air talisman @ {} each (library offer flow)", qty, TALISMAN_SELL_PRICE);
            boolean ok = Rs2GrandExchange.sellItem("Air talisman", qty, TALISMAN_SELL_PRICE);
            Global.sleep(500, 800);
            int placed = openTalismanSellPrice();
            log.info("[SS] SELL_TALISMANS: sellItem ok={} offer price on the exchange = {}", ok, placed);
            if (placed > 0 && placed != TALISMAN_SELL_PRICE && this.sellTries < 3) {
                this.sellTries++;
                log.warn("[SS] SELL_TALISMANS: offer price {} != {} cancelling and re-offering (try {})", placed, TALISMAN_SELL_PRICE, this.sellTries);
                Rs2GrandExchange.abortAllOffers(false);
                Global.sleep(800, 1200);
            }
            return;
        }
        if (talismanSellStillOpen()) {
            Global.sleep(800, 1200);
            return;
        }
        log.info("[SS] SELL_TALISMANS: collecting sale proceeds");
        Rs2GrandExchange.collectAllToInventory();
        Global.sleep(800, 1200);
        if ((openTalismanSellPrice() > 0 || !Rs2Inventory.hasItem("Coins")) && !Rs2Inventory.hasItem("Coins"))
            return;
        if (this.endgame) {
            if (geBuyActive(MIND_RUNE_ID) || endgameRuneShortfall() <= 0 || coins() >= endgameRuneCost()) {
                this.phase = Phase.BUY_RUNES;
                log.info("[SS] SELL_TALISMANS complete ({} coins, {} mind runes) - buying the FFA rune stack", coins(), mindRunes());
            } else {
                log.info("[SS] SELL_TALISMANS complete ({} coins) - {} mind runes still needed (about {} coins), farming more",
                        coins(), endgameRuneShortfall(), endgameRuneCost());
                restartEndgameFarmCycle();
            }
            return;
        }
        this.phase = (this.restock && allGearOwnedOrEquipped()) ? Phase.BUY_RUNES : Phase.BUY_GEAR;
        log.info("[SS] SELL_TALISMANS complete ({} coins), moving to {}", coins(), this.phase);
    }

    private boolean geBuyActive(int itemId) {
        return Microbot.getClientThread().runOnClientThreadOptional(() -> {
            GrandExchangeOffer[] offers = Microbot.getClient().getGrandExchangeOffers();
            if (offers == null)
                return false;
            for (GrandExchangeOffer o : offers) {
                if (o != null && o.getItemId() == itemId && o.getState() == GrandExchangeOfferState.BUYING)
                    return true;
            }
            return false;
        }).orElse(false);
    }

    private void doBuyGear() {
        if (!Rs2GrandExchange.isOpen()) {
            Rs2GrandExchange.openExchange();
            Global.sleepUntil(Rs2GrandExchange::isOpen, 3000);
            return;
        }
        List<String> kitNow = kit();
        if (this.gearIndex >= kitNow.size()) {
            this.phase = Phase.BUY_RUNES;
            log.info("[SS] BUY_GEAR complete, moving to BUY_RUNES");
            return;
        }
        String name = kitNow.get(this.gearIndex);
        int id = GEAR_IDS[this.gearIndex];
        if (this.gearGaveUp.contains(name)) {
            this.gearIndex++;
            return;
        }
        if (!ownsOrWears(name)) {
            Rs2GrandExchange.collectAllToInventory();
            Global.sleepUntil(() -> ownsOrWears(name), 2500);
        }
        if (ownsOrWears(name)) {
            this.gearIndex++;
            this.gearOfferPlaced.put(name, false);
            log.info("[SS] BUY_GEAR: have {}, moving to next item ({} remaining)", name, kitNow.size() - this.gearIndex);
            return;
        }
        long now = System.currentTimeMillis();
        boolean active = geBuyActive(id);
        boolean placed = this.gearOfferPlaced.getOrDefault(name, false);
        long at = this.gearOfferAt.getOrDefault(name, 0L);
        int base = priceOf(name);
        int price = gearPrice(name);
        if (active) {
            if (placed && now - at > GEAR_FILL_WAIT_MS && price < base + GEAR_MAX_BUMP) {
                log.warn("[SS] BUY_GEAR: {} @ {} did not fill in {}s - cancelling, trying {}", name, price, GEAR_FILL_WAIT_MS / 1000, price + 1);
                Rs2GrandExchange.abortAllOffers(false);
                Global.sleep(800, 1200);
                Rs2GrandExchange.collectAllToInventory();
                Global.sleep(500, 800);
                this.gearPriceOverride.put(name, price + 1);
                this.gearOfferPlaced.put(name, false);
                return;
            }
            if (placed && now - at > GEAR_FILL_WAIT_MS * 3) {
                log.warn("[SS] BUY_GEAR: {} never filled at {} - skipping", name, price);
                this.gearGaveUp.add(name);
                this.gearIndex++;
                return;
            }
            Global.sleep(500, 800);
            return;
        }
        if (placed && now - at < 4000L) {
            Global.sleep(500, 800);
            return;
        }
        this.gearOfferPlaced.put(name, false);
        int coins = coins();
        if (coins < price) {
            log.warn("[SS] BUY_GEAR: not enough coins for {} (need {}, have {}) - skipping", name, price, coins);
            this.gearGaveUp.add(name);
            this.gearIndex++;
            return;
        }
        log.info("[SS] BUY_GEAR: placing buy offer for {} @ {}", name, price);
        Rs2GrandExchange.buyItem(name, price, 1);
        this.gearOfferPlaced.put(name, true);
        this.gearOfferAt.put(name, now);
        Global.sleep(600, 900);
    }

    private int runeBasePrice(int id) {
        return (id == MIND_RUNE_ID) ? MIND_RUNE_BUY_PRICE : WATER_RUNE_BUY_PRICE;
    }

    private int runePrice(int id) {
        return this.runePrice.getOrDefault(id, runeBasePrice(id));
    }

    private void doBuyRunes() {
        if (!Rs2GrandExchange.isOpen()) {
            Rs2GrandExchange.openExchange();
            Global.sleepUntil(Rs2GrandExchange::isOpen, 3000);
            return;
        }
        Rs2GrandExchange.collectAllToInventory();
        Global.sleep(300, 500);
        long now = System.currentTimeMillis();
        if (this.endgame) {
            doBuyEndgameRunes(now);
            return;
        }
        if (mindRunes() < MIND_RUNE_TARGET && buyRuneStep(MIND_RUNE_ID, MIND_RUNE_NAME, MIND_RUNE_TARGET, now))
            return;
        if (waterRunes() < WATER_RUNE_TARGET && buyRuneStep(WATER_RUNE_ID, WATER_RUNE_NAME, WATER_RUNE_TARGET, now))
            return;
        Rs2GrandExchange.closeExchange();
        Global.sleepUntil(() -> !Rs2GrandExchange.isOpen(), 3000);
        this.phase = Phase.EQUIP_GEAR;
        log.info("[SS] BUY_RUNES complete (mind {}, water {}, coins left {}), moving to EQUIP_GEAR", mindRunes(), waterRunes(), coins());
    }

    private void doBuyEndgameRunes(long now) {
        if (mindRunes() < ENDGAME_MIND_RUNE_TARGET) {
            if (buyRuneStep(MIND_RUNE_ID, MIND_RUNE_NAME, ENDGAME_MIND_RUNE_TARGET, now))
                return;
            if (geBuyActive(MIND_RUNE_ID)) {
                Global.sleep(600, 900);
                return;
            }
            if (mindRunes() < ENDGAME_MIND_RUNE_TARGET) {
                log.info("[SS] BUY_RUNES: {} mind runes still needed, only {} coins at {} each - farming more talismans",
                        ENDGAME_MIND_RUNE_TARGET - mindRunes(), coins(), runePrice(MIND_RUNE_ID));
                restartEndgameFarmCycle();
                return;
            }
        }
        Rs2GrandExchange.closeExchange();
        Global.sleepUntil(() -> !Rs2GrandExchange.isOpen(), 3000);
        this.clanWarsTeleportTries = 0;
        this.lobbyWalkTries = 0;
        this.phase = Phase.TELEPORT_CLAN_WARS;
        log.info("[SS] BUY_RUNES complete ({} mind runes, {} coins left) - teleporting to Clan Wars", mindRunes(), coins());
    }

    private boolean buyRuneStep(int id, String name, int target, long now) {
        int have = qtyOf(id, name);
        if (have >= target)
            return false;
        boolean active = geBuyActive(id);
        boolean placed = this.runeOfferPlaced.getOrDefault(id, false);
        long at = this.runeOfferAt.getOrDefault(id, 0L);
        int base = runeBasePrice(id);
        int price = runePrice(id);
        if (active) {
            if (placed && now - at > RUNE_FILL_WAIT_MS && price < base + RUNE_MAX_BUMP) {
                log.warn("[SS] BUY_RUNES: {} @ {} did not fill in {}s - cancelling, trying {}", name, price, RUNE_FILL_WAIT_MS / 1000, price + 1);
                Rs2GrandExchange.abortAllOffers(false);
                Global.sleep(800, 1200);
                Rs2GrandExchange.collectAllToInventory();
                Global.sleep(500, 800);
                this.runePrice.put(id, price + 1);
                this.runeOfferPlaced.put(id, false);
                return true;
            }
            if (placed && now - at > RUNE_FILL_WAIT_MS * 3) {
                log.warn("[SS] BUY_RUNES: {} never filled at {} - leaving the offer to finish", name, price);
                return false;
            }
            Global.sleep(500, 800);
            return true;
        }
        if (placed && now - at < 4000L) {
            Global.sleep(500, 800);
            return true;
        }
        this.runeOfferPlaced.put(id, false);
        int coins = coins();
        int qty = Math.min(target - have, coins / price);
        if (qty <= 0) {
            log.warn("[SS] BUY_RUNES: not enough coins for {} (price {}, have {})", name, price, coins);
            return false;
        }
        log.info("[SS] BUY_RUNES: placing buy offer for {} {} @ {}", qty, name, price);
        Rs2GrandExchange.buyItem(name, price, qty);
        this.runeOfferPlaced.put(id, true);
        this.runeOfferAt.put(id, now);
        Global.sleep(600, 900);
        return true;
    }

    private int endgameRuneShortfall() {
        return Math.max(0, ENDGAME_MIND_RUNE_TARGET - mindRunes());
    }

    private int endgameRuneCost() {
        int shortfall = endgameRuneShortfall();
        if (shortfall <= 0)
            return 0;
        return shortfall * runePrice(MIND_RUNE_ID) + ENDGAME_COIN_BUFFER;
    }

    private void beginEndgameRunes() {
        this.endgame = true;
        this.restock = false;
        this.farm = false;
        this.talismanBatch = 1;
        this.batchesThisRun = TALISMAN_BATCHES;
        this.groundDropCount = 0;
        this.dropsSinceLastRefresh = 0;
        this.sellTries = 0;
        this.clanWarsTeleportTries = 0;
        this.lobbyWalkTries = 0;
        resetMarketState();
        if (endgameRuneShortfall() <= 0) {
            this.phase = Phase.TELEPORT_CLAN_WARS;
        } else if (coins() >= endgameRuneCost()) {
            this.phase = Phase.WALK_TO_GE;
        } else {
            this.phase = Phase.GO_TO_DUKE;
        }
        log.info("[SS] Endgame rune grind started - {} mind runes needed ({}), {} coins held, phase {}",
                endgameRuneShortfall(), ENDGAME_MIND_RUNE_TARGET, coins(), this.phase);
    }

    private void restartEndgameFarmCycle() {
        if (Rs2GrandExchange.isOpen()) {
            Rs2GrandExchange.abortAllOffers(false);
            Global.sleep(800, 1200);
            Rs2GrandExchange.collectAllToInventory();
            Global.sleep(300, 500);
            Rs2GrandExchange.closeExchange();
            Global.sleepUntil(() -> !Rs2GrandExchange.isOpen(), 3000);
        }
        this.talismanBatch = 1;
        this.batchesThisRun = TALISMAN_BATCHES;
        this.groundDropCount = 0;
        this.dropsSinceLastRefresh = 0;
        this.sellTries = 0;
        resetMarketState();
        this.phase = Phase.GO_TO_DUKE;
        log.info("[SS] Endgame farm cycle restarted - moving to GO_TO_DUKE");
    }

    private void doEquipGear() {
        if (!Rs2Equipment.isWearing("Staff of air") && Rs2Inventory.hasItem("Staff of air")) {
            Rs2Inventory.interact("Staff of air", "Wield");
            Global.sleep(400, 700);
            return;
        }
        for (String name : kit()) {
            if (Rs2Equipment.isWearing(name) || !Rs2Inventory.hasItem(name))
                continue;
            Rs2Inventory.wear(name);
            Global.sleep(400, 700);
            return;
        }
        boolean wasRestock = this.restock;
        this.restock = false;
        if (this.farm) {
            restartFarmCycle();
            return;
        }
        if (!wasRestock && trainNeeded()) {
            this.tpTries = 0;
            this.phase = trainEntryPhase();
            log.info("[SS] EQUIP_GEAR complete - training next (Magic {}/{}, Defence {}/{})", magicLvl(), MAGIC_TARGET, defenceLvl(), DEFENCE_TARGET);
            return;
        }
        this.phase = Phase.DONE;
        log.info("[SS] EQUIP_GEAR complete, self-sufficient startup DONE");
    }

    private int finalizeTries = 0;

    private void doFinalize() {
        if (Rs2Dialogue.isInDialogue()) {
            Rs2Dialogue.clickContinue();
            Global.sleep(300, 500);
            return;
        }
        if (Rs2GrandExchange.isOpen()) {
            Rs2GrandExchange.closeExchange();
            Global.sleepUntil(() -> !Rs2GrandExchange.isOpen(), 2000);
            return;
        }
        if (Rs2Bank.isOpen()) {
            Rs2Bank.closeBank();
            Global.sleepUntil(() -> !Rs2Bank.isOpen(), 2000);
            return;
        }
        if (!Rs2Equipment.isWearing("Staff of air") && Rs2Inventory.hasItem("Staff of air")) {
            Rs2Inventory.interact("Staff of air", "Wield");
            Global.sleep(500, 800);
            return;
        }
        Rs2Tab.switchTo(InterfaceTab.INVENTORY);
        int drop = nextDropId();
        if (drop == -1 || this.finalizeTries++ > 80) {
            this.finalizeTries = 0;
            this.phase = Phase.DONE;
            log.info("[SS] FINALIZE complete - inventory keeps the gear and runes, staff worn: {}", Rs2Equipment.isWearing("Staff of air"));
            return;
        }
        log.info("[SS] FINALIZE: dropping item {}", drop);
        Rs2Inventory.drop(drop);
        Global.sleep(250, 450);
    }

    private int nextDropId() {
        try {
            ItemContainer c = Microbot.getClientThread().runOnClientThreadOptional(() -> Microbot.getClient().getItemContainer(93)).orElse(null);
            if (c == null)
                return -1;
            for (Item it : c.getItems()) {
                if (it == null || it.getId() <= 0 || it.getQuantity() <= 0)
                    continue;
                if (isKeptItem(it.getId()))
                    continue;
                return it.getId();
            }
        } catch (Throwable t) {
            log.warn("[SS] FINALIZE: inventory read failed: {}", t.toString());
        }
        return -1;
    }

    private boolean isKeptItem(int id) {
        for (int keep : KEEP_ITEM_IDS) {
            if (keep == id)
                return true;
        }
        return false;
    }

    private int tpTries = 0;

    private long lastTp = 0L;

    private long noRunesSince = 0L;

    private boolean nearLumbridge(WorldPoint p) {
        return (p != null && p.getPlane() <= 2 && p.getY() < 4000 && p.distanceTo2D(LUMBRIDGE) <= 45);
    }

    private boolean nearWizardTower(WorldPoint p) {
        return (p != null && p.getY() < 4000 && p.distanceTo2D(WIZARD_TOWER_BOTTOM) <= 40);
    }

    private boolean onWizardTowerTop(WorldPoint p) {
        return (p != null && p.getPlane() == 2 && p.getX() >= WIZARD_TOWER_MIN_X && p.getX() <= WIZARD_TOWER_MAX_X
                && p.getY() >= WIZARD_TOWER_MIN_Y && p.getY() <= WIZARD_TOWER_MAX_Y);
    }

    public boolean trainNeeded() {
        return magicLvl() < MAGIC_TARGET
                || defenceLvl() < DEFENCE_TARGET
                || totalLevel() <= TOTAL_LEVEL_TARGET;
    }

    private int totalLevel() {
        return Microbot.getClientThread()
                .runOnClientThreadOptional(() -> Microbot.getClient().getTotalLevel())
                .orElse(0);
    }

    private Phase trainEntryPhase() {
        WorldPoint p = Rs2Player.getWorldLocation();
        return nearWizardTower(p) ? Phase.TRAIN_WALK : Phase.TRAIN_TELEPORT;
    }

    private void doTrainTeleport() {
        WorldPoint p = Rs2Player.getWorldLocation();
        if (nearLumbridge(p)) {
            this.phase = Phase.TRAIN_WALK;
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastTp < 14000L) {
            Global.sleep(300, 500);
            return;
        }
        if (this.tpTries >= 3) {
            log.warn("[SS] TRAIN: home teleport did not work {} times - walking to the Wizard Tower", this.tpTries);
            Rs2Walker.walkTo(WIZARD_TOWER_TOP, 3);
            this.phase = Phase.TRAIN_WALK;
            return;
        }
        this.lastTp = now;
        this.tpTries++;
        log.info("[SS] TRAIN: Lumbridge Home Teleport (try {})", this.tpTries);
        Rs2Tab.switchTo(InterfaceTab.MAGIC);
        Global.sleepUntil(() -> (Rs2Tab.getCurrentTab() == InterfaceTab.MAGIC), 2000);
        Global.sleep(250, 400);
        Widget w = Rs2Widget.getWidget(218, 6);
        if (w == null) {
            log.warn("[SS] TRAIN: home teleport widget (218.6) not found");
            return;
        }
        Rs2Widget.clickWidget(w);
        boolean arrived = Global.sleepUntil(() -> {
            WorldPoint t = Rs2Player.getWorldLocation();
            return nearLumbridge(t);
        }, 35000);
        if (arrived) {
            Rs2Tab.switchTo(InterfaceTab.INVENTORY);
            this.phase = Phase.TRAIN_WALK;
        }
    }

    private void doTrainWalk() {
        WorldPoint p = Rs2Player.getWorldLocation();
        if (p == null)
            return;
        if (onWizardTowerTop(p)) {
            this.phase = Phase.TRAIN;
            log.info("[SS] TRAIN: at the Wizard Tower top floor");
            return;
        }
        Rs2Walker.walkTo(WIZARD_TOWER_TOP, 2);
    }

    private long lastAutoCastAt = 0L;

    private int autoCastRejectLevel = -1;

    private void ensureAutoCast() {
        if (mindRunes() <= 0)
            return;
        final boolean defensive = defenceLvl() < DEFENCE_TARGET;
        Rs2CombatSpells want = desiredAutoCastSpell();
        final int wantedDefensive = defensive ? DEFENSIVE_CASTING_ON : 0;
        if (Rs2Magic.getCurrentAutoCastSpell() == want
                && Microbot.getVarbitValue(VarbitID.AUTOCAST_DEFMODE) == wantedDefensive)
            return;
        long now = System.currentTimeMillis();
        if (now - this.lastAutoCastAt < 3500L)
            return;
        this.lastAutoCastAt = now;
        if (Rs2Combat.setAutoCastSpell(want, defensive)) {
            this.autoCastRejectLevel = -1;
            log.info("[SS] TRAIN: {} autocast set to {}", defensive ? "defensive" : "standard", want);
        } else if (want != Rs2CombatSpells.WIND_STRIKE) {
            this.autoCastRejectLevel = magicLvl();
            log.warn("[SS] TRAIN: could not set defensive autocast to {} at Magic {} - staying on Wind Strike", want, magicLvl());
        } else {
            log.warn("[SS] TRAIN: failed to set autocast to {}", want);
        }
    }

    private Rs2CombatSpells desiredAutoCastSpell() {
        if (magicLvl() >= WATER_STRIKE_MAGIC_LEVEL
                && waterRunes() > 0
                && this.autoCastRejectLevel != magicLvl())
            return Rs2CombatSpells.WATER_STRIKE;
        return Rs2CombatSpells.WIND_STRIKE;
    }

    private void doTrain() {
        if (Rs2Dialogue.isInDialogue()) {
            Rs2Dialogue.clickContinue();
            Global.sleep(300, 500);
            return;
        }
        if (!trainNeeded()) {
            log.info("[SS] TRAIN complete (Magic {}, Defence {}, Total {}) - starting the rune grind", magicLvl(), defenceLvl(), totalLevel());
            beginEndgameRunes();
            return;
        }
        WorldPoint p = Rs2Player.getWorldLocation();
        if (!onWizardTowerTop(p)) {
            this.phase = Phase.TRAIN_WALK;
            return;
        }
        if (!Rs2Equipment.isWearing("Staff of air") && Rs2Inventory.hasItem("Staff of air")) {
            log.info("[SS] TRAIN: wielding the Staff of air");
            Rs2Inventory.interact("Staff of air", "Wield");
            Global.sleep(700, 1000);
            return;
        }
        if (!hasBattleRunes()) {
            if (this.noRunesSince == 0L)
                this.noRunesSince = System.currentTimeMillis();
            if (System.currentTimeMillis() - this.noRunesSince > 25000L) {
                log.warn("[SS] TRAIN: out of mind runes - leaving (restock will handle it)");
                this.phase = Phase.DONE;
                return;
            }
            Global.sleep(300, 500);
            return;
        }
        this.noRunesSince = 0L;
        ensureAutoCast();
        Actor tgt = Rs2Player.getInteracting();
        boolean fighting = (tgt instanceof NPC && !tgt.isDead());
        if (fighting) {
            Global.sleep(100, 170);
            return;
        }
        Rs2NpcModel demon = Microbot.getRs2NpcCache().query()
                .withName(LESSER_DEMON_NAME)
                .where(npc -> !npc.isDead())
                .nearest(10);
        if (demon == null || !demon.click("Attack")) {
            Global.sleep(250, 450);
            return;
        }
        Global.sleepUntil(() -> (Rs2Player.getInteracting() != null), 1500);
    }

    private void doTeleportClanWars() {
        WorldPoint p = Rs2Player.getWorldLocation();
        if (nearClanWars(p)) {
            this.phase = Phase.WALK_TO_LOBBY;
            log.info("[SS] TELEPORT_CLAN_WARS: already at Clan Wars/Ferox Enclave");
            return;
        }
        if (this.clanWarsTeleportTries >= 3) {
            log.warn("[SS] TELEPORT_CLAN_WARS: minigame teleport did not land after {} tries - walking instead", this.clanWarsTeleportTries);
            Rs2Walker.walkTo(CLAN_WARS_TILE, 3);
            this.phase = Phase.WALK_TO_LOBBY;
            return;
        }
        this.clanWarsTeleportTries++;
        log.info("[SS] TELEPORT_CLAN_WARS: using the Clan Wars minigame teleport (try {})", this.clanWarsTeleportTries);
        if (minigameTeleportToClanWars()) {
            this.phase = Phase.WALK_TO_LOBBY;
        } else {
            log.warn("[SS] TELEPORT_CLAN_WARS: teleport attempt {} failed", this.clanWarsTeleportTries);
            Global.sleep(600, 1000);
        }
    }

    private void doWalkToLobby() {
        if (nearLobby(Rs2Player.getWorldLocation())) {
            this.phase = Phase.DONE;
            log.info("[SS] WALK_TO_LOBBY complete - handing over to the FFA loop");
            return;
        }
        if (this.lobbyWalkTries >= 6) {
            log.warn("[SS] WALK_TO_LOBBY: could not reach the lobby after {} walks - handing over anyway", this.lobbyWalkTries);
            this.phase = Phase.DONE;
            return;
        }
        this.lobbyWalkTries++;
        Rs2Walker.walkTo(LOBBY_TILE, 2);
    }

    private boolean nearClanWars(WorldPoint p) {
        return (p != null && p.getY() < 4000 && p.distanceTo2D(CLAN_WARS_TILE) <= CLAN_WARS_ARRIVAL_DISTANCE);
    }

    private boolean nearLobby(WorldPoint p) {
        return p != null && LOBBY_AREA.contains(p);
    }

    private boolean minigameTeleportToClanWars() {
        if (Rs2Dialogue.isInDialogue()) {
            Rs2Dialogue.clickContinue();
            Global.sleep(300, 500);
            return false;
        }
        if (!Rs2Tab.switchTo(InterfaceTab.CHAT)) {
            log.warn("[SS] TELEPORT_CLAN_WARS: could not open the grouping/chat tab");
            return false;
        }
        Global.sleepUntil(() -> Rs2Tab.getCurrentTab() == InterfaceTab.CHAT, 2000);
        Widget groupingBtn = Rs2Widget.getWidget(GROUPING_BUTTON_COMPONENT_ID);
        if (groupingBtn == null)
            return false;
        if (!Arrays.equals(groupingBtn.getOnOpListener(), GROUPING_SELECTED_OP)) {
            Rs2Widget.clickWidget(groupingBtn);
            Global.sleepUntil(() -> {
                Widget b = Rs2Widget.getWidget(GROUPING_BUTTON_COMPONENT_ID);
                return b != null && Arrays.equals(b.getOnOpListener(), GROUPING_SELECTED_OP);
            }, 2000);
        }
        Widget selectedWidget = Rs2Widget.getWidget(SELECTED_MINIGAME);
        if (selectedWidget == null)
            return false;
        if (selectedWidget.getText() == null || !selectedWidget.getText().equalsIgnoreCase(CLAN_WARS_MINIGAME)) {
            Widget dropdownBtn = Rs2Widget.getWidget(DROPDOWN_BUTTON_COMPONENT_ID);
            if (dropdownBtn == null)
                return false;
            if (dropdownBtn.getSpriteId() != DROPDOWN_SELECTED_SPRITE_ID) {
                Rs2Widget.clickWidget(dropdownBtn);
                Global.sleepUntil(() -> {
                    Widget open = Rs2Widget.getWidget(DROPDOWN_BUTTON_COMPONENT_ID);
                    return Rs2Widget.findWidget(DROPDOWN_SELECTED_SPRITE_ID, open == null ? List.of() : List.of(open)) != null;
                }, 2000);
            }
            Widget list = Rs2Widget.getWidget(MINIGAME_LIST);
            if (list == null || list.getDynamicChildren() == null)
                return false;
            Widget destinationWidget = Rs2Widget.findWidget(CLAN_WARS_MINIGAME, Arrays.asList(list.getDynamicChildren()));
            if (destinationWidget == null) {
                log.warn("[SS] TELEPORT_CLAN_WARS: '{}' not found in the minigame list", CLAN_WARS_MINIGAME);
                return false;
            }
            NewMenuEntry entry = new NewMenuEntry()
                    .option("Select")
                    .target("")
                    .identifier(1)
                    .type(MenuAction.CC_OP)
                    .param0(destinationWidget.getIndex())
                    .param1(list.getId())
                    .forceLeftClick(false);
            Microbot.doInvoke(entry, new Rectangle(1, 1));
            boolean selected = Global.sleepUntil(() -> {
                Widget w = Rs2Widget.getWidget(SELECTED_MINIGAME);
                return w != null && w.getText() != null && w.getText().equalsIgnoreCase(CLAN_WARS_MINIGAME);
            }, 2000);
            if (!selected)
                return false;
        }
        Widget teleportBtn = Rs2Widget.getWidget(MINIGAME_TELEPORT_BUTTON);
        if (teleportBtn == null)
            return false;
        Rs2Widget.clickWidget(teleportBtn);
        Global.sleepUntil(Rs2Player::isAnimating, 2000);
        Global.sleepUntil(() -> !Rs2Player.isAnimating() && !hasMinigameTeleportGraphic(), 20000);
        Rs2Tab.switchTo(InterfaceTab.INVENTORY);
        return nearClanWars(Rs2Player.getWorldLocation());
    }

    private boolean hasMinigameTeleportGraphic() {
        for (int graphic : MINIGAME_TELEPORT_GRAPHICS) {
            if (Rs2Player.hasSpotAnimation(graphic))
                return true;
        }
        return false;
    }
}
