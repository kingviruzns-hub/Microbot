package net.runelite.client.plugins.microbot.viruzosbot;

import java.awt.*;

import net.runelite.client.plugins.microbot.viruzosbot.Config;
import java.io.IOException;
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
            if (json == null || json.trim().isEmpty()) return;
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

    public double getKdRatio() {
        return deaths > 0 ? (double) successfulHits / deaths : successfulHits;
    }

    public double getAttacksPerMinute() {
        long elapsed = System.currentTimeMillis() - sessionStartMs;
        double minutes = elapsed / 60000.0;
        return minutes > 0 ? totalAttacks / minutes : 0;
    }

    public double getPrayerAccuracy() {
        int total = correctPrayers + missedPrayers;
        return total > 0 ? (double) correctPrayers / total * 100.0 : 0;
    }

    public double getAverageAttackGap() {
        if (attackTimeCount == 0) return 0;
        long sum = 0;
        int count = Math.min(attackTimeCount, attackTimes.length);
        for (int i = 0; i < count; i++) {
            sum += attackTimes[i];
        }
        return (double) sum / count / 1000.0;
    }

    public double getCombatTimePercentage() {
        long combatTimeMs = totalCombatTimeMs;
        if (inCombat) {
            combatTimeMs += System.currentTimeMillis() - combatSessionStartMs;
        }
        long elapsed = System.currentTimeMillis() - sessionStartMs;
        return elapsed > 0 ? (double) combatTimeMs / elapsed * 100.0 : 0;
    }

    public long getSessionElapsed() {
        return System.currentTimeMillis() - sessionStartMs;
    }

    public String formatElapsed() {
        long elapsed = getSessionElapsed();
        return String.format("%d:%02d:%02d", elapsed / 3600000, (elapsed % 3600000) / 60000, (elapsed % 60000) / 1000);
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

    public void resetSession() {
        sessionStartMs = System.currentTimeMillis();
        totalAttacks = 0;
        successfulHits = 0;
        prayerSwitches = 0;
        correctPrayers = 0;
        missedPrayers = 0;
        timesTargetFoughtBack = 0;
        timesTargetRan = 0;
        reportsFiled = 0;
        deaths = 0;
        playersScanned = 0;
        dragAttempts = 0;
        antiLureTriggers = 0;
        mistakesMade = 0;
        lastAttackMs = 0L;
        totalCombatTimeMs = 0L;
        combatSessionStartMs = 0L;
        inCombat = false;
        attackTimeIndex = 0;
        attackTimeCount = 0;
        nextTickSaveMs = 0L;
    }

    public void drawStats(Graphics g, int x, int y, double fatigueFactor) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int line = y;
        FontMetrics fm;
        int boxWidth = 188;
        int leftX = x - 3;
        int rowH = 14;
        int rightX = leftX + boxWidth - 6;

        g.setFont(new Font("Arial", Font.BOLD, 12));

        // Header with icon
        g.setColor(new Color(26, 180, 250, 30));
        g.fillRect(leftX, line, boxWidth, 16);
        g.setColor(new Color(26, 180, 250));
        String headerText = "SESSION STATS";
        g.drawString(headerText, leftX + 6, line + 12);
        g.setColor(new Color(26, 180, 250, 40));
        g.drawLine(leftX + 6, line + 16, leftX + boxWidth - 6, line + 16);
        line += 22;

        g.setFont(new Font("Consolas", Font.PLAIN, 11));
        fm = g.getFontMetrics();

        // === COMBAT SECTION ===

        // Attack row
        g.setColor(new Color(60, 60, 60, 40));
        g.fillRect(leftX + 3, line - 10, boxWidth - 6, rowH);
        g.setColor(new Color(180, 200, 220));
        String attStr = String.format("%,d", totalAttacks);
        g.drawString("Attacks: " + attStr, leftX + 6, line);
        line += rowH;

        // Hit row
        g.setColor(new Color(60, 60, 60, 40));
        g.fillRect(leftX + 3, line - 10, boxWidth - 6, rowH);
        g.setColor(new Color(130, 200, 130));
        String hitStr = String.format("%,d", successfulHits);
        g.drawString("Hits: " + hitStr, leftX + 6, line);
        line += rowH;

        // APM & Gap row
        g.setColor(new Color(200, 200, 200));
        String apmStr = String.format("%.1f", getAttacksPerMinute());
        String gapStr = String.format("%.1fs", getAverageAttackGap());
        g.drawString("APM: " + apmStr, leftX + 6, line);
        g.drawString("Gap: " + gapStr, rightX - fm.stringWidth("Gap: " + gapStr), line);
        line += rowH;

        // Separator
        g.setColor(new Color(255, 255, 255, 15));
        g.drawLine(leftX + 8, line, leftX + boxWidth - 8, line);
        line += 6;

        // Combat time with progress bar
        double combatPct = getCombatTimePercentage();
        Color combatColor = combatPct > 70 ? new Color(80, 220, 90) :
                           combatPct > 40 ? new Color(255, 210, 70) :
                           new Color(180, 180, 180);
        g.setColor(combatColor);
        String ctStr = String.format("Combat: %.0f%%", combatPct);
        g.drawString(ctStr, leftX + 6, line);
        drawMiniBar(g, rightX - 44, line - 7, combatPct / 100.0, combatColor);
        line += rowH;

        // Prayer with progress bar
        double prayerAcc = getPrayerAccuracy();
        Color prayerColor = prayerAcc > 80 ? new Color(80, 220, 90) :
                           prayerAcc > 50 ? new Color(255, 210, 70) :
                           new Color(240, 100, 100);
        g.setColor(prayerColor);
        String prStr = String.format("Prayer: %.0f%%", prayerAcc);
        g.drawString(prStr, leftX + 6, line);
        String swStr = prayerSwitches + " sw";
        g.setFont(new Font("Consolas", Font.PLAIN, 9));
        g.setColor(new Color(140, 140, 140));
        g.drawString(swStr, rightX - 44 - fm.stringWidth(swStr) - 4, line + 1);
        drawMiniBar(g, rightX - 44, line - 7, prayerAcc / 100.0, prayerColor);
        g.setFont(new Font("Consolas", Font.PLAIN, 11));
        line += rowH;

        // K/D row
        double kd = getKdRatio();
        Color kdColor = kd > 3.0 ? new Color(80, 255, 80) :
                       kd > 2.0 ? new Color(100, 230, 100) :
                       kd > 0.5 ? new Color(200, 200, 200) :
                       new Color(230, 100, 100);
        g.setColor(kdColor);
        String kdStr = String.format("K/D: %.2f", kd);
        g.drawString(kdStr, leftX + 6, line);
        g.setColor(new Color(160, 160, 160));
        String fbStr = "F/B: " + timesTargetFoughtBack;
        g.drawString(fbStr, rightX - fm.stringWidth(fbStr), line);
        line += rowH;

        // Separator
        g.setColor(new Color(255, 255, 255, 15));
        g.drawLine(leftX + 8, line, leftX + boxWidth - 8, line);
        line += 5;

        // === MISC SECTION ===

        // Misc row 1
        g.setColor(new Color(60, 60, 60, 40));
        g.fillRect(leftX + 3, line - 10, boxWidth - 6, rowH);
        g.setColor(new Color(170, 180, 190));
        String repStr = "Reports: " + reportsFiled;
        g.drawString(repStr, leftX + 6, line);
        g.setColor(new Color(180, 170, 200));
        String dragStr = "Drags: " + dragAttempts;
        g.drawString(dragStr, rightX - fm.stringWidth(dragStr), line);
        line += rowH;

        // Misc row 2
        g.setColor(new Color(170, 180, 190));
        String scanStr = "Scans: " + playersScanned;
        g.drawString(scanStr, leftX + 6, line);
        g.setColor(new Color(180, 170, 200));
        String lureStr = "Lures: " + antiLureTriggers;
        g.drawString(lureStr, rightX - fm.stringWidth(lureStr), line);
        line += rowH;

        // Misc row 3
        g.setColor(new Color(200, 160, 160));
        String mistStr = "Mistakes: " + mistakesMade;
        g.drawString(mistStr, leftX + 6, line);
        g.setColor(new Color(160, 180, 200));
        String ranStr = "Ran: " + timesTargetRan;
        g.drawString(ranStr, rightX - fm.stringWidth(ranStr), line);
        line += rowH;

        // Separator
        g.setColor(new Color(255, 255, 255, 15));
        g.drawLine(leftX + 8, line, leftX + boxWidth - 8, line);
        line += 6;

        // Fatigue with progress bar
        g.setColor(new Color(60, 60, 60, 40));
        g.fillRect(leftX + 3, line - 10, boxWidth - 6, rowH);
        Color fatColor = fatigueFactor > 8.0 ? new Color(255, 90, 90) :
                        fatigueFactor > 4.0 ? new Color(255, 200, 70) :
                        new Color(100, 200, 255);
        g.setColor(fatColor);
        String fatStr = String.format("Fatigue: %.2f", fatigueFactor);
        g.drawString(fatStr, leftX + 6, line);
        double fatPct = Math.min(1.0, fatigueFactor / 15.0);
        drawMiniBar(g, rightX - 44, line - 7, fatPct, fatColor);
    }

    private void drawMiniBar(Graphics g, int x, int y, double pct, Color color) {
        int w = 42;
        int h = 6;
        g.setColor(new Color(25, 25, 30));
        g.fillRoundRect(x, y, w, h, 2, 2);
        int fillW = (int) Math.round(w * Math.min(1.0, Math.max(0.0, pct)));
        if (fillW > 0) {
            Color glow = new Color(color.getRed(), color.getGreen(), color.getBlue(), 60);
            g.setColor(glow);
            g.fillRoundRect(Math.max(x, x - 1), y - 1, Math.min(w + 2, fillW + 2), h + 2, 3, 3);
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 220));
            g.fillRoundRect(x, y, fillW, h, 2, 2);
        }
        g.setColor(new Color(60, 60, 65, 120));
        g.drawRoundRect(x, y, w, h, 2, 2);
    }

    public String generateStatusSummary() {
        return String.format("[%s] %d atk, %.1f apm, K/D %.2f, prayer %.0f%% (%d sw), %d reps, %d drags, %d mistakes, %d lures, %d scans",
                formatElapsed(), totalAttacks, getAttacksPerMinute(), getKdRatio(), getPrayerAccuracy(),
                prayerSwitches, reportsFiled, dragAttempts, mistakesMade, antiLureTriggers, playersScanned);
    }
}