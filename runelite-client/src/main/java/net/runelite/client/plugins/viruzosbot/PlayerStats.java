package net.runelite.client.plugins.viruzosbot;

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

public class PlayerStats {
    private static final int MAX_BULLIED = 10;
    private static final long SAVE_INTERVAL_MS = 5_000L;

    private final AtomicLong totalRuntimeMs = new AtomicLong(0);
    private long sessionStartMs = 0L;
    private long currentSessionStartMs = 0L;
    private long xpAtStart = 0L;
    private long totalXpEarned = 0L;
    private final LinkedHashMap<String, Integer> bulliedAttacks = new LinkedHashMap<>();
    private long lastSaveMs = System.currentTimeMillis();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>(){}.getType();

    public void startSession() {
        sessionStartMs = System.currentTimeMillis();
        currentSessionStartMs = sessionStartMs;
        xpAtStart = getTotalCombatXp();
        load();
    }

    public void recordAttack(String targetName) {
        if (targetName == null) return;
        String key = targetName.toLowerCase(Locale.ROOT).trim();
        if (key.isEmpty()) return;
        bulliedAttacks.merge(key, 1, Integer::sum);
        trimBullied();
    }

    public void updateXpEarned() {
        long currentXp = getTotalCombatXp();
        long gained = currentXp - xpAtStart;
        if (gained > 0) {
            totalXpEarned += gained;
            xpAtStart = currentXp;
        }
    }

    public boolean shouldPeriodicSave() {
        long now = System.currentTimeMillis();
        if (now - lastSaveMs >= SAVE_INTERVAL_MS) {
            lastSaveMs = now;
            return true;
        }
        return false;
    }

    public void save() {
        updateXpEarned();
        long now = System.currentTimeMillis();
        long currentRuntimeMs = now - sessionStartMs;
        totalRuntimeMs.addAndGet(currentRuntimeMs);
        sessionStartMs = now;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("Current Runtime", formatRuntime(currentRuntimeMs));
        data.put("Runtime", formatRuntime(totalRuntimeMs.get()));
        data.put("RuntimeMs", totalRuntimeMs.get());
        data.put("XP Earned", totalXpEarned);

        List<Map<String, Object>> bulliedList = new ArrayList<>();
        for (Map.Entry<String, Integer> e : bulliedAttacks.entrySet()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", e.getKey());
            entry.put("attacks", e.getValue());
            bulliedList.add(entry);
        }
        data.put("Most Bullied", bulliedList);

        Path path = Paths.get(Config.getStatsFilePath());
        try {
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
            Files.writeString(path, GSON.toJson(data));
        } catch (IOException e) {
            System.err.println("PlayerStats: Error saving stats.json: " + e.getMessage());
        }
    }

    public void load() {
        Path path = Paths.get(Config.getStatsFilePath());
        if (!Files.exists(path)) return;

        try {
            String json = Files.readString(path);
            Map<String, Object> data = GSON.fromJson(json, MAP_TYPE);
            if (data == null) return;

            if (data.containsKey("RuntimeMs")) {
                totalRuntimeMs.set(((Number) data.get("RuntimeMs")).longValue());
            }
            if (data.containsKey("XP Earned")) {
                totalXpEarned = ((Number) data.get("XP Earned")).longValue();
            }
            if (data.containsKey("Most Bullied")) {
                List<Map<String, Object>> list = GSON.fromJson(
                        GSON.toJson(data.get("Most Bullied")),
                        new TypeToken<List<Map<String, Object>>>(){}.getType()
                );
                bulliedAttacks.clear();
                if (list != null) {
                    for (Map<String, Object> entry : list) {
                        String name = (String) entry.get("name");
                        int attacks = ((Number) entry.get("attacks")).intValue();
                        if (name != null) bulliedAttacks.put(name, attacks);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("PlayerStats: Error loading stats.json: " + e.getMessage());
        }

        sessionStartMs = System.currentTimeMillis();
        xpAtStart = getTotalCombatXp();
    }

    public String getRuntimeString() {
        long total = totalRuntimeMs.get() + (System.currentTimeMillis() - sessionStartMs);
        return formatRuntime(total);
    }

    public String getCurrentRuntimeString() {
        long start = currentSessionStartMs > 0 ? currentSessionStartMs : sessionStartMs;
        return formatRuntime(System.currentTimeMillis() - start);
    }

    public long getTotalXpEarned() {
        updateXpEarned();
        return totalXpEarned;
    }

    public List<Map.Entry<String, Integer>> getTopBullied(int limit) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(bulliedAttacks.entrySet());
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        return list.subList(0, Math.min(limit, list.size()));
    }

    public String generateStatusSummary() {
        updateXpEarned();
        long total = totalRuntimeMs.get() + (System.currentTimeMillis() - sessionStartMs);
        StringBuilder sb = new StringBuilder();
        sb.append("Runtime: ").append(formatRuntime(total));
        sb.append(" | XP Earned: ").append(totalXpEarned);
        if (!bulliedAttacks.isEmpty()) {
            sb.append(" | Most Bullied: ");
            int i = 0;
            for (Map.Entry<String, Integer> e : bulliedAttacks.entrySet()) {
                if (i++ > 0) sb.append(", ");
                sb.append(e.getKey()).append("(").append(e.getValue()).append(")");
                if (i >= 3) break;
            }
        }
        return sb.toString();
    }

    private void trimBullied() {
        while (bulliedAttacks.size() > MAX_BULLIED) {
            String lowestKey = null;
            int lowestVal = Integer.MAX_VALUE;
            for (Map.Entry<String, Integer> e : bulliedAttacks.entrySet()) {
                if (e.getValue() < lowestVal) {
                    lowestVal = e.getValue();
                    lowestKey = e.getKey();
                }
            }
            if (lowestKey != null) bulliedAttacks.remove(lowestKey);
            else break;
        }
    }

    private long getTotalCombatXp() {
        return Microbot.getClientThread().runOnClientThreadOptional(() -> {
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
    }

    private String formatRuntime(long ms) {
        long hours = ms / 3600000;
        long minutes = (ms % 3600000) / 60000;
        long seconds = (ms % 60000) / 1000;
        return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }
}