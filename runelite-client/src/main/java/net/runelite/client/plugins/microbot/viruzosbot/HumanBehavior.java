package net.runelite.client.plugins.microbot.viruzosbot;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.viruzosbot.Config;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;

import lombok.Getter;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ThreadLocalRandom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class HumanBehavior {
    private static final int SCREEN_WIDTH = 765;
    private static final int SCREEN_HEIGHT = 503;
    private static final int ANGLE_UNITS = 2048;

    private long lastNaturalMouseMove = 0L;
    private static final long NATURAL_MOVE_INTERVAL_MS = 30000;

    private long sessionStartTime;
    @Getter
    private int totalActions;
    @Getter
    private int currentFatigueLevel;
    private long nextDistractionTime;

    public HumanBehavior() {
        this.sessionStartTime = System.currentTimeMillis();
        this.totalActions = 0;
        this.currentFatigueLevel = 0;
        scheduleNextDistraction();
        load();
    }

    public void onAction() {
        totalActions++;
    }

    public int getFatigueDelay() {
        long elapsed = System.currentTimeMillis() - sessionStartTime;
        long minutesElapsed = elapsed / 60000;
        if (minutesElapsed < 5) return 0;
        int fatigue;
        if (minutesElapsed < 30) {
            fatigue = ThreadLocalRandom.current().nextInt(0, 50);
        } else if (minutesElapsed < 60) {
            fatigue = ThreadLocalRandom.current().nextInt(25, 120);
        } else if (minutesElapsed < 120) {
            fatigue = ThreadLocalRandom.current().nextInt(50, 250);
        } else {
            fatigue = ThreadLocalRandom.current().nextInt(100, 400);
        }
        if (totalActions > 0 && totalActions % 50 == 0) {
            fatigue += ThreadLocalRandom.current().nextInt(20, 80);
        }
        currentFatigueLevel = fatigue;
        return fatigue;
    }

    public boolean isDistracted() {
        long now = System.currentTimeMillis();
        if (now >= nextDistractionTime) {
            scheduleNextDistraction();
            return ThreadLocalRandom.current().nextInt(100) < 20;
        }
        return false;
    }

    private void scheduleNextDistraction() {
        long baseDelay = 60000 + ThreadLocalRandom.current().nextInt(180000);
        long fatigueExtra = getFatigueDelay() * 15L;
        nextDistractionTime = System.currentTimeMillis() + baseDelay + fatigueExtra;
    }

    public void handleDistraction() {
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < 50) {
            Global.sleep(1500 + ThreadLocalRandom.current().nextInt(4000));
        } else if (roll < 62) {
            if (Rs2Tab.switchTo(InterfaceTab.INVENTORY)) {
                Global.sleep(600 + ThreadLocalRandom.current().nextInt(1000));
                Rs2Tab.switchTo(InterfaceTab.INVENTORY);
            }
        } else if (roll < 74) {
            Microbot.naturalMouse.moveOffScreen();
            Global.sleep(2000 + ThreadLocalRandom.current().nextInt(4000));
        } else if (roll < 82) {
            int lookX = ThreadLocalRandom.current().nextInt(200, 550);
            int lookY = ThreadLocalRandom.current().nextInt(150, 350);
            naturalMouseMovement(lookX, lookY);
        } else if (roll < 88) {
            rotateCamera(getCameraYaw() + ThreadLocalRandom.current().nextInt(-15, 15),
                    getCameraPitch() + ThreadLocalRandom.current().nextInt(-8, 8));
            Global.sleep(300 + ThreadLocalRandom.current().nextInt(800));
        } else {
            Global.sleep(3000 + ThreadLocalRandom.current().nextInt(6000));
        }
    }

    public int getHumanReactionTime(ReactionContext context) {
        int baseDelay;
        int fatigueExtra = getFatigueDelay();
        double gaussianNoise = ThreadLocalRandom.current().nextGaussian() * 0.15;
        switch (context) {
            case COMBAT:
                baseDelay = 180 + ThreadLocalRandom.current().nextInt(500) + (int)(fatigueExtra * 0.3);
                break;
            case WALKING:
                baseDelay = 250 + ThreadLocalRandom.current().nextInt(700) + (int)(fatigueExtra * 0.4);
                break;
            case SWITCHING_PRAYERS:
                baseDelay = 120 + ThreadLocalRandom.current().nextInt(350) + (int)(fatigueExtra * 0.15);
                break;
            case CLICKING_PORTAL:
                baseDelay = 400 + ThreadLocalRandom.current().nextInt(1200) + (int)(fatigueExtra * 0.5);
                break;
            case EQUIPPING:
                baseDelay = 350 + ThreadLocalRandom.current().nextInt(800) + (int)(fatigueExtra * 0.3);
                break;
            case IDLE:
                baseDelay = 600 + ThreadLocalRandom.current().nextInt(2000) + (int)(fatigueExtra * 0.6);
                break;
            default:
                baseDelay = 250 + ThreadLocalRandom.current().nextInt(600) + (int)(fatigueExtra * 0.2);
        }
        int delay = Math.max(50, (int)(baseDelay * (1.0 + gaussianNoise)));
        if (ThreadLocalRandom.current().nextDouble() < 0.03) {
            delay += ThreadLocalRandom.current().nextInt(500, 2000);
        }
        return delay;
    }

    public boolean simulateHumanError(double errorRate) {
        return ThreadLocalRandom.current().nextDouble() < errorRate;
    }

    public void naturalMouseMovement(int targetX, int targetY) {
        Point startPos = mousePosition();
        int startX = startPos.x;
        int startY = startPos.y;
        int distance = Math.max(1, (int) Math.hypot(targetX - startX, targetY - startY));

        // Human mouse speed varies with distance and session fatigue
        double speedMod = 1.0;
        long elapsed = System.currentTimeMillis() - sessionStartTime;
        if (elapsed > 3600000) speedMod = 0.85 + ThreadLocalRandom.current().nextDouble() * 0.3;
        if (elapsed > 7200000) speedMod = 0.7 + ThreadLocalRandom.current().nextDouble() * 0.4;

        int steps = Math.max(8, (int)(distance / (2.5 * speedMod)));
        steps = Math.min(steps, 100);
        double spread = distance * 0.12 + ThreadLocalRandom.current().nextDouble() * 8;

        // Control points with asymmetric bias for natural arc
        double arcBias = ThreadLocalRandom.current().nextDouble() * 0.2 - 0.1;
        int cp1x = (int) (startX + (targetX - startX) * (0.2 + arcBias) + ThreadLocalRandom.current().nextGaussian() * spread * 0.4);
        int cp1y = (int) (startY + (targetY - startY) * (0.2 + arcBias) + ThreadLocalRandom.current().nextGaussian() * spread * 0.4);
        int cp2x = (int) (startX + (targetX - startX) * (0.8 - arcBias) + ThreadLocalRandom.current().nextGaussian() * spread * 0.4);
        int cp2y = (int) (startY + (targetY - startY) * (0.8 - arcBias) + ThreadLocalRandom.current().nextGaussian() * spread * 0.4);

        // Micro-pauses during movement (humans pause mid-motion)
        int pauseAt = steps > 15 ? ThreadLocalRandom.current().nextInt(steps / 3, 2 * steps / 3) : -1;
        boolean didPause = false;

        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            double u = 1.0 - t;
            int currentX = (int) (u * u * u * startX + 3 * u * u * t * cp1x + 3 * u * t * t * cp2x + t * t * t * targetX);
            int currentY = (int) (u * u * u * startY + 3 * u * u * t * cp1y + 3 * u * t * t * cp2y + t * t * t * targetY);
            Microbot.getMouse().move(new net.runelite.api.Point(currentX, currentY));

            // Bell-shaped velocity curve with occasional micro-pauses
            double velocityWeight = Math.sin(t * Math.PI);
            int stepDelay;
            if (i == pauseAt && !didPause && ThreadLocalRandom.current().nextDouble() < 0.35) {
                stepDelay = 60 + ThreadLocalRandom.current().nextInt(120);
                didPause = true;
            } else {
                stepDelay = 5 + (int) (16 * (1.0 - velocityWeight * 0.5) / speedMod);
            }
            Global.sleep(Math.max(2, stepDelay));
        }

        // Fine-tune landing with possible micro-correction
        Point endPos = mousePosition();
        if (Math.abs(endPos.x - targetX) > 2 || Math.abs(endPos.y - targetY) > 2) {
            Microbot.getMouse().move(new net.runelite.api.Point(targetX, targetY));
            Global.sleep(ThreadLocalRandom.current().nextInt(10, 40));
        }
    }

    public boolean shouldHesitate() {
        return ThreadLocalRandom.current().nextInt(100) < 12;
    }

    public int addHesitationDelay() {
        return ThreadLocalRandom.current().nextInt(200, 1200);
    }

    public boolean microMissclick() {
        return ThreadLocalRandom.current().nextInt(100) < 4;
    }

    public boolean shouldLookAround() {
        return ThreadLocalRandom.current().nextInt(100) < 5;
    }

    public void lookAround() {
        int yawChange = ThreadLocalRandom.current().nextInt(-25, 25);
        int pitchChange = ThreadLocalRandom.current().nextInt(-12, 12);
        rotateCamera(
                Math.max(0, Math.min(360, getCameraYaw() + yawChange)),
                Math.max(128, Math.min(383, getCameraPitch() + pitchChange))
        );
        Global.sleep(300 + ThreadLocalRandom.current().nextInt(1500));
    }

    public void resetSession() {
        sessionStartTime = System.currentTimeMillis();
        totalActions = 0;
        currentFatigueLevel = 0;
    }

    public void saveTrackerInfo() {
        save();
    }

    public void save() {
        try {
            Path path = Paths.get(Config.getHumanBehaviorFilePath());
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
            java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
            data.put("sessionStartTime", sessionStartTime);
            data.put("totalActions", totalActions);
            data.put("currentFatigueLevel", currentFatigueLevel);
            data.put("nextDistractionTime", nextDistractionTime);
            data.put("lastNaturalMouseMove", lastNaturalMouseMove);
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(tmp, new GsonBuilder().setPrettyPrinting().create().toJson(data));
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception moveEx) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            System.out.println("HumanBehavior save error: " + e.getMessage());
        }
    }

    public void load() {
        try {
            Path path = Paths.get(Config.getHumanBehaviorFilePath());
            if (!Files.exists(path)) return;
            String json = Files.readString(path);
            if (json == null || json.trim().isEmpty()) return;
            Gson gson = new Gson();
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.Map<String, Object>>(){}.getType();
            java.util.Map<String, Object> data = gson.fromJson(json, type);
            if (data == null) return;
            if (data.get("sessionStartTime") instanceof Number) sessionStartTime = ((Number)data.get("sessionStartTime")).longValue();
            if (data.get("totalActions") instanceof Number) totalActions = ((Number)data.get("totalActions")).intValue();
            if (data.get("currentFatigueLevel") instanceof Number) currentFatigueLevel = ((Number)data.get("currentFatigueLevel")).intValue();
            if (data.get("nextDistractionTime") instanceof Number) nextDistractionTime = ((Number)data.get("nextDistractionTime")).longValue();
            if (data.get("lastNaturalMouseMove") instanceof Number) lastNaturalMouseMove = ((Number)data.get("lastNaturalMouseMove")).longValue();
        } catch (Exception e) {
            System.out.println("HumanBehavior load error: " + e.getMessage());
        }
    }

    public long getSessionDurationMinutes() {
        return (System.currentTimeMillis() - sessionStartTime) / 60000;
    }

    public enum ReactionContext {
        COMBAT,
        WALKING,
        SWITCHING_PRAYERS,
        CLICKING_PORTAL,
        EQUIPPING,
        IDLE
    }

    public void naturalIdle() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastNaturalMouseMove;
        long adjustedInterval = (long)(NATURAL_MOVE_INTERVAL_MS * (1.5 + Math.random()));
        if (elapsed < adjustedInterval) return;
        lastNaturalMouseMove = now;
        double roll = ThreadLocalRandom.current().nextDouble();
        if (roll < 0.06) {
            int x = randInclusive(200, 550);
            int y = randInclusive(150, 350);
            naturalMouseMovement(x, y);
        } else if (roll < 0.10) {
            rotateCamera(
                    getCameraYaw() + randInclusive(-10, 10),
                    getCameraPitch() + randInclusive(-5, 5)
            );
        } else if (roll < 0.13) {
            Point cur = mousePosition();
            int nx = Math.max(0, Math.min(SCREEN_WIDTH, cur.x + ThreadLocalRandom.current().nextInt(-3, 4)));
            int ny = Math.max(0, Math.min(SCREEN_HEIGHT, cur.y + ThreadLocalRandom.current().nextInt(-3, 4)));
            if (Math.abs(nx - cur.x) + Math.abs(ny - cur.y) > 1) {
                Microbot.getMouse().move(new net.runelite.api.Point(nx, ny));
            }
        }
    }

    public long getNaturalAttackDelay() {
        long elapsed = System.currentTimeMillis() - sessionStartTime;
        double fatigueMod = 1.0;
        if (elapsed > 3600000) fatigueMod = 1.0 + (elapsed - 3600000) / 7200000.0 * 0.3;

        double mean = 180 * fatigueMod;
        double std = 60 * fatigueMod;
        double raw = mean + ThreadLocalRandom.current().nextGaussian() * std;
        long delay = Math.max(60, (long) raw);

        // Occasional slow reaction (distracted)
        if (ThreadLocalRandom.current().nextDouble() < 0.04) {
            delay += randInclusive(600, 2500);
        }

        // Very rare double-take (look away, look back)
        if (ThreadLocalRandom.current().nextDouble() < 0.008) {
            delay += randInclusive(1500, 4000);
        }

        return delay;
    }

    public AntiBanSuggestion getContextualAntiBanAction(double fatigueFactor) {
        long elapsed = System.currentTimeMillis() - sessionStartTime;
        double hours = elapsed / 3600000.0;

        // Action pool shifts over time - fresh sessions have more variety, fatigued sessions simpler
        if (hours < 1.0) {
            return getFreshSessionAntiBan(fatigueFactor);
        } else if (hours < 3.0) {
            return getNormalSessionAntiBan(fatigueFactor);
        } else {
            return getFatiguedSessionAntiBan(fatigueFactor);
        }
    }

    private AntiBanSuggestion getFreshSessionAntiBan(double fatigueFactor) {
        double roll = ThreadLocalRandom.current().nextDouble();
        double boost = fatigueFactor * 0.02;
        if (roll < 0.35 + boost) {
            return new AntiBanSuggestion("idle_gaze", 0.5);
        } else if (roll < 0.50 + boost) {
            return new AntiBanSuggestion("microbreak", 0.4);
        } else if (roll < 0.62 + boost) {
            return new AntiBanSuggestion("camera_turn", 0.3);
        } else if (roll < 0.72) {
            return new AntiBanSuggestion("tab_cycle", 0.2);
        } else if (roll < 0.80) {
            return new AntiBanSuggestion("skill_check", 0.15);
        } else if (roll < 0.86) {
            return new AntiBanSuggestion("inventory_hover", 0.1);
        } else if (roll < 0.92) {
            return new AntiBanSuggestion("prayer_check", 0.1);
        } else {
            return new AntiBanSuggestion("equipment_check", 0.1);
        }
    }

    private AntiBanSuggestion getNormalSessionAntiBan(double fatigueFactor) {
        double roll = ThreadLocalRandom.current().nextDouble();
        double boost = fatigueFactor * 0.015;
        if (roll < 0.40 + boost) {
            return new AntiBanSuggestion("idle_gaze", 0.5);
        } else if (roll < 0.55 + boost) {
            return new AntiBanSuggestion("microbreak", 0.4);
        } else if (roll < 0.67 + boost) {
            return new AntiBanSuggestion("camera_turn", 0.3);
        } else if (roll < 0.76) {
            return new AntiBanSuggestion("tab_cycle", 0.2);
        } else if (roll < 0.83) {
            return new AntiBanSuggestion("skill_check", 0.15);
        } else if (roll < 0.89) {
            return new AntiBanSuggestion("inventory_hover", 0.15);
        } else if (roll < 0.94) {
            return new AntiBanSuggestion("prayer_check", 0.15);
        } else {
            return new AntiBanSuggestion("equipment_check", 0.15);
        }
    }

    private AntiBanSuggestion getFatiguedSessionAntiBan(double fatigueFactor) {
        double roll = ThreadLocalRandom.current().nextDouble();
        double boost = fatigueFactor * 0.02;
        if (roll < 0.45 + boost) {
            return new AntiBanSuggestion("idle_gaze", 0.6);
        } else if (roll < 0.62 + boost) {
            return new AntiBanSuggestion("microbreak", 0.5);
        } else if (roll < 0.75 + boost) {
            return new AntiBanSuggestion("camera_turn", 0.4);
        } else if (roll < 0.84) {
            return new AntiBanSuggestion("tab_cycle", 0.25);
        } else if (roll < 0.91) {
            return new AntiBanSuggestion("skill_check", 0.2);
        } else {
            return new AntiBanSuggestion("inventory_hover", 0.2);
        }
    }

    public static class AntiBanSuggestion {
        public final String action;
        public final double durationWeight;
        public AntiBanSuggestion(String action, double durationWeight) {
            this.action = action;
            this.durationWeight = durationWeight;
        }
    }

    public int getNaturalClickVariation() {
        double roll = ThreadLocalRandom.current().nextDouble();
        if (roll < 0.08) return ThreadLocalRandom.current().nextInt(-15, 15);
        if (roll < 0.2) return ThreadLocalRandom.current().nextInt(-6, 6);
        if (roll < 0.5) return ThreadLocalRandom.current().nextInt(-3, 3);
        return 0;
    }

    public Point applyNaturalClickVariation(Point target) {
        int varX = getNaturalClickVariation();
        int varY = getNaturalClickVariation();
        return new Point(
                Math.max(0, Math.min(SCREEN_WIDTH, target.x + varX)),
                Math.max(0, Math.min(SCREEN_HEIGHT, target.y + varY))
        );
    }

    public void occasionalMisclick(int areaWidth, int areaHeight, double fatigueFactor) {
        if (ThreadLocalRandom.current().nextDouble() >= 0.015 + fatigueFactor * 0.002) return;
        int maxOffX = Math.max(4, Math.min(areaWidth / 8, 40));
        int maxOffY = Math.max(4, Math.min(areaHeight / 8, 40));
        int offX = ThreadLocalRandom.current().nextInt(-maxOffX, maxOffX + 1);
        int offY = ThreadLocalRandom.current().nextInt(-maxOffY, maxOffY + 1);
        Point current = mousePosition();
        Point misclickTarget = new Point(
                Math.max(0, Math.min(SCREEN_WIDTH, current.x + offX)),
                Math.max(0, Math.min(SCREEN_HEIGHT, current.y + offY))
        );

        // Aggressive overshoot then correct
        double heavyOvershoot = ThreadLocalRandom.current().nextDouble();
        if (heavyOvershoot < 0.1) {
            int farOffX = offX * 2 + ThreadLocalRandom.current().nextInt(-5, 6);
            int farOffY = offY * 2 + ThreadLocalRandom.current().nextInt(-5, 6);
            Point farPoint = new Point(
                    Math.max(0, Math.min(SCREEN_WIDTH, current.x + farOffX)),
                    Math.max(0, Math.min(SCREEN_HEIGHT, current.y + farOffY))
            );
            naturalMouseMovement(farPoint.x, farPoint.y);
            Global.sleep(randInclusive(80, 200));
        }

        naturalMouseMovement(misclickTarget.x, misclickTarget.y);

        // Sometimes pause on wrong spot before correcting
        if (ThreadLocalRandom.current().nextDouble() < 0.3) {
            Global.sleep(randInclusive(50, 200));
        }

        Global.sleep(randInclusive(100, 300));
        Point currentPos = mousePosition();
        if (Math.abs(currentPos.x - current.x) > 5 || Math.abs(currentPos.y - current.y) > 5) {
            naturalMouseMovement(current.x, current.y);
        } else {
            Microbot.getMouse().move(new net.runelite.api.Point(current.x, current.y));
        }
        Global.sleep(randInclusive(80, 250));
    }

    private static int randInclusive(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static Point mousePosition() {
        net.runelite.client.plugins.microbot.util.mouse.Mouse mouse = Microbot.getMouse();
        Point pos = mouse == null ? null : mouse.getMousePosition();
        return pos != null ? pos : new Point(0, 0);
    }

    private static int getCameraYaw() {
        return Rs2Camera.getAngle();
    }

    private static int getCameraPitch() {
        return Rs2Camera.getPitch();
    }

    private static void rotateCamera(int yawDegrees, int pitch) {
        int normalized = ((yawDegrees % 360) + 360) % 360;
        int yawUnits = (int) Math.round(normalized * (ANGLE_UNITS / 360.0)) % ANGLE_UNITS;
        Rs2Camera.setYaw(yawUnits);
        Rs2Camera.setPitch(pitch);
    }
}