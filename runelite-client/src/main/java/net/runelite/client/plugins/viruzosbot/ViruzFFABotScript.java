package net.runelite.client.plugins.viruzosbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.runelite.api.Actor;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.widgets.Widget;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.misc.Rs2UiHelper;
import net.runelite.client.plugins.microbot.util.mouse.Mouse;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.player.Rs2PlayerModel;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.nio.file.Paths.get;

public class ViruzFFABotScript extends Script
{
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ViruzFFABotScript.class);
	private static final String VERSION = "1.0.0";
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
	private final Rectangle attackBox = new Rectangle(10, 70, 14, 14);
	private final Rectangle walkToTargetBox = new Rectangle(10, 90, 14, 14);
	private final Rectangle quickprayerBox = new Rectangle(10, 110, 14, 14);
	private final Rectangle prayerOffBox = new Rectangle(10, 130, 14, 14);
	private final Rectangle switchPrayersBox = new Rectangle(10, 150, 14, 14);
	private final Rectangle freeRoamBox = new Rectangle(10, 170, 14, 14);
	private final Rectangle protectBluePhatsBox = new Rectangle(10, 190, 14, 14);
	private final Rectangle reportBox = new Rectangle(10, 210, 14, 14);
	private final Rectangle chatGrabberBox = new Rectangle(10, 230, 14, 14);
	private final Rectangle focusPlayersBox = new Rectangle(10, 250, 14, 14);
	private final Rectangle lureMultiBox = new Rectangle(10, 270, 14, 14);
	private final Rectangle scanHillsBox = new Rectangle(10, 290, 14, 14);
	private final Rectangle noDragBox = new Rectangle(10, 310, 14, 14);
	private final Rectangle unlistBox = new Rectangle(10, 330, 14, 14);
	private final Rectangle whitelistBox = new Rectangle(10, 350, 14, 14);
	private final Rectangle purgeListButton = new Rectangle(12, 0, 85, 16);
	private final Rectangle reEnergizeButton = new Rectangle(102, 0, 85, 16);
	private final Rectangle locationComboBox = new Rectangle(10, 40, 110, 20);
	private final Rectangle hideGuiButton = new Rectangle(236, 4, 20, 20);
	private final Rectangle guiToggleButton = new Rectangle(0, 0, 30, 30);
	private boolean guiHidden = false;
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
	private boolean attackEnabled = true;
	private boolean walkToTargetEnabled = false;
	private boolean quickprayersEnabled = false;
	private boolean prayerOffEnabled = false;
	private boolean switchPrayersEnabled = false;
	private boolean freeRoamEnabled = false;
	private boolean protectBluePhatsEnabled = false;
	private boolean reportEnabled = false;
	private boolean comboBoxExpanded = false;
	private int antiLureScan = 0;
	private int ffaScanMisses = 0;
	private int antiLureFails = 0;
	public volatile String status = "Paused";
	private String selectedLocation = "None selected";
	private int currentThreshold = getRandomThreshold();
	private WorldArea currentArea = null;
	private long lastSelectedLocationWalkFinishedAtMs = 0L;
	private Timer timer2;
	private final Timer arrowCheckTimer = new Timer(0L);
	private boolean isAttacking = false;
	private Set<String> attackSet = new HashSet<>();
	private final Set<String> clanMembers = new HashSet<>();
	private final Set<String> lureDragSet = new HashSet<>();
	private long nextActionTime;
	private long fatigueStart = System.currentTimeMillis();
	private int actionsSinceBreak = 0;
	private double ultradianPhase = 0.0;
	private long fatigueSpikeEndTime = 0L;
	private double fatigueSpikeMagnitude = 0.0;
	private long performanceStreakEndTime = 0L;
	private double performanceStreakBoost = 0.0;
	private boolean forcedBreakPending = false;
	private long nextFatigueCheck = System.currentTimeMillis() + 60000;
	private long nextUltradianUpdateMs = System.currentTimeMillis() + Rs2Random.between(600000, 1200000);
	private long fatigueSpikeCheckTime = System.currentTimeMillis() + 180000;
	private double sessionFatiguePeak = 0.0;
	private final Set<String> BLOCKED_NAMES = Set.of("Viruz SO", "Zoloroc", "vRolts", "Hans Taween");
	private final List<String> authorizedUsers = new ArrayList<>();
	private final String filePath = Config.getNsListFilePath();
	private static final String BLUE_PHAT_NAME = "Blue partyhat";
	private static final String STAFF_OF_AIR = "Staff of air";
	private final Set<String> targets = new HashSet<>();
	private final Set<String> bluePhatNames = new HashSet<>();
	private final Map<String, WorldArea> areaMap = new HashMap<>();
	private final List<String> locationOptions = Arrays.asList("None selected", "Portal", "Hills single", "Lava single", "Pond single", "Middle", "Hills", "Pond", "Lava");
	private static final Set<Integer> MAGIC_ATTACK_ANIMATIONS = Set.of(1162, 1163, 1164, 1165, 1978, 1979);
	private long nextAfkBreakTime = System.currentTimeMillis() + Rs2Random.between(1800000, 3600000);
	private static final Set<Integer> RANGE_ATTACK_ANIMATIONS = Set.of(426, 4230, 929);
	private static final Set<Integer> MELEE_ATTACK_ANIMATIONS = Set.of(390, 386, 388, 400, 401, 407, 406, 393, 395, 412, 422, 419);
	private static final String[] MELEE_WEAPONS = new String[]{"Rune scimitar", "Rune sword", "Rune longsword", "Rune axe", "Rune pickaxe", "Rune mace", "Rune warhammer", "Rune battleaxe", "Rune dagger", "Gilded scimitar", "Gilded spade", "Gilded pickaxe", "Gilded axe"};
	private static final String[] MELEE_SHIELDS = new String[]{"Rune kiteshield", "Rune kiteshield (t)", "Rune kiteshield (g)", "Saradomin kiteshield", "Zamorak kiteshield", "Bandos kiteshield", "Armadyl kiteshield", "Guthix kiteshield", "Ancient kiteshield", "Gilded kiteshield", "Rune shield (h1)", "Rune shield (h2)", "Rune shield (h3)", "Rune shield (h4)", "Rune shield (h5)"};
	private static final String[] MELEE_BODY = new String[]{"Rune platebody", "Rune platebody (t)", "Rune platebody (g)", "Saradomin platebody", "Zamorak platebody", "Bandos platebody", "Armadyl platebody", "Guthix platebody", "Ancient platebody", "Gilded platebody", "Rune platebody (h1)", "Rune platebody (h2)", "Rune platebody (h3)", "Rune platebody (h4)", "Rune platebody (h5)"};
	private static final String[] MELEE_LEGS = new String[]{"Rune platelegs", "Rune platelegs (t)", "Rune platelegs (g)", "Saradomin platelegs", "Zamorak platelegs", "Bandos platelegs", "Armadyl platelegs", "Guthix platelegs", "Ancient platelegs", "Gilded platelegs", "Rune platelegs (h1)", "Rune platelegs (h2)", "Rune platelegs (h3)", "Rune platelegs (h4)", "Rune platelegs (h5)"};
	private static final String[] RANGED_WEAPONS = new String[]{"Maple shortbow"};
	private static final String[] RANGED_BODY = new String[]{"Gilded d'hide body", "Green d'hide body", "Green d'hide body (g)", "Green d'hide body (t)"};
	private static final String[] RANGED_CHAPS = new String[]{"Gilded d'hide chaps", "Green d'hide chaps", "Green d'hide chaps (g)", "Green d'hide chaps (t)"};

	private static final long CHAT_GRAB_COOLDOWN_MS = 2500L;
	private final AtomicBoolean chatGrabberEnabled = new AtomicBoolean(false);
	private boolean focusPlayersEnabled = false;
	private boolean lureMultiEnabled = false;
	private boolean hillScanActive = false;
	private boolean hillScanEnabled = false;
	private boolean noDragEnabled = false;
	private boolean unlistEnabled = false;
	private boolean whitelistEnabled = false;
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
	private double energyBoost = 0.0;
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
	private final AIAssistant aiAssistant = new AIAssistant();
	private static final int SCREEN_WIDTH = 765;
	private static final int SCREEN_HEIGHT = 503;
	private long lastNaturalMouseMove = 0L;
	private static final long NATURAL_MOVE_INTERVAL_MS = 30000;
	private long sessionStartTime = System.currentTimeMillis();
	private int totalActions;
	private int currentFatigueLevel;
	private long nextDistractionTime;
	private final SessionTracker sessionTracker = new SessionTracker();
	private final NonTargetTracker nonTargetTracker = new NonTargetTracker();
	private final PlayerStats playerStats = new PlayerStats();
	private long nextNonTargetScanMs = 0L;
	private long noValidTargetCooldownUntilMs = 0L;

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

	private final Map<String, WorldPoint> playerLastLocations = new HashMap<>();

	public enum ChatEventType {
		CLAN_CHAT, CHAT_CHANNEL, PRIVATE_INFO, OTHER
	}

	public void handleChatMessage(ChatMessage message) {
		if (message == null) return;
		final String senderRaw = message.getName();
		if (senderRaw == null) return;
		final String sender = senderRaw.replace('\u00A0', ' ');
		final String raw = message.getMessage();
		final ChatEventType type = resolveType(message);
		if (type == ChatEventType.CLAN_CHAT) {
			clanMembers.add(sender.toLowerCase(Locale.ROOT).trim());
		}
		final boolean relevantType = type == ChatEventType.CLAN_CHAT
				|| type == ChatEventType.CHAT_CHANNEL
				|| type == ChatEventType.PRIVATE_INFO;
		if (relevantType && isAuthorized(sender) && raw != null) {
			final String text = raw.trim().toLowerCase(Locale.ROOT);
			if (text.equals("!grab on")) {
				chatGrabberEnabled.set(true);
				log.info("Chat grabber: ON");
				sendDiscordEmbed("📥 Chat grabber: **ON**");
				return;
			}
			if (text.equals("!grab off")) {
				chatGrabberEnabled.set(false);
				log.info("Chat grabber: OFF");
				sendDiscordEmbed("📥 Chat grabber: **OFF**");
				return;
			}
			if (text.startsWith("!setthreshold ")) {
				try {
					int val = Integer.parseInt(raw.substring(14).trim());
					this.setCurrentThreshold(val);
					log.info("Threshold set to: " + val);
					sendDiscordEmbed("⚙️ Threshold set to: **" + val + " ms**");
				} catch (NumberFormatException e) {
					log.info("Invalid threshold value.");
				}
				return;
			}
			if (text.equals("!getthreshold")) {
				int val = this.getCurrentThreshold();
				log.info("Current threshold: " + val);
				sendDiscordEmbed("⚙️ Current threshold: **" + val + " ms**");
				return;
			}
			if (text.startsWith("!add ")) {
				final String nameToAdd = raw.substring(5).trim();
				if (!nameToAdd.isEmpty()) {
					addNameToList(nameToAdd);
				}
				return;
			}
			if (text.startsWith("!del ")) {
				final String nameToDel = raw.substring(5).trim();
				if (!nameToDel.isEmpty()) {
					removeNameFromList(nameToDel);
				}
				return;
			}
			if (text.startsWith("!setarea ")) {
				String areaStr = raw.substring(9).trim();
				if (!areaStr.isEmpty()) {
					String[] parts = areaStr.split(" ");
					if (parts.length == 4) {
						try {
							int x1 = Integer.parseInt(parts[0]);
							int y1 = Integer.parseInt(parts[1]);
							int x2 = Integer.parseInt(parts[2]);
							int y2 = Integer.parseInt(parts[3]);
							this.currentArea = new WorldArea(x1, y1, Math.abs(x2 - x1) + 1, Math.abs(y2 - y1) + 1, 0);
							log.info("Target area set to: " + x1 + ", " + y1 + ", " + x2 + ", " + y2);
							sendDiscordEmbed("📍 Target area set to: **" + x1 + ", " + y1 + ", " + x2 + ", " + y2 + "**");
						} catch (NumberFormatException e) {
							log.info("Invalid area coordinates");
						}
					} else if (areaStr.equalsIgnoreCase("null") || areaStr.equalsIgnoreCase("off")) {
						this.currentArea = null;
						log.info("Target area cleared");
						sendDiscordEmbed("📍 Target area cleared");
					}
				}
				return;
			}
			if (text.startsWith("!say ")) {
				String sayText = raw.substring(5).trim();
				if (!sayText.isEmpty()) {
					Rs2Keyboard.typeString(sayText);
					Rs2Keyboard.enter();
				}
				return;
			}
			if (text.equals("!stats")) {
				String summary = sessionTracker.generateStatusSummary();
				String pStats = playerStats.generateStatusSummary();
				log.info(summary);
				log.info(pStats);
				sendDiscordEmbed("📊 " + summary + "\n📈 " + pStats);
				return;
			}
			if (text.startsWith("!profile ")) {
				String targetName = raw.substring(9).trim();
				if (!targetName.isEmpty()) {
					AIAssistant.PlayerProfile profile = aiAssistant.getPlayerProfiles().get(targetName.toLowerCase());
					if (profile != null) {
						String info = "Profile for " + targetName + ": threat=" + String.format("%.2f", profile.threatDisplay())
								+ " fights=" + profile.encounterCount + " foughtBack=" + profile.fightBackRatio();
						log.info(info);
						sendDiscordEmbed(info);
					} else {
						log.info("No profile found for " + targetName);
					}
				}
				return;
			}
			if (text.equals("!reset")) {
				sessionTracker.resetSession();
				log.info("Session stats reset");
				sendDiscordEmbed("🔄 Session stats reset");
				return;
			}
			if (text.startsWith("!unlist ")) {
				final String namesToUnlist = raw.substring(8).trim();
				if (!namesToUnlist.isEmpty()) {
					unlistNames(namesToUnlist);
				}
				return;
			}
		}
		grabChatMessage(message, type);
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

	private void loadAuthorizedUsers() {
		Path path = get(Config.getBluePhatsFilePath());
		authorizedUsers.clear();
		if (!Files.exists(path)) {
			log.info("Authorization file not found at " + path + ". Remote commands disabled.");
			return;
		}
		try {
			String json = Files.readString(path, StandardCharsets.UTF_8);
			Gson gson = new Gson();
			Type type = new TypeToken<List<String>>(){}.getType();
			List<String> loaded = gson.fromJson(json, type);
			if (loaded != null) {
				for (String name : loaded) {
					if (name != null) {
						authorizedUsers.add(name.toLowerCase(Locale.ROOT).replace("\u00A0", " ").trim());
					}
				}
				log.info("Loaded " + authorizedUsers.size() + " authorized users from bluephats.json.");
			}
		} catch (IOException e) {
			log.info("Failed to parse bluephats.json: " + e.getMessage());
		}
	}

	private boolean isAuthorized(String sender) {
		if (sender == null || authorizedUsers.isEmpty()) {
			return false;
		}
		String cleanSender = sender.toLowerCase(Locale.ROOT).replace("\u00A0", " ").trim();
		return authorizedUsers.contains(cleanSender);
	}

	private void grabChatMessage(ChatMessage message, ChatEventType type) {
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
		String playerName = "";
		Rs2PlayerModel local = Rs2Player.getLocalPlayer();
		if (local != null && local.getName() != null) {
			playerName = local.getName();
		}
		String runtime = this.timer2 != null ? this.timer2.formatTime() : "00:00:00";
		DiscordWebhook.sendEmbed(playerName, VERSION, this.status, runtime, event);
	}

	private void saveListToFile() {
		File file = new File(Config.getNsListFilePath());
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
			for (String name : this.attackSet) {
				bw.write(name);
				bw.newLine();
			}
			log.info("Successfully saved " + this.attackSet.size() + " names to nslist.txt.");
		} catch (IOException e) {
			log.info("Error saving nslist.txt: " + e.getMessage());
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
				log.info("Loaded " + this.bluePhatNames.size() + " blue phat players.");
			}
		} catch (Exception e) {
			log.info("Error loading bluephats.json: " + e.getMessage());
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
			log.info("Saved " + this.bluePhatNames.size() + " blue phat players to bluephats.json.");
		} catch (Exception e) {
			log.info("Error saving bluephats.json: " + e.getMessage());
		}
	}

	private void addNameToList(String name) {
		String lowerName = name.toLowerCase().trim();
		for (String blocked : BLOCKED_NAMES) {
			if (blocked.equalsIgnoreCase(lowerName)) {
				log.info("Attempted to add blocked name: " + name + ". Ignoring.");
				return;
			}
		}
		if (clanMembers.contains(lowerName)) {
			log.info("Refusing to add clan member to nslist: " + name);
			return;
		}
		if (this.attackSet.contains(lowerName)) {
			log.info("Name already in list: " + name);
			return;
		}
		this.attackSet.add(lowerName);
		this.targets.add(lowerName);
		saveListToFile();
		log.info("Added " + name + " to nslist.txt");
		sendDiscordEmbed("📝 Added **" + name + "** to nslist.txt");
	}

	private void addLureBotToDragList(String name) {
		if (name == null) return;
		String lowerName = name.toLowerCase(Locale.ROOT).trim();
		if (lowerName.isEmpty()) return;
		if (lureDragSet.contains(lowerName)) return;
		lureDragSet.add(lowerName);
		saveLureDragSet();
		log.info("Added " + name + " to luredrag.txt");
		sendDiscordEmbed("Added **" + name + "** to luredrag.txt");
	}

	private void saveLureDragSet() {
		try {
			Path path = get(Config.getLureDragFilePath());
			Path parent = path.getParent();
			if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
			Files.write(path, lureDragSet, StandardCharsets.UTF_8);
			log.info("Saved " + lureDragSet.size() + " names to luredrag.txt.");
		} catch (IOException e) {
			log.info("Error saving luredrag.txt: " + e.getMessage());
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
			log.info("Loaded " + lureDragSet.size() + " names from luredrag.txt.");
		} catch (IOException e) {
			log.info("Error loading luredrag.txt: " + e.getMessage());
		}
	}

	private void removeNameFromList(String name) {
		final String lowerName = name.toLowerCase(Locale.ROOT).trim();
		if (lowerName.isEmpty()) return;
		if (this.attackSet.remove(lowerName)) {
			this.targets.remove(lowerName);
			saveListToFile();
			log.info("Removed " + name + " from nslist.txt");
			sendDiscordEmbed("🗑️ Removed **" + name + "** from nslist.txt");
		} else {
			log.info("Name not found in list: " + name);
		}
	}

	private void removeNameFromAiData(String name) {
		boolean removed = aiAssistant.removeProfile(name);
		if (removed) {
			log.info("Removed " + name + " from ai_data.json");
		} else {
			log.info(name + " not found in ai_data.json");
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
		log.info("Unlist complete. Removed: " + removedNames);
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
		log.info("Whitelist complete. Added: " + addedNames);
		sendDiscordEmbed("⚪ Whitelisted: **" + addedNames + "**");
	}

	private boolean isTargetWearingBluePartyhat(Player target) {
		if (target == null) return false;
		try {
			Map<net.runelite.api.kit.KitType, Integer> equipped = Rs2Player.getPlayerEquipmentIds(new Rs2PlayerModel(target));
			if (equipped == null) return false;
			for (Map.Entry<net.runelite.api.kit.KitType, Integer> e : equipped.entrySet()) {
				net.runelite.api.ItemComposition comp = Microbot.getClientThread().runOnClientThreadOptional(() ->
						Microbot.getClient().getItemDefinition(e.getValue())).orElse(null);
				if (comp != null && comp.getName() != null && comp.getName().equalsIgnoreCase(BLUE_PHAT_NAME)) {
					return true;
				}
			}
		} catch (Exception e) {
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
		log.info("BluePhat detected and handled: " + rawName);
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
	private void setCurrentThreshold(int val) { this.currentThreshold = val; }
	private int getCurrentThreshold() { return this.currentThreshold; }

	private long lastInCombatMs = 0L;
	private boolean wasInFfaArea = false;

	@Override
	public boolean run() {
		startSession();
		mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
			try {
				if (!Microbot.isLoggedIn()) return;
				if (!super.run()) return;
				onLoop();
			} catch (Exception ex) {
				log.info("Error in main loop: " + ex.getMessage());
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
		loadAuthorizedUsers();
		this.timer2 = new Timer();
		initializeAreaMap();
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
		sendDiscordEmbed("✅ Script started");
	}

	private int getRandomThreshold() {
		return ThreadLocalRandom.current().nextInt(5000, 10001);
	}

	public void handleMouseClick(Point point) {
		if (this.guiHidden) {
			if (this.guiToggleButton.contains(point)) {
				this.guiHidden = false;
				this.log("GUI shown");
			}
			return;
		}
		if (this.selectedLocation == null) {
			return;
		}
		if (this.hideGuiButton.contains(point)) {
			this.guiHidden = true;
			this.log("GUI hidden");
			return;
		}
		if (this.locationComboBox.contains(point)) {
			this.comboBoxExpanded = !this.comboBoxExpanded;
			this.log(this.comboBoxExpanded ? "Location dropdown expanded" : "Location dropdown collapsed");
		} else if (this.comboBoxExpanded) {
			for (int i = 0; i < this.locationOptions.size(); ++i) {
				int optionY = this.locationComboBox.y + (i + 1) * 20;
				Rectangle optionRect = new Rectangle(this.locationComboBox.x, optionY, this.locationComboBox.width, 20);
				if (optionRect.contains(point)) {
					this.selectedLocation = this.locationOptions.get(i);
					this.currentArea = this.areaMap.get(this.selectedLocation);
					this.log("Selected location: " + this.selectedLocation);
					this.comboBoxExpanded = false;
					return;
				}
			}
			this.comboBoxExpanded = false;
		} else if (this.attackBox.contains(point)) {
			this.setAttackEnabled(!this.isAttackEnabled());
			String state = this.isAttackEnabled() ? "On" : "Off";
			this.log("Attack is set to " + state);
		} else if (this.walkToTargetBox.contains(point)) {
			this.setWalkToTargetEnabled(!this.isWalkToTargetEnabled());
			String state = this.isWalkToTargetEnabled() ? "On" : "Off";
			this.log("Walk to target is set to " + state);
		} else if (this.quickprayerBox.contains(point)) {
			this.setQuickprayersEnabled(!this.isQuickprayersEnabled());
			String state = this.isQuickprayersEnabled() ? "On" : "Off";
			this.log("Prayer is set to " + state);
		} else if (this.prayerOffBox.contains(point)) {
			this.setPrayerOffEnabled(!this.isPrayerOffEnabled());
			String state = this.isPrayerOffEnabled() ? "On" : "Off";
			this.log("Turn prayer off if no target is set to " + state);
		} else if (this.switchPrayersBox.contains(point)) {
			this.setSwitchPrayersEnabled(!this.isSwitchPrayersEnabled());
			String state = this.isSwitchPrayersEnabled() ? "On" : "Off";
			this.log("Switch prayers is set to " + state);
		} else if (this.freeRoamBox.contains(point)) {
			this.setFreeRoamEnabled(!this.isFreeRoamEnabled());
			String state = this.isFreeRoamEnabled() ? "On" : "Off";
			this.log("Free roam is set to " + state);
		} else if (this.protectBluePhatsBox.contains(point)) {
			this.setProtectBluePhatsEnabled(!this.isProtectBluePhatsEnabled());
			String state = this.isProtectBluePhatsEnabled() ? "On" : "Off";
			this.log("Protect BluePhats is set to " + state);
		} else if (this.reportBox.contains(point)) {
			this.setReportEnabled(!this.isReportEnabled());
			String state = this.isReportEnabled() ? "On" : "Off";
			this.log("Report is set to " + state);
		} else if (this.chatGrabberBox.contains(point)) {
			this.chatGrabberEnabled.set(!this.chatGrabberEnabled.get());
			String state = this.chatGrabberEnabled.get() ? "On" : "Off";
			this.log("Chat grabber is set to " + state);
		} else if (this.focusPlayersBox.contains(point)) {
			this.focusPlayersEnabled = !this.focusPlayersEnabled;
			if (this.focusPlayersEnabled) {
				SwingUtilities.invokeLater(() -> {
					String input = JOptionPane.showInputDialog(null,
							"Enter player name(s) to focus (comma-separated):",
							"Focus Players", JOptionPane.PLAIN_MESSAGE);
					if (input != null && !input.trim().isEmpty()) {
						focusPlayerNames.clear();
						for (String name : input.split(",")) {
							String trimmed = name.trim().toLowerCase(Locale.ROOT);
							if (!trimmed.isEmpty()) {
								focusPlayerNames.add(trimmed);
							}
						}
						this.log("Focus players set to: " + String.join(", ", focusPlayerNames));
						sendDiscordEmbed("🎯 Focus players set to: **" + String.join(", ", focusPlayerNames) + "**");
					} else {
						focusPlayersEnabled = false;
					}
				});
			} else {
				focusPlayerNames.clear();
				this.log("Focus players disabled");
			}
		} else if (this.lureMultiBox.contains(point)) {
			this.lureMultiEnabled = !this.lureMultiEnabled;
			if (this.lureMultiEnabled) {
				this.noDragEnabled = false;
			}
			String state = this.lureMultiEnabled ? "On" : "Off";
			this.log("Lure Multi is set to " + state);
		} else if (this.scanHillsBox.contains(point)) {
			if (!this.hillScanActive) {
				this.hillScanEnabled = true;
				this.log("Scan Hills requested");
			}
		} else if (this.noDragBox.contains(point)) {
			this.noDragEnabled = !this.noDragEnabled;
			if (this.noDragEnabled) {
				this.lureMultiEnabled = false;
			}
			String state = this.noDragEnabled ? "On" : "Off";
			this.log("No Drag is set to " + state);
		} else if (this.unlistBox.contains(point)) {
			this.unlistEnabled = !this.unlistEnabled;
			if (this.unlistEnabled) {
				SwingUtilities.invokeLater(() -> {
					String input = JOptionPane.showInputDialog(null,
							"Enter player name(s) to unlist (comma-separated):",
							"Unlist", JOptionPane.PLAIN_MESSAGE);
					if (input != null && !input.trim().isEmpty()) {
						unlistNames(input);
						unlistEnabled = false;
					} else {
						unlistEnabled = false;
						this.log("Unlist cancelled");
					}
				});
			} else {
				this.log("Unlist disabled");
			}
		} else if (this.whitelistBox.contains(point)) {
			this.whitelistEnabled = !this.whitelistEnabled;
			if (this.whitelistEnabled) {
				SwingUtilities.invokeLater(() -> {
					String input = JOptionPane.showInputDialog(null,
							"Enter player name(s) to whitelist (comma-separated):",
							"Whitelist", JOptionPane.PLAIN_MESSAGE);
					if (input != null && !input.trim().isEmpty()) {
						whitelistNames(input);
						whitelistEnabled = false;
					} else {
						whitelistEnabled = false;
						this.log("Whitelist cancelled");
					}
				});
			} else {
				this.log("Whitelist disabled");
			}
		} else if (this.purgeListButton.contains(point)) {
			this.purgeInactiveProfiles();
		} else if (this.reEnergizeButton.contains(point)) {
			this.reEnergize();
		}
	}

	public boolean isPointInGui(Point p) {
		if (this.guiHidden) {
			return this.guiToggleButton.contains(p);
		}
		return this.hideGuiButton.contains(p)
				|| this.locationComboBox.contains(p)
				|| this.attackBox.contains(p)
				|| this.walkToTargetBox.contains(p)
				|| this.quickprayerBox.contains(p)
				|| this.prayerOffBox.contains(p)
				|| this.switchPrayersBox.contains(p)
				|| this.freeRoamBox.contains(p)
				|| this.protectBluePhatsBox.contains(p)
				|| this.reportBox.contains(p)
				|| this.chatGrabberBox.contains(p)
				|| this.focusPlayersBox.contains(p)
				|| this.lureMultiBox.contains(p)
				|| this.scanHillsBox.contains(p)
				|| this.noDragBox.contains(p)
				|| this.unlistBox.contains(p)
				|| this.whitelistBox.contains(p)
				|| this.purgeListButton.contains(p)
				|| this.reEnergizeButton.contains(p);
	}

	private void onLoop() {
		try {
			Rs2PlayerModel localPlayer = Rs2Player.getLocalPlayer();
			if (localPlayer == null) return;
			WorldPoint localTile = localPlayer.getWorldLocation();
			if (localTile == null) return;
			if (localTile.getPlane() == 1) {
				this.climbdownStairs();
				this.clickPortal();
			}
			if (this.hillScanEnabled && !this.hillScanActive) {
				this.runTrackerScans();
			}
			this.onAction();

			if (this.isDistracted() && !localPlayer.isInCombat() && this.isAtDesignatedArea(localPlayer)) {
				this.handleDistraction();
			}

			if (this.shouldLookAround() && !localPlayer.isInCombat() && this.isAtDesignatedArea(localPlayer)) {
				this.lookAround();
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
			if (now >= this.nextAttackListReloadAtMs && !localPlayer.isInCombat() && !localPlayer.isMoving()) {
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
			if (now >= this.nextAntiBanAtMs && !localPlayer.isInCombat() && !this.AT_LOBBY.contains(localTile) && this.isAtDesignatedArea(localPlayer)) {
				this.performAntiban();
				long fatiguePenalty = (long) (this.getFatigueFactor(now - sessionStartMs) * 500L);
				this.nextAntiBanAtMs = now + Rs2Random.between((int) ANTIBAN_MIN_INTERVAL_MS, (int) ANTIBAN_MAX_INTERVAL_MS) + fatiguePenalty;
			}
			this.checkHalfHourlyArrows();
			if (!this.AT_LOBBY.contains(localTile)) {
				this.avoidObstacles();
			}
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
			aiAssistant.onTick();
			if (now >= nextAutoRetaliateCheckMs && !autoRetaliateOffVerified) {
				ensureAutoRetaliateOff();
			}
			dismissRandomEvents();
			handleAttackerRetaliation(localPlayer);
			if (now >= nextNonTargetScanMs) {
				scanNonTargets(localPlayer);
				nextNonTargetScanMs = now + 5000 + Rs2Random.between(0, 3000);
			}
			nonTargetTracker.tickSave();
			if (playerStats.shouldPeriodicSave()) {
				playerStats.save();
			}
			sessionTracker.tickSave();
			int delay = (int) Math.max(100, Rs2Random.between(250, 500) * (1.0 - aiAssistant.getPlayerProfiles().size() * 0.001));
			delay = Math.min(delay, 600);
			this.sleepWithFatigue(delay, delay);
		} catch (Exception ex) {
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
		this.areaMap.put("Hills", new WorldArea(3325, 4813, 11, 8, 0));
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
			if (Rs2Random.between(0, 100) < 3 + (int) (fatigueFactor * 0.2)) {
				this.randomCameraRotation();
			}
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
			if (lastInCombatMs > 0) {
				sessionTracker.recordTargetRan();
				lastInCombatMs = 0L;
			}
			this.gotoArea();
		} else if (this.isFreeRoamEnabled()) {
			if (!this.FREE_ROAM_AREA.contains(tile) && !this.FREE_ROAM_AREA_2.contains(tile)) {
				this.waitBeforeWalkBack();
				this.antiLure();
			} else if (Rs2Random.between(0, 6) == 1 && !localPlayer.isInCombat()) {
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

	private void handleInFFA(Rs2PlayerModel localPlayer) {
		if (!localPlayer.isInCombat()) {
			if (lastInCombatMs > 0 && System.currentTimeMillis() - lastInCombatMs > 3000) {
				sessionTracker.recordCombatEnd();
				if (Rs2Random.between(0, 100) < 25) {
					this.postCombatGlance();
				}
			}
		} else {
			lastInCombatMs = System.currentTimeMillis();
			lastCombatActivityMs = System.currentTimeMillis();
		}
		if (this.isAttackEnabled()) {
			this.status = "Waiting";
			if (!localPlayer.isInCombat()) {
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
				if (!localPlayer.isInCombat()) {
					sessionTracker.recordScan();
					this.attackingPlayer();
				}
			}
		} else {
			if (!localPlayer.isInCombat() && Rs2Random.between(0, 100) < 2) {
				this.idleGaze();
			}
			this.status = "Afking";
		}
		if (this.isPrayerOffEnabled() && !localPlayer.isInCombat()) {
			this.turnPrayersOff();
		}
		if (!this.isQuickprayersEnabled() && this.isSwitchPrayersEnabled() && localPlayer.isInCombat()
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
			if (attacker != null && !localPlayer.isInCombat()) {
				this.status = "Protecting BluePhat from " + attacker.getName();
				Rectangle clickbox = getActorClickbox(attacker);
				if (clickbox != null) {
					Point clickPoint = Rs2UiHelper.getClickingPoint(clickbox, true);
					naturalMouseMovement(clickPoint.x, clickPoint.y);
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
		if (local.isInCombat() || local.isMoving()) {
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
		return Rs2Player.getPlayers(predicate).min(Comparator.comparingDouble(p -> {
			WorldPoint loc = p.getWorldLocation();
			if (loc == null || localLoc == null) return Double.MAX_VALUE;
			return (double) loc.distanceTo(localLoc);
		})).orElse(null);
	}

	private boolean isPlayerOnScreen(Rs2PlayerModel p) {
		if (p == null || p.getWorldLocation() == null) return false;
		Boolean visible = Microbot.getClientThread().runOnClientThreadOptional(() -> {
			LocalPoint lp = LocalPoint.fromWorld(Microbot.getClient().getTopLevelWorldView(), p.getWorldLocation());
			return lp != null && Rs2Camera.isTileOnScreen(lp);
		}).orElse(Boolean.FALSE);
		return visible != null && visible;
	}

	private Player findReportTarget() {
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
		Point clickPoint = Rs2UiHelper.getClickingPoint(clickbox, true);
		Microbot.getMouse().click(clickPoint, true);
		Global.sleep(60, 160);
		Boolean invoked = Microbot.getClientThread().runOnClientThreadOptional(() -> {
			MenuEntry[] entries = Microbot.getClient().getMenuEntries();
			for (MenuEntry entry : entries) {
				if (entry.getOption() == null || !entry.getOption().equalsIgnoreCase("Report")) continue;
				NewMenuEntry nme = new NewMenuEntry(entry.getOption(), entry.getTarget(),
						entry.getIdentifier(), entry.getType(), entry.getParam0(), entry.getParam1(), false);
				Microbot.doInvoke(nme, clickbox);
				return true;
			}
			return false;
		});
		return invoked != null && invoked;
	}

	private boolean clickWidget(Widget widget) {
		if (widget == null) return false;
		Boolean clickable = Microbot.getClientThread().runOnClientThreadOptional(() -> {
			if (widget.isHidden() || widget.getBounds() == null) return false;
			Rectangle bounds = widget.getBounds();
			if (bounds.width <= 0 || bounds.height <= 0) return false;
			Microbot.getMouse().click(Rs2UiHelper.getClickingPoint(bounds, true));
			return true;
		});
		return clickable != null && clickable;
	}

// ::continues::