package net.runelite.client.plugins.microbot.viruzosbot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ThreadLocalRandom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SessionTracker {
    public SessionTracker() {
        load();
    }
    private long sessionStartMs = System.currentTimeMillis();
    private int totalAttacks = 0;
    private int successfulHits = 0;
    private int prayerSwitches = 0;
    private int correctPrayers = 0;
    private int missedPrayers = 0;
    private int timesTargetFoughtBack = 0;
    private int timesTargetRan = 0;
    private int reportsFiled = 0;
    private int deaths = 0;
    private int playersScanned = 0;
    private int dragAttempts = 0;
    private int antiLureTriggers = 0;
    private int mistakesMade = 0;
    private long lastAttackMs = 0L;
    private long totalCombatTimeMs = 0L;
    private long combatSessionStartMs = 0L;
    private boolean inCombat = false;

    private final long[] attackTimes = new long[100];
    private int attackTimeIndex = 0;
    private int attackTimeCount = 0;
    private long nextTickSaveMs = 0L;
    private static final long TICK_SAVE_INTERVAL_MS = 5_000L;

    public void recordAttack() {
        totalAttacks++;
        long now = System.currentTimeMillis();
        if (lastAttackMs > 0) {
            long gap = now - lastAttackMs;
            attackTimes[attackTimeIndex] = gap;
            attackTimeIndex = (attackTimeIndex + 1) % attackTimes.length;
            if (attackTimeCount < attackTimes.length) {
                attackTimeCount++;
            }
        }
        lastAttackMs = now;
        if (!inCombat) {
            combatSessionStartMs = now;
            inCombat = true;
        }
    }

    public void recordCombatEnd() {
        if (inCombat) {
            totalCombatTimeMs += System.currentTimeMillis() - combatSessionStartMs;
            inCombat = false;
        }
    }

    public void recordSuccessfulHit() { successfulHits++; }
    public void recordPrayerSwitch() { prayerSwitches++; }
    public void recordCorrectPrayer() { correctPrayers++; }
    public void recordMissedPrayer() { missedPrayers++; }
    public void recordTargetFoughtBack() { timesTargetFoughtBack++; }
    public void recordTargetRan() { timesTargetRan++; }
    public void recordReport() { reportsFiled++; }
    public void recordDeath() { deaths++; }
    public void recordScan() { playersScanned++; }
    public void recordAntiLure() { antiLureTriggers++; }
    public void recordMistake() { mistakesMade++; }
    public void recordAntiBan() {}

    public void tickSave() {
        long now = System.currentTimeMillis();
        if (now >= nextTickSaveMs) {
            if (inCombat) {
                totalCombatTimeMs += now - combatSessionStartMs;
                combatSessionStartMs = now;
            }
            nextTickSaveMs = now + TICK_SAVE_INTERVAL_MS;
        }
    }

    public void saveTrackerInfo() {
        save();
    }

    public void save() {
        try {
            Path path = Paths.get(Config.getSessionFilePath());
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
            java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
            data.put("sessionStartMs", sessionStartMs);
            data.put("totalAttacks", totalAttacks);
            data.put("successfulHits", successfulHits);
            data.put("prayerSwitches", prayerSwitches);
            data.put("correctPrayers", correctPrayers);
            data.put("missedPrayers", missedPrayers);
            data.put("timesTargetFoughtBack", timesTargetFoughtBack);
            data.put("timesTargetRan", timesTargetRan);
            data.put("reportsFiled", reportsFiled);
            data.put("deaths", deaths);
            data.put("playersScanned", playersScanned);
            data.put("dragAttempts", dragAttempts);
            data.put("antiLureTriggers", antiLureTriggers);
            data.put("mistakesMade", mistakesMade);
            data.put("lastAttackMs", lastAttackMs);
            data.put("totalCombatTimeMs", totalCombatTimeMs);
            data.put("combatSessionStartMs", combatSessionStartMs);
            data.put("inCombat", inCombat);
            data.put("attackTimes", attackTimes);
            data.put("attackTimeIndex", attackTimeIndex);
            data.put("attackTimeCount", attackTimeCount);
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(tmp, new GsonBuilder().setPrettyPrinting().create().toJson(data));
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception moveEx) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            System.out.println("SessionTracker save error: " + e.getMessage());
        }
    }

    public void load() {
        try {
            Path path = Paths.get(Config.getSessionFilePath());
            if (!Files.exists(path)) return;
            String json = Files.readString(path);
            if (json.trim().isEmpty()) return;
            Gson gson = new Gson();
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.Map<String, Object>>(){}.getType();
            java.util.Map<String, Object> data = gson.fromJson(json, type);
            if (data == null) return;
            if (data.get("sessionStartMs") instanceof Number) sessionStartMs = ((Number)data.get("sessionStartMs")).longValue();
            if (data.get("totalAttacks") instanceof Number) totalAttacks = ((Number)data.get("totalAttacks")).intValue();
            if (data.get("successfulHits") instanceof Number) successfulHits = ((Number)data.get("successfulHits")).intValue();
            if (data.get("prayerSwitches") instanceof Number) prayerSwitches = ((Number)data.get("prayerSwitches")).intValue();
            if (data.get("correctPrayers") instanceof Number) correctPrayers = ((Number)data.get("correctPrayers")).intValue();
            if (data.get("missedPrayers") instanceof Number) missedPrayers = ((Number)data.get("missedPrayers")).intValue();
            if (data.get("timesTargetFoughtBack") instanceof Number) timesTargetFoughtBack = ((Number)data.get("timesTargetFoughtBack")).intValue();
            if (data.get("timesTargetRan") instanceof Number) timesTargetRan = ((Number)data.get("timesTargetRan")).intValue();
            if (data.get("reportsFiled") instanceof Number) reportsFiled = ((Number)data.get("reportsFiled")).intValue();
            if (data.get("deaths") instanceof Number) deaths = ((Number)data.get("deaths")).intValue();
            if (data.get("playersScanned") instanceof Number) playersScanned = ((Number)data.get("playersScanned")).intValue();
            if (data.get("dragAttempts") instanceof Number) dragAttempts = ((Number)data.get("dragAttempts")).intValue();
            if (data.get("antiLureTriggers") instanceof Number) antiLureTriggers = ((Number)data.get("antiLureTriggers")).intValue();
            if (data.get("mistakesMade") instanceof Number) mistakesMade = ((Number)data.get("mistakesMade")).intValue();
            if (data.get("lastAttackMs") instanceof Number) lastAttackMs = ((Number)data.get("lastAttackMs")).longValue();
            if (data.get("totalCombatTimeMs") instanceof Number) totalCombatTimeMs = ((Number)data.get("totalCombatTimeMs")).longValue();
            if (data.get("combatSessionStartMs") instanceof Number) combatSessionStartMs = ((Number)data.get("combatSessionStartMs")).longValue();
            if (data.get("inCombat") instanceof Boolean) inCombat = (Boolean) data.get("inCombat");
            if (data.get("attackTimeIndex") instanceof Number) attackTimeIndex = ((Number)data.get("attackTimeIndex")).intValue();
            if (data.get("attackTimeCount") instanceof Number) attackTimeCount = ((Number)data.get("attackTimeCount")).intValue();
            Object at = data.get("attackTimes");
            if (at instanceof java.util.List) {
                java.util.List<?> list = (java.util.List<?>) at;
                for (int i = 0; i < Math.min(list.size(), attackTimes.length); i++) {
                    Object v = list.get(i);
                    if (v instanceof Number) attackTimes[i] = ((Number)v).longValue();
                }
            }
        } catch (Exception e) {
            System.out.println("SessionTracker load error: " + e.getMessage());
        }
    }

    public double getAttacksPerMinute() {
        long elapsed = System.currentTimeMillis() - sessionStartMs;
        double minutes = elapsed / 60000.0;
        return minutes > 0 ? totalAttacks / minutes : 0;
    }

    public int getRecommendedScanInterval(double fatigueFactor) {
        double base = 4000;
        double apm = getAttacksPerMinute();
        if (apm > 8) base *= 0.7;
        else if (apm < 3) base *= 1.2;
        double var = base * fatigueFactor * 0.03;
        double interval = base + ThreadLocalRandom.current().nextGaussian() * var;
        return (int) Math.max(1500, Math.min(12000, interval));
    }

    public int getRecommendedPostCombatPause(double fatigueFactor) {
        double base = 2000;
        double apm = getAttacksPerMinute();
        if (apm > 6) base *= 0.6;
        double var = base * fatigueFactor * 0.04;
        double pause = base + ThreadLocalRandom.current().nextGaussian() * var;
        return (int) Math.max(600, Math.min(8000, pause));
    }
}
