package net.runelite.client.plugins.microbot.viruzosbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.runelite.api.ItemComposition;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.kit.KitType;
import net.runelite.client.plugins.microbot.Microbot;

import lombok.Getter;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class AIAssistant {
    private static final String AI_DATA_PATH = Config.getNsListFilePath().replace("nslist.txt", "ai_data.json");
    private static final int HISTORY_SIZE = 10;
    private static final double LEARNING_RATE = 0.2;
    private static final double DEFAULT_TARGET_SCORE = 0.5;

    @Getter
    private final Map<String, PlayerProfile> playerProfiles = new ConcurrentHashMap<>();
    private final Map<Integer, Long> animationCooldowns = new ConcurrentHashMap<>();
    private final List<CombatEncounter> recentEncounters = new LinkedList<>();
    private final Map<String, AnimationPattern> animationPatterns = new ConcurrentHashMap<>();
    private long lastSaveTime = System.currentTimeMillis();
    private static final long SAVE_INTERVAL_MS = 5_000L;
    private boolean modelDirty = false;

    public AIAssistant() {
        loadModel();
    }

    public void learnFromEncounter(String targetName, int theirCombatLevel, String[] theirGear, int theirHp, boolean theyFoughtBack, boolean theyRan, boolean weWon) {
        if (targetName == null || targetName.isEmpty()) return;
        PlayerProfile profile = playerProfiles.computeIfAbsent(targetName.toLowerCase(), k -> new PlayerProfile());
        profile.combatLevel = theirCombatLevel;
        if (theirGear != null) profile.gear = theirGear;
        profile.lastSeenHp = theirHp;
        profile.encounterCount++;
        if (theyFoughtBack) profile.timesFoughtBack++;
        if (theyRan) profile.timesRanToSafe++;
        if (weWon) profile.timesWeWon++;

        double threatDelta = (theyFoughtBack ? 0.15 : -0.05) + (theyRan ? -0.1 : 0) + (weWon ? -0.08 : 0.08);
        if (theirCombatLevel > 0 && theirCombatLevel < 30) {
            threatDelta -= 0.15;
        }
        profile.threatScore = Math.max(0.0, Math.min(1.0, profile.threatScore + threatDelta * LEARNING_RATE));
        profile.lastSeenAt = System.currentTimeMillis();

        CombatEncounter ce = new CombatEncounter(targetName, theirCombatLevel, theyFoughtBack, theyRan, weWon, System.currentTimeMillis());
        recentEncounters.add(ce);
        if (recentEncounters.size() > HISTORY_SIZE * 10) recentEncounters.remove(0);
        modelDirty = true;
        conditionalSave();
    }

    private static final long MIN_RECORD_INTERVAL_MS = 2000L;

    public void recordAttackedBy(String attackerName, boolean theyRanAfterAttacking) {
        if (attackerName == null || attackerName.isEmpty()) return;
        String key = attackerName.toLowerCase(Locale.ROOT);
        PlayerProfile profile = playerProfiles.computeIfAbsent(key, k -> new PlayerProfile());
        long now = System.currentTimeMillis();
        if (now - profile.lastAttackOnUsAt < MIN_RECORD_INTERVAL_MS) {
            profile.lastSeenAt = now;
            modelDirty = true;
            conditionalSave();
            return;
        }
        profile.timesAttackedUs++;
        profile.lastAttackOnUsAt = now;
        profile.lastSeenAt = now;
        if (theyRanAfterAttacking) {
            profile.timesHitAndRan++;
            profile.consecutiveHitAndRuns++;
            profile.threatScore = Math.min(1.0, profile.threatScore + 0.12 * LEARNING_RATE);
        } else {
            profile.consecutiveHitAndRuns = 0;
        }
        modelDirty = true;
        conditionalSave();
    }

    public void recordSeenPlayer(String playerName, int combatLevel) {
        recordSeenPlayer(playerName, combatLevel, null);
    }

    public void recordSeenPlayer(String playerName, int combatLevel, String[] gear) {
        if (playerName == null || playerName.isEmpty()) return;
        String key = playerName.toLowerCase(Locale.ROOT).trim();
        PlayerProfile profile = playerProfiles.computeIfAbsent(key, k -> new PlayerProfile());
        profile.lastSeenAt = System.currentTimeMillis();
        if (combatLevel > 0) profile.combatLevel = combatLevel;
        if (gear != null) profile.gear = gear;
        modelDirty = true;
        conditionalSave();
    }

    public void trackPlayer(String playerName, int combatLevel) {
        recordSeenPlayer(playerName, combatLevel);
    }

    public void trackPlayer(String playerName, int combatLevel, String[] gear) {
        recordSeenPlayer(playerName, combatLevel, gear);
    }

    public void updateLastSeen(String playerName) {
        recordSeenPlayer(playerName, -1);
    }

    public void updateLastSeen(String playerName, int combatLevel) {
        recordSeenPlayer(playerName, combatLevel);
    }

    public void recordPlayerSeen(String playerName, int combatLevel) {
        recordSeenPlayer(playerName, combatLevel);
    }

    public void observePlayer(String playerName, int combatLevel) {
        recordSeenPlayer(playerName, combatLevel);
    }

    public boolean isLikelyLureBot(String playerName) {
        if (playerName == null) return false;
        PlayerProfile profile = playerProfiles.get(playerName.toLowerCase(Locale.ROOT));
        if (profile == null) return false;
        if (profile.timesAttackedUs < 3) return false;
        double hitAndRunRatio = (double) profile.timesHitAndRan / profile.timesAttackedUs;
        return hitAndRunRatio > 0.55;
    }

    public boolean isRepeatedLowLevelAttacker(String playerName) {
        if (playerName == null) return false;
        PlayerProfile profile = playerProfiles.get(playerName.toLowerCase(Locale.ROOT));
        if (profile == null) return false;
        return profile.timesAttackedUs >= 3 && profile.combatLevel > 50;
    }

    public void learnAnimationPattern(String attackerName, int animationId, long now) {
        Long lastRecorded = animationCooldowns.get(animationId);
        if (lastRecorded != null && now - lastRecorded < 5000) return;
        animationCooldowns.put(animationId, now);
        AnimationPattern pattern = animationPatterns.computeIfAbsent(attackerName.toLowerCase(), k -> new AnimationPattern());
        pattern.addAnimation(animationId);
    }

    public CombatStyle predictNextAttack(String attackerName) {
        AnimationPattern pattern = animationPatterns.get(attackerName.toLowerCase());
        if (pattern == null) return CombatStyle.UNKNOWN;
        return pattern.predictNext();
    }

    public double getTargetScore(Player candidate, Player localPlayer, Set<String> attackSet, boolean localInMulti, double fatigueFactor) {
        if (candidate == null || !exists(candidate)) return -1;
        String name = candidate.getName();
        if (name == null) return -1;
        String key = name.toLowerCase();

        boolean inList = attackSet.contains(key);
        if (!inList) return -0.5;
        PlayerProfile profile = playerProfiles.get(key);
        if (profile == null) {
            profile = playerProfiles.computeIfAbsent(key, k -> new PlayerProfile());
            profile.combatLevel = getCombatLevel(candidate);
            profile.lastSeenAt = System.currentTimeMillis();
            modelDirty = true;
        } else {
            profile.lastSeenAt = System.currentTimeMillis();
            int cb = getCombatLevel(candidate);
            if (cb > 0) profile.combatLevel = cb;
            modelDirty = true;
        }
        double score = DEFAULT_TARGET_SCORE;
        long timeSinceSeen = System.currentTimeMillis() - profile.lastSeenAt;
        double decay = Math.exp(-timeSinceSeen / (double) THREAT_DECAY_MS);
        double aggressiveness = (double) profile.timesFoughtBack / Math.max(1, profile.encounterCount);
        double cowardice = (double) profile.timesRanToSafe / Math.max(1, profile.encounterCount);
        score += (1.0 - aggressiveness) * 0.25;
        score -= (cowardice > 0.6 ? 0.15 : 0);
        score -= profile.threatScore * 0.2;
        score += (profile.weWonRatio() > 0.6 ? 0.1 : 0);
        score += decay * 0.1;
        if (profile.timesAttackedUs >= 3 && profile.hitAndRunRatio() > 0.5) {
            score -= 0.35;
        }
        if (profile.timesAttackedUs >= 5 && profile.combatLevel > 0 && profile.combatLevel < 60) {
            score -= 0.2;
        }
        double distance = distanceBetween(localPlayer, candidate);
        score -= Math.max(0, (distance - 3) * 0.02);
        score -= fatigueFactor * 0.008;
        if (isInCombat(candidate) && !localInMulti) score -= 0.3;
        try {
            Integer hp = extractHealth();
            if (hp != null && hp < 50) score += 0.15;
        } catch (Exception ignored) {}
        return Math.max(0, Math.min(1.0, score));
    }

    public Player selectBestTarget(List<Player> candidates, Player localPlayer, Set<String> attackSet, boolean localInMulti, double fatigueFactor) {
        return candidates.stream()
                .filter(p -> p != null && exists(p))
                .max(Comparator.comparingDouble(p -> getTargetScore(p, localPlayer, attackSet, localInMulti, fatigueFactor)))
                .orElse(null);
    }

    public CombatStyle detectCombatStyle(Player player) {
        if (player == null) return CombatStyle.UNKNOWN;
        int anim = getAnimation(player);
        if (MAGIC_ANIMS.contains(anim)) return CombatStyle.MAGIC;
        if (RANGE_ANIMS.contains(anim)) return CombatStyle.RANGED;
        if (MELEE_ANIMS.contains(anim)) return CombatStyle.MELEE;

        String weaponName = extractWeaponName(player);
        if (weaponName != null) {
            if (matchAnyArray(weaponName, MELEE_WEAPONS)) return CombatStyle.MELEE;
            if (matchAnyArray(weaponName, RANGED_WEAPONS)) return CombatStyle.RANGED;
            if (matchAnyArray(weaponName, MAGIC_WEAPONS)) return CombatStyle.MAGIC;

            String lower = weaponName.toLowerCase(Locale.ROOT);
            if (containsAny(lower, "bow", "arrow", "crossbow", "dart", "knife", "javelin", "thrownaxe", "shortbow")) return CombatStyle.RANGED;
            if (containsAny(lower, "staff", "wand", "robe", "hat", "book", "magic")) return CombatStyle.MAGIC;
            if (containsAny(lower, "scimitar", "sword", "axe", "mace", "warhammer", "battleaxe", "dagger", "spear", "halberd", "claw", "whip", "pickaxe", "spade", "longsword")) return CombatStyle.MELEE;
        }

        return detectStyleFromEquipment(player);
    }

    private CombatStyle detectStyleFromEquipment(Player player) {
        try {
            List<String> equipment = getEquipmentNames(player);
            int mCount = 0, rCount = 0, magCount = 0;
            for (String name : equipment) {
                if (matchAnyArray(name, MELEE_BODY) || matchAnyArray(name, MELEE_LEGS) || matchAnyArray(name, MELEE_SHIELDS)) mCount++;
                else if (matchAnyArray(name, RANGED_BODY) || matchAnyArray(name, RANGED_CHAPS)) rCount++;
                else if (matchAnyArray(name, MAGIC_BODY) || matchAnyArray(name, MAGIC_LEGS)) magCount++;
            }
            return dominantStyle(mCount, rCount, magCount);
        } catch (Exception ignored) {
            return CombatStyle.UNKNOWN;
        }
    }

    public long calculateHumanReactionDelay(double fatigueFactor, boolean isUrgent) {
        double baseMean = isUrgent ? 180 : 350;
        double baseStd = isUrgent ? 60 : 150;
        double gaussian = ThreadLocalRandom.current().nextGaussian();
        long delay = (long) (baseMean + baseStd * gaussian * (1.0 + fatigueFactor * 0.04));
        delay = Math.max(isUrgent ? 80 : 100, Math.min(delay, isUrgent ? 800 : 2000));
        if (ThreadLocalRandom.current().nextDouble() < 0.03 + fatigueFactor * 0.003) {
            delay += ThreadLocalRandom.current().nextInt(200, 1200);
        }
        return delay;
    }

    public boolean shouldMakeHumanMistake(double fatigueFactor, long lastMistakeAt, long mistakeCooldown) {
        long now = System.currentTimeMillis();
        if (now - lastMistakeAt < mistakeCooldown) return false;
        double chance = 0.01 + fatigueFactor * 0.003;
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    public boolean shouldLeftClickInstead(boolean targetIsLowHp, double fatigueFactor) {
        double safeFatigue = Math.max(0.0, fatigueFactor);
        double chance = 0.15;
        if (targetIsLowHp) chance += 0.15;
        chance += safeFatigue * 0.005;
        chance = Math.max(0.0, Math.min(1.0, chance));
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    public String[] generateWeaponList(Player p) {
        if (p == null) return new String[0];
        List<String> gear = new ArrayList<>();
        try {
            gear.addAll(getEquipmentNames(p));
        } catch (Exception ignored) {}
        if (!gear.isEmpty()) {
            return gear.toArray(new String[0]);
        }
        CombatStyle style = detectCombatStyle(p);
        String weaponGuess;
        switch (style) {
            case MELEE:
                weaponGuess = "Melee weapon";
                break;
            case RANGED:
                weaponGuess = "Ranged weapon";
                break;
            case MAGIC:
                weaponGuess = "Magic weapon";
                break;
            default:
                weaponGuess = "Unknown weapon";
        }
        return new String[]{weaponGuess, style.name()};
    }

    public double calculateAdaptiveThreshold(double baseThreshold, double fatigueFactor) {
        double variability = 0.15 + fatigueFactor * 0.02;
        double raw = baseThreshold * (1.0 + ThreadLocalRandom.current().nextGaussian() * variability * 0.3);
        return Math.max(500, Math.min(20000, raw));
    }

    public void saveModel() {
        try {
            Path path = Path.of(AI_DATA_PATH);
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
            ModelData data = new ModelData(new HashMap<>(playerProfiles), new LinkedList<>(recentEncounters), new HashMap<>(animationPatterns));
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(data);
            Files.writeString(path, json);
            modelDirty = false;
        } catch (Exception e) {
            System.out.println("[AIAssistant] Failed to save model: " + e.getMessage());
        }
    }

    public boolean removeProfile(String playerName) {
        if (playerName == null || playerName.isEmpty()) return false;
        String key = playerName.toLowerCase();
        boolean removed = playerProfiles.remove(key) != null;
        animationPatterns.remove(key);
        recentEncounters.removeIf(e -> e.targetName != null && e.targetName.equalsIgnoreCase(key));
        if (removed) {
            modelDirty = true;
            saveModel();
        }
        return removed;
    }

    public void clearQuitStatus(String playerName) {
        if (playerName == null || playerName.isEmpty()) return;
        String key = playerName.toLowerCase(Locale.ROOT).trim();
        PlayerProfile profile = playerProfiles.get(key);
        if (profile == null || profile.status == null || !profile.status.trim().toLowerCase(Locale.ROOT).startsWith("quit")) return;
        profile.status = null;
        modelDirty = true;
        saveModel();
    }

    public void onTick() {
        decayAnimationCooldowns();
        conditionalSave();
    }

    private void conditionalSave() {
        long now = System.currentTimeMillis();
        if (modelDirty && now - lastSaveTime >= SAVE_INTERVAL_MS) {
            saveModel();
            lastSaveTime = now;
        } else if (now - lastSaveTime >= SAVE_INTERVAL_MS) {
            lastSaveTime = now;
        }
    }

    private void loadModel() {
        try {
            Path path = Path.of(AI_DATA_PATH);
            if (!Files.exists(path)) return;
            String json = Files.readString(path);
            Gson gson = new Gson();
            Type type = new TypeToken<ModelData>(){}.getType();
            ModelData data = gson.fromJson(json, type);
            if (data != null) {
                if (data.profiles != null) playerProfiles.putAll(data.profiles);
                if (data.encounters != null) recentEncounters.addAll(data.encounters);
                if (data.patterns != null) animationPatterns.putAll(data.patterns);
            }
        } catch (Exception e) {
            System.out.println("[AIAssistant] No prior model data found, starting fresh.");
        }
    }

    private void decayAnimationCooldowns() {
        long now = System.currentTimeMillis();
        animationCooldowns.entrySet().removeIf(e -> now - e.getValue() > 5000);
    }

    private String extractWeaponName(Player player) {
        if (player == null) return null;
        try {
            List<String> equipment = getEquipmentNames(player);
            for (String name : equipment) {
                if (matchAnyArray(name, MELEE_WEAPONS) || matchAnyArray(name, RANGED_WEAPONS) || matchAnyArray(name, MAGIC_WEAPONS)) {
                    return name;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static boolean matchAnyArray(String name, String... items) {
        for (String item : items) {
            if (item.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private Integer extractHealth() {
        return null;
    }

    public enum Reaction {
        PRAY_MELEE, PRAY_RANGED, PRAY_MAGIC, DRAG_TO_SAFE, RUN_AWAY, FIGHT_BACK, IGNORE
    }

    public static class SuggestedReaction {
        public final Reaction reaction;
        public final double priority;
        public final String reason;
        public SuggestedReaction(Reaction reaction, double priority, String reason) {
            this.reaction = reaction;
            this.priority = priority;
            this.reason = reason;
        }
    }

    public SuggestedReaction suggestReaction(
            Player opponent,
            Player localPlayer,
            boolean isInSafeZone,
            boolean prayersAvailable,
            double fatigueFactor,
            Set<String> attackSet
    ) {
        if (opponent == null || localPlayer == null || !exists(opponent) || opponent.getName() == null) {
            return new SuggestedReaction(Reaction.IGNORE, 0, "no opponent");
        }
        String key = opponent.getName().toLowerCase(Locale.ROOT);
        PlayerProfile profile = playerProfiles.get(key);
        double threatScore = profile != null ? profile.threatScore : 0.5;
        int opponentCombat = profile != null && profile.combatLevel > 0 ? profile.combatLevel : getCombatLevel(opponent);
        int localCombat = getCombatLevel(localPlayer);
        double distance = distanceBetween(localPlayer, opponent);

        boolean knownLureBot = isLikelyLureBot(key);
        boolean inAttackSet = attackSet != null && attackSet.contains(key);

        if (knownLureBot && !inAttackSet) {
            return new SuggestedReaction(Reaction.IGNORE,
                    clamp01(0.9 * (1.0 - fatigueFactor * 0.006)), "lure bot ignored");
        }
        if (knownLureBot && isInSafeZone) {
            return new SuggestedReaction(Reaction.DRAG_TO_SAFE,
                    clamp01(0.8 * (1.0 - fatigueFactor * 0.006)), "lure bot in safe zone");
        }

        CombatStyle predictedStyle = predictNextAttack(key);
        double styleConfidence = 0;
        if (predictedStyle != CombatStyle.UNKNOWN) {
            AnimationPattern pattern = animationPatterns.get(key);
            if (pattern != null && pattern.count >= 2) {
                styleConfidence = Math.min(1.0, pattern.count / 10.0);
            }
        }

        double fatigueJitter = ThreadLocalRandom.current().nextGaussian() * fatigueFactor * 0.02;
        double fatigueThresholdMod = fatigueFactor * 0.04;
        double fatiguePriorityMod = 1.0 - fatigueFactor * 0.006;

        if (styleConfidence > 0.5 - fatigueThresholdMod && prayersAvailable) {
            return new SuggestedReaction(combatStyleToReaction(predictedStyle),
                    clamp01(styleConfidence * fatiguePriorityMod + fatigueJitter), "predicted " + predictedStyle);
        }

        if (styleConfidence > 0.35 - fatigueThresholdMod && prayersAvailable) {
            CombatStyle detected = detectCombatStyle(opponent);
            if (detected != CombatStyle.UNKNOWN) {
                return new SuggestedReaction(combatStyleToReaction(detected),
                        clamp01(0.5 * fatiguePriorityMod + fatigueJitter), "detected " + detected);
            }
        }

        double distanceMod = Math.max(0, 1.0 - distance / 20.0);
        double dragThreshold = 0.6 - fatigueThresholdMod - distanceMod * 0.1;
        if (!isInSafeZone && threatScore > dragThreshold) {
            return new SuggestedReaction(Reaction.DRAG_TO_SAFE,
                    clamp01(threatScore * fatiguePriorityMod + fatigueJitter + distanceMod * 0.1),
                    "high threat " + String.format("%.1f", threatScore));
        }

        double moderateDragThreshold = 0.45 - fatigueThresholdMod - distanceMod * 0.1;
        if (!isInSafeZone && (threatScore > moderateDragThreshold || (profile != null && profile.timesFoughtBack > profile.encounterCount / 2))) {
            return new SuggestedReaction(Reaction.DRAG_TO_SAFE,
                    clamp01(0.5 * fatiguePriorityMod + fatigueJitter + distanceMod * 0.1), "moderate threat");
        }

        int cbGap = 15 - (int)(fatigueFactor * 2);
        if (opponentCombat > 0 && localCombat > 0 && opponentCombat > localCombat + Math.max(5, cbGap)) {
            return new SuggestedReaction(Reaction.RUN_AWAY,
                    clamp01(0.7 * fatiguePriorityMod + fatigueJitter), "cb gap " + (opponentCombat - localCombat));
        }

        double fightThreshold = 0.4 + fatigueThresholdMod;
        if (threatScore < fightThreshold && (profile == null || profile.weWonRatio() > 0.5)) {
            return new SuggestedReaction(Reaction.FIGHT_BACK,
                    clamp01(0.6 * fatiguePriorityMod + fatigueJitter), "favorable matchup");
        }

        if (threatScore < fightThreshold) {
            return new SuggestedReaction(Reaction.IGNORE, clamp01(0.2 * fatiguePriorityMod), "low threat");
        }

        return new SuggestedReaction(Reaction.IGNORE, Math.max(0.05, clamp01(0.1 * fatiguePriorityMod)), "default");
    }

    private Reaction combatStyleToReaction(CombatStyle style) {
        switch (style) {
            case MELEE: return Reaction.PRAY_MELEE;
            case RANGED: return Reaction.PRAY_RANGED;
            case MAGIC: return Reaction.PRAY_MAGIC;
            default: return Reaction.IGNORE;
        }
    }

    private static double clamp01(double value) {
        return Math.min(1.0, Math.max(0, value));
    }

    private boolean containsAny(String text, String... substrings) {
        for (String sub : substrings) {
            if (text.contains(sub)) return true;
        }
        return false;
    }

    private static final Set<Integer> MAGIC_ANIMS = Set.of(1162, 1163, 1164, 1165, 1978, 1979);
    private static final Set<Integer> RANGE_ANIMS = Set.of(426, 4230, 929);
    private static final Set<Integer> MELEE_ANIMS = Set.of(390, 386, 388, 400, 401, 407, 406, 393, 395, 412, 422, 419);
    private static final long THREAT_DECAY_MS = 15 * 60 * 1000L;

    private static final String[] MELEE_WEAPONS = {"Rune scimitar", "Rune sword", "Rune longsword", "Rune axe", "Rune pickaxe", "Rune mace", "Rune warhammer", "Rune battleaxe", "Rune dagger", "Gilded scimitar", "Gilded spade", "Gilded pickaxe", "Gilded axe"};
    private static final String[] MELEE_SHIELDS = {"Rune kiteshield", "Rune kiteshield (t)", "Rune kiteshield (g)", "Saradomin kiteshield", "Zamorak kiteshield", "Bandos kiteshield", "Armadyl kiteshield", "Guthix kiteshield", "Ancient kiteshield", "Gilded kiteshield", "Rune shield (h1)", "Rune shield (h2)", "Rune shield (h3)", "Rune shield (h4)", "Rune shield (h5)"};
    private static final String[] MELEE_BODY = {"Rune platebody", "Rune platebody (t)", "Rune platebody (g)", "Saradomin platebody", "Zamorak platebody", "Bandos platebody", "Armadyl platebody", "Guthix platebody", "Ancient platebody", "Gilded platebody", "Rune platebody (h1)", "Rune platebody (h2)", "Rune platebody (h3)", "Rune platebody (h4)", "Rune platebody (h5)"};
    private static final String[] MELEE_LEGS = {"Rune platelegs", "Rune platelegs (t)", "Rune platelegs (g)", "Saradomin platelegs", "Zamorak platelegs", "Bandos platelegs", "Armadyl platelegs", "Guthix platelegs", "Ancient platelegs", "Gilded platelegs", "Rune platelegs (h1)", "Rune platelegs (h2)", "Rune platelegs (h3)", "Rune platelegs (h4)", "Rune platelegs (h5)"};
    private static final String[] RANGED_WEAPONS = {"Maple shortbow"};
    private static final String[] RANGED_BODY = {"Gilded d'hide body", "Green d'hide body", "Green d'hide body (g)", "Green d'hide body (t)"};
    private static final String[] RANGED_CHAPS = {"Gilded d'hide chaps", "Green d'hide chaps", "Green d'hide chaps (g)", "Green d'hide chaps (t)"};
    private static final String[] MAGIC_WEAPONS = {"Staff of air", "Staff of water", "Staff of fire", "Staff of earth", "Bryophyta's staff"};
    private static final String[] MAGIC_BODY = {"Blue wizard robe", "Black robe", "Zamorak monk top"};
    private static final String[] MAGIC_LEGS = {"Blue skirt", "Black skirt", "Zamorak monk bottom"};

    public enum CombatStyle {
        MELEE, RANGED, MAGIC, UNKNOWN
    }

    public static class PlayerProfile {
        double threatScore = 0.5;
        int combatLevel = -1;
        String[] gear = new String[0];
        int lastSeenHp = 99;
        int encounterCount = 0;
        int timesFoughtBack = 0;
        int timesRanToSafe = 0;
        int timesWeWon = 0;
        int timesAttackedUs = 0;
        int timesHitAndRan = 0;
        int consecutiveHitAndRuns = 0;
        long lastSeenAt = System.currentTimeMillis();
        long lastAttackOnUsAt = 0L;
        String status = null;

        public double weWonRatio() {
            return encounterCount > 0 ? (double) timesWeWon / encounterCount : 0;
        }
        public double fightBackRatio() {
            return encounterCount > 0 ? (double) timesFoughtBack / encounterCount : 0;
        }
        public double hitAndRunRatio() {
            return timesAttackedUs > 0 ? (double) timesHitAndRan / timesAttackedUs : 0;
        }
        public double threatDisplay() {
            return threatScore;
        }
    }

    private static class CombatEncounter {
        String targetName;
        int combatLevel;
        boolean foughtBack;
        boolean ranToSafe;
        boolean weWon;
        long timestamp;
        CombatEncounter(String targetName, int combatLevel, boolean foughtBack, boolean ranToSafe, boolean weWon, long timestamp) {
            this.targetName = targetName;
            this.combatLevel = combatLevel;
            this.foughtBack = foughtBack;
            this.ranToSafe = ranToSafe;
            this.weWon = weWon;
            this.timestamp = timestamp;
        }
    }

    private static class ModelData {
        Map<String, PlayerProfile> profiles;
        List<CombatEncounter> encounters;
        Map<String, AnimationPattern> patterns;
        @SuppressWarnings("unused") // required by Gson
        ModelData() {}
        ModelData(Map<String, PlayerProfile> profiles, List<CombatEncounter> encounters, Map<String, AnimationPattern> patterns) {
            this.profiles = profiles;
            this.encounters = encounters;
            this.patterns = patterns;
        }
    }

    private static CombatStyle dominantStyle(double melee, double ranged, double magic) {
        if (magic > ranged && magic > melee) return CombatStyle.MAGIC;
        if (ranged > melee && ranged > magic) return CombatStyle.RANGED;
        if (melee > ranged && melee > magic) return CombatStyle.MELEE;
        return CombatStyle.UNKNOWN;
    }

    static class AnimationPattern {
        List<Integer> recentAnims = new ArrayList<>();
        int count = 0;

        void addAnimation(int animId) {
            recentAnims.add(animId);
            if (recentAnims.size() > 20) recentAnims.remove(0);
            count++;
        }

        CombatStyle predictNext() {
            if (count < 2) return CombatStyle.UNKNOWN;
            double mScore = 0, rScore = 0, magScore = 0;
            int size = recentAnims.size();
            int start = Math.max(0, size - 10);
            for (int i = start; i < size; i++) {
                double weight = 1.0 + 0.5 * (double)(i - start) / Math.max(1, size - start - 1);
                int anim = recentAnims.get(i);
                if (MAGIC_ANIMS.contains(anim)) magScore += weight;
                else if (RANGE_ANIMS.contains(anim)) rScore += weight;
                else if (MELEE_ANIMS.contains(anim)) mScore += weight;
            }
            double total = mScore + rScore + magScore;
            if (total < 1.5) return CombatStyle.UNKNOWN;
            double dominance = Math.max(mScore, Math.max(rScore, magScore)) / total;
            if (dominance < 0.45) return CombatStyle.UNKNOWN;
            return dominantStyle(mScore, rScore, magScore);
        }
    }

    private static boolean exists(Player player) {
        return Microbot.getClientThread().runOnClientThreadOptional(() -> playerExistsOnClientThread(player)).orElse(false);
    }

    private static boolean playerExistsOnClientThread(Player player) {
        net.runelite.api.Client client = Microbot.getClient();
        net.runelite.api.WorldView worldView = client.getTopLevelWorldView();
        return client.getLocalPlayer() != null && worldView != null
                && worldView.players().stream().anyMatch(p -> p == player);
    }

    private static double distanceBetween(Player from, Player to) {
        return Microbot.getClientThread().runOnClientThreadOptional(() -> {
            WorldPoint a = from == null ? null : from.getWorldLocation();
            WorldPoint b = to == null ? null : to.getWorldLocation();
            if (a == null || b == null) return Double.MAX_VALUE;
            double dx = a.getX() - b.getX();
            double dy = a.getY() - b.getY();
            return Math.sqrt(dx * dx + dy * dy);
        }).orElse(Double.MAX_VALUE);
    }

    private static boolean isInCombat(Player player) {
        return Microbot.getClientThread().runOnClientThreadOptional(() -> player.getInteracting() != null).orElse(false);
    }

    private static int getAnimation(Player player) {
        return Microbot.getClientThread().runOnClientThreadOptional(player::getAnimation).orElse(-1);
    }

    private static int getCombatLevel(Player player) {
        return Microbot.getClientThread().runOnClientThreadOptional(player::getCombatLevel).orElse(0);
    }

    private static List<String> getEquipmentNames(Player player) {
        return Microbot.getClientThread().runOnClientThreadOptional(() -> {
            List<String> names = new ArrayList<>();
            PlayerComposition composition = player.getPlayerComposition();
            if (composition == null) return names;
            for (KitType kitType : KitType.values()) {
                int equipmentId = composition.getEquipmentId(kitType);
                if (equipmentId < 0) continue;
                ItemComposition itemComposition = Microbot.getItemManager().getItemComposition(equipmentId);
                String name = itemComposition.getName();
                if (name == null || name.equalsIgnoreCase("null")) continue;
                names.add(name);
            }
            return names;
        }).orElse(new ArrayList<>());
    }
}