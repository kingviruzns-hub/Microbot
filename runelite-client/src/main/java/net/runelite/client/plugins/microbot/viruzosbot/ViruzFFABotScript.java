package net.runelite.client.plugins.microbot.viruzosbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Actor;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.MenuEntry;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.kit.KitType;
import net.runelite.api.widgets.Widget;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.RuneLiteDebug;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.api.tileobject.models.TileObjectType;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.misc.Rs2UiHelper;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.player.Rs2PlayerModel;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.walker.WalkerState;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;


import java.awt.Point;
import java.awt.Rectangle;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.Paths.get;


public class ViruzFFABotScript extends Script
{
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ViruzFFABotScript.class);
	static final String VERSION = "1.0.4";

	public static void main(String[] args) throws Exception {
		RuneLiteDebug.pluginsToDebug.add(ViruzFFABotPlugin.class);
		RuneLiteDebug.main(args);
	}

    private void applyConfig() {
		if (config == null) return;
		this.selectedLocation = config.location() != null ? config.location().getDisplayName() : "None selected";
		this.attackEnabled = config.attack();
		this.walkToTargetEnabled = config.walkToTarget();
		this.quickprayersEnabled = config.quickPrayer();
		this.prayerOffEnabled = config.prayerOff();
		this.switchPrayersEnabled = config.switchPrayers();
		this.freeRoamEnabled = config.freeRoam();
		this.protectBluePhatsEnabled = config.protectBluePhats();
		this.reportEnabled = config.report();
		this.chatGrabberEnabled.set(config.chatGrabber());
		this.lureMultiEnabled = config.lureMulti();
		this.noDragEnabled = config.noDrag();
		this.dragModeEnabled = config.dragMode();
		if (this.lureMultiEnabled) {
			this.noDragEnabled = false;
		}
		this.hillScanEnabled = config.scanHills();

		focusPlayerNames.clear();
		String focus = config.focusPlayers();
		if (focus != null && !focus.trim().isEmpty()) {
			for (String name : focus.split(",")) {
				String trimmed = name.trim().toLowerCase(Locale.ROOT);
				if (!trimmed.isEmpty()) {
					focusPlayerNames.add(trimmed);
				}
			}
		}
		this.focusPlayersEnabled = !focusPlayerNames.isEmpty();

		String unlist = config.unlist();
		if (unlist != null && !unlist.trim().isEmpty()) {
			unlistNames(unlist);
		}
		String whitelist = config.whitelist();
		if (whitelist != null && !whitelist.trim().isEmpty()) {
			whitelistNames(whitelist);
		}
	}
	private static final int REPORT_WIDGET_PARENT = 875;
	private static final int REASON_PARENT_CHILD_INDEX = 19;
	private static final int MACRO_REASON_CHILD_INDEX = 0;
	private static final long POST_REPORT_COOLDOWN_MS = 1000L;
	private static final int MAX_FAILS = 10;
	private static final double MAX_TARGET_DISTANCE = 15.0;
	private static final long POST_WALK_ATTACK_WINDOW_MS = 5000L;
	private static final int POST_WALK_ATTACK_SLEEP_MIN_MS = 50;
	private static final int POST_WALK_ATTACK_SLEEP_MAX_MS = 200;
	private static final String RUNE_ARROW = "Rune arrow";
	private static final String ADAMANT_ARROW = "Adamant arrow";
	private static final String MITHRIL_ARROW = "Mithril arrow";
	private final WorldArea AT_LOBBY = new WorldArea(3128, 3625, 11, 11, 0);
	private final WorldArea SAFE_ZONE = new WorldArea(3318, 4748, 21, 12, 0);
	private final WorldArea IN_FFA = new WorldArea(3274, 4760, 106, 94, 0);
	private final WorldArea STUCK = new WorldArea(3126, 3618, 18, 9, 1);
	private final WorldArea FREE_ROAM_AREA = new WorldArea(3274, 4760, 77, 92, 0);
	private final WorldArea FREE_ROAM_AREA_2 = new WorldArea(3274, 4760, 107, 6, 0);
	private final WorldArea AT_MULTI = new WorldArea(3275, 4800, 104, 54, 0);
	private final WorldArea TOWER1 = new WorldArea(3364, 4824, 8, 5, 0);
	private final WorldArea TOWER2 = new WorldArea(3371, 4819, 4, 10, 0);
	private final WorldArea SECOND_TOWER1 = new WorldArea(3364, 4812, 3, 6, 0);
	private final WorldArea SECOND_TOWER2 = new WorldArea(3366, 4812, 4, 3, 0);
	private final WorldArea SECOND_TOWER3 = new WorldArea(3369, 4806, 5, 9, 0);
	private static final WorldPoint DRAG_EAST = new WorldPoint(3376, 4762, 0);
	private static final WorldPoint DRAG_WEST = new WorldPoint(3277, 4762, 0);
	private static final int DRAG_SAFE_ZONE_Y = 4758;
	private static final int DRAG_ARRIVE_DISTANCE = 1;
	private static final int DRAG_CLOSE_DISTANCE = 3;
	private static final int DRAG_FOLLOW_DISTANCE = 8;
	private static final long DRAG_RETALIATE_TIMEOUT_MS = 4000L;
	private static final long DRAG_CLOSE_TIMEOUT_MS = 8000L;
	private static final long DRAG_WALK_TIMEOUT_MS = 20_000L;
	private static final int DRAG_STEP_SLEEP_MIN_MS = 150;
	private static final int DRAG_STEP_SLEEP_MAX_MS = 350;
	private static final int DRAG_MAX_STEPS = 300;
	private static final int DRAG_MAX_REENGAGE_ATTEMPTS = 5;
	@Getter
	@Setter
	private boolean attackEnabled = true;
	@Getter
	@Setter
	private boolean walkToTargetEnabled = false;
	@Getter
	@Setter
	private boolean quickprayersEnabled = false;
	@Getter
	@Setter
	private boolean prayerOffEnabled = false;
	@Getter
	@Setter
	private boolean switchPrayersEnabled = false;
	@Getter
	@Setter
	private boolean freeRoamEnabled = false;
	@Getter
	@Setter
	private boolean protectBluePhatsEnabled = false;
	@Getter
	@Setter
	private boolean reportEnabled = false;
	private int antiLureScan = 0;
	private int ffaScanMisses = 0;
	private int antiLureFails = 0;
	public volatile String status = "Paused";
	private volatile String lastKnownPlayerName = "";
	private final AtomicBoolean sessionInitialized = new AtomicBoolean(false);
	private volatile boolean sessionAnnounced = false;
	String selectedLocation = "None selected";
	private final int currentThreshold = getRandomThreshold();
	private WorldArea currentArea = null;
	private long lastSelectedLocationWalkFinishedAtMs = 0L;
	Timer timer2;
	private final Timer arrowCheckTimer = new Timer(0L);
	private boolean isAttacking = false;
	private Set<String> attackSet = new HashSet<>();
	private final Set<String> clanMembers = new HashSet<>();
	private final Set<String> lureDragSet = new HashSet<>();
	private long nextActionTime;
	long fatigueStart = System.currentTimeMillis();
	private int actionsSinceBreak = 0;
	private double ultradianPhase = 0.0;
	long fatigueSpikeEndTime = 0L;
	private double fatigueSpikeMagnitude = 0.0;
	long performanceStreakEndTime = 0L;
	private double performanceStreakBoost = 0.0;
	private boolean forcedBreakPending = false;
	private long nextFatigueCheck = System.currentTimeMillis() + 60000;
	private long nextUltradianUpdateMs = System.currentTimeMillis() + Rs2Random.between(600000, 1200000);
	private long fatigueSpikeCheckTime = System.currentTimeMillis() + 180000;
	private double sessionFatiguePeak = 0.0;
	private final Set<String> BLOCKED_NAMES = Set.of("Viruz SO", "Zoloroc", "vRolts", "Hans Taween");
	private final String filePath = Config.getNsListFilePath();
	private static final String BLUE_PHAT_NAME = "Blue partyhat";
	private static final String STAFF_OF_AIR = "Staff of air";
	private final Set<String> targets = new HashSet<>();
	private final Set<String> bluePhatNames = new HashSet<>();
	private final Map<String, WorldArea> areaMap = new HashMap<>();
	private static final Set<Integer> MAGIC_ATTACK_ANIMATIONS = Set.of(1162, 1163, 1164, 1165, 1978, 1979);
	private long nextAfkBreakTime = System.currentTimeMillis() + Rs2Random.between(1800000, 3600000);
	private static final Set<Integer> RANGE_ATTACK_ANIMATIONS = Set.of(426, 4230, 929);
	private static final Set<Integer> MELEE_ATTACK_ANIMATIONS = Set.of(390, 386, 388, 400, 401, 407, 406, 393, 395, 412, 422, 419);
	private static final String[] MELEE_WEAPONS = new String[]{"Rune scimitar", "Rune sword", "Rune longsword", "Rune axe", "Rune pickaxe", "Rune mace", "Rune warhammer", "Rune battleaxe", "Rune dagger", "Gilded scimitar", "Gilded spade", "Gilded pickaxe", "Gilded axe"};

	private static final long CHAT_GRAB_COOLDOWN_MS = 2500L;
	final AtomicBoolean chatGrabberEnabled = new AtomicBoolean(false);
	boolean focusPlayersEnabled = false;
	boolean lureMultiEnabled = false;
	private boolean hillScanActive = false;
	boolean hillScanEnabled = false;
	boolean noDragEnabled = false;
	@Getter
	@Setter
	private boolean dragModeEnabled = false;
	@Setter
    private ViruzFFABotConfig config;
	private final Set<String> focusPlayerNames = new HashSet<>();
	private final Map<String, Long> lastGrabbedBySender = new HashMap<>();
	private long nextReportAttemptAtMs = 0L;
	private long nextReportListReloadAtMs = 0L;
	private static final long REPORT_ATTEMPT_MIN_INTERVAL_MS = 45_000L;
	private static final long REPORT_ATTEMPT_MAX_INTERVAL_MS = 180_000L;
	private static final long REPORT_LIST_RELOAD_MIN_INTERVAL_MS = 60_000L;
	private static final long ATTACK_LIST_RELOAD_INTERVAL_MS = 10L * 60L * 1000L;
	private static final int COMBAT_WIDGET_PARENT = 593;
	private static final int[] AUTO_RETALIATE_CANDIDATE_CHILDS = {25, 27, 30, 4, 5};
	private boolean autoRetaliateOffVerified = false;
	private long nextAutoRetaliateCheckMs = 0L;
	private static final long AUTO_RETALIATE_CHECK_INTERVAL_MS = 300_000L;
	private static final int[] RANDOM_EVENT_DIALOG_PARENTS = {231, 233, 217, 11};
	private static final int RANDOM_EVENT_CONTINUE_CHILD = 3;
	private long nextRandomEventCheckMs = 0L;
	private static final long RANDOM_EVENT_CHECK_INTERVAL_MS = 5_000L;
	private long nextAttackListReloadAtMs = 0L;
	private volatile boolean reloadNsListRequested = false;
	double energyBoost = 0.0;
	private long lastEnergyBoostTime = System.currentTimeMillis();
	private long nextEnergyBoostCheck = System.currentTimeMillis() + 60000;
	private long lastAttackAttemptAtMs = 0L;
	private String lastAttackTargetName = null;
	private long lastSameTargetAttemptAtMs = 0L;
	private static final long MIN_ATTACK_ATTEMPT_INTERVAL_MS = 600L;
	private static final long SAME_TARGET_COOLDOWN_MS = 1200L;
	private static final long TARGET_SKIP_COOLDOWN_MS = 5L * 60L * 1000L;
	private static final long LOCATION_SWITCH_IDLE_MS = 60L * 120L * 2000L;
	private static final List<String> AUTO_SWITCH_LOCATIONS = Arrays.asList("Portal", "Middle", "Hills single", "Hills");
	private final Map<String, Long> targetCooldownUntilMs = new HashMap<>();
	private long lastCombatActivityMs = System.currentTimeMillis();
	private long lastAutoLocationSwitchMs = 0L;
	private long sessionStartMs = System.currentTimeMillis();
	private long forcedBreakUntilMs = 0L;
	private long lastForcedBreakMs = 0L;
	private long nextAntiBanAtMs = 0L;
	final AIAssistant aiAssistant = new AIAssistant();
	final SessionTracker sessionTracker = new SessionTracker();
	final NonTargetTracker nonTargetTracker = new NonTargetTracker();
	final PlayerStats playerStats = new PlayerStats();
	private long nextNonTargetScanMs = 0L;
	private long noValidTargetCooldownUntilMs = 0L;
	private static final int MAX_SAFE_ZONE_RETURN_ATTEMPTS = 8;
	private boolean safeZoneEscapeActive = false;
	private WorldPoint safeZoneReturnTarget = null;
	private int safeZoneReturnAttempts = 0;

	private static final long MAX_SESSION_MS = 5L * 60L * 60L * 1000L;
	private static final long FORCED_BREAK_MIN_MS = 5L * 60L * 1000L;
	private static final long FORCED_BREAK_MAX_MS = 15L * 60L * 1000L;
	private static final long MIN_BREAK_INTERVAL_MS = 2L * 60L * 60L * 1000L;
	private static final long ANTIBAN_MIN_INTERVAL_MS = 120_000L;
	private static final long ANTIBAN_MAX_INTERVAL_MS = 300_000L;
	private long lastMistakeMs = 0L;
	private static final long MISTAKE_COOLDOWN_MS = 45_000L;
	private static final int AUTO_NS_ADD_ATTACKS_THRESHOLD = 15;
	private static final long AUTO_NS_ADD_MIN_EVENT_INTERVAL_MS = 750L;
	private static final int MIN_AUTO_ADD_ATTACKER_LEVEL = 50;
	private final Map<String, Integer> attackerAutoNsAddCounts = new HashMap<>();
	private final Map<String, Long> attackerAutoNsAddLastEventAtMs = new HashMap<>();
	private final Map<String, Integer> attackerAutoNsAddLastAttackAnim = new HashMap<>();
	private static final int ATTACK_STYLE_WIDGET_PARENT = 593;
	private static final int[] ATTACK_STYLE_CHILDS = {7, 8, 10};
	private int attackStyleIndex = 0;
	private int lastSwitchLevelForCurrentStyle = -1;
	private int nextStyleSwitchGain = ThreadLocalRandom.current().nextInt(4, 7);
	private long nextAttackStyleCheckMs = 0L;

	public enum ChatEventType {
		CLAN_CHAT, CHAT_CHANNEL, PRIVATE_INFO, OTHER
	}

	public void handleChatMessage(ChatMessage message) {
		if (message == null) return;
		final String senderRaw = message.getName();
		if (senderRaw == null) return;
		final String sender = senderRaw.replace('\u00A0', ' ');
		final ChatEventType type = resolveType(message);
		if (type == ChatEventType.CLAN_CHAT) {
			clanMembers.add(sender.toLowerCase(Locale.ROOT).trim());
		}
		grabChatMessage(message);
	}

	private ChatEventType resolveType(ChatMessage message) {
		try {
			switch (message.getType()) {
				case CLAN_CHAT:
				case CLAN_MESSAGE:
				case CLAN_GUEST_MESSAGE:
					return ChatEventType.CLAN_CHAT;
				case FRIENDSCHAT:
					return ChatEventType.CHAT_CHANNEL;
				case PRIVATECHAT:
				case PRIVATECHATOUT:
				case MODPRIVATECHAT:
					return ChatEventType.PRIVATE_INFO;
				default:
					return ChatEventType.OTHER;
			}
		} catch (Exception e) {
			return ChatEventType.OTHER;
		}
	}

	private void grabChatMessage(ChatMessage message) {
		if (!chatGrabberEnabled.get()) return;
		String senderRaw = message.getName();
		String content = message.getMessage();
		if (senderRaw == null || content == null) return;
		String sender = senderRaw.replace('\u00A0', ' ');
		long now = System.currentTimeMillis();
		long last = lastGrabbedBySender.getOrDefault(sender.toLowerCase(Locale.ROOT), 0L);
		if (now - last < CHAT_GRAB_COOLDOWN_MS) return;
		lastGrabbedBySender.put(sender.toLowerCase(Locale.ROOT), now);
		String prefix = "";
		WorldPoint currentTile = getLocalPlayerLocation();
		if (currentTile != null) {
			if (AT_LOBBY.contains(currentTile)) {
				prefix = "[Lobby] ";
			} else if (IN_FFA.contains(currentTile)) {
				String subAreaName = "";
				for (Map.Entry<String, WorldArea> entry : areaMap.entrySet()) {
					if (entry.getValue().contains(currentTile)) {
						subAreaName = entry.getKey();
						break;
					}
				}
				if (!subAreaName.isEmpty()) {
					prefix = "[FFA - " + subAreaName + "] ";
				} else {
					prefix = "[FFA] ";
				}
			}
		}
		String safe = content.replace("\\", "\\\\").replace("\"", "\\\"");
		DiscordWebhook2.sendMessage("📩 " + prefix + "**" + sender + "**: " + safe);
	}

	private void sendDiscordEmbed(String event) {
		try {
			DiscordWebhook.sendEmbed(getAccountName(), VERSION, this.status,
					this.timer2 != null ? this.timer2.formatTime() : "00:00:00", event);
		} catch (Exception e) {
			log.info("Failed to send Discord embed: {}", e.getMessage());
		}
	}

	private String getAccountName() {
		try {
			if (Microbot.isLoggedIn()) {
				Rs2PlayerModel local = Rs2Player.getLocalPlayer();
				if (local != null) {
					String name = local.getName();
					if (name != null && !name.isEmpty()) {
						lastKnownPlayerName = name;
					}
				}
			}
		} catch (Exception ignored) {
		}
		return lastKnownPlayerName != null ? lastKnownPlayerName : "";
	}

	private void saveListToFile() {
		File file = new File(Config.getNsListFilePath());
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
			for (String name : this.attackSet) {
				bw.write(name);
				bw.newLine();
			}
			log.info("Successfully saved {} names to nslist.txt.", this.attackSet.size());
		} catch (IOException e) {
			log.info("Error saving nslist.txt: {}", e.getMessage());
		}
	}

	private void loadBluePhatSet() {
		this.bluePhatNames.clear();
		Path path = get(Config.getBluePhatsFilePath());
		if (!Files.exists(path)) {
			log.info("bluephats.json not found, starting fresh.");
			return;
		}
		try {
			String json = Files.readString(path, StandardCharsets.UTF_8);
			Gson gson = new Gson();
			Type type = new TypeToken<List<String>>(){}.getType();
			List<String> loaded = gson.fromJson(json, type);
			if (loaded != null) {
				for (String name : loaded) {
					this.bluePhatNames.add(name.toLowerCase().trim());
				}
				log.info("Loaded {} blue phat players.", this.bluePhatNames.size());
			}
		} catch (Exception e) {
			log.info("Error loading bluephats.json: {}", e.getMessage());
		}
	}

	private void saveBluePhatSet() {
		try {
			Path path = get(Config.getBluePhatsFilePath());
			Path parent = path.getParent();
			if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
			List<String> list = new ArrayList<>(this.bluePhatNames);
			String json = new GsonBuilder().setPrettyPrinting().create().toJson(list);
			Files.writeString(path, json);
			log.info("Saved {} blue phat players to bluephats.json.", this.bluePhatNames.size());
		} catch (Exception e) {
			log.info("Error saving bluephats.json: {}", e.getMessage());
		}
	}

	private void addNameToList(String name) {
		String lowerName = name.toLowerCase().trim();
		for (String blocked : BLOCKED_NAMES) {
			if (blocked.equalsIgnoreCase(lowerName)) {
				log.info("Attempted to add blocked name: {}. Ignoring.", name);
				return;
			}
		}
		if (clanMembers.contains(lowerName)) {
			log.info("Refusing to add clan member to nslist: {}", name);
			return;
		}
		if (this.attackSet.contains(lowerName)) {
			log.info("Name already in list: {}", name);
			return;
		}
		this.attackSet.add(lowerName);
		this.targets.add(lowerName);
		saveListToFile();
		log.info("Added {} to nslist.txt", name);
		sendDiscordEmbed("📝 Added **" + name + "** to nslist.txt");
	}

	private void addLureBotToDragList(String name) {
		if (name == null) return;
		String lowerName = name.toLowerCase(Locale.ROOT).trim();
		if (lowerName.isEmpty()) return;
		if (lureDragSet.contains(lowerName)) return;
		lureDragSet.add(lowerName);
		saveLureDragSet();
		log.info("Added {} to luredrag.txt", name);
		sendDiscordEmbed("Added **" + name + "** to luredrag.txt");
	}

	private void saveLureDragSet() {
		try {
			Path path = get(Config.getLureDragFilePath());
			Path parent = path.getParent();
			if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
			Files.write(path, lureDragSet, StandardCharsets.UTF_8);
			log.info("Saved {} names to luredrag.txt.", lureDragSet.size());
		} catch (IOException e) {
			log.info("Error saving luredrag.txt: {}", e.getMessage());
		}
	}

	private void loadLureDragSet() {
		lureDragSet.clear();
		Path path = get(Config.getLureDragFilePath());
		if (!Files.exists(path)) {
			log.info("luredrag.txt not found, starting fresh.");
			return;
		}
		try {
			List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
			for (String line : lines) {
				String trimmed = line.trim().toLowerCase(Locale.ROOT);
				if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
					lureDragSet.add(trimmed);
				}
			}
			log.info("Loaded {} names from luredrag.txt.", lureDragSet.size());
		} catch (IOException e) {
			log.info("Error loading luredrag.txt: {}", e.getMessage());
		}
	}

	private void removeNameFromList(String name) {
		final String lowerName = name.toLowerCase(Locale.ROOT).trim();
		if (lowerName.isEmpty()) return;
		if (this.attackSet.remove(lowerName)) {
			this.targets.remove(lowerName);
			saveListToFile();
			log.info("Removed {} from nslist.txt", name);
			sendDiscordEmbed("🗑️ Removed **" + name + "** from nslist.txt");
		} else {
			log.info("Name not found in list: {}", name);
		}
	}

	private void removeNameFromAiData(String name) {
		boolean removed = aiAssistant.removeProfile(name);
		if (removed) {
			log.info("Removed {} from ai_data.json", name);
		} else {
			log.info("{} not found in ai_data.json", name);
		}
	}

	private void unlistNames(String input) {
		if (input == null || input.trim().isEmpty()) return;
		List<String> namesToRemove = new ArrayList<>();
		for (String name : input.split(",")) {
			String trimmed = name.trim();
			if (!trimmed.isEmpty()) {
				namesToRemove.add(trimmed);
			}
		}
		if (namesToRemove.isEmpty()) {
			log.info("Unlist: no valid names entered");
			return;
		}
		StringBuilder removedNames = new StringBuilder();
		for (String name : namesToRemove) {
			String lowerName = name.toLowerCase(Locale.ROOT).trim();
			boolean removedFromNs = this.attackSet.remove(lowerName);
			this.targets.remove(lowerName);
			boolean removedFromAi = aiAssistant.removeProfile(lowerName);
			if (removedFromNs || removedFromAi) {
				if (removedNames.length() > 0) removedNames.append(", ");
				removedNames.append(name);
			}
		}
		saveListToFile();
		readMainUserFile();
		loadList();
		log.info("Unlist complete. Removed: {}", removedNames);
		sendDiscordEmbed("🗑️ Unlisted: **" + removedNames + "**");
	}

	private void whitelistNames(String input) {
		if (input == null || input.trim().isEmpty()) return;
		List<String> namesToAdd = new ArrayList<>();
		for (String name : input.split(",")) {
			String trimmed = name.trim();
			if (!trimmed.isEmpty()) {
				namesToAdd.add(trimmed);
			}
		}
		if (namesToAdd.isEmpty()) {
			log.info("Whitelist: no valid names entered");
			return;
		}
		StringBuilder addedNames = new StringBuilder();
		for (String name : namesToAdd) {
			String lowerName = name.toLowerCase(Locale.ROOT).trim();
			if (bluePhatNames.add(lowerName)) {
				if (addedNames.length() > 0) addedNames.append(", ");
				addedNames.append(name);
			}
			this.attackSet.remove(lowerName);
			this.targets.remove(lowerName);
			aiAssistant.removeProfile(lowerName);
		}
		saveBluePhatSet();
		saveListToFile();
		readMainUserFile();
		loadList();
		loadBluePhatSet();
		log.info("Whitelist complete. Added: {}", addedNames);
		sendDiscordEmbed("⚪ Whitelisted: **" + addedNames + "**");
	}

	private boolean isTargetWearingBluePartyhat(Player target) {
		if (target == null) return false;
		try {
			Map<net.runelite.api.kit.KitType, Integer> equipped = Rs2Player.getPlayerEquipmentIds(new Rs2PlayerModel(target));
			for (Map.Entry<net.runelite.api.kit.KitType, Integer> e : equipped.entrySet()) {
				net.runelite.api.ItemComposition comp = Microbot.getClientThread().runOnClientThreadOptional(() ->
						Microbot.getClient().getItemDefinition(e.getValue())).orElse(null);
				if (comp != null && comp.getName() != null && comp.getName().equalsIgnoreCase(BLUE_PHAT_NAME)) {
					return true;
				}
			}
		} catch (Exception e) {
			// ignore item definition lookup failures
		}
		return false;
	}

	private void handleBluePhatDetection(Player target) {
		if (target == null) return;
		String rawName = target.getName();
		if (rawName == null || rawName.isEmpty()) return;
		final String lowerName = rawName.toLowerCase(Locale.ROOT).trim();

		if (bluePhatNames.contains(lowerName)) return;

		bluePhatNames.add(lowerName);
		saveBluePhatSet();

		if (this.attackSet.remove(lowerName)) {
			this.targets.remove(lowerName);
			saveListToFile();
		}

		removeNameFromAiData(rawName);

		String safeName = rawName.replace("\\", "\\\\").replace("\"", "\\\"");
		sendDiscordEmbed("Removed from target-list for being fellow BluePhat - **" + safeName + "**");
		log.info("BluePhat detected and handled: {}", rawName);
	}

	static class Timer {
		private long startMs;
		Timer(long s) { this.startMs = s; }
		Timer() { reset(); }
		long elapsed() { return System.currentTimeMillis() - startMs; }
		void reset() { startMs = System.currentTimeMillis(); }
		String formatTime() {
			long e = elapsed();
			long h = e / 3600000L;
			long m = (e % 3600000L) / 60000L;
			long se = (e % 60000L) / 1000L;
			return String.format("%02d:%02d:%02d", h, m, se);
		}
	}

	private void log(String message) { log.info(message); }
	private WorldPoint getLocalPlayerLocation() {
		Rs2PlayerModel local = Rs2Player.getLocalPlayer();
		return local != null ? local.getWorldLocation() : null;
	}
	private long lastInCombatMs = 0L;
	private boolean wasInFfaArea = false;
	private long postCombatPauseUntilMs = 0L;

	@Override
	public boolean run() {
		sessionInitialized.set(false);
		sessionAnnounced = false;
		mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
			try {
				if (!sessionInitialized.get()) {
					startSession();
					sessionInitialized.set(true);
				}
				if (!Microbot.isLoggedIn()) return;
				if (!super.run()) return;
				if (!sessionAnnounced) {
					sessionAnnounced = true;
					sendDiscordEmbed("✅ Script started");
				}
				onLoop();
			} catch (Throwable ex) {
				if (ex.getCause() instanceof InterruptedException) {
					Thread.currentThread().interrupt();
					log.info("Script interrupted, shutting down");
					if (mainScheduledFuture != null) {
						mainScheduledFuture.cancel(true);
					}
					shutdown();
					return;
				}
				log.info("Error in main loop: {}", ex.getMessage());
			}
		}, 0, 600, TimeUnit.MILLISECONDS);
		return true;
	}

	@Override
	public void shutdown() {
		playerStats.save();
		aiAssistant.saveModel();
		sendDiscordEmbed("🛑 Stopped script");
		super.shutdown();
	}

	private void startSession() {
		Config.load();
		applyConfig();
		this.timer2 = new Timer();
		initializeAreaMap();
		this.currentArea = this.areaMap.get(this.selectedLocation);
		removeBlockedNamesFromFile(this.filePath);
		removeDuplicateNamesFromAllLists();
		setNextPreventLogout();
		readMainUserFile();
		loadList();
		loadBluePhatSet();
		loadLureDragSet();
		ensureAutoRetaliateOff();
		playerStats.startSession();
		this.sessionStartMs = System.currentTimeMillis();
		this.nextAntiBanAtMs = System.currentTimeMillis() + Rs2Random.between(120000, 240000);
		this.nextAttackListReloadAtMs = System.currentTimeMillis() + ATTACK_LIST_RELOAD_INTERVAL_MS;
		this.nextUltradianUpdateMs = System.currentTimeMillis() + Rs2Random.between(600000, 1200000);
		lastSwitchLevelForCurrentStyle = Rs2Player.getRealSkillLevel(Skill.ATTACK);
		nextStyleSwitchGain = ThreadLocalRandom.current().nextInt(4, 7);
	}

	private int getRandomThreshold() {
		return ThreadLocalRandom.current().nextInt(5000, 10001);
	}



	private void onLoop() {
		try {
			if (this.reloadNsListRequested) {
				this.reloadNsListRequested = false;
				this.reloadNsListNow();
			}
			Rs2PlayerModel localPlayer = Rs2Player.getLocalPlayer();
			if (localPlayer == null) return;
			WorldPoint localTile = localPlayer.getWorldLocation();
			if (localTile == null) return;
			if (localTile.getPlane() == 1) {
				try {
					this.climbdownStairs();
					this.clickPortal();
				} catch (Throwable t) {
					if (t.getCause() instanceof InterruptedException) {
						Thread.currentThread().interrupt();
						return;
					}
					throw t;
				}
			}
			this.onAction();

			final boolean inSafeZone = this.handleSafeZoneEscape(localPlayer);

			if (this.isDistracted() && !isPlayerInCombat(localPlayer) && this.isAtDesignatedArea(localPlayer)) {
				this.handleDistraction();
			}

			if (this.shouldLookAround() && !isPlayerInCombat(localPlayer) && this.isAtDesignatedArea(localPlayer)) {
				this.lookAround();
			}

			if (!isPlayerInCombat(localPlayer) && this.isAtDesignatedArea(localPlayer)) {
				this.naturalIdle();
			}

			final long now = System.currentTimeMillis();
			if (now >= forcedBreakUntilMs && now - lastForcedBreakMs >= MIN_BREAK_INTERVAL_MS) {
				long sessionElapsed = now - sessionStartMs;
				long forcedBreakMaxDeferMs = 30L * 60L * 1000L;
				if (sessionElapsed >= MAX_SESSION_MS) {
					if (this.AT_LOBBY.contains(localTile)) {
						this.takeForcedBreak();
						Global.sleep(Rs2Random.between(3000, 5000));
						return;
					} else {
						this.forcedBreakPending = true;
						this.status = "Forced break queued (waiting for lobby)";
						if (sessionElapsed >= MAX_SESSION_MS + forcedBreakMaxDeferMs) {
							this.takeForcedBreak();
							Global.sleep(Rs2Random.between(3000, 5000));
							return;
						}
					}
				}
			}
			if (now >= this.nextAttackListReloadAtMs && !isPlayerInCombat(localPlayer) && !isPlayerMoving(localPlayer)) {
				this.readMainUserFile();
				this.loadList();
				this.removeDuplicateNamesFromAllLists();
				this.nextAttackListReloadAtMs = now + ATTACK_LIST_RELOAD_INTERVAL_MS;
			}
			this.trackRepeatAttackerAutoNsList(localPlayer, now);
			this.maybeAutoSwitchLocation();
			if (now >= this.nextActionTime) {
				this.setNextPreventLogout();
			}
			if (now >= this.nextAntiBanAtMs && !isPlayerInCombat(localPlayer) && !this.AT_LOBBY.contains(localTile) && this.isAtDesignatedArea(localPlayer) && !inSafeZone) {
				this.performAntiban();
				long fatiguePenalty = (long) (this.getFatigueFactor(now - sessionStartMs) * 500L);
				this.nextAntiBanAtMs = now + Rs2Random.between((int) ANTIBAN_MIN_INTERVAL_MS, (int) ANTIBAN_MAX_INTERVAL_MS) + fatiguePenalty;
			}
			this.checkHalfHourlyArrows();
			if (!this.AT_LOBBY.contains(localTile)) {
				this.avoidObstacles();
			}
			if (!inSafeZone) {
				if ((this.currentArea == null || !this.currentArea.contains(localTile)) && !this.STUCK.contains(localTile)) {
					this.handleOutOfArea(localPlayer);
				} else if (this.STUCK.contains(localTile)) {
					wasInFfaArea = false;
					this.sleepWithFatigue(1000, 3000);
					if (Rs2Random.between(0, 100) < 30) {
						this.performAntiban();
					}
					this.clickPortal();
				} else if (this.IN_FFA.contains(localTile)) {
					wasInFfaArea = true;
					this.handleInFFA(localPlayer);
				}
			}
			aiAssistant.onTick();
			if (now >= nextAutoRetaliateCheckMs && !autoRetaliateOffVerified) {
				ensureAutoRetaliateOff();
			}
			dismissRandomEvents();
			handleAttackerRetaliation(localPlayer);
			if (now >= nextNonTargetScanMs) {
				scanNonTargets(localPlayer);
				double scanFatigue = this.getFatigueFactor(now - sessionStartMs);
				nextNonTargetScanMs = now + sessionTracker.getRecommendedScanInterval(scanFatigue);
			}
			nonTargetTracker.tickSave();
			if (playerStats.shouldPeriodicSave()) {
				playerStats.save();
			}
			sessionTracker.tickSave();
			int delay = (int) Math.max(100, Rs2Random.between(250, 500) * (1.0 - aiAssistant.getPlayerProfiles().size() * 0.001));
			delay = Math.min(delay, 600);
			this.sleepWithFatigue(delay, delay);
		} catch (Throwable ex) {
			if (ex instanceof InterruptedException || (ex.getCause() instanceof InterruptedException)) {
				Thread.currentThread().interrupt();
				this.log("onLoop interrupted");
				throw ex;
			}
			this.log("Error in onLoop: " + ex.getMessage());
			java.io.StringWriter sw = new java.io.StringWriter();
			java.io.PrintWriter pw = new java.io.PrintWriter(sw);
			ex.printStackTrace(pw);
			pw.flush();
			this.log(sw.toString());
			Global.sleep(Rs2Random.between(1000, 2000));
		}
	}

	private void initializeAreaMap() {
		this.areaMap.put("Portal", new WorldArea(3322, 4761, 14, 6, 0));
		this.areaMap.put("Middle", new WorldArea(3326, 4768, 7, 17, 0));
		this.areaMap.put("Hills single", new WorldArea(3328, 4797, 5, 3, 0));
		this.areaMap.put("Lava single", new WorldArea(3313, 4797, 6, 3, 0));
		this.areaMap.put("Pond single", new WorldArea(3343, 4797, 9, 3, 0));
		this.areaMap.put("Hills", new WorldArea(3325, 4805, 10, 8, 0));
		this.areaMap.put("Pond", new WorldArea(3346, 4800, 7, 16, 0));
		this.areaMap.put("Lava", new WorldArea(3313, 4800, 6, 16, 0));
	}

	private void handleAfkBreak() {
		this.status = "Taking human-like AFK break";
		long now = System.currentTimeMillis();
		long runtime = now - this.fatigueStart;
		double fatigueFactor = this.getFatigueFactor(runtime);
		double fatigueMultiplier = 1.0 + fatigueFactor * 0.04;
		long breakDuration = (long) (Rs2Random.between(30000, 220000) * fatigueMultiplier);
		breakDuration = Math.min(breakDuration, 420000L);
		this.log("Starting AFK break for " + (breakDuration / 1000L) + " seconds (fatigue: " + String.format("%.2f", fatigueFactor) + ")");
		long breakEndTime = now + breakDuration;
		while (System.currentTimeMillis() < breakEndTime) {
			int sleepTime = Rs2Random.between(5000, 15000);
			this.sleepWithFatigue(sleepTime, sleepTime + 2000);
			this.status = "AFK break: " + ((breakEndTime - System.currentTimeMillis()) / 1000L) + "s left";
		}
		double recoveryFactor = 0.5 + (fatigueFactor / 20.0) * 0.3;
		this.fatigueStart += (long) (breakDuration * recoveryFactor);
		this.energyBoost = Math.min(2.0, this.energyBoost + fatigueFactor * 0.025);
		this.fatigueSpikeEndTime = 0L;
		this.fatigueSpikeMagnitude = 0.0;
		this.sessionFatiguePeak *= 0.7;
		long nextBreakInMs = (long) (Rs2Random.between(900000, 3600000) / (1.0 + Math.min(fatigueFactor, 12.0) * 0.05));
		this.nextAfkBreakTime = System.currentTimeMillis() + nextBreakInMs;
		sendDiscordEmbed("AFK break finished. Next break at " + new java.util.Date(this.nextAfkBreakTime));
		this.log("AFK break finished. Next break at " + new java.util.Date(this.nextAfkBreakTime));
		this.status = "AFK break finished";
	}

	private void handleOutOfArea(Rs2PlayerModel localPlayer) {
		WorldPoint tile = localPlayer.getWorldLocation();
		if (tile == null) {
			return;
		}
		if (this.currentArea == null) {
			this.status = "No location selected";
			if (this.AT_LOBBY.contains(tile)) {
				if (this.forcedBreakPending) {
					this.forcedBreakPending = false;
					this.takeForcedBreak();
				} else if (System.currentTimeMillis() >= this.nextAfkBreakTime) {
					this.handleAfkBreak();
				} else {
					this.clickPortal();
				}
			}
		} else if (this.AT_LOBBY.contains(tile)) {
			if (wasInFfaArea && lastInCombatMs > 0) {
				sessionTracker.recordDeath();
				wasInFfaArea = false;
				lastInCombatMs = 0L;
			}
			if (this.forcedBreakPending) {
				this.forcedBreakPending = false;
				this.takeForcedBreak();
			} else if (System.currentTimeMillis() >= this.nextAfkBreakTime) {
				this.handleAfkBreak();
			} else {
				this.clickPortal();
			}
		} else if (this.SAFE_ZONE.contains(tile)) {
			this.handleSafeZoneEscape(localPlayer);
		} else if (this.isFreeRoamEnabled()) {
			if (!this.FREE_ROAM_AREA.contains(tile) && !this.FREE_ROAM_AREA_2.contains(tile)) {
				this.waitBeforeWalkBack();
				this.antiLure();
			} else if (Rs2Random.between(0, 6) == 1 && !isPlayerInCombat(localPlayer)) {
				this.waitBeforeWalkBack();
				this.antiLure();
			}
		} else {
			this.waitBeforeWalkBack();
			this.antiLure();
		}
	}

	private boolean isAtDesignatedArea(Rs2PlayerModel localPlayer) {
		if (this.hillScanActive) return true;
		if (this.currentArea == null) return true;
		if (localPlayer == null) return true;
		WorldPoint tile = localPlayer.getWorldLocation();
		if (tile == null) return true;
		return this.AT_LOBBY.contains(tile)
				|| this.SAFE_ZONE.contains(tile)
				|| this.STUCK.contains(tile)
				|| this.currentArea.contains(tile);
	}

	/**
	 * Detects being lured or dragged into the SAFE_ZONE (a spot where fights cannot happen) and walks
	 * back to {@link #currentArea}. The walk is driven one step per tick through
	 * {@link Rs2Walker#walkStep(WorldPoint, int)} so the script stays responsive and can re-check the
	 * player position between steps. Returns true while the escape is in progress so the caller can skip
	 * the regular combat and area handling for the current tick.
	 */
	private boolean handleSafeZoneEscape(Rs2PlayerModel localPlayer) {
		if (localPlayer == null) {
			return false;
		}
		WorldPoint tile = localPlayer.getWorldLocation();
		if (tile == null || !this.SAFE_ZONE.contains(tile)) {
			if (this.safeZoneEscapeActive) {
				this.safeZoneEscapeActive = false;
				this.resetSafeZoneReturn();
			}
			return false;
		}
		if (lastInCombatMs > 0) {
			sessionTracker.recordTargetRan();
			lastInCombatMs = 0L;
		}
		if (this.currentArea == null) {
			this.status = "Safe zone - no area selected";
			this.sleepWithFatigue(500, 1200);
			return true;
		}
		if (isPlayerInCombat(localPlayer)) {
			this.status = "In combat in safe zone";
			this.sleepWithFatigue(300, 800);
			return true;
		}
		final String areaName = this.getAreaNameFor(this.currentArea);
		if (!this.safeZoneEscapeActive) {
			this.safeZoneEscapeActive = true;
			this.safeZoneReturnAttempts = 0;
			this.safeZoneReturnTarget = this.pickSafeZoneReturnTarget();
			this.status = "Lured into safe zone - walking back to " + areaName;
			this.log(this.status);
			sessionTracker.recordAntiLure();
			sendDiscordEmbed("Dragged into safe zone, walking back to " + areaName);
		}
		if (this.safeZoneReturnTarget == null) {
			this.safeZoneReturnTarget = this.pickSafeZoneReturnTarget();
		}
		final WalkerState state = Rs2Walker.walkStep(this.safeZoneReturnTarget, 1);
		switch (state) {
			case ARRIVED:
				if (this.currentArea.contains(tile)) {
					this.resetSafeZoneReturn();
					this.safeZoneEscapeActive = false;
					this.lastSelectedLocationWalkFinishedAtMs = System.currentTimeMillis();
					this.status = "Back in " + areaName;
					this.log(this.status);
				} else {
					++this.safeZoneReturnAttempts;
					this.safeZoneReturnTarget = this.pickSafeZoneReturnTarget();
				}
				break;
			case UNREACHABLE:
			case EXIT:
				++this.safeZoneReturnAttempts;
				this.safeZoneReturnTarget = this.pickSafeZoneReturnTarget();
				this.log("Safe zone escape could not reach target, retry " + this.safeZoneReturnAttempts);
				break;
			default:
				break;
		}
		if (this.safeZoneReturnAttempts >= MAX_SAFE_ZONE_RETURN_ATTEMPTS) {
			this.safeZoneReturnAttempts = 0;
			this.safeZoneReturnTarget = null;
			final WorldPoint fallback = this.pickSafeZoneReturnTarget();
			if (fallback != null) {
				this.status = "Walking back to " + areaName;
				Rs2Walker.walkTo(fallback, 1);
			}
			Rs2Walker.setTarget(null, "viruz: safe zone escape finished");
		}
		this.sleepWithFatigue(100, 300);
		return true;
	}

	private WorldPoint pickSafeZoneReturnTarget() {
		if (this.currentArea == null) {
			return null;
		}
		for (int attempt = 0; attempt < 10; ++attempt) {
			WorldPoint candidate = randomAreaTile(this.currentArea);
			if (candidate != null && Rs2Walker.isWalkableInCollisionMap(candidate)) {
				return candidate;
			}
		}
		return randomAreaTile(this.currentArea);
	}

	private void resetSafeZoneReturn() {
		this.safeZoneReturnAttempts = 0;
		this.safeZoneReturnTarget = null;
		Rs2Walker.setTarget(null, "viruz: safe zone escape finished");
	}


	private void handleInFFA(Rs2PlayerModel localPlayer) {
		if (!isPlayerInCombat(localPlayer)) {
			if (lastInCombatMs > 0) {
				final long combatEndNow = System.currentTimeMillis();
				if (postCombatPauseUntilMs == 0L) {
					double postCombatFatigue = this.getFatigueFactor(combatEndNow - fatigueStart);
					postCombatPauseUntilMs = combatEndNow + sessionTracker.getRecommendedPostCombatPause(postCombatFatigue);
				}
				if (combatEndNow >= postCombatPauseUntilMs) {
					sessionTracker.recordCombatEnd();
					postCombatPauseUntilMs = 0L;
					if (Rs2Random.between(0, 100) < 25) {
						this.postCombatGlance();
					}
				}
			}
		} else {
			lastInCombatMs = System.currentTimeMillis();
			lastCombatActivityMs = System.currentTimeMillis();
			postCombatPauseUntilMs = 0L;
		}
		if (this.isAttackEnabled()) {
			this.status = "Waiting";
			if (!isPlayerInCombat(localPlayer)) {
				final long now = System.currentTimeMillis();
				final long sinceWalk = now - this.lastSelectedLocationWalkFinishedAtMs;
				final boolean justArrived = sinceWalk >= 0L && sinceWalk <= POST_WALK_ATTACK_WINDOW_MS;
				long fatigueR = System.currentTimeMillis() - fatigueStart;
				double fatigueF = this.getFatigueFactor(fatigueR);
				int scanDelay;
				if (justArrived) {
					scanDelay = Rs2Random.between(POST_WALK_ATTACK_SLEEP_MIN_MS, POST_WALK_ATTACK_SLEEP_MAX_MS);
				} else {
					scanDelay = (int) (Rs2Random.between(50, 250) * (1.0 + fatigueF * 0.05));
				}
				this.sleepWithFatigue(scanDelay, scanDelay + Rs2Random.between(20, 80));
				if (!isPlayerInCombat(localPlayer)) {
					sessionTracker.recordScan();
					this.attackingPlayer();
				}
			}
		} else {

			this.status = "Afking";
		}
		if (this.isPrayerOffEnabled() && !isPlayerInCombat(localPlayer)) {
			this.turnPrayersOff();
		}
		if (!this.isQuickprayersEnabled() && this.isSwitchPrayersEnabled() && isPlayerInCombat(localPlayer)
				&& !this.AT_LOBBY.contains(localPlayer.getWorldLocation())
				&& !this.SAFE_ZONE.contains(localPlayer.getWorldLocation())
				&& !this.STUCK.contains(localPlayer.getWorldLocation())) {
			this.checkPrayers();
		}
		if (this.isProtectBluePhatsEnabled()
				&& !this.AT_LOBBY.contains(localPlayer.getWorldLocation())
				&& !this.SAFE_ZONE.contains(localPlayer.getWorldLocation())
				&& !this.STUCK.contains(localPlayer.getWorldLocation())) {
			Player attacker = findAttackerOfBluePhat(localPlayer);
			if (attacker != null && !isPlayerInCombat(localPlayer)) {
				this.status = "Protecting BluePhat from " + attacker.getName();
				Rectangle clickbox = getActorClickbox(attacker);
				if (clickbox != null) {
					net.runelite.api.Point clickPoint = Rs2UiHelper.getClickingPoint(clickbox, true);
					naturalMouseMovement(clickPoint.getX(), clickPoint.getY());
				}
				this.sleepWithFatigue(50, 150);
				if (rightClickInteract(attacker)) {
					this.log("Attacking " + attacker.getName() + " (attacking a BluePhat)");
					sendDiscordEmbed("Protecting BluePhat - attacking " + attacker.getName());
				}
			}
		}
		if (this.isReportEnabled() && this.currentArea != null && this.currentArea.contains(localPlayer.getWorldLocation())) {
			this.handleReporting();
		}
		this.checkAttackStyleSwitch();
	}

	private void handleReporting() {
		final long now = System.currentTimeMillis();
		if (now < nextReportAttemptAtMs) {
			return;
		}
		nextReportAttemptAtMs = now + Rs2Random.between(
				(int) REPORT_ATTEMPT_MIN_INTERVAL_MS,
				(int) REPORT_ATTEMPT_MAX_INTERVAL_MS
		);
		final Rs2PlayerModel local = Rs2Player.getLocalPlayer();
		if (local == null || local.getWorldLocation() == null) {
			return;
		}
		if (isPlayerInCombat(local) || isPlayerMoving(local)) {
			return;
		}
		if (now >= nextReportListReloadAtMs) {
			this.loadList();
			nextReportListReloadAtMs = now + REPORT_LIST_RELOAD_MIN_INTERVAL_MS;
		}
		if (this.targets.isEmpty()) {
			this.sleepWithFatigue(250, 600);
			return;
		}
		final Rs2PlayerModel target = this.findReportTarget();
		if (target == null || target.getName() == null || target.getWorldLocation() == null) {
			this.sleepWithFatigue(250, 800);
			return;
		}
		WorldPoint localLoc = local.getWorldLocation();
		if (localLoc.distanceTo(target.getWorldLocation()) > MAX_TARGET_DISTANCE) {
			this.status = "Report target too far";
			this.sleepWithFatigue(250, 800);
			return;
		}
		if (Rs2Widget.getWidget(REPORT_WIDGET_PARENT) != null) {
			this.log("Report widget already open; skipping new report attempt.");
			return;
		}
		this.status = "Attempting report";
		this.log("Attempting to report: " + target.getName());
		final boolean menuClicked = rightClickReport(target);
		if (!menuClicked) {
			this.log("Could not open report option for " + target.getName());
			return;
		}
		this.sleepWithFatigue(120, 350);
		final boolean appeared = Global.sleepUntil(
				() -> Rs2Widget.getWidget(REPORT_WIDGET_PARENT) != null,
				3000
		);
		if (!appeared) {
			this.log("Report widget did not appear for " + target.getName());
			return;
		}
		final Widget reasonParent = Rs2Widget.getWidget(REPORT_WIDGET_PARENT, REASON_PARENT_CHILD_INDEX);
		if (reasonParent == null) {
			this.log("Couldn't find report reason container (widget indices may have changed).");
			return;
		}
		final Widget macroReason = reasonParent.getChild(MACRO_REASON_CHILD_INDEX);
		if (macroReason == null) {
			this.log("Couldn't find macro reason option (widget indices may have changed).");
			return;
		}
		this.sleepWithFatigue(90, 220);
		if (!clickWidget(macroReason)) {
			this.log("Failed to select macro reason for " + target.getName());
			return;
		}
		Global.sleepUntil(() -> Rs2Widget.getWidget(REPORT_WIDGET_PARENT) == null, 1500);
		this.status = "Report submitted";
		sessionTracker.recordReport();
		this.log("Submitted report for " + target.getName() + ". Cooldown " + POST_REPORT_COOLDOWN_MS + "ms.");
		sendDiscordEmbed("reported " + target.getName());
		Global.sleep((int) POST_REPORT_COOLDOWN_MS);
	}

	private Rs2PlayerModel closestPlayer(Predicate<Rs2PlayerModel> predicate) {
		final WorldPoint localLoc = getLocalPlayerLocation();
		return getPlayers(predicate).min(Comparator.comparingDouble(p -> {
			WorldPoint loc = p.getWorldLocation();
			if (loc == null || localLoc == null) return Double.MAX_VALUE;
			return loc.distanceTo(localLoc);
		})).orElse(null);
	}

	private boolean isPlayerOnScreen(Actor p) {
		if (p == null || p.getWorldLocation() == null) return false;
		return Microbot.getClientThread().runOnClientThreadOptional(() -> {
			LocalPoint lp = LocalPoint.fromWorld(Microbot.getClient().getTopLevelWorldView(), p.getWorldLocation());
			return lp != null && Rs2Camera.isTileOnScreen(lp);
		}).orElse(Boolean.FALSE);
	}

	private Rs2PlayerModel findReportTarget() {
		final Rs2PlayerModel local = Rs2Player.getLocalPlayer();
		final String localName = local != null && local.getName() != null ? local.getName() : "";
		final WorldPoint localLoc = local != null ? local.getWorldLocation() : null;

		Rs2PlayerModel target = closestPlayer(p ->
				p != null
						&& p.getName() != null
						&& !p.getName().equalsIgnoreCase(localName)
						&& isPlayerOnScreen(p)
						&& localLoc != null
						&& p.getWorldLocation() != null
						&& p.getWorldLocation().distanceTo(localLoc) <= MAX_TARGET_DISTANCE
						&& this.IN_FFA.contains(p.getWorldLocation())
						&& this.targets.contains(p.getName().toLowerCase(Locale.ROOT)));

		if (target != null) {
			this.ffaScanMisses = 0;
		} else {
			++this.ffaScanMisses;
			if (this.ffaScanMisses >= MAX_FAILS) {
				target = closestPlayer(p ->
						p != null
								&& p.getName() != null
								&& !p.getName().equalsIgnoreCase(localName)
								&& isPlayerOnScreen(p)
								&& localLoc != null
								&& p.getWorldLocation() != null
								&& p.getWorldLocation().distanceTo(localLoc) <= MAX_TARGET_DISTANCE
								&& this.targets.contains(p.getName().toLowerCase(Locale.ROOT)));

				if (target != null) {
					this.log("No FFA target after " + MAX_FAILS + " scans. Reporting outside FFA: " + target.getName());
				}
			}
		}
		return target;
	}

	private Rectangle getActorClickbox(Actor actor) {
		if (actor == null) return null;
		return Microbot.getClientThread().runOnClientThreadOptional(() -> Rs2UiHelper.getActorClickbox(actor)).orElse(null);
	}

	private boolean rightClickReport(Player target) {
		Rectangle clickbox = getActorClickbox(target);
		if (clickbox == null) return false;
		net.runelite.api.Point clickPoint = Rs2UiHelper.getClickingPoint(clickbox, true);
		Microbot.getMouse().click(clickPoint, true);
		Global.sleep(60, 160);
		return Microbot.getClientThread().runOnClientThreadOptional(() -> {
			MenuEntry[] entries = Microbot.getClient().getMenu().getMenuEntries();
			for (MenuEntry entry : entries) {
				if (entry.getOption() == null || !entry.getOption().equalsIgnoreCase("Report")) continue;
				NewMenuEntry nme = new NewMenuEntry(entry.getOption(), entry.getTarget(),
						entry.getIdentifier(), entry.getType(), entry.getParam0(), entry.getParam1(), false);
				Microbot.doInvoke(nme, clickbox);
				return true;
			}
			return false;
		}).orElse(false);
	}

	private boolean clickWidget(Widget widget) {
		if (widget == null) return false;
		return Microbot.getClientThread().runOnClientThreadOptional(() -> {
			if (widget.isHidden() || widget.getBounds() == null) return false;
			Rectangle bounds = widget.getBounds();
			if (bounds.width <= 0 || bounds.height <= 0) return false;
			Microbot.getMouse().click(Rs2UiHelper.getClickingPoint(bounds, true));
			return true;
		}).orElse(false);
	}

	private final HumanBehavior humanBehavior = new HumanBehavior();

	private void naturalMouseMovement(int targetX, int targetY) { humanBehavior.naturalMouseMovement(targetX, targetY); }

	private boolean microMissclick() { return humanBehavior.microMissclick(); }

	private void occasionalMisclick(double fatigueFactor) { humanBehavior.occasionalMisclick(200, 150, fatigueFactor); }

	private Point applyNaturalClickVariation(Point target) { return humanBehavior.applyNaturalClickVariation(target); }

	private long getNaturalAttackDelay() { return humanBehavior.getNaturalAttackDelay(); }

	private void resetSession() { humanBehavior.resetSession(); }

	private int getHumanReactionTime(HumanBehavior.ReactionContext context) { return humanBehavior.getHumanReactionTime(context); }

	private void onAction() { humanBehavior.onAction(); }

	private boolean simulateHumanError() { return humanBehavior.simulateHumanError(0.02); }

	private int getCurrentFatigueLevel() { return humanBehavior.getCurrentFatigueLevel(); }

	private HumanBehavior.AntiBanSuggestion getContextualAntiBanAction(double fatigueFactor) { return humanBehavior.getContextualAntiBanAction(fatigueFactor); }

	private boolean isDistracted() { return humanBehavior.isDistracted(); }

	private void handleDistraction() { humanBehavior.handleDistraction(); }

	private boolean shouldLookAround() { return humanBehavior.shouldLookAround(); }

	private void lookAround() { humanBehavior.lookAround(); }

	private void naturalIdle() { humanBehavior.naturalIdle(); }

	private boolean shouldHesitate() { return humanBehavior.shouldHesitate(); }

	private int addHesitationDelay() { return humanBehavior.addHesitationDelay(); }

	private boolean isAttacking() { return this.isAttacking; }

	private void setAttacking(boolean attacking) { this.isAttacking = attacking; }

	private boolean isPlayerInCombat(Rs2PlayerModel playerModel) {
		if (playerModel == null) return false;
		return playerModel.getInteracting() != null;
	}

	private boolean isPlayerMoving(Rs2PlayerModel playerModel) {
		return Rs2Player.isMoving(playerModel);
	}

	private boolean playerExists(Actor actor) {
		if (actor == null) return false;
		if (actor instanceof Rs2PlayerModel) {
			return actor.getWorldLocation() != null;
		}
		return Microbot.getClientThread().runOnClientThreadOptional(() -> actor.getWorldLocation() != null).orElse(false);
	}

	private double distanceBetween(Rs2PlayerModel a, Rs2PlayerModel b) {
		if (a == null || b == null) return Double.MAX_VALUE;
		WorldPoint la = a.getWorldLocation();
		WorldPoint lb = b.getWorldLocation();
		if (la == null || lb == null) return Double.MAX_VALUE;
		return Math.hypot(la.getX() - lb.getX(), la.getY() - lb.getY());
	}

	private Actor getInteractingWithMe() {
		Rs2PlayerModel local = Rs2Player.getLocalPlayer();
		if (local == null) return null;
		return local.getInteracting();
	}

	private boolean notInteractingWith(String name) {
		if (name == null) return true;
		Actor interacting = getInteractingWithMe();
		return interacting == null || interacting.getName() == null
				|| !interacting.getName().toLowerCase(Locale.ROOT).trim().equals(name);
	}

	private int getAnimationOf(Actor actor) {
		if (actor == null) return -1;
		return Microbot.getClientThread().runOnClientThreadOptional(actor::getAnimation).orElse(-1);
	}

	private Point getClickablePoint(Actor actor) {
		Rectangle clickbox = getActorClickbox(actor);
		if (clickbox == null) return null;
		net.runelite.api.Point rp = Rs2UiHelper.getClickingPoint(clickbox, true);
		return new Point(rp.getX(), rp.getY());
	}

	private int getCameraYawDegrees() {
		return normalizeYaw(Rs2Camera.getAngle());
	}

	private void rotateCameraTo(int yawDegrees, int pitch) {
		int yawUnits = (int) Math.round(normalizeYaw(yawDegrees) * (2048 / 360.0)) % 2048;
		int clampedPitch = Math.max(128, Math.min(383, pitch));
		Rs2Camera.setYaw(yawUnits);
		Rs2Camera.setPitch(clampedPitch);
	}

	private int getYawToFace(WorldPoint target) {
		WorldPoint current = getLocalPlayerLocation();
		if (current == null || target == null) return getCameraYawDegrees();
		double dx = target.getX() - current.getX();
		double dy = target.getY() - current.getY();
		double angleToTarget = Math.toDegrees(Math.atan2(dy, dx));
		return normalizeYaw(90 - (int) Math.round(angleToTarget));
	}

	private int getPitchForTile(Actor target) {
		WorldPoint local = getLocalPlayerLocation();
		WorldPoint tile = target == null ? null : target.getWorldLocation();
		if (local == null || tile == null) return 383;
		int dist = local.distanceTo(tile);
		if (dist <= 2) return 383;
		if (dist <= 6) return 255;
		return 128;
	}

	private WorldPoint randomAreaTile(WorldArea area) {
		if (area == null) return null;
		int x = area.getX() + ThreadLocalRandom.current().nextInt(area.getWidth());
		int y = area.getY() + ThreadLocalRandom.current().nextInt(area.getHeight());
		return new WorldPoint(x, y, area.getPlane());
	}

	private String getAreaNameFor(WorldArea area) {
		for (Map.Entry<String, WorldArea> entry : this.areaMap.entrySet()) {
			if (entry.getValue().equals(area)) {
				return entry.getKey();
			}
		}
		return "";
	}

	private void attackingPlayer() {
		if (this.isAttacking()) {
			return;
		}
		final long now = System.currentTimeMillis();
		if (now < this.noValidTargetCooldownUntilMs) {
			this.status = "Waiting for targets to finish combat";
			return;
		}
		this.setAttacking(true);
		try {
			Rs2PlayerModel localPlayer = Rs2Player.getLocalPlayer();
			if (!playerExists(localPlayer)) {
				this.status = "Local player not ready";
				return;
			}
			if (isPlayerInCombat(localPlayer)) {
				this.status = "Already engaged";
				return;
			}
			if (this.attackSet.isEmpty()) {
				this.status = "Attack list empty";
				return;
			}
			String localName = localPlayer.getName() == null ? "" : localPlayer.getName().toLowerCase(Locale.ROOT).trim();
			WorldPoint localTile = localPlayer.getWorldLocation();
			if (localTile == null) {
				this.status = "Local tile not ready";
				return;
			}
			boolean localInMulti = this.AT_MULTI.contains(localTile);
			boolean localInSafeZone = this.SAFE_ZONE.contains(localTile);
			targetCooldownUntilMs.entrySet().removeIf(e -> now >= e.getValue());

			List<Rs2PlayerModel> candidateModels = getPlayers(p ->
					this.isTargetValid(p, localPlayer, localName, localTile, localInSafeZone)
							&& !isTargetOnCooldown(p.getName()))
					.collect(Collectors.toList());
			List<Player> candidates = new ArrayList<>(candidateModels);

			long fatigueRuntime = System.currentTimeMillis() - fatigueStart;
			double fatigueFactor = this.getFatigueFactor(fatigueRuntime);

			Rs2PlayerModel target = null;
			if (focusPlayersEnabled && !focusPlayerNames.isEmpty()) {
				target = candidateModels.stream()
						.filter(p -> p.getName() != null && focusPlayerNames.contains(p.getName().toLowerCase(Locale.ROOT)))
						.findFirst().orElse(null);
				if (target != null) {
					this.status = "Focusing " + target.getName();
				}
			}
			if (target == null) {
				Player best = aiAssistant.selectBestTarget(candidates, localPlayer, attackSet, localInMulti, fatigueFactor);
				if (best != null) {
					target = new Rs2PlayerModel(best);
				}
			}

			if (target == null && !candidateModels.isEmpty()) {
				target = candidateModels.get(ThreadLocalRandom.current().nextInt(candidateModels.size()));
			}

			if (target == null) {
				this.status = "No valid targets found";
				long fatigueR = now - fatigueStart;
				double fatigueF = this.getFatigueFactor(fatigueR);
				long cooldown = (long) (Rs2Random.between(5000, 15000) * (1.0 + fatigueF * 0.08));
				this.noValidTargetCooldownUntilMs = now + cooldown;
				return;
			}

			this.noValidTargetCooldownUntilMs = 0L;

			if (!playerExists(target)) {
				this.status = "Target disappeared";
				return;
			}

			if (isTargetWearingBluePartyhat(target)) {
				handleBluePhatDetection(target);
				this.status = "BluePhat detected, finding new target";
				return;
			}
			{
				String tn = (target.getName() == null) ? "" : target.getName().toLowerCase(Locale.ROOT).trim();
				if (!tn.isEmpty() && clanMembers.contains(tn)) {
					this.status = "Target is a clan member, skipping";
					return;
				}
			}
			final String targetName = (target.getName() == null) ? "" : target.getName().toLowerCase(Locale.ROOT).trim();
			if (this.isPortalDragEligible(target)) {
				this.dragPortalTarget(target, localPlayer);
				return;
			}
			if (!localInSafeZone && isLureBot(target.getName()) && !noDragEnabled) {
				lureDragToSafe(target, localPlayer);
				return;
			}
			if (!localInSafeZone && lureMultiEnabled && !isLureBot(target.getName())) {
				lureDragToHills(target, localPlayer);
				return;
			}
			long adaptiveCooldown = (long) (MIN_ATTACK_ATTEMPT_INTERVAL_MS * (getAdaptiveThreshold() / 5000.0));
			adaptiveCooldown = Math.max(400, Math.min(adaptiveCooldown, 5000));
			if (now - lastAttackAttemptAtMs < adaptiveCooldown) {
				this.status = "Attack cooldown";
				return;
			}
			if (!targetName.isEmpty()
					&& targetName.equals(lastAttackTargetName)
					&& now - lastSameTargetAttemptAtMs < SAME_TARGET_COOLDOWN_MS) {
				this.status = "Same target cooldown";
				return;
			}
			if (this.walkToTargetEnabled) {
				boolean reached = this.approachTargetIfNeeded(localPlayer, target);
				if (!reached) {
					this.log("Failed to reach target");
					this.status = "Failed to reach target";
					return;
				}
				this.status = "Approached target";
			}
			lastAttackAttemptAtMs = now;
			lastCombatActivityMs = now;
			lastAttackTargetName = targetName.isEmpty() ? null : targetName;
			lastSameTargetAttemptAtMs = now;

			long fatigueRuntime2 = now - fatigueStart;
			double fatigueFactor2 = this.getFatigueFactor(fatigueRuntime2);
			long hesitationMs = aiAssistant.calculateHumanReactionDelay(fatigueFactor2, true);
			if (hesitationMs > 30) {
				this.sleepWithFatigue((int) Math.min(hesitationMs, 150), (int) Math.min(hesitationMs + 60, 250));
			}

			if (aiAssistant.shouldMakeHumanMistake(fatigueFactor2, lastMistakeMs, MISTAKE_COOLDOWN_MS)) {
				this.lastMistakeMs = now;
				this.status = "Hesitating...";
				this.sleepWithFatigue(80, 250);
			}

			if (this.shouldHesitate()) {
				this.status = "Hesitating...";
				int hesitation = this.addHesitationDelay();
				this.sleepWithFatigue(Math.min(hesitation / 2, 400), hesitation);
			}

			if (ThreadLocalRandom.current().nextDouble() < 0.01 + fatigueFactor2 * 0.001) {
				this.status = "Second glance";
				Point tp = getClickablePoint(target);
				if (tp != null) {
					naturalMouseMovement(tp.x, tp.y);
					this.sleepWithFatigue(100, 250);
				}
			}

			occasionalMisclick(fatigueFactor2 * 0.5);

			long attackReaction = getNaturalAttackDelay();
			this.sleepWithFatigue((int) attackReaction, (int) (attackReaction + 80));

			if (ThreadLocalRandom.current().nextDouble() < 0.04 + fatigueFactor2 * 0.0015) {
				Point tp = getClickablePoint(target);
				if (tp != null) {
					int overshootX = tp.x + Rs2Random.between(-25, 25);
					int overshootY = tp.y + Rs2Random.between(-15, 15);
					naturalMouseMovement(
							Math.max(0, Math.min(765, overshootX)),
							Math.max(0, Math.min(503, overshootY))
					);
					this.sleepWithFatigue(80, 250);
				}
			}

			boolean interacted = false;
			if (playerExists(target) && distanceBetween(localPlayer, target) <= MAX_TARGET_DISTANCE) {
				boolean onScreen = isPlayerOnScreen(target);
				boolean nearby = distanceBetween(localPlayer, target) <= 5.0;
				if (!onScreen && !nearby) {
					onScreen = this.rotateCameraToFace(target);
				}
				if (onScreen || nearby) {
					interacted = rightClickInteract(target);
				}
			}
			boolean attacked = interacted && !targetName.isEmpty()
					&& Global.sleepUntil(() -> !notInteractingWith(targetName), 1500);
			if (attacked) {
				this.status = "Attacking " + target.getName();
				this.log(this.status);
				sendDiscordEmbed("Attacking " + target.getName());
				recordTargetCooldown(target.getName());
				sessionTracker.recordAttack();
				playerStats.recordAttack(target.getName());
				int timeout = Rs2Random.between(1200, 3000);
				for (int elapsed = 0; !isPlayerInCombat(localPlayer) && elapsed < timeout; elapsed += Rs2Random.between(50, 150)) {
					this.sleepWithFatigue(20, 50);
				}
				Actor interacting = getInteractingWithMe();
				boolean foughtBack = interacting instanceof Player
						&& interacting.getName() != null
						&& targetName.equals(interacting.getName().toLowerCase(Locale.ROOT).trim());
				if (foughtBack) sessionTracker.recordTargetFoughtBack();
				String[] gear = aiAssistant.generateWeaponList(target);
				aiAssistant.learnFromEncounter(target.getName(), target.getCombatLevel(), gear, 99, foughtBack, false, true);
				if (!playerExists(target)) sessionTracker.recordSuccessfulHit();
			} else if (interacted) {
				this.status = "Attack did not land on " + target.getName();
			} else {
				this.status = "Couldn't attack target";
			}
		} finally {
			this.setAttacking(false);
		}
	}

	private boolean rotateCameraToFace(Actor target) {
		if (!playerExists(target)) return false;
		if (isPlayerOnScreen(target)) return true;
		WorldPoint targetTile = target.getWorldLocation();
		if (targetTile == null) return false;
		int desiredYaw = getYawToFace(targetTile);
		int desiredPitch = Math.max(128, Math.min(383, this.getPitchForTile(target)));
		this.status = "Rotating camera to " + target.getName();
		int steps = Rs2Random.between(2, 3);
		for (int i = 1; i <= steps; i++) {
			double progress = i / (double) steps;
			int yawDiff = this.shortestYawDiff(getCameraYawDegrees(), desiredYaw);
			int pitchDiff = desiredPitch - Rs2Camera.getPitch();
			int nextYaw = this.normalizeYaw(getCameraYawDegrees() + (int) (yawDiff * progress) + Rs2Random.between(-2, 2));
			int nextPitch = Math.max(128, Math.min(383, Rs2Camera.getPitch() + (int) (pitchDiff * progress) + Rs2Random.between(-2, 2)));
			rotateCameraTo(nextYaw, nextPitch);
			this.sleepWithFatigue(40, 120);
		}
		rotateCameraTo(desiredYaw, desiredPitch);
		this.sleepWithFatigue(30, 100);
		if (isPlayerOnScreen(target)) return true;
		int[] corrections = {10, -10, 20, -20};
		for (int correction : corrections) {
			rotateCameraTo(this.normalizeYaw(desiredYaw + correction), desiredPitch);
			this.sleepWithFatigue(25, 70);
			if (isPlayerOnScreen(target)) return true;
		}
		return false;
	}

	private int shortestYawDiff(int from, int to) {
		int diff = ((to - from) % 360 + 360) % 360;
		if (diff > 180) diff -= 360;
		return diff;
	}

	private int normalizeYaw(int yaw) {
		return ((yaw % 360) + 360) % 360;
	}

	private void trackRepeatAttackerAutoNsList(Rs2PlayerModel localPlayer, long now) {
		if (!isPlayerInCombat(localPlayer)) {
			return;
		}
		Actor interacting = getInteractingWithMe();
		if (!(interacting instanceof Player)) {
			return;
		}
		Rs2PlayerModel attacker = new Rs2PlayerModel((Player) interacting);
		if (!playerExists(attacker)) {
			return;
		}
		if (attacker.getCombatLevel() < MIN_AUTO_ADD_ATTACKER_LEVEL) {
			return;
		}
		String attackerName = attacker.getName();
		if (attackerName == null) {
			return;
		}
		final String attackerKey = attackerName.toLowerCase(Locale.ROOT).trim();
		if (attackerKey.isEmpty()) {
			return;
		}
		String localName = localPlayer.getName() == null ? "" : localPlayer.getName().toLowerCase(Locale.ROOT).trim();
		if (!localName.isEmpty() && attackerKey.equals(localName)) {
			return;
		}
		int animation = getAnimationOf(attacker);
		boolean isAttackAnimation =
				this.isMeleeAttack(animation) || this.isRangeAttack(animation) || this.isMagicAttack(animation);
		if (isAttackAnimation) {
			aiAssistant.learnAnimationPattern(attackerName, animation, now);
		}
		boolean theyRan = !isPlayerInCombat(localPlayer);
		aiAssistant.recordAttackedBy(attackerName, theyRan);
		if (!isAttackAnimation) {
			return;
		}
		Integer lastAnim = this.attackerAutoNsAddLastAttackAnim.get(attackerKey);
		long lastEventAt = this.attackerAutoNsAddLastEventAtMs.getOrDefault(attackerKey, 0L);
		boolean animChanged = lastAnim == null || lastAnim != animation;
		boolean enoughTimePassed = now - lastEventAt >= AUTO_NS_ADD_MIN_EVENT_INTERVAL_MS;
		if (!animChanged && !enoughTimePassed) {
			return;
		}
		int newCount = this.attackerAutoNsAddCounts.getOrDefault(attackerKey, 0) + 1;
		this.attackerAutoNsAddCounts.put(attackerKey, newCount);
		this.attackerAutoNsAddLastEventAtMs.put(attackerKey, now);
		this.attackerAutoNsAddLastAttackAnim.put(attackerKey, animation);
		if (newCount > AUTO_NS_ADD_ATTACKS_THRESHOLD && !this.attackSet.contains(attackerKey)) {
			if (isTargetWearingBluePartyhat(attacker)) {
				handleBluePhatDetection(attacker);
				this.attackerAutoNsAddCounts.remove(attackerKey);
				this.attackerAutoNsAddLastEventAtMs.remove(attackerKey);
				this.attackerAutoNsAddLastAttackAnim.remove(attackerKey);
				return;
			}
			if (clanMembers.contains(attackerKey)) {
				this.attackerAutoNsAddCounts.remove(attackerKey);
				this.attackerAutoNsAddLastEventAtMs.remove(attackerKey);
				this.attackerAutoNsAddLastAttackAnim.remove(attackerKey);
				return;
			}
			int combatLevel = attacker.getCombatLevel();
			if (combatLevel < MIN_AUTO_ADD_ATTACKER_LEVEL) {
				return;
			}
			this.log("Auto-adding attacker to nslist.txt after " + newCount + " attacks: " + attackerName);
			sendDiscordEmbed("Auto-adding attacker to nslist.txt after " + newCount + " attacks: " + attackerName);
			this.addNameToList(attackerName);
			this.attackerAutoNsAddCounts.remove(attackerKey);
			this.attackerAutoNsAddLastEventAtMs.remove(attackerKey);
			this.attackerAutoNsAddLastAttackAnim.remove(attackerKey);
		}
	}

	private boolean rightClickInteract(Actor target) {
		if (target == null) return false;
		if (this.microMissclick()) {
			this.sleepWithFatigue(100, 400);
			return false;
		}
		long fatigueR = System.currentTimeMillis() - fatigueStart;
		double fatigueF = this.getFatigueFactor(fatigueR);
			occasionalMisclick(fatigueF * 0.5);
		Point clickPoint = getClickablePoint(target);
		if (clickPoint == null) return false;
		clickPoint = applyNaturalClickVariation(clickPoint);

		if (ThreadLocalRandom.current().nextDouble() < 0.025 + fatigueF * 0.0015) {
			int overshootX = clickPoint.x + Rs2Random.between(-20, 20);
			int overshootY = clickPoint.y + Rs2Random.between(-12, 12);
			naturalMouseMovement(
					Math.max(0, Math.min(765, overshootX)),
					Math.max(0, Math.min(503, overshootY))
			);
			this.sleepWithFatigue(60, 180);
		}

		naturalMouseMovement(clickPoint.x, clickPoint.y);
		this.sleepWithFatigue(30, 120);

		if (ThreadLocalRandom.current().nextDouble() < 0.02 + fatigueF * 0.001) {
			Global.sleep(Rs2Random.between(50, 200));
		}

		Rs2PlayerModel rpm = (target instanceof Rs2PlayerModel) ? (Rs2PlayerModel) target : new Rs2PlayerModel((Player) target);
		return Rs2Player.attack(rpm);
	}

	private Rs2PlayerModel findAttackerOfBluePhat(Rs2PlayerModel localPlayer) {
		if (!playerExists(localPlayer) || this.bluePhatNames.isEmpty()) return null;
		String localName = localPlayer.getName() != null ? localPlayer.getName().toLowerCase(Locale.ROOT).trim() : "";
		WorldPoint localLoc = localPlayer.getWorldLocation();
		if (localLoc == null) return null;
		return getPlayers(p -> p != null && p.getName() != null
				&& !p.getName().toLowerCase(Locale.ROOT).trim().equals(localName)
				&& p.getWorldLocation() != null
				&& distanceBetween(localPlayer, p) <= MAX_TARGET_DISTANCE
				&& this.IN_FFA.contains(p.getWorldLocation())
				&& isAttackerOfBluePhat(p))
				.min(Comparator.comparingDouble(p -> {
					WorldPoint loc = p.getWorldLocation();
					return loc == null ? Double.MAX_VALUE : localLoc.distanceTo(loc);
				}))
				.orElse(null);
	}

	private boolean isAttackerOfBluePhat(Rs2PlayerModel p) {
		Actor interacting = p.getInteracting();
		if (!(interacting instanceof Player)) return false;
		String targetName = interacting.getName();
		return targetName != null
				&& this.bluePhatNames.contains(targetName.toLowerCase(Locale.ROOT).trim());
	}

	private boolean isLureBot(String name) {
		if (name == null) return false;
		String key = name.toLowerCase(Locale.ROOT).trim();
		if (lureDragSet.contains(key)) return true;
		return aiAssistant.isLikelyLureBot(key);
	}

	private boolean isStaffOfAirOnly(Rs2PlayerModel target) {
		if (!playerExists(target)) return false;
		try {
			Map<KitType, String> equipment = Rs2Player.getPlayerEquipmentNames(target);
			if (equipment == null) return false;
			int equippedCount = 0;
			boolean hasStaffOfAir = false;
			for (String name : equipment.values()) {
				if (name != null && !name.isEmpty()) {
					equippedCount++;
					if (name.equalsIgnoreCase(STAFF_OF_AIR)) {
						hasStaffOfAir = true;
					}
				}
			}
			return equippedCount == 1 && hasStaffOfAir;
		} catch (Exception e) {
			return false;
		}
	}

	private void lureDragToSafe(Rs2PlayerModel target, Rs2PlayerModel localPlayer) {
		if (localPlayer == null || !playerExists(target)) return;
		String targetName = target.getName();
		if (targetName == null) return;

		addLureBotToDragList(targetName);
		ensureAutoRetaliateOff();

		this.status = "Luring " + targetName + " to safe zone";
		this.log("Luring lure bot " + targetName + " to safe zone");
		sendDiscordEmbed("Luring lure bot to safe zone: " + targetName);

		if (!isPlayerInCombat(localPlayer) || notInteractingWith(targetName.toLowerCase(Locale.ROOT).trim())) {
			boolean attacked = rightClickInteract(target);
			if (!attacked) return;
			Global.sleepUntil(() -> isPlayerInCombat(Rs2Player.getLocalPlayer()), 3000);
		}

		WorldPoint safeCenter = new WorldPoint(3328, 4753, 0);
		for (int attempts = 0; attempts < 20 && playerExists(target) && playerExists(localPlayer); attempts++) {
			WorldPoint lt = localPlayer.getWorldLocation();
			if (lt != null && SAFE_ZONE.contains(lt)) return;
			Rs2Walker.walkTo(safeCenter, 1);
			Global.sleep(Rs2Random.between(400, 800));
		}
	}

	private void lureDragToHills(Rs2PlayerModel target, Rs2PlayerModel localPlayer) {
		if (localPlayer == null || !playerExists(target)) return;
		String targetName = target.getName();
		if (targetName == null) return;

		this.status = "Luring " + targetName + " to Hills";
		this.log("Luring " + targetName + " to Hills");
		sendDiscordEmbed("Luring to Hills: " + targetName);

		if (!isPlayerInCombat(localPlayer) || notInteractingWith(targetName.toLowerCase(Locale.ROOT).trim())) {
			boolean attacked = rightClickInteract(target);
			if (!attacked) return;
			Global.sleepUntil(() -> isPlayerInCombat(Rs2Player.getLocalPlayer()), 3000);
		}

		WorldArea hillsArea = this.areaMap.get("Hills");
		if (hillsArea == null) return;
		WorldPoint hillsTile = randomAreaTile(hillsArea);
		for (int attempts = 0; attempts < 20 && playerExists(target) && playerExists(localPlayer); attempts++) {
			WorldPoint localTile = localPlayer.getWorldLocation();
			if (localTile != null && hillsArea.contains(localTile)) return;
			if (localTile != null && SAFE_ZONE.contains(localTile)) return;
			Rs2Walker.walkTo(hillsTile, 1);
			Global.sleep(Rs2Random.between(400, 800));
		}
	}

	private boolean isPortalDragEligible(Rs2PlayerModel target) {
		if (!this.dragModeEnabled || target == null) return false;
		WorldArea portal = this.areaMap.get("Portal");
		if (this.currentArea == null || !this.currentArea.equals(portal)) return false;
		WorldPoint targetTile = target.getWorldLocation();
		return targetTile != null && portal.contains(targetTile);
	}

	/**
	 * Drag-Mode: attacks a Portal target, waits until it retaliates, then walks it to the east or
	 * west drag tile. At the drag tile it waits for the target to catch up; if the target stopped
	 * following it re-attacks and keeps dragging. Once the target is following again the script runs
	 * through the safe zone (y = 4758) and back into Portal.
	 */
	private void dragPortalTarget(Rs2PlayerModel target, Rs2PlayerModel localPlayer) {
		if (localPlayer == null || !playerExists(target)) return;
		WorldPoint localTile = localPlayer.getWorldLocation();
		WorldArea portal = this.areaMap.get("Portal");
		if (localTile == null || portal == null || !portal.contains(localTile)) return;

		final String targetName = target.getName();
		if (targetName == null) return;
		final String targetKey = targetName.toLowerCase(Locale.ROOT).trim();
		if (targetKey.isEmpty() || !this.attackSet.contains(targetKey)) return;

		final boolean dragEast = ThreadLocalRandom.current().nextBoolean();
		final WorldPoint dragPoint = dragEast ? DRAG_EAST : DRAG_WEST;
		final String direction = dragEast ? "east" : "west";

		this.status = "Drag-Mode: engaging " + targetName;
		this.log("Drag-Mode: dragging " + targetName + " " + direction);
		sendDiscordEmbed("Drag-Mode: dragging " + targetName + " " + direction);

		if (this.attackAndWaitForRetaliation(target, targetKey, localPlayer)) {
			this.status = "Drag-Mode: " + targetName + " did not retaliate";
			Rs2Walker.setTarget(null, "viruz: drag-mode disengage");
			return;
		}
		this.sessionTracker.recordAttack();
		this.playerStats.recordAttack(targetName);
		this.status = "Drag-Mode: dragging " + targetName + " " + direction;

		boolean reached = this.dragTargetToPoint(target, targetKey, localPlayer, dragPoint);
		if (!reached) {
			this.status = "Drag-Mode: drag interrupted";
			Rs2Walker.setTarget(null, "viruz: drag-mode interrupted");
			return;
		}

		boolean close = this.waitForTargetClose(target, localPlayer, DRAG_CLOSE_TIMEOUT_MS);
		int reengages = 0;
		while (!close && reengages++ < DRAG_MAX_REENGAGE_ATTEMPTS && playerExists(target)) {
			this.status = "Drag-Mode: target not following, re-attacking " + targetName;
			if (this.attackAndWaitForRetaliation(target, targetKey, localPlayer)) {
				break;
			}
			this.dragTargetToPoint(target, targetKey, localPlayer, dragPoint);
			close = this.waitForTargetClose(target, localPlayer, DRAG_CLOSE_TIMEOUT_MS);
		}

		if (!close) {
			this.status = "Drag-Mode: " + targetName + " stopped following";
			this.log(this.status);
			Rs2Walker.setTarget(null, "viruz: drag-mode target lost");
			return;
		}

		this.runSafeZoneAndReturnToPortal(localPlayer);
	}

	private boolean dragTargetToPoint(Rs2PlayerModel target,
									  String targetKey,
									  Rs2PlayerModel localPlayer,
									  WorldPoint dragPoint) {
		int reengageFailures = 0;
		long lastReengageAtMs = 0L;
		for (int step = 0; step < DRAG_MAX_STEPS; step++) {
			if (!playerExists(target) || !playerExists(localPlayer)) return false;
			WorldPoint localTile = localPlayer.getWorldLocation();
			WorldPoint targetTile = target.getWorldLocation();
			if (localTile == null) return false;
			if (localTile.distanceTo(dragPoint) <= DRAG_ARRIVE_DISTANCE) return true;
			if (targetTile != null && targetTile.distanceTo(dragPoint) <= DRAG_ARRIVE_DISTANCE) return true;

			if (!this.isTargetFollowing(target, localPlayer)) {
				long now = System.currentTimeMillis();
				boolean targetInRange = targetTile != null
						&& localTile.distanceTo(targetTile) <= MAX_TARGET_DISTANCE;
				if (targetInRange && now - lastReengageAtMs >= 2000L) {
					lastReengageAtMs = now;
					this.status = "Drag-Mode: re-attacking " + target.getName();
					if (this.attackAndWaitForRetaliation(target, targetKey, localPlayer)
							&& ++reengageFailures >= DRAG_MAX_REENGAGE_ATTEMPTS) {
						return false;
					}
					if (!playerExists(target)) return false;
				}
			}

			WalkerState state = Rs2Walker.walkStep(dragPoint, DRAG_ARRIVE_DISTANCE);
			if (state == WalkerState.ARRIVED) return true;
			if (state == WalkerState.UNREACHABLE || state == WalkerState.EXIT) return false;
			this.sleepWithFatigue(DRAG_STEP_SLEEP_MIN_MS, DRAG_STEP_SLEEP_MAX_MS);
		}
		return false;
	}

	private boolean attackAndWaitForRetaliation(Rs2PlayerModel target,
												String targetKey,
												Rs2PlayerModel localPlayer) {
		if (target == null || localPlayer == null || targetKey == null || targetKey.isEmpty()) return true;
		if (!playerExists(target)) return true;
		if (this.isTargetRetaliating(target, localPlayer)) return false;

		if (this.notInteractingWith(targetKey)) {
			if (!this.isPlayerOnScreen(target) && !this.rotateCameraToFace(target)) {
				return true;
			}
			if (!this.rightClickInteract(target)) {
				return true;
			}
			Global.sleepUntil(() -> !this.notInteractingWith(targetKey), 1500);
		}

		boolean retaliated = Global.sleepUntil(
				() -> this.isTargetRetaliating(target, localPlayer),
				(int) DRAG_RETALIATE_TIMEOUT_MS);
		if (retaliated) {
			this.lastAttackAttemptAtMs = System.currentTimeMillis();
			this.lastSameTargetAttemptAtMs = this.lastAttackAttemptAtMs;
			this.lastCombatActivityMs = this.lastAttackAttemptAtMs;
			this.lastAttackTargetName = targetKey;
		}
		return !retaliated;
	}

	private boolean isTargetRetaliating(Rs2PlayerModel target, Rs2PlayerModel localPlayer) {
		if (localPlayer == null || !playerExists(target)) return false;
		Actor interacting = target.getInteracting();
		if (!(interacting instanceof Player)) return false;
		String localName = normalizeActorName(localPlayer.getName());
		String interactingName = normalizeActorName(interacting.getName());
		return !localName.isEmpty() && localName.equals(interactingName);
	}

	private boolean isTargetFollowing(Rs2PlayerModel target, Rs2PlayerModel localPlayer) {
		if (localPlayer == null || !playerExists(target)) return false;
		if (this.isTargetRetaliating(target, localPlayer)) return true;
		WorldPoint localTile = localPlayer.getWorldLocation();
		WorldPoint targetTile = target.getWorldLocation();
		if (localTile == null || targetTile == null) return false;
		return localTile.distanceTo(targetTile) <= DRAG_FOLLOW_DISTANCE;
	}

	private boolean waitForTargetClose(Rs2PlayerModel target, Rs2PlayerModel localPlayer, long timeoutMs) {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			if (!playerExists(target) || !playerExists(localPlayer)) {
				return false;
			}
			if (this.isTargetCloseOrRetaliating(target, localPlayer)) return true;
			this.sleepWithFatigue(250, 500);
		}
		return false;
	}

	private boolean isTargetCloseOrRetaliating(Rs2PlayerModel target, Rs2PlayerModel localPlayer) {
		if (localPlayer == null || !playerExists(target)) return false;
		WorldPoint localTile = localPlayer.getWorldLocation();
		WorldPoint targetTile = target.getWorldLocation();
		if (localTile == null || targetTile == null) return false;
		int distance = localTile.distanceTo(targetTile);
		return distance <= DRAG_CLOSE_DISTANCE
				|| (distance <= DRAG_FOLLOW_DISTANCE && this.isTargetRetaliating(target, localPlayer));
	}

	private void runSafeZoneAndReturnToPortal(Rs2PlayerModel localPlayer) {
		WorldPoint localTile = localPlayer == null ? null : localPlayer.getWorldLocation();
		if (localTile == null) return;

		final int safeMinX = this.SAFE_ZONE.getX();
		final int safeMaxX = safeMinX + this.SAFE_ZONE.getWidth() - 1;
		final int safeX = Math.max(safeMinX, Math.min(localTile.getX(), safeMaxX));
		final WorldPoint safePoint = new WorldPoint(safeX, DRAG_SAFE_ZONE_Y, 0);

		this.status = "Drag-Mode: running to safe zone";
		this.log(this.status);
		if (!this.walkDragPoint(safePoint, DRAG_ARRIVE_DISTANCE, DRAG_WALK_TIMEOUT_MS)) {
			this.status = "Drag-Mode: could not reach safe zone";
			this.log(this.status);
			return;
		}

		final WorldArea portal = this.areaMap.get("Portal");
		final WorldPoint portalPoint = portal == null ? null : this.randomAreaTile(portal);
		if (portalPoint == null) return;

		this.status = "Drag-Mode: returning to Portal through safe zone";
		this.log(this.status);
		if (this.walkDragPoint(portalPoint, DRAG_ARRIVE_DISTANCE, DRAG_WALK_TIMEOUT_MS)) {
			this.status = "Drag-Mode: back at Portal";
			this.lastSelectedLocationWalkFinishedAtMs = System.currentTimeMillis();
		} else {
			this.status = "Drag-Mode: returning to Portal";
			this.log(this.status);
		}
	}

	private boolean walkDragPoint(WorldPoint point, int distance, long timeoutMs) {
		if (point == null) return false;
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			WalkerState state = Rs2Walker.walkStep(point, distance);
			if (state == WalkerState.ARRIVED) {
				Rs2Walker.setTarget(null, "viruz: drag-mode walk complete");
				return true;
			}
			if (state == WalkerState.UNREACHABLE || state == WalkerState.EXIT) {
				Rs2Walker.setTarget(null, "viruz: drag-mode walk aborted");
				return false;
			}
			this.sleepWithFatigue(DRAG_STEP_SLEEP_MIN_MS, DRAG_STEP_SLEEP_MAX_MS);
		}
		Rs2Walker.setTarget(null, "viruz: drag-mode walk timed out");
		return false;
	}

	private static String normalizeActorName(String name) {
		return name == null ? "" : name.replace('\u00A0', ' ').trim().toLowerCase(Locale.ROOT);
	}

	private boolean isTargetValid(Rs2PlayerModel player,
								  Rs2PlayerModel localPlayer,
								  String localName,
								  WorldPoint localTile,
								  boolean localInSafeZone) {
		if (!playerExists(player) || !playerExists(localPlayer) || localTile == null) {
			return false;
		}
		String rawName = player.getName();
		if (rawName == null) {
			return false;
		}
		String playerName = rawName.toLowerCase(Locale.ROOT).trim();
		if (playerName.isEmpty()) {
			return false;
		}
		WorldPoint playerTile = player.getWorldLocation();
		if (playerTile == null) {
			return false;
		}
		boolean isDifferentPlayer = !playerName.equals(localName);
		boolean isMatch = this.attackSet.contains(playerName);
		boolean isNotBluePhat = !this.bluePhatNames.contains(playerName);
		boolean isNotClanMember = !this.clanMembers.contains(playerName);
		boolean isNotLureBot = !isLureBot(playerName);
		boolean isNotStaffOfAirOnly = !isStaffOfAirOnly(player);
		boolean isInFfaArea = this.IN_FFA.contains(playerTile);
		boolean isOutsideSafeZone =
				!this.SAFE_ZONE.contains(playerTile) && !localInSafeZone;
		Actor interacting = player.getInteracting();
		boolean isNotInCombatOrMulti =
				!isPlayerInCombat(player)
						|| (interacting instanceof Player && interacting.getName() != null
						&& interacting.getName().toLowerCase(Locale.ROOT).trim().equals(localName));
		boolean isWithinMaxDistance = distanceBetween(localPlayer, player) <= MAX_TARGET_DISTANCE;
		return isMatch
				&& isDifferentPlayer
				&& isNotBluePhat
				&& isNotClanMember
				&& isNotLureBot
				&& isNotStaffOfAirOnly
				&& isInFfaArea
				&& isWithinMaxDistance
				&& isOutsideSafeZone
				&& isNotInCombatOrMulti;
	}

	private boolean isTargetOnCooldown(String targetName) {
		if (targetName == null) return false;
		Long until = targetCooldownUntilMs.get(targetName.toLowerCase(Locale.ROOT).trim());
		return until != null && System.currentTimeMillis() < until;
	}

	private void recordTargetCooldown(String targetName) {
		if (targetName == null) return;
		String key = targetName.toLowerCase(Locale.ROOT).trim();
		if (!key.isEmpty()) {
			targetCooldownUntilMs.put(key, System.currentTimeMillis() + TARGET_SKIP_COOLDOWN_MS);
		}
	}

	private boolean approachTargetIfNeeded(Rs2PlayerModel localPlayer, Rs2PlayerModel target) {
		int maxAttempts = 15;
		for (int attempts = 0; playerExists(target) && distanceBetween(localPlayer, target) > MAX_TARGET_DISTANCE && attempts < maxAttempts; ++attempts) {
			Rs2Walker.walkTo(target.getWorldLocation(), 1);
			this.sleepWithFatigue(200, 400);
		}
		return playerExists(target) && distanceBetween(localPlayer, target) <= MAX_TARGET_DISTANCE;
	}

	private void checkAttackStyleSwitch() {
		final long now = System.currentTimeMillis();
		if (now < nextAttackStyleCheckMs) return;
		nextAttackStyleCheckMs = now + 180000;
		if (lastSwitchLevelForCurrentStyle < 0) return;
		Rs2PlayerModel localPlayer = Rs2Player.getLocalPlayer();
		if (localPlayer == null || isPlayerInCombat(localPlayer) || isPlayerMoving(localPlayer)) return;
		Skill skill = getSkillForStyle(attackStyleIndex);
		int currentLevel = Rs2Player.getRealSkillLevel(skill);
		int realGain = Math.abs(currentLevel - lastSwitchLevelForCurrentStyle);
		if (realGain < nextStyleSwitchGain) return;
		if (currentLevel >= lastSwitchLevelForCurrentStyle + nextStyleSwitchGain) {
			attackStyleIndex = (attackStyleIndex + 1) % ATTACK_STYLE_CHILDS.length;
			Skill newSkill = getSkillForStyle(attackStyleIndex);
			lastSwitchLevelForCurrentStyle = Rs2Player.getRealSkillLevel(newSkill);
			nextStyleSwitchGain = ThreadLocalRandom.current().nextInt(4, 7);
			if (Rs2Tab.switchTo(InterfaceTab.COMBAT)) {
				this.sleepWithFatigue(200, 600);
				Widget styleChild = Rs2Widget.getWidget(ATTACK_STYLE_WIDGET_PARENT, ATTACK_STYLE_CHILDS[attackStyleIndex]);
				if (clickWidget(styleChild)) {
					this.log("Switched attack style to " + newSkill.name());
					this.status = "Style: " + newSkill.name();
				}
				Rs2Tab.switchTo(InterfaceTab.INVENTORY);
			}
		}
	}

	private Skill getSkillForStyle(int index) {
		switch (index) {
			case 1: return Skill.STRENGTH;
			case 2: return Skill.DEFENCE;
			default: return Skill.ATTACK;
		}
	}

	private void checkPrayers() {
		int prayerPoints = Rs2Player.getBoostedSkillLevel(Skill.PRAYER);
		Rs2PlayerModel localPlayer = Rs2Player.getLocalPlayer();
		if (localPlayer == null) return;
		Actor interactingChar = getInteractingWithMe();
		this.sleepWithFatigue(250, 1500);
		if (isPlayerInCombat(localPlayer) && interactingChar instanceof Player) {
			Rs2PlayerModel opponent = new Rs2PlayerModel((Player) interactingChar);
			if (opponent.getName() == null) return;
			int animation = getAnimationOf(opponent);
			if (opponent.getCombatLevel() >= MIN_AUTO_ADD_ATTACKER_LEVEL) {
				aiAssistant.learnAnimationPattern(opponent.getName(), animation, System.currentTimeMillis());
			}

			WorldPoint localTile = localPlayer.getWorldLocation();
			boolean isInSafeZone = localTile != null && this.SAFE_ZONE.contains(localTile);
			boolean prayersAvailable = prayerPoints > 0;
			long fatigueRuntime = System.currentTimeMillis() - fatigueStart;
			double fatigueFactor = this.getFatigueFactor(fatigueRuntime);

			AIAssistant.SuggestedReaction suggestion = aiAssistant.suggestReaction(
					opponent, localPlayer, isInSafeZone, prayersAvailable, fatigueFactor, this.attackSet);

			switch (suggestion.reaction) {
				case PRAY_MAGIC:
					if (prayersAvailable) this.handleMagicAttack(prayerPoints);
					break;
				case PRAY_MELEE:
					if (prayersAvailable) this.handleMeleeAttack(prayerPoints);
					break;
				case PRAY_RANGED:
					if (prayersAvailable) this.handleRangeAttack(prayerPoints);
					break;
				case DRAG_TO_SAFE:
					if (!isInSafeZone) {
						this.status = "AI: Drag to safe - " + suggestion.reason;
						Rs2Walker.walkTo(new WorldPoint(3328, 4753, 0), 1);
					}
					break;
				case RUN_AWAY:
					this.status = "AI: Running away - " + suggestion.reason;
					Rs2Walker.walkTo(new WorldPoint(3328, 4753, 0), 1);
					break;
				case FIGHT_BACK:
					if (this.isMagicAttack(animation) && prayersAvailable) {
						this.handleMagicAttack(prayerPoints);
					} else if (this.isMeleeAttack(animation) && prayersAvailable) {
						this.handleMeleeAttack(prayerPoints);
					} else if (this.isRangeAttack(animation) && prayersAvailable) {
						this.handleRangeAttack(prayerPoints);
					}
					break;
				case IGNORE:
				default:
					break;
			}
		}
	}

	private void handleMagicAttack(int prayerPoints) {
		if (Rs2Player.getRealSkillLevel(Skill.PRAYER) >= 37 && !Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_MAGIC)) {
			if (prayerPoints > 0) {
				this.activateProtectFromMagic();
			}
		} else if (Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_MELEE) || Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_RANGE)) {
			this.switchToProtectFromMagic();
		}

	}

	private void handleMeleeAttack(int prayerPoints) {
		if (Rs2Player.getRealSkillLevel(Skill.PRAYER) >= 43 && !Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_MELEE)) {
			if (prayerPoints > 0) {
				this.activateProtectFromMelee();
			}
		} else if (Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_MAGIC) || Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_RANGE)) {
			this.switchToProtectFromMelee();
		}

	}

	private void handleRangeAttack(int prayerPoints) {
		if (Rs2Player.getRealSkillLevel(Skill.PRAYER) >= 40 && !Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_RANGE)) {
			if (prayerPoints > 0) {
				this.activateProtectFromRanged();
			}
		} else if (Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_MAGIC) || Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_MELEE)) {
			this.switchToProtectFromRanged();
		}

	}

	private void turnPrayersOff() {
		if (this.quickprayersEnabled && Rs2Prayer.isQuickPrayerEnabled()) {
			this.log("Turning off quick prayers.");
			Rs2Prayer.toggleQuickPrayer(false);
		} else if (Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_MAGIC) || Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_RANGE) || Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_MELEE)) {
			this.log("Turning off prayers as we are not in combat or have insufficient prayer points.");
			Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MAGIC, false);
			Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_RANGE, false);
			Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, false);
		}
	}

	private void switchToProtectFromMagic() {
		int prayerPoints = Rs2Player.getBoostedSkillLevel(Skill.PRAYER);
		if (this.isSwitchPrayersEnabled() && prayerPoints > 0) {
			this.status = "Switching to Protect from Magic";
			this.log(this.status);
			this.maybeMakePrayerMistake(Rs2PrayerEnum.PROTECT_MAGIC, Rs2PrayerEnum.PROTECT_MELEE, Rs2PrayerEnum.PROTECT_RANGE);
			Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, false);
			Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_RANGE, false);
			Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MAGIC, true);
			sessionTracker.recordPrayerSwitch();
			sessionTracker.recordCorrectPrayer();
			this.sleepWithFatigue(250, 1500);
		}
	}

	private void switchToProtectFromMelee() {
		int prayerPoints = Rs2Player.getBoostedSkillLevel(Skill.PRAYER);
		if (this.isSwitchPrayersEnabled() && prayerPoints > 0) {
			this.status = "Switching to Protect from Melee";
			this.log(this.status);
			this.maybeMakePrayerMistake(Rs2PrayerEnum.PROTECT_MELEE, Rs2PrayerEnum.PROTECT_MAGIC, Rs2PrayerEnum.PROTECT_RANGE);
			Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MAGIC, false);
			Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_RANGE, false);
			Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, true);
			sessionTracker.recordPrayerSwitch();
			sessionTracker.recordCorrectPrayer();
			this.sleepWithFatigue(250, 1500);
		}
	}

	private void switchToProtectFromRanged() {
		int prayerPoints = Rs2Player.getBoostedSkillLevel(Skill.PRAYER);
		if (this.isSwitchPrayersEnabled() && prayerPoints > 0) {
			this.status = "Switching to Protect from Ranged";
			this.log(this.status);
			this.maybeMakePrayerMistake(Rs2PrayerEnum.PROTECT_RANGE, Rs2PrayerEnum.PROTECT_MAGIC, Rs2PrayerEnum.PROTECT_MELEE);
			Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MAGIC, false);
			Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, false);
			Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_RANGE, true);
			sessionTracker.recordPrayerSwitch();
			this.sleepWithFatigue(250, 1500);
		}
	}

	private void activateProtectFromMagic() {
		int prayerPoints = Rs2Player.getBoostedSkillLevel(Skill.PRAYER);
		if (this.isSwitchPrayersEnabled() && prayerPoints > 0) {
			this.status = "Turning on Protect from Magic";
			this.log(this.status);
			Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MAGIC, true);
			sessionTracker.recordPrayerSwitch();
			sessionTracker.recordCorrectPrayer();
			this.sleepWithFatigue(250, 1500);
		}
	}

	private void activateProtectFromRanged() {
		int prayerPoints = Rs2Player.getBoostedSkillLevel(Skill.PRAYER);
		if (this.isSwitchPrayersEnabled() && prayerPoints > 0) {
			Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_RANGE, true);
			this.status = "Turning on Protect from Ranged";
			this.log(this.status);
			sessionTracker.recordPrayerSwitch();
			sessionTracker.recordCorrectPrayer();
			this.sleepWithFatigue(250, 1500);
		}
	}

	private void activateProtectFromMelee() {
		int prayerPoints = Rs2Player.getBoostedSkillLevel(Skill.PRAYER);
		if (this.isSwitchPrayersEnabled() && prayerPoints > 0) {
			Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, true);
			this.status = "Turning on Protect from Melee";
			this.log(this.status);
			sessionTracker.recordPrayerSwitch();
			sessionTracker.recordCorrectPrayer();
			this.sleepWithFatigue(250, 1500);
		}
	}

	private void maybeMakePrayerMistake(Rs2PrayerEnum correct, Rs2PrayerEnum alt1, Rs2PrayerEnum alt2) {
		long runtime = System.currentTimeMillis() - fatigueStart;
		double fatigueFactor = this.getFatigueFactor(runtime);
		if (fatigueFactor > 2.0 && ThreadLocalRandom.current().nextDouble() < fatigueFactor * 0.005
				&& System.currentTimeMillis() - lastMistakeMs > MISTAKE_COOLDOWN_MS) {
			this.lastMistakeMs = System.currentTimeMillis();
			this.status = "Prayer mistake!";
			this.log("Made a prayer mistake (fatigue: " + String.format("%.2f", fatigueFactor) + ")");
			sessionTracker.recordMissedPrayer();
			sessionTracker.recordMistake();
			Rs2PrayerEnum mistaken = ThreadLocalRandom.current().nextBoolean() ? alt1 : alt2;
			if (mistaken != correct) {
				Rs2Prayer.toggle(mistaken, true);
				this.sleepWithFatigue(200, 800);
				Rs2Prayer.toggle(mistaken, false);
				this.sleepWithFatigue(100, 400);
			}
		}
	}

	private boolean isMagicAttack(int animation) {
		return MAGIC_ATTACK_ANIMATIONS.contains(animation);
	}

	private boolean isRangeAttack(int animation) {
		return RANGE_ATTACK_ANIMATIONS.contains(animation);
	}

	private boolean isMeleeAttack(int animation) {
		return MELEE_ATTACK_ANIMATIONS.contains(animation);
	}

	private void waitBeforeWalkBack() {
		this.status = "Waiting";
		this.sleepWithFatigue(50, 150);
	}

	private void antiLure() {
		if (this.hillScanActive) return;
		Rs2PlayerModel localPlayer = Rs2Player.getLocalPlayer();
		if (this.currentArea == null || localPlayer == null) {
			return;
		}
		String areaName = this.getAreaNameFor(this.currentArea);
		WorldPoint currentTile = localPlayer.getWorldLocation();
		while (localPlayer != null && currentTile != null
				&& !this.currentArea.contains(currentTile)
				&& !this.AT_LOBBY.contains(currentTile)
				&& !this.SAFE_ZONE.contains(currentTile)
				&& !this.STUCK.contains(currentTile)) {
			if (this.TOWER1.contains(currentTile) || this.TOWER2.contains(currentTile)
					|| this.SECOND_TOWER1.contains(currentTile) || this.SECOND_TOWER2.contains(currentTile)
					|| this.SECOND_TOWER3.contains(currentTile)) {
				++this.antiLureFails;
				if (this.antiLureFails >= 10) {
					this.status = "Relogging";
					this.log(this.status);
					Rs2Player.logout();
					Global.sleep(3000);
					this.antiLureFails = 0;
					break;
				}
			}
			if (this.antiLureScan == 0) {
				++this.antiLureScan;
				this.status = "Being lured walking back";
				this.log(this.status);
				sessionTracker.recordAntiLure();
				sendDiscordEmbed("Detected possible drag/lure, returning to " + areaName);
			}
			Rs2Walker.walkTo(randomAreaTile(this.currentArea));
			Global.sleep(300);
			localPlayer = Rs2Player.getLocalPlayer();
			currentTile = localPlayer != null ? localPlayer.getWorldLocation() : null;
		}
		if (currentTile != null && this.currentArea.contains(currentTile)
				&& !this.TOWER1.contains(currentTile) && !this.TOWER2.contains(currentTile)
				&& !this.SECOND_TOWER1.contains(currentTile) && !this.SECOND_TOWER2.contains(currentTile)
				&& !this.SECOND_TOWER3.contains(currentTile)) {
			this.antiLureFails = 0;
			this.lastSelectedLocationWalkFinishedAtMs = System.currentTimeMillis();
		}
	}

	private void quickPrayer() {
		int reactionDelay = this.getHumanReactionTime(HumanBehavior.ReactionContext.COMBAT);
		Global.sleep(reactionDelay);
		Rs2Prayer.toggleQuickPrayer(true);
		this.sleepWithFatigue(500, 3000);
	}

	private void clickPortal() {
		try {
			this.resetSession();
			if (this.currentArea == null) {
				return;
			}
			this.waiting();
			int randomPrayers = ThreadLocalRandom.current().nextInt(2);
			if (randomPrayers == 0 && this.quickprayersEnabled && !this.switchPrayersEnabled) {
				this.status = "Turning on prayers";
				this.log(this.status);
				this.quickPrayer();
			}
			this.status = "Clicking portal";
			this.log(this.status);
			sendDiscordEmbed("Entering " + this.getAreaNameFor(this.currentArea));
			int reactionDelay = this.getHumanReactionTime(HumanBehavior.ReactionContext.CLICKING_PORTAL);
			Global.sleep(reactionDelay);
			WorldPoint areaTile = randomAreaTile(this.currentArea);
			if (areaTile != null) {
				Rs2Walker.walkTo(areaTile);
			}
			this.sleepWithFatigue(3000, 4500);
			this.lastSelectedLocationWalkFinishedAtMs = System.currentTimeMillis();
			if (randomPrayers == 1 && this.quickprayersEnabled && !this.switchPrayersEnabled) {
				this.status = "Turning on prayers";
				this.log(this.status);
				this.quickPrayer();
			}
		} catch (Throwable t) {
			if (t.getCause() instanceof InterruptedException) {
				Thread.currentThread().interrupt();
				throw t;
			}
			log.debug("clickPortal error", t);
		}
	}

	private void waiting() {
		this.status = "Waiting";
		if (Rs2Random.between(0, 100) < 6) {
			this.performAntiban();
		}
		if (this.isDistracted()) {
			this.handleDistraction();
		}
		this.sleepWithFatigue(1000, 2500);
	}

	private void gotoArea() {
		if (this.currentArea == null) {
			return;
		}
		int reactionDelay = this.getHumanReactionTime(HumanBehavior.ReactionContext.WALKING);
		Global.sleep(reactionDelay);
		this.waiting();
		String areaName = this.getAreaNameFor(this.currentArea);
		this.status = "Walking to " + areaName;
		this.log(this.status);
		sendDiscordEmbed("Walking to " + areaName);
		int maxAttempts = this.getMaxAttempts(areaName);
		boolean walked = false;
		for (int attempts = 0; attempts < maxAttempts; ++attempts) {
			Rs2PlayerModel localPlayer = Rs2Player.getLocalPlayer();
			if (localPlayer != null && localPlayer.getWorldLocation() != null
					&& this.currentArea.contains(localPlayer.getWorldLocation())) {
				break;
			}
			WorldPoint entranceTile = randomAreaTile(this.currentArea);
			if (entranceTile == null) break;
			Rs2Walker.walkTo(entranceTile, 1);
			Rs2PlayerModel afterWalk = Rs2Player.getLocalPlayer();
			if (afterWalk != null && afterWalk.getWorldLocation() != null
					&& this.currentArea.contains(afterWalk.getWorldLocation())) {
				walked = true;
				break;
			}
			this.sleepForArea(areaName);
		}
		if (walked) {
			this.lastSelectedLocationWalkFinishedAtMs = System.currentTimeMillis();
		}
	}

	private int getMaxAttempts(String areaName) {
		switch (areaName) {
			case "Portal":
				return 2;
			case "Middle":
				return 3;
			case "Hills single":
			case "Lava single":
			case "Pond single":
				return 5;
			case "Pond":
			case "Lava":
			case "Hills":
				return 7;
			default:
				return 1;
		}
	}

	private void sleepForArea(String areaName) {
		switch (areaName.toLowerCase()) {
			case "portal":
				this.sleepWithFatigue(3000, 5500);
				break;
			case "hills single":
				this.sleepWithFatigue(2000, 4500);
				break;
			case "lava single":
			case "pond single":
			case "middle":
			case "pond":
			case "lava":
				this.sleepWithFatigue(1750, 2500);
				break;
			default:
				this.sleepWithFatigue(2500, 4500);
		}
	}

	private int getAdaptiveThreshold() {
		long fatigueR = System.currentTimeMillis() - fatigueStart;
		double fatigueF = this.getFatigueFactor(fatigueR);
		return (int) Math.round(aiAssistant.calculateAdaptiveThreshold(currentThreshold, fatigueF));
	}

	private void checkHalfHourlyArrows() {
		if (this.arrowCheckTimer.elapsed() >= 1800000L) {
			this.checkArrows();
			this.arrowCheckTimer.reset();
		}
	}

	private void checkArrows() {
		if (isUsingMeleeWeaponEquipped()) {
			return;
		}
		Rs2PlayerModel local = Rs2Player.getLocalPlayer();
		if (local != null && !isPlayerInCombat(local)) {
			if (!Rs2Inventory.isOpen()) {
				Rs2Tab.switchTo(InterfaceTab.INVENTORY);
				this.sleepWithFatigue(500, 2750);
				int elapsed = 0;

				int sleep;
				for (int timeout = 2000; !Rs2Inventory.isOpen() && elapsed < timeout; elapsed += sleep) {
					sleep = Rs2Random.between(50, 150);
					this.sleepWithFatigue(sleep, sleep + 50);
				}
			}
			Rs2ItemModel arrows = Rs2Inventory.get(RUNE_ARROW, ADAMANT_ARROW, MITHRIL_ARROW);
			if (arrows != null) {
				this.log("Equipping arrows");
				this.sleepWithFatigue(350, 1750);

				if (Rs2Inventory.interact(arrows, "Wield")) {
					int elapsed = 0;

					int sleep;
					for (int maxWait = 1500; Rs2Inventory.hasItem(RUNE_ARROW, ADAMANT_ARROW, MITHRIL_ARROW) && elapsed < maxWait; elapsed += sleep) {
						sleep = Rs2Random.between(50, 150);
						this.sleepWithFatigue(sleep, sleep + 50);
					}
				}
			}
		}
	}

	private boolean isUsingMeleeWeaponEquipped() {
		Rs2ItemModel weapon = Rs2Equipment.get(EquipmentInventorySlot.WEAPON);
		if (weapon == null || weapon.getName() == null) return false;
		String weaponName = weapon.getName();
		for (String meleeWeapon : MELEE_WEAPONS) {
			if (meleeWeapon.equalsIgnoreCase(weaponName)) {
				return true;
			}
		}
		return false;
	}

	private void setNextPreventLogout() {
		long delay = 150000L + (long) (Math.random() * (double) 120000.0F);
		this.nextActionTime = System.currentTimeMillis() + delay;
	}





	private int ArrayIndexSkillOffset(Skill skill) {
		switch (skill) {
			case ATTACK: return 0;
			case STRENGTH: return 1;
			case DEFENCE: return 2;
			case HITPOINTS: return 3;
			case RANGED: return 4;
			default: return 5;
		}
	}





	private void performAntiban() {
		try {
			this.onAction();
			if (this.simulateHumanError()) {
				this.status = "Mistake...";
				Microbot.naturalMouse.moveOffScreen();
				Global.sleep(1000 + ThreadLocalRandom.current().nextInt(3000));
				return;
			}
			int fatigueVariance = this.getCurrentFatigueLevel();
			if (fatigueVariance > 100 && Rs2Random.between(0, 100) < 15) {
				this.sleepWithFatigue(500, 2000);
				return;
			}

			long runtime = System.currentTimeMillis() - fatigueStart;
			double fatigueF = this.getFatigueFactor(runtime);
			double contextualChance = fatigueF > 8.0 ? 0.50 : fatigueF > 4.0 ? 0.35 : 0.25;
			if (ThreadLocalRandom.current().nextDouble() < contextualChance) {
				HumanBehavior.AntiBanSuggestion suggestion = getContextualAntiBanAction(fatigueF);
				if (suggestion != null) {
					executeAntiBanSuggestion(suggestion.action);
					return;
				}
			}
			int roll = Rs2Random.between(1, 500);
			if (roll <= 180) {
				this.sleepWithFatigue(2000, 8000);
			} else if (roll <= 265) {
				this.pseudoAfk();
			} else if (roll <= 290) {
				this.stretchMicrobreak();
			} else if (roll <= 335) {
				if (Rs2Random.between(0, 100) < 50) {
					this.checkEquipmentTab();
				} else {
					this.sleepWithFatigue(2000, 8000);
				}
			} else if (roll <= 365) {
				this.walkAdjust();
			} else if (roll <= 400) {
				this.doubleTabCycle();
			} else if (roll <= 449) {
				this.runTrackerScans();
			} else if (roll <= 455) {
				Microbot.naturalMouse.moveOffScreen();
				this.sleepWithFatigue(2000, 5000);
			} else {
				this.sleepWithFatigue(3000, 10000);
			}
			if (Rs2Random.between(0, 6) == 0) {
				sendDiscordEmbed("Running Anti-Ban routine");
			}
		} catch (RuntimeException ex) {
			this.log("Antiban interrupted: " + ex.getMessage());
		}
	}

	private void executeAntiBanSuggestion(String action) {
		long now = System.currentTimeMillis();
		long runtime = now - fatigueStart;
		double fatigueF = this.getFatigueFactor(runtime);
		switch (action) {
			case "camera_turn":
				break;
			case "tab_cycle":
				this.doubleTabCycle();
				break;
			case "microbreak":
				this.stretchMicrobreak();
				break;
			case "prayer_check":
				if (Rs2Tab.switchTo(InterfaceTab.PRAYER)) {
					this.sleepWithFatigue(400, 1200);
					Rs2Tab.switchTo(InterfaceTab.INVENTORY);
				}
				break;
			case "equipment_check":
				this.checkEquipmentTab();
				break;
			case "right_click_drag":
				this.rightClickDrag();
				break;
			case "world_map":
				this.openWorldMap();
				break;
			case "spellbook_check":
				this.checkSpellbook();
				break;
			case "emote_check":
				this.emoteTabHover();
				break;
			default:
				this.sleepWithFatigue((int) (300 * (1.0 + fatigueF * 0.04)), (int) (1000 * (1.0 + fatigueF * 0.04)));
		}
	}

	private void maybeAutoSwitchLocation() {
		if (!this.attackEnabled || this.currentArea == null || this.selectedLocation == null) return;
		long now = System.currentTimeMillis();
		if (now - this.lastCombatActivityMs <= LOCATION_SWITCH_IDLE_MS) return;
		if (now - this.lastAutoLocationSwitchMs < LOCATION_SWITCH_IDLE_MS) return;
		List<String> candidates = new ArrayList<>();
		for (String loc : AUTO_SWITCH_LOCATIONS) {
			if (!loc.equals(this.selectedLocation)) {
				candidates.add(loc);
			}
		}
		if (candidates.isEmpty()) return;
		String newLocation = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
		this.lastAutoLocationSwitchMs = now;
		this.selectedLocation = newLocation;
		this.currentArea = this.areaMap.get(newLocation);
		this.status = "Auto-switching location to " + newLocation;
		this.log(this.status);
		sendDiscordEmbed("Auto-switching location to **" + newLocation + "** (No opps since 1hr)");
	}

	private void runTrackerScans() {
		this.hillScanActive = true;
		this.status = "Trackers Scan: starting";
		this.log("Trackers Scan started - Ignoring anti-lures temporarily.");
		sendDiscordEmbed("Updating Trackers..");

		try {
			WorldArea[] scanAreas = {this.AT_LOBBY, this.SAFE_ZONE, this.areaMap.get("Portal"), this.areaMap.get("Middle"), this.areaMap.get("Hills")};
			String[] scanAreaNames = {"Lobby", "Safezone", "Portal", "Middle"};

			for (int i = 0; i < scanAreas.length; i++) {
				WorldArea destArea = scanAreas[i];
				String areaName = scanAreaNames[i];
				if (destArea == null) {
					this.log("Trackers Scan: could not find area for " + areaName);
					continue;
				}

				this.status = "Trackers Scan: walking to " + areaName;
				Rs2PlayerModel localPlayer = Rs2Player.getLocalPlayer();
				if (!playerExists(localPlayer)) {
					this.log("Trackers Scan: local player not ready");
					return;
				}

				WorldPoint destTile = randomAreaTile(destArea);
				int maxAttempts = 10;
				for (int attempt = 0; attempt < maxAttempts; attempt++) {
					localPlayer = Rs2Player.getLocalPlayer();
					if (!playerExists(localPlayer)) break;
					if (localPlayer.getWorldLocation() != null && destArea.contains(localPlayer.getWorldLocation())) break;
					Rs2Walker.walkTo(destTile);
					this.sleepWithFatigue(1200, 2500);
				}

				localPlayer = Rs2Player.getLocalPlayer();
				if (playerExists(localPlayer)
						&& localPlayer.getWorldLocation() != null && destArea.contains(localPlayer.getWorldLocation())) {
					this.status = "Trackers Scan: scanning at " + areaName;
					this.log("Trackers Scan: performing full tracker scan/save at " + areaName);
					sessionTracker.recordScan();
					sessionTracker.tickSave();
					nonTargetTracker.tickSave();
					playerStats.save();
					this.sleepWithFatigue(500, 1500);
					this.log("Trackers Scan: " + areaName + " scan complete");
				} else {
					this.log("Trackers Scan: failed to reach " + areaName);
				}
			}

			Rs2PlayerModel localPlayer = Rs2Player.getLocalPlayer();
			if (playerExists(localPlayer)) {
				this.status = "Trackers Scan: updating tracker info";
				this.log("Trackers Scan: updating all tracker info for GUI");
				this.readMainUserFile();
				this.loadList();
				this.scanNonTargets(localPlayer);
				this.sessionTracker.recordScan();
				this.sessionTracker.tickSave();
				this.nonTargetTracker.tickSave();
				this.playerStats.updateXpEarned();
				this.playerStats.save();
				this.aiAssistant.saveModel();
				this.sleepWithFatigue(300, 800);
			}
		} finally {
			this.hillScanActive = false;
			this.hillScanEnabled = false;
			this.status = "Trackers Scan finished";
		}
	}



	private void postCombatGlance() {
		this.status = "Quick equipment check";
		this.log(this.status);
		if (Rs2Random.between(0, 100) < 50) {
			if (Rs2Tab.switchTo(InterfaceTab.INVENTORY)) {
				this.sleepWithFatigue(300, 900);
				Rs2ItemModel food = Rs2Inventory.get(i -> i != null && i.getName() != null
						&& (i.getName().toLowerCase(Locale.ROOT).contains("shark")
						|| i.getName().toLowerCase(Locale.ROOT).contains("lobster")
						|| i.getName().toLowerCase(Locale.ROOT).contains("swordfish")
						|| i.getName().toLowerCase(Locale.ROOT).contains("monkfish")));
				if (food != null) {
					int slot = food.getSlot();
					int col = slot % 4;
					int row = slot / 4;
					naturalMouseMovement(
							566 + col * 42 + Rs2Random.between(-6, 6),
							214 + row * 36 + Rs2Random.between(-6, 6));
					this.sleepWithFatigue(200, 600);
				}
			}
		} else {
			if (Rs2Tab.switchTo(InterfaceTab.EQUIPMENT)) {
				this.sleepWithFatigue(300, 800);
				Rs2ItemModel weapon = Rs2Equipment.get(EquipmentInventorySlot.WEAPON);
				if (weapon != null) {
					naturalMouseMovement(Rs2Random.between(600, 660), Rs2Random.between(150, 200));
					this.sleepWithFatigue(400, 1000);
				}
			}
		}
		if (Rs2Random.between(0, 100) < 20) {
			Rs2Tab.switchTo(InterfaceTab.COMBAT);
			this.sleepWithFatigue(200, 500);
			Rs2Tab.switchTo(InterfaceTab.INVENTORY);
		}
	}

	private void sleepWithFatigue(int minMillis, int maxMillis) {
		if (maxMillis <= minMillis) {
			maxMillis = minMillis + 1;
		}

		long now = System.currentTimeMillis();
		long runtime = now - this.fatigueStart;
		double fatigueFactor = this.getFatigueFactor(runtime);

		if (now > this.nextEnergyBoostCheck) {
			double boostChance = 0.02 + (this.sessionFatiguePeak > 12.0 ? 0.04 : 0.0);
			if (ThreadLocalRandom.current().nextDouble() < boostChance) {
				double boostValue = 0.3 + ThreadLocalRandom.current().nextDouble() * 1.2;
				this.energyBoost = Math.min(3.0, this.energyBoost + boostValue);
				this.log("Energy surge! Level: " + String.format("%.2f", this.energyBoost));
				this.lastEnergyBoostTime = now;
				this.nextAfkBreakTime += (long) (boostValue * 300000.0);
			}
			this.nextEnergyBoostCheck = now + Rs2Random.between(300000, 900000);
		}

		if (this.energyBoost > 0.01) {
			double decayHours = (double) (now - this.lastEnergyBoostTime) / 3600000.0;
			this.energyBoost = Math.max(0.0, this.energyBoost * Math.exp(-decayHours * 12.0) - 0.005);
			this.lastEnergyBoostTime = now;
		}

		if (now > this.fatigueSpikeCheckTime && fatigueFactor > 5.0 && ThreadLocalRandom.current().nextDouble() < 0.025) {
			this.fatigueSpikeMagnitude = 0.3 + ThreadLocalRandom.current().nextDouble() * 0.7;
			this.fatigueSpikeEndTime = now + (long) (Rs2Random.between(30000, 120000) * (1.0 + fatigueFactor * 0.04));
			this.log("Fatigue spike! +" + String.format("%.2f", this.fatigueSpikeMagnitude) + " for " + ((this.fatigueSpikeEndTime - now) / 1000) + "s");
			this.fatigueSpikeCheckTime = now + Rs2Random.between(300000, 600000);
		}

		if (now > this.nextFatigueCheck && ThreadLocalRandom.current().nextDouble() < 0.008 && fatigueFactor > 3.0) {
			this.performanceStreakBoost = 0.5 + ThreadLocalRandom.current().nextDouble() * 0.8;
			this.performanceStreakEndTime = now + Rs2Random.between(120000, 300000);
			this.log("Second wind! Performance streak boost: +" + String.format("%.2f", this.performanceStreakBoost));
			this.nextFatigueCheck = now + Rs2Random.between(600000, 1800000);
		}

		if (now > this.nextUltradianUpdateMs) {
			this.ultradianPhase = ThreadLocalRandom.current().nextDouble() * Math.PI * 2.0;
			this.nextUltradianUpdateMs = now + Rs2Random.between(600000, 1200000);
		}

		double attentionBase = 0.75 + 0.25 * Math.sin((double) runtime / 1800000.0);
		double fatiguePenalty = fatigueFactor * 0.015;
		double energyBonus = this.energyBoost * 0.05;
		double streakBonus = (now < this.performanceStreakEndTime) ? this.performanceStreakBoost * 0.1 : 0.0;
		double attention = Math.min(1.15, Math.max(0.55, attentionBase - fatiguePenalty + energyBonus + streakBonus));

		int baseSleep = Rs2Random.between(minMillis, maxMillis);
		double fatigueMult = 1.0 + Math.min(fatigueFactor * 0.035, 0.7);
		double attentionMult = 1.0 + (1.0 - attention) * 0.5;
		double variance = Math.exp(ThreadLocalRandom.current().nextGaussian() * 0.08);
		int total = (int) ((double) baseSleep * fatigueMult * attentionMult * variance);

		if (now < this.fatigueSpikeEndTime) {
			total = (int) (total * (1.0 + this.fatigueSpikeMagnitude * 0.3));
		}

		if (this.energyBoost > 0.5) {
			total = (int) (total * Math.max(0.6, 1.0 - this.energyBoost * 0.08));
		}

		if (ThreadLocalRandom.current().nextDouble() < 0.005 + fatigueFactor * 0.002) {
			total += Rs2Random.between(500, 3000);
		}

		total = Math.max(50, Math.min(total, 15000));

		int elapsed = 0;
		while (elapsed < total) {
			int chunkSize = Rs2Random.between(30, 180);
			int chunk = Math.min(total - elapsed, chunkSize);
			Global.sleep(chunk);
			elapsed += chunk;

			double microMoveChance = 0.0008 + fatigueFactor * 0.0003;
			if (ThreadLocalRandom.current().nextDouble() < microMoveChance) {
				Point p = Microbot.getMouse().getMousePosition();
				if (p != null) {
					int dx = Rs2Random.between(-6, 6);
					int dy = Rs2Random.between(-6, 6);
					if (Math.abs(dx) + Math.abs(dy) > 1) {
						Microbot.getMouse().move(new net.runelite.api.Point(
								Math.max(0, Math.min(765, p.x + dx)),
								Math.max(0, Math.min(503, p.y + dy))));
					}
				}
			}

			if (total > 2000 && ThreadLocalRandom.current().nextDouble() < 0.0008 + fatigueFactor * 0.0002) {
				int gazeX = Rs2Random.between(100, 650);
				int gazeY = Rs2Random.between(50, 450);
				naturalMouseMovement(gazeX, gazeY);
				Global.sleep(Rs2Random.between(200, 600));
				Point cur = Microbot.getMouse().getMousePosition();
				if (cur != null && cur.x > 50 && cur.x < 715 && cur.y > 30 && cur.y < 480) {
					naturalMouseMovement(cur.x + Rs2Random.between(-3, 3),
							cur.y + Rs2Random.between(-3, 3));
				}
			}

			if (total > 3000 && ThreadLocalRandom.current().nextDouble() < 0.0004 + fatigueFactor * 0.00015) {
				rotateCameraTo(
						getCameraYawDegrees() + Rs2Random.between(-8, 8),
						Rs2Camera.getPitch() + Rs2Random.between(-5, 5)
				);
				Global.sleep(Rs2Random.between(150, 400));
			}

			if (ThreadLocalRandom.current().nextDouble() < 0.003 + fatigueFactor * 0.0003) {
				Global.sleep(Rs2Random.between(50, 150));
			}
		}

		if (Rs2Random.between(0, 1000) < 1 + (int) (fatigueFactor * 0.05) + (this.energyBoost > 2.0 ? 1 : 0)) {
			this.performAntiban();
		}

		++this.actionsSinceBreak;
		double fatigueAdjust = Math.max(0.55, 1.0 - fatigueFactor * 0.008);
		int actionsLimit = (int) ((Rs2Random.between(25, 60) + this.energyBoost * 20) * fatigueAdjust / (1.0 + fatigueFactor * 0.02));
		if (this.actionsSinceBreak > actionsLimit) {
			int maxBreak = (int) Math.min(5000.0, 1000.0 + fatigueFactor * 250.0);
			Global.sleep(Rs2Random.between(500, maxBreak));
			this.actionsSinceBreak = 0;
			if (Rs2Random.between(0, 100) < 3 + (int) (fatigueFactor * 0.15)) {
				this.performAntiban();
			}
		}
	}

	double getFatigueFactor(long runtime) {
		if (runtime <= 0) return 0.0;
		double hours = (double) runtime / 3600000.0;
		long now = System.currentTimeMillis();

		double effectiveHours = Math.max(0.0, hours - 1.5);
		double baseFatigue = 15.0 / (1.0 + Math.exp(4.5 - effectiveHours * 0.5));

		double ultradianAmp = 0.2 + baseFatigue * 0.03;
		double ultradianCycle = ultradianAmp * Math.sin(hours * 2.0 * Math.PI / 1.5 + this.ultradianPhase);

		double fatigue = getFatigue(now, baseFatigue, ultradianCycle);

		if (fatigue > this.sessionFatiguePeak) {
			this.sessionFatiguePeak = fatigue;
		}

		return fatigue;
	}

	private double getFatigue(long now, double baseFatigue, double ultradianCycle) {
		double microFatigue = Math.min(1.5, this.actionsSinceBreak * 0.015);

		double recovery = 0.0;
		long idleDuration = now - this.lastAttackAttemptAtMs;
		if (idleDuration > 8000) {
			double idleMinutes = Math.min(idleDuration / 60000.0, 10.0);
			recovery = Math.min(2.5, idleMinutes * 0.12);
		}

		double spike = (now < this.fatigueSpikeEndTime) ? this.fatigueSpikeMagnitude : 0.0;

		double streakBoost = (now < this.performanceStreakEndTime) ? this.performanceStreakBoost : 0.0;

		double energyReduction = this.energyBoost * 0.8;

		double fatigue = baseFatigue + ultradianCycle + microFatigue + spike
				- recovery - energyReduction - streakBoost;
		return Math.max(0.0, Math.min(20.0, fatigue));
	}

	private void pseudoAfk() {
		this.status = "Looking away";
		this.log(this.status);
		Microbot.naturalMouse.moveOffScreen();
		this.sleepWithFatigue(3000, 8000);
		if (Rs2Random.between(0, 100) < 30) {
			rotateCameraTo(getCameraYawDegrees() + Rs2Random.between(-25, 25), Rs2Camera.getPitch() + Rs2Random.between(-15, 15));
		}
	}

	private void stretchMicrobreak() {
		this.status = "Micro break";
		this.log(this.status);
		Microbot.naturalMouse.moveOffScreen();
		this.sleepWithFatigue(4000, 10000);
        Rs2Random.between(0, 100);
    }

	private void doubleTabCycle() {
		this.status = "Flicking through tabs";
		InterfaceTab[] order = {
				InterfaceTab.INVENTORY, InterfaceTab.PRAYER, InterfaceTab.MAGIC, InterfaceTab.COMBAT,
				InterfaceTab.EQUIPMENT, InterfaceTab.SKILLS, InterfaceTab.QUESTS, InterfaceTab.FRIENDS,
				InterfaceTab.CHAT, InterfaceTab.SETTINGS, InterfaceTab.MUSIC, InterfaceTab.LOGOUT
		};
		int startIdx = Rs2Random.between(0, order.length - 2);
		InterfaceTab open = Rs2Tab.getCurrentTab();
		if (open != null) {
			InterfaceTab first = order[startIdx];
			if (first != open) {
				Rs2Tab.switchTo(first);
				this.sleepWithFatigue(150, 450);
			}
			InterfaceTab second = order[startIdx + 1];
			if (second != first) {
				Rs2Tab.switchTo(second);
				this.sleepWithFatigue(200, 500);
			}
			Rs2Tab.switchTo(open);
			this.sleepWithFatigue(200, 600);
		}
	}

	private void rightClickDrag() {
		this.status = "Right-click dragging";
		int startX = Rs2Random.between(200, 500);
		int startY = Rs2Random.between(150, 350);
		int endX = startX + ThreadLocalRandom.current().nextInt(-35, 36);
		int endY = startY + ThreadLocalRandom.current().nextInt(-35, 36);
		naturalMouseMovement(startX, startY);
		Global.sleep(Rs2Random.between(100, 300));
		Point pos = Microbot.getMouse().getMousePosition();
		if (pos != null) {
			Microbot.getMouse().click(new net.runelite.api.Point(pos.x, pos.y), true);
			this.sleepWithFatigue(80, 200);
			naturalMouseMovement(endX, endY);
			Global.sleep(Rs2Random.between(60, 180));
			Microbot.getMouse().click();
		}
		this.sleepWithFatigue(100, 300);
	}





	private void openWorldMap() {
		this.status = "Checking world map";
		Rectangle mmBounds = Microbot.getClientThread().runOnClientThreadOptional(() -> {
			Widget mm = net.runelite.client.plugins.microbot.util.walker.Rs2MiniMap.getMinimapDrawWidget();
			return mm == null ? null : mm.getBounds();
		}).orElse(null);
		if (mmBounds == null) {
			mmBounds = new Rectangle(600, 20, 120, 120);
		}
		naturalMouseMovement(mmBounds.x + mmBounds.width / 2, mmBounds.y + mmBounds.height / 2);
		Global.sleep(Rs2Random.between(400, 1200));
		if (Rs2Random.between(0, 100) < 30) {
			Microbot.getMouse().click();
			Global.sleep(Rs2Random.between(200, 600));
		}
	}



	private void walkAdjust() {
		this.status = "Adjusting position";
		Rs2PlayerModel local = Rs2Player.getLocalPlayer();
		if (local == null) return;
		WorldPoint current = local.getWorldLocation();
		if (current == null) return;
		int dx = Rs2Random.between(-2, 2);
		int dy = Rs2Random.between(-2, 2);
		if (dx == 0 && dy == 0) dx = 1;
		WorldPoint target = current.dx(dx).dy(dy);
		Rs2Walker.walkTo(target, 1);
		Global.sleep(Rs2Random.between(600, 1500));
		if (Rs2Random.between(0, 100) < 50) {
			Rs2Walker.walkTo(current, 1);
			Global.sleep(Rs2Random.between(500, 1200));
		}
	}





	private void checkSpellbook() {
		this.status = "Checking spellbook";
		if (Rs2Tab.switchTo(InterfaceTab.MAGIC)) {
			Global.sleep(Rs2Random.between(500, 1300));
			int spX = Rs2Random.between(100, 600);
			int spY = Rs2Random.between(100, 350);
			naturalMouseMovement(spX, spY);
			Global.sleep(Rs2Random.between(400, 1000));
			Rs2Tab.switchTo(InterfaceTab.INVENTORY);
		}
	}





	private void climbdownStairs() {
		Rs2PlayerModel localPlayer = Rs2Player.getLocalPlayer();
		if (localPlayer == null) return;
		WorldPoint loc = localPlayer.getWorldLocation();
		if (loc == null) return;
		if (loc.getPlane() == 1) {
			this.log("Player is on an upper level (Z=1). Searching for climbable object...");
			Rs2TileObjectModel climbableObject = this.findClimbableObject();
			if (climbableObject != null) {
				String objName = tileObjectName(climbableObject);
				this.log("Found climbable object: " + objName + " at " + climbableObject.getWorldLocation());
				String climbOption = tileObjectHasAction(climbableObject, "Climb-down") ? "Climb-down" : this.getFallbackClimbOption(climbableObject);
				if (climbOption != null) {
					this.log("Using action: " + climbOption + " on " + objName);
					if (climbableObject.click(climbOption)) {
						Global.sleepUntil(() -> {
							Rs2PlayerModel p = Rs2Player.getLocalPlayer();
							WorldPoint w = p == null ? null : p.getWorldLocation();
							return w != null && w.getPlane() == 0;
						}, 5000);
					}
					localPlayer = Rs2Player.getLocalPlayer();
					WorldPoint w2 = localPlayer == null ? null : localPlayer.getWorldLocation();
					if (w2 != null && w2.getPlane() == 0) {
						this.log("Successfully climbed down. Returning to combat area");
						this.gotoArea();
					} else {
						this.log("Failed to climb down.");
					}
				} else {
					this.log("No valid climb option found for " + objName);
				}
			} else {
				this.log("No climbable objects found on Z=1.");
			}
		}
	}

	private Rs2TileObjectModel findClimbableObject() {
		String[] names = {"Stairs", "Ladder", "Staircase", "Spiral staircase", "Trapdoor"};
		for (String name : names) {
			Rs2TileObjectModel obj = Microbot.getRs2TileObjectCache().query()
					.where(o -> o.getTileObjectType() == TileObjectType.GAME)
					.withName(name)
					.firstOnClientThread();
			if (obj != null) {
				return obj;
			}
		}
		return Microbot.getRs2TileObjectCache().query()
				.where(o -> o.getTileObjectType() == TileObjectType.GAME)
				.where(o -> tileObjectHasAction(o, "Climb-down"))
				.firstOnClientThread();
	}

	private String getFallbackClimbOption(Rs2TileObjectModel gameObject) {
		for (String action : new String[]{"Climb Down", "Climb", "Go-down"}) {
			if (tileObjectHasAction(gameObject, action)) {
				return action;
			}
		}
		return null;
	}

	private void avoidObstacles() {
		Rs2TileObjectModel obstacle = Microbot.getRs2TileObjectCache().query()
				.where(o -> o.getTileObjectType() == TileObjectType.GAME)
				.where(o -> tileObjectHasAction(o, "Climb-up") || tileObjectHasAction(o, "Climb-down"))
				.within(8)
				.firstOnClientThread();
		Rs2PlayerModel localPlayer = Rs2Player.getLocalPlayer();
		if (obstacle == null || localPlayer == null) return;
		WorldPoint lt = localPlayer.getWorldLocation();
		WorldPoint ot = obstacle.getWorldLocation();
		if (lt != null && lt.distanceTo(ot) < 8) {
			this.log("Avoiding climbing obstacle");
			Rs2Walker.walkTo(lt.dx(Rs2Random.between(-2, 2)).dy(Rs2Random.between(-2, 2)));
			Global.sleep(Rs2Random.between(2000, 3000));
		}
	}

	private void scanNonTargets(Rs2PlayerModel localPlayer) {
		if (localPlayer == null || localPlayer.getName() == null) return;
		String localName = localPlayer.getName().toLowerCase(Locale.ROOT).trim();
		String location = getLocationContext(localPlayer);
		List<Rs2PlayerModel> allPlayers = getPlayers(p -> p != null && p.getName() != null)
				.collect(Collectors.toList());
		for (Rs2PlayerModel p : allPlayers) {
			String playerName = p.getName();
			if (playerName == null) continue;
			String name = playerName.toLowerCase(Locale.ROOT).trim();
			if (name.equals(localName)) continue;
			nonTargetTracker.record(p, location);
			aiAssistant.trackPlayer(playerName, p.getCombatLevel());
		}
	}

	private String getLocationContext(Rs2PlayerModel localPlayer) {
		if (localPlayer == null) return "Unknown";
		WorldPoint tile = localPlayer.getWorldLocation();
		if (tile == null) return "Unknown";
		if (AT_LOBBY.contains(tile)) return "Lobby";
		if (SAFE_ZONE.contains(tile)) return "Safe zone";
		if (STUCK.contains(tile)) return "Stuck";
		if (IN_FFA.contains(tile)) {
			String areaName = "";
			for (Map.Entry<String, WorldArea> entry : areaMap.entrySet()) {
				if (entry.getValue().contains(tile)) {
					areaName = entry.getKey();
					break;
				}
			}
			if (!areaName.isEmpty()) return "FFA - " + areaName;
			return "FFA";
		}
		return "Unknown";
	}

	private List<String> readLines(Path path) throws IOException {
		try {
			return Files.readAllLines(path, StandardCharsets.UTF_8);
		} catch (java.nio.charset.MalformedInputException e) {
			this.log("Malformed UTF-8 input, trying ISO_8859_1");
			return Files.readAllLines(path, StandardCharsets.ISO_8859_1);
		}
	}

	private void removeDuplicateNames(String filePath) {
		try {
			Path fileLocation = get(filePath);
			if (!Files.exists(fileLocation)) {
				return;
			}
			if (filePath.toLowerCase(Locale.ROOT).endsWith(".json")) {
				removeDuplicateJsonNames(fileLocation);
				return;
			}
			List<String> lines = readLines(fileLocation);
			Set<String> uniqueLines = new LinkedHashSet<>();
			boolean modified = false;

			for (String line : lines) {
				String trimmed = line.trim();
				if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
					if (!uniqueLines.add(trimmed.toLowerCase())) {
						modified = true;
						this.log("Removing duplicate name: " + trimmed);
					}
				}
			}

			if (modified) {
				Files.write(fileLocation, uniqueLines, StandardCharsets.UTF_8);
				this.log(fileLocation.getFileName() + " updated: removed duplicates.");
			}
		} catch (Exception e) {
			this.log("Error in removeDuplicateNames: " + e);
		}
	}

	private void removeDuplicateJsonNames(Path fileLocation) {
		try {
			String json = Files.readString(fileLocation, StandardCharsets.UTF_8);
			if (json.isBlank()) {
				return;
			}
			com.google.gson.JsonElement root = new com.google.gson.JsonParser().parse(json);
			boolean modified = false;

			if (root.isJsonObject()) {
				com.google.gson.JsonObject obj = root.getAsJsonObject();
				for (String mapKey : new String[]{"profiles", "patterns"}) {
					if (obj.has(mapKey) && obj.get(mapKey).isJsonObject()) {
						com.google.gson.JsonObject map = obj.getAsJsonObject(mapKey);
						com.google.gson.JsonObject deduped = new com.google.gson.JsonObject();
						java.util.LinkedHashMap<String, com.google.gson.JsonElement> byLower = new java.util.LinkedHashMap<>();
						for (Map.Entry<String, com.google.gson.JsonElement> e : map.entrySet()) {
							String lower = e.getKey().toLowerCase(Locale.ROOT).trim();
							if (byLower.containsKey(lower)) {
								modified = true;
								this.log("Removing duplicate name in " + fileLocation.getFileName() + ": " + e.getKey());
							} else {
								byLower.put(lower, e.getValue());
							}
						}
						for (Map.Entry<String, com.google.gson.JsonElement> e : byLower.entrySet()) {
							deduped.add(e.getKey(), e.getValue());
						}
						obj.add(mapKey, deduped);
					}
				}
				if (obj.has("encounters") && obj.get("encounters").isJsonArray()) {
					com.google.gson.JsonArray arr = obj.getAsJsonArray("encounters");
					com.google.gson.JsonArray deduped = new com.google.gson.JsonArray();
					java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
					for (com.google.gson.JsonElement el : arr) {
						String tname = null;
						if (el.isJsonObject() && el.getAsJsonObject().has("targetName")) {
							com.google.gson.JsonElement t = el.getAsJsonObject().get("targetName");
							if (t.isJsonPrimitive() && t.getAsJsonPrimitive().isString()) {
								tname = t.getAsString().toLowerCase(Locale.ROOT).trim();
							}
						}
						if (tname != null && !tname.isEmpty()) {
							if (!seen.add(tname)) {
								modified = true;
								this.log("Removing duplicate encounter in " + fileLocation.getFileName() + ": " + tname);
								continue;
							}
						}
						deduped.add(el);
					}
					obj.add("encounters", deduped);
				}
				root = obj;
			} else if (root.isJsonArray()) {
				com.google.gson.JsonArray arr = root.getAsJsonArray();
				com.google.gson.JsonArray deduped = new com.google.gson.JsonArray();
				java.util.LinkedHashMap<String, com.google.gson.JsonElement> byLower = new java.util.LinkedHashMap<>();
				for (com.google.gson.JsonElement el : arr) {
					String key = "";
					if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
						key = el.getAsString().toLowerCase(Locale.ROOT).trim();
					} else if (el.isJsonObject() && el.getAsJsonObject().has("playerName")) {
						com.google.gson.JsonElement pn = el.getAsJsonObject().get("playerName");
						if (pn.isJsonPrimitive() && pn.getAsJsonPrimitive().isString()) {
							key = pn.getAsString().toLowerCase(Locale.ROOT).trim();
						}
					}
					if (key.isEmpty()) {
						deduped.add(el);
						continue;
					}
					com.google.gson.JsonElement existing = byLower.get(key);
					if (existing == null) {
						byLower.put(key, el);
					} else {
						modified = true;
						this.log("Removing duplicate name in " + fileLocation.getFileName() + ": " + key);
						boolean keepNewer = el.isJsonObject() && existing.isJsonObject()
								&& el.getAsJsonObject().has("lastSeenMs") && existing.getAsJsonObject().has("lastSeenMs")
								&& el.getAsJsonObject().get("lastSeenMs").getAsLong() > existing.getAsJsonObject().get("lastSeenMs").getAsLong();
						if (keepNewer) {
							byLower.put(key, el);
						}
					}
				}
				for (com.google.gson.JsonElement el : byLower.values()) {
					deduped.add(el);
				}
				if (deduped.size() != arr.size()) {
					modified = true;
				}
				root = deduped;
			}

			if (modified) {
				Files.writeString(fileLocation,
						new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root),
						StandardCharsets.UTF_8);
				this.log(fileLocation.getFileName() + " updated: removed duplicates.");
			}
		} catch (Exception e) {
			this.log("Error in removeDuplicateJsonNames for " + fileLocation.getFileName() + ": " + e);
		}
	}

	private void removeDuplicateNamesFromAllLists() {
		removeDuplicateNames(this.filePath);
		removeDuplicateNames(Config.getLureDragFilePath());
		removeDuplicateNames(Config.getBluePhatsFilePath());
		removeDuplicateNames(Config.getAiDataFilePath());
		removeDuplicateNames(Config.getLastSeenFilePath());
	}

	private void removeBlockedNamesFromFile(String filePath) {
		try {
			Path fileLocation = get(filePath);
			if (!Files.exists(fileLocation)) {
				return;
			}
			List<String> lines = readLines(fileLocation);
			Set<String> uniqueLines = new LinkedHashSet<>();
			boolean modified = false;

			for (String line : lines) {
				String trimmed = line.trim();
				String lowerLine = trimmed.toLowerCase();
				boolean isBlocked = false;

				for (String blockedName : this.BLOCKED_NAMES) {
					if (lowerLine.equals(blockedName.toLowerCase())) {
						isBlocked = true;
						break;
					}
				}

				if (isBlocked) {
					modified = true;
					this.log("Removing blocked name from file: " + trimmed);
				} else if (!trimmed.isEmpty()) {
					if (!uniqueLines.add(line)) {
						modified = true;
					}
				}
			}

			if (modified) {
				Files.write(fileLocation, uniqueLines, StandardCharsets.UTF_8);
				this.log("nslist.txt updated: removed blocked names and/or duplicates.");
			}
		} catch (Exception e) {
			this.log("Error in removeBlockedNamesFromFile: " + e);
		}
	}

	private void loadList() {
		Path path = get(this.filePath);
		if (!Files.exists(path)) {
			this.log("File not found: " + this.filePath);
		} else {
			try {
				removeBlockedNamesFromFile(this.filePath);
				removeDuplicateNames(this.filePath);
				List<String> lines = readLines(path);

				Set<String> trimmed = lines.stream()
						.map(String::trim)
						.filter(s -> !s.isEmpty() && !s.startsWith("#"))
						.map(String::toLowerCase)
						.collect(Collectors.toSet());

				this.targets.clear();
				this.targets.addAll(trimmed);
				this.log("Successfully loaded " + this.attackSet.size() + " names from nslist.txt.");
			} catch (IOException e) {
				this.log("Error reading file: " + e.getMessage());
			}
		}
	}

	private void readMainUserFile() {
		this.attackSet.clear();

		Path path = get(this.filePath);
		if (!Files.exists(path)) {
			return;
		}

		try {
			removeBlockedNamesFromFile(this.filePath);
			List<String> lines = readLines(path);

			this.attackSet = lines.stream()
					.map(String::trim)
					.filter((name) -> !name.isEmpty())
					.map(String::toLowerCase)
					.collect(Collectors.toSet());
		} catch (IOException var7) {
			this.log("Error reading main user file: " + var7.getMessage());
		}
	}

	private void checkEquipmentTab() {
		if (!Rs2Tab.switchTo(InterfaceTab.EQUIPMENT)) return;
		this.sleepWithFatigue(300, 800);
		Rs2ItemModel weapon = Rs2Equipment.get(EquipmentInventorySlot.WEAPON);
		if (weapon != null) {
			Point p = getInventoryItemClickPoint(weapon);
			if (p != null) {
				naturalMouseMovement(p.x, p.y);
			}
			this.sleepWithFatigue(500, 1500);
		}
		this.status = "Checking equipment";
		this.log(this.status);
		if (Rs2Random.between(0, 100) < 30) {
			Rs2Tab.switchTo(InterfaceTab.INVENTORY);
		}
	}



	private void emoteTabHover() {
		if (!Rs2Tab.switchTo(InterfaceTab.EMOTES)) return;
		this.sleepWithFatigue(400, 1000);
		this.status = "Browsing emotes";
		this.log(this.status);
		this.sleepWithFatigue(600, 1500);
		Rs2Tab.switchTo(InterfaceTab.INVENTORY);
	}

	private void takeForcedBreak() {
		this.forcedBreakPending = false;
		long runtime = System.currentTimeMillis() - this.fatigueStart;
		double fatigueFactor = this.getFatigueFactor(runtime);
		double fatigueMult = 1.0 + fatigueFactor * 0.05;
		long breakDuration = (long) (Rs2Random.between((int) FORCED_BREAK_MIN_MS, (int) FORCED_BREAK_MAX_MS) * fatigueMult);
		breakDuration = Math.min(breakDuration, 900000L);
		this.status = "Taking a break";
		this.log("Starting forced break for " + (breakDuration / 1000L) + " seconds (fatigue: " + String.format("%.2f", fatigueFactor) + ")");
		sendDiscordEmbed("Forced " + (breakDuration / 1000L / 60L) + "min break ~ ");
		Rs2PlayerModel local = Rs2Player.getLocalPlayer();
		WorldPoint localTile = local == null ? null : local.getWorldLocation();
		if (Rs2Random.between(0, 100) < 30 && localTile != null && this.AT_LOBBY.contains(localTile)) {
			Rs2Player.logout();
		}
		long breakEnd = System.currentTimeMillis() + breakDuration;
		while (System.currentTimeMillis() < breakEnd) {
			int sleepTime = Rs2Random.between(5000, 15000);
			this.sleepWithFatigue(sleepTime, sleepTime + 2000);
            Rs2Random.between(0, 100);
            this.status = "Forced break: " + ((breakEnd - System.currentTimeMillis()) / 1000L) + "s left";
		}
		double recoveryFactor = 0.6 + (fatigueFactor / 20.0) * 0.2;
		this.fatigueStart += (long) (breakDuration * recoveryFactor);
		this.energyBoost = Math.min(2.5, this.energyBoost + fatigueFactor * 0.0375);
		this.fatigueSpikeEndTime = 0L;
		this.fatigueSpikeMagnitude = 0.0;
		this.sessionFatiguePeak *= 0.5;
		this.forcedBreakUntilMs = System.currentTimeMillis() + Rs2Random.between(30000, 120000);
		this.lastForcedBreakMs = System.currentTimeMillis();
		this.sessionStartMs = System.currentTimeMillis();
		this.actionsSinceBreak = 0;
		if (Microbot.isLoggedIn()) {
			this.status = "Break over, returning";
			this.log("Forced break finished, resuming");
		} else {
			this.log("Break over, logging back in");
		}
	}

	private Point getInventoryItemClickPoint(Rs2ItemModel item) {
		if (item == null) return null;
		Rectangle b = Rs2Inventory.itemBounds(item);
		if (b == null) return null;
		return new Point(b.x + b.width / 2, b.y + b.height / 2);
	}

	private Point getClickablePointForObject(net.runelite.api.TileObject obj) {
		if (obj == null) return null;
		java.awt.Shape poly = Microbot.getClientThread().runOnClientThreadOptional(() -> tileObjectCanvasTilePoly(obj)).orElse(null);
		if (poly == null) return null;
		Rectangle b = poly.getBounds();
		return new Point(b.x + b.width / 2, b.y + b.height / 2);
	}

	private static java.awt.Shape tileObjectCanvasTilePoly(net.runelite.api.TileObject obj) {
		return obj.getCanvasTilePoly();
	}

	private static Stream<Rs2PlayerModel> getPlayers(Predicate<Rs2PlayerModel> predicate) {
		final Player localPlayer = Microbot.getClient().getLocalPlayer();
		return Microbot.getRs2PlayerCache().getStream()
				.map(p -> new Rs2PlayerModel(p.getPlayer()))
				.filter(p -> p.getPlayer() != localPlayer)
				.filter(predicate);
	}

	private static boolean tileObjectHasAction(Rs2TileObjectModel obj, String action) {
		if (obj == null || action == null) return false;
		final ObjectComposition comp = obj.getObjectComposition();
		if (comp == null || comp.getActions() == null) return false;
		final String needle = action.toLowerCase(Locale.ROOT);
		return Arrays.stream(comp.getActions()).filter(Objects::nonNull)
				.anyMatch(a -> a.toLowerCase(Locale.ROOT).contains(needle));
	}

	private static String tileObjectName(Rs2TileObjectModel obj) {
		if (obj == null) return null;
		final String name = obj.getName();
		if (name == null || name.equals("null")) return null;
		return name;
	}

	private void ensureAutoRetaliateOff() {
		if (autoRetaliateOffVerified) return;
		try {
			if (Rs2Tab.switchTo(InterfaceTab.COMBAT)) {
				Global.sleep(Rs2Random.between(300, 800));
				for (int childIdx : AUTO_RETALIATE_CANDIDATE_CHILDS) {
					Widget w = Rs2Widget.getWidget(COMBAT_WIDGET_PARENT);
					if (w == null) continue;
					Widget wc = w.getChild(childIdx);
					if (wc != null && !wc.isHidden()) {
						String[] actions = wc.getActions();
						if (actions != null) {
							for (String action : actions) {
								if (action != null && action.toLowerCase(Locale.ROOT).contains("auto")) {
									Rs2Widget.clickWidget(wc);
									Global.sleep(Rs2Random.between(200, 500));
									autoRetaliateOffVerified = true;
									this.log("Auto-retaliate toggled off via widget " + childIdx);
									return;
								}
							}
						}
					}
				}
				Rs2Tab.switchTo(InterfaceTab.INVENTORY);
			}
		} catch (Exception e) {
			this.log("Could not toggle auto-retaliate: " + e.getMessage());
		} finally {
			nextAutoRetaliateCheckMs = System.currentTimeMillis() + AUTO_RETALIATE_CHECK_INTERVAL_MS;
		}
	}

	private void dismissRandomEvents() {
		long now = System.currentTimeMillis();
		if (now < nextRandomEventCheckMs) return;
		nextRandomEventCheckMs = now + RANDOM_EVENT_CHECK_INTERVAL_MS;
		for (int parent : RANDOM_EVENT_DIALOG_PARENTS) {
			try {
				Widget w2 = Rs2Widget.getWidget(parent);
				if (w2 == null) continue;
				Widget continueBtn = w2.getChild(RANDOM_EVENT_CONTINUE_CHILD);
				if (continueBtn != null && !continueBtn.isHidden()) {
					String txt = continueBtn.getText();
					if (txt != null && (txt.toLowerCase(Locale.ROOT).contains("click here to continue") || txt.toLowerCase(Locale.ROOT).contains("continue"))) {
						Rs2Widget.clickWidget(continueBtn);
						Global.sleep(Rs2Random.between(500, 1500));
						this.log("Dismissed random event dialog (widget " + parent + ")");
						sessionTracker.recordAntiBan();
						return;
					}
				}
			} catch (Exception ignored) {}
		}
		try {
			Widget w231 = Rs2Widget.getWidget(231);
			if (w231 == null) return;
			Widget npcDialog = w231.getChild(0);
			if (npcDialog != null && !npcDialog.isHidden()) {
				Widget npcName = w231.getChild(1);
				if (npcName != null) {
					String nm = npcName.getText();
					String name = nm == null ? "" : nm.toLowerCase(Locale.ROOT);
					if (name.contains("mysterious") || name.contains("drill") || name.contains("genie")
							|| name.contains("captain") || name.contains("beekeeper") || name.contains("gravedigger")
							|| name.contains("sandwich") || name.contains("frog") || name.contains("mime")
							|| name.contains("quiz") || name.contains("demon") || name.contains("pillory")
							|| name.contains("maze") || name.contains("strange")) {
						Widget cont = w231.getChild(3);
						if (cont != null && !cont.isHidden()) {
							Rs2Widget.clickWidget(cont);
							Global.sleep(Rs2Random.between(400, 1200));
							this.log("Dismissed random event: " + nm);
							sessionTracker.recordAntiBan();
						}
					}
				}
			}
		} catch (Exception ignored) {}
	}

	private void handleAttackerRetaliation(Rs2PlayerModel localPlayer) {
		if (!playerExists(localPlayer)) return;
		if (this.isAttacking()) return;
		Player attacker = getCharacterInteractingWithMe();
		if (!playerExists(attacker) || attacker.getName() == null) return;
		String attackerKey = attacker.getName().toLowerCase(Locale.ROOT).trim();

		WorldPoint localTile = localPlayer.getWorldLocation();
		boolean isInSafeZone = localTile != null && this.SAFE_ZONE.contains(localTile);
		long fatigueRuntime = System.currentTimeMillis() - fatigueStart;
		double fatigueFactor = this.getFatigueFactor(fatigueRuntime);
		boolean prayersAvailable = Rs2Player.getBoostedSkillLevel(Skill.PRAYER) > 0;

		AIAssistant.SuggestedReaction suggestion = aiAssistant.suggestReaction(
				attacker, localPlayer, isInSafeZone, prayersAvailable, fatigueFactor, this.attackSet);

		switch (suggestion.reaction) {
			case DRAG_TO_SAFE:
				if (!isInSafeZone && !noDragEnabled) {
					addLureBotToDragList(attacker.getName());
					ensureAutoRetaliateOff();
					this.status = "AI: Drag to safe - " + suggestion.reason;
					lureDragToSafe(new Rs2PlayerModel(attacker), localPlayer);
					return;
				}
				if (noDragEnabled) {
					this.status = "AI: No Drag - fighting " + attacker.getName();
				} else {
					this.status = "AI: Lure bot in safe, ignoring: " + attacker.getName();
					return;
				}
			case FIGHT_BACK:
				if (isPlayerInCombat(localPlayer)) return;
				this.status = "AI: Retaliating against " + attacker.getName() + " - " + suggestion.reason;
				long delay = aiAssistant.calculateHumanReactionDelay(fatigueFactor, true);
				this.sleepWithFatigue((int) delay, (int) (delay + 200));
				if (playerExists(attacker) && !isPlayerInCombat(localPlayer)) {
					boolean attacked = rightClickInteract(attacker);
					if (attacked) {
						this.log("AI retaliation on " + attacker.getName());
						sessionTracker.recordAttack();
						playerStats.recordAttack(attacker.getName());
						String[] gear = aiAssistant.generateWeaponList(attacker);
						aiAssistant.learnFromEncounter(attacker.getName(), attacker.getCombatLevel(), gear, 99, true, false, true);
					}
				}
				return;
			case PRAY_MAGIC:
			case PRAY_MELEE:
			case PRAY_RANGED:
				int prayerPoints = Rs2Player.getBoostedSkillLevel(Skill.PRAYER);
				if (prayersAvailable) {
					if (suggestion.reaction == AIAssistant.Reaction.PRAY_MAGIC) this.handleMagicAttack(prayerPoints);
					else if (suggestion.reaction == AIAssistant.Reaction.PRAY_MELEE) this.handleMeleeAttack(prayerPoints);
					else this.handleRangeAttack(prayerPoints);
				}
				break;
			case IGNORE:
			default:
				break;
		}

		boolean theyRanAway = !isPlayerInCombat(localPlayer);
		if (attacker.getCombatLevel() >= MIN_AUTO_ADD_ATTACKER_LEVEL) {
			aiAssistant.recordAttackedBy(attacker.getName(), theyRanAway);
			if (aiAssistant.isRepeatedLowLevelAttacker(attackerKey) && !theyRanAway && !this.attackSet.contains(attackerKey)) {
				if (!clanMembers.contains(attackerKey)) {
					this.addNameToList(attacker.getName());
					this.log("Auto-added persistent low-level attacker to nslist: " + attacker.getName());
				}
			}
		}
	}

	private Player getCharacterInteractingWithMe() {
		Rs2PlayerModel local = Rs2Player.getLocalPlayer();
		if (local == null) return null;
		Player localPlayer = local.getPlayer();
		if (localPlayer == null) return null;
		return getPlayers(p -> p != null && p != local
				&& p.getInteracting() instanceof Player
				&& p.getInteracting() == localPlayer).findFirst().orElse(null);
	}

	public void resetFatigue() {
		long now = System.currentTimeMillis();
		this.fatigueStart = now;
		this.fatigueSpikeEndTime = 0L;
		this.fatigueSpikeMagnitude = 0.0;
		this.performanceStreakEndTime = 0L;
		this.performanceStreakBoost = 0.0;
		this.energyBoost = 0.0;
		this.lastEnergyBoostTime = now;
		this.nextEnergyBoostCheck = now + 60000;
		this.nextFatigueCheck = now + 60000;
		this.nextUltradianUpdateMs = now + Rs2Random.between(600000, 1200000);
		this.fatigueSpikeCheckTime = now + 180000;
		this.sessionFatiguePeak = 0.0;
		this.actionsSinceBreak = 0;
		this.forcedBreakPending = false;
		this.forcedBreakUntilMs = 0L;
		this.nextAfkBreakTime = now + Rs2Random.between(1800000, 3600000);
		this.status = "Fatigue reset";
		this.log("Fatigue manually reset");
	}

	public void reloadNsList() {
		this.reloadNsListRequested = true;
	}

	private void reloadNsListNow() {
		this.readMainUserFile();
		this.loadList();
		this.nextAttackListReloadAtMs = System.currentTimeMillis() + ATTACK_LIST_RELOAD_INTERVAL_MS;
		this.status = "Reloaded nslist (" + this.attackSet.size() + " names)";
		this.log("Manually reloaded nslist.txt: " + this.attackSet.size() + " names");
	}

	public int getTotalActions() {
		return humanBehavior.getTotalActions();
	}

	public long getSessionDurationMinutes() {
		return humanBehavior.getSessionDurationMinutes();
	}
}
