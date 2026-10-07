package net.runelite.client.plugins.viruzosbot;

import java.awt.*;
import java.util.concurrent.ThreadLocalRandom;

public class SessionTracker {
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
            if (attackTimeCount < attackTimes.length) {
                attackTimes[attackTimeIndex] = gap;
                attackTimeIndex = (attackTimeIndex + 1) % attackTimes.length;
                attackTimeCount = Math.min(attackTimeCount + 1, attackTimes.length);
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
        long elapsed = System.currentTimeMillis() - sessionStartMs;
        return elapsed > 0 ? (double) totalCombatTimeMs / elapsed * 100.0 : 0;
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
        dragAttempts = 0;
        attackTimeCount = 0;
        totalCombatTimeMs = 0;
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