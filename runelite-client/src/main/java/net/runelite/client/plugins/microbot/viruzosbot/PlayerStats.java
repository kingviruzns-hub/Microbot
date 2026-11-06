package net.runelite.client.plugins.microbot.viruzosbot;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import org.jetbrains.annotations.NotNull;

public class PlayerStats {
    private static final int MAX_BULLIED = 20;
    private static final long SAVE_INTERVAL_MS = 5_000L;

    // Total runtime persisted to stats.json as of lastPersistMs.
    private final AtomicLong totalRuntimeMs = new AtomicLong(0);
    // Wall-clock start of the current session (used for "Current Runtime").
    private volatile long sessionStartMs = 0L;
    // When totalRuntimeMs was last read from / written to stats.json.
    private volatile long lastPersistMs = 0L;
    private long xpAtStart = 0L;
    private long totalXpEarned = 0L;

    // Attacks already persisted to stats.json.
    private final LinkedHashMap<String, Integer> persistedBulliedAttacks = new LinkedHashMap<>();
    // Attacks recorded this session that have not been persisted yet.
    private final LinkedHashMap<String, Integer> sessionBulliedAttacks = new LinkedHashMap<>();

    private long lastSaveMs = System.currentTimeMillis();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, Object>>(){}.getType();
    private static final Type BULLIED_LIST_TYPE = new TypeToken<List<Map<String, Object>>>(){}.getType();

    public synchronized void startSession() {
        // Keep anything that was recorded but not written yet before starting fresh.
        // Do not call save() here: the gap since the last save may include time the
        // client was offline, which must not count as runtime.
        mergeSessionIntoPersisted();

        sessionStartMs = System.currentTimeMillis();
        lastPersistMs = sessionStartMs;
        long currentXp = getTotalCombatXp();
        xpAtStart = currentXp > 0 ? currentXp : 0;
        load();
    }

    public synchronized void recordAttack(String targetName) {
        if (targetName == null) return;
        String key = targetName.toLowerCase(Locale.ROOT).trim();
        if (key.isEmpty()) return;
        sessionBulliedAttacks.merge(key, 1, Integer::sum);
        trimBullied();
    }

    public synchronized void updateXpEarned() {
        long currentXp = getTotalCombatXp();
        if (currentXp <= 0) {
            return;
        }
        if (xpAtStart <= 0) {
            xpAtStart = currentXp;
            return;
        }
        long gained = currentXp - xpAtStart;
        if (gained > 0) {
            totalXpEarned += gained;
            xpAtStart = currentXp;
        } else if (gained < 0) {
            xpAtStart = currentXp;
        }
    }

    public synchronized boolean shouldPeriodicSave() {
        long now = System.currentTimeMillis();
        if (now - lastSaveMs >= SAVE_INTERVAL_MS) {
            lastSaveMs = now;
            return true;
        }
        return false;
    }

    public void saveTrackerInfo() {
        save();
    }

    public synchronized void save() {
        updateXpEarned();
        long now = System.currentTimeMillis();
        if (sessionStartMs <= 0L) {
            sessionStartMs = now;
        }
        if (lastPersistMs <= 0L) {
            lastPersistMs = sessionStartMs;
        }
        long deltaMs = now - lastPersistMs;
        if (deltaMs < 0L) {
            deltaMs = 0L;
        }

        // Always refresh from stats.json before adding this session's changes so a
        // previous session's totals are never overwritten by this one.
        Path path = Paths.get(Config.getStatsFilePath());
        DiskStats disk;
        try {
            disk = readFromDisk(path);
        } catch (Exception e) {
            // Do not write if the existing file exists but cannot be read, otherwise
            // we would replace valid totals with only this session's numbers.
            System.err.println("PlayerStats: Could not read stats.json before saving; skipping save: " + e.getMessage());
            return;
        }

        long baseRuntimeMs = totalRuntimeMs.get();
        if (disk != null) {
            baseRuntimeMs = Math.max(baseRuntimeMs, disk.runtimeMs);
            totalXpEarned = Math.max(totalXpEarned, disk.xpEarned);
            for (Map.Entry<String, Integer> e : disk.bullied.entrySet()) {
                persistedBulliedAttacks.merge(e.getKey(), e.getValue(), Math::max);
            }
        }
        long newTotalRuntimeMs = baseRuntimeMs + deltaMs;

        // Sort before trimming so the file always lists the highest attack counts first.
        LinkedHashMap<String, Integer> mergedBullied = sortedBulliedAttacks(mergedBulliedAttacks());
        trimBullied(mergedBullied);

        Map<String, Object> data = getData(now, newTotalRuntimeMs, mergedBullied);

        boolean written = false;
        try {
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(data));
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception moveEx) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
            written = true;
        } catch (IOException e) {
            System.err.println("PlayerStats: Error saving stats.json: " + e.getMessage());
        }

        if (written) {
            totalRuntimeMs.set(newTotalRuntimeMs);
            lastPersistMs = now;
            persistedBulliedAttacks.clear();
            persistedBulliedAttacks.putAll(mergedBullied);
            sessionBulliedAttacks.clear();
        }
    }

    private @NotNull Map<String, Object> getData(long now, long newTotalRuntimeMs, LinkedHashMap<String, Integer> mergedBullied) {
        long sessionRuntimeMs = now - sessionStartMs;
        if (sessionRuntimeMs < 0L) {
            sessionRuntimeMs = 0L;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("Current Runtime", formatRuntime(sessionRuntimeMs));
        data.put("Runtime", formatRuntime(newTotalRuntimeMs));
        data.put("RuntimeMs", newTotalRuntimeMs);
        data.put("XP Earned", totalXpEarned);

        List<Map<String, Object>> bulliedList = new ArrayList<>();
        for (Map.Entry<String, Integer> e : mergedBullied.entrySet()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", e.getKey());
            entry.put("attacks", e.getValue());
            bulliedList.add(entry);
        }
        data.put("Most Bullied", bulliedList);
        return data;
    }

    public synchronized void load() {
        Path path = Paths.get(Config.getStatsFilePath());
        try {
            DiskStats disk = readFromDisk(path);
            if (disk != null) {
                totalRuntimeMs.set(Math.max(totalRuntimeMs.get(), disk.runtimeMs));
                totalXpEarned = Math.max(totalXpEarned, disk.xpEarned);
                for (Map.Entry<String, Integer> e : disk.bullied.entrySet()) {
                    persistedBulliedAttacks.merge(e.getKey(), e.getValue(), Math::max);
                }
                trimBullied();
            }
        } catch (Exception e) {
            System.err.println("PlayerStats: Error loading stats.json: " + e.getMessage());
        }

        long now = System.currentTimeMillis();
        if (sessionStartMs <= 0L) {
            sessionStartMs = now;
        }
        lastPersistMs = now;

        long currentXp = getTotalCombatXp();
        if (currentXp > 0) {
            xpAtStart = currentXp;
        } else if (xpAtStart <= 0) {
            xpAtStart = 0;
        }
    }

    // Deliberately not synchronized: the HUD calls this from the client thread while
    // save() may be waiting on the client thread to read XP.
    public String getRuntimeString() {
        long now = System.currentTimeMillis();
        long anchor = lastPersistMs > 0L ? lastPersistMs : sessionStartMs;
        long total = totalRuntimeMs.get();
        if (anchor > 0L) {
            total += Math.max(0L, now - anchor);
        }
        return formatRuntime(total);
    }

    public synchronized List<Map.Entry<String, Integer>> getTopBullied(int limit) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(sortedBulliedAttacks(mergedBulliedAttacks()).entrySet());
        return list.subList(0, Math.min(limit, list.size()));
    }

    private LinkedHashMap<String, Integer> sortedBulliedAttacks(Map<String, Integer> attacks) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(attacks.entrySet());
        entries.sort((a, b) -> {
            int byAttacks = Integer.compare(b.getValue(), a.getValue());
            return byAttacks != 0 ? byAttacks : a.getKey().compareToIgnoreCase(b.getKey());
        });

        LinkedHashMap<String, Integer> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : entries) {
            sorted.put(e.getKey(), e.getValue());
        }
        return sorted;
    }

    private DiskStats readFromDisk(Path path) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }
        String json = Files.readString(path);
        if (json.trim().isEmpty()) {
            return null;
        }
        Map<String, Object> data = GSON.fromJson(json, MAP_TYPE);
        if (data == null) {
            return null;
        }

        DiskStats stats = new DiskStats();
        Object runtimeObj = data.get("RuntimeMs");
        if (runtimeObj instanceof Number) {
            stats.runtimeMs = ((Number) runtimeObj).longValue();
        }
        Object xpObj = data.get("XP Earned");
        if (xpObj instanceof Number) {
            stats.xpEarned = ((Number) xpObj).longValue();
        }
        Object bulliedObj = data.get("Most Bullied");
        if (bulliedObj != null) {
            List<Map<String, Object>> list = GSON.fromJson(GSON.toJson(bulliedObj), BULLIED_LIST_TYPE);
            if (list != null) {
                for (Map<String, Object> entry : list) {
                    if (entry == null) continue;
                    Object nameObj = entry.get("name");
                    Object attacksObj = entry.get("attacks");
                    if (!(nameObj instanceof String)) continue;
                    String name = ((String) nameObj).toLowerCase(Locale.ROOT).trim();
                    if (name.isEmpty()) continue;
                    int attacks = attacksObj instanceof Number ? ((Number) attacksObj).intValue() : 0;
                    if (attacks > 0) {
                        stats.bullied.merge(name, attacks, Integer::sum);
                    }
                }
            }
        }
        return stats;
    }

    private LinkedHashMap<String, Integer> mergedBulliedAttacks() {
        LinkedHashMap<String, Integer> merged = new LinkedHashMap<>(persistedBulliedAttacks);
        for (Map.Entry<String, Integer> e : sessionBulliedAttacks.entrySet()) {
            merged.merge(e.getKey(), e.getValue(), Integer::sum);
        }
        return merged;
    }

    private void mergeSessionIntoPersisted() {
        for (Map.Entry<String, Integer> e : sessionBulliedAttacks.entrySet()) {
            persistedBulliedAttacks.merge(e.getKey(), e.getValue(), Integer::sum);
        }
        sessionBulliedAttacks.clear();
    }

    private void trimBullied() {
        LinkedHashMap<String, Integer> merged = mergedBulliedAttacks();
        while (merged.size() > MAX_BULLIED) {
            String lowestKey = lowestKey(merged);
            if (lowestKey == null) break;
            merged.remove(lowestKey);
            persistedBulliedAttacks.remove(lowestKey);
            sessionBulliedAttacks.remove(lowestKey);
        }
    }

    private void trimBullied(LinkedHashMap<String, Integer> merged) {
        while (merged.size() > MAX_BULLIED) {
            String lowestKey = lowestKey(merged);
            if (lowestKey == null) break;
            merged.remove(lowestKey);
        }
    }

    private String lowestKey(Map<String, Integer> map) {
        String lowestKey = null;
        int lowestVal = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (e.getValue() < lowestVal) {
                lowestVal = e.getValue();
                lowestKey = e.getKey();
            }
        }
        return lowestKey;
    }

    private long getTotalCombatXp() {
        try {
            var clientThread = Microbot.getClientThread();
            if (clientThread == null) return 0L;
            return clientThread.runOnClientThreadOptional(() -> {
                long total = 0;
                Skill[] combatSkills = {Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.HITPOINTS, Skill.RANGED, Skill.PRAYER, Skill.MAGIC};
                for (Skill skill : combatSkills) {
                    try {
                        total += Microbot.getClient().getSkillExperience(skill);
                    } catch (Exception e) {
                        int level = Microbot.getClient().getRealSkillLevel(skill);
                        total += Experience.getXpForLevel(level);
                    }
                }
                return total;
            }).orElse(0L);
        } catch (Exception e) {
            return 0L;
        }
    }

    private String formatRuntime(long ms) {
        long hours = ms / 3600000;
        long minutes = (ms % 3600000) / 60000;
        long seconds = (ms % 60000) / 1000;
        return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }

    private static final class DiskStats {
        private long runtimeMs;
        private long xpEarned;
        private final LinkedHashMap<String, Integer> bullied = new LinkedHashMap<>();
    }
}
