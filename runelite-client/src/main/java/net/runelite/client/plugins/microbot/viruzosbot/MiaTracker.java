package net.runelite.client.plugins.microbot.viruzosbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import lombok.Getter;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MiaTracker {
    public static final long MIA_TIMEOUT_MS = 25L * 60L * 1000L;

    private static final long SAVE_INTERVAL_MS = 10_000L;
    private static final long STALE_ENTRY_MS = 7L * 24L * 60L * 60L * 1000L;
    private static final Set<String> MONITORED_LOCATIONS =
            Set.of("lobby", "safe zone", "safezone", "portal", "middle", "hills");

    private final Map<String, TrackedTarget> targets = new ConcurrentHashMap<>();
    private final String filePath;
    private long nextSaveMs = 0L;

    public MiaTracker() {
        this.filePath = Config.getMiaTargetsFilePath();
        load();
    }

    @Getter
    public static class TrackedTarget {
        private String name;
        private String location;
        private long lastSeenMs;
        private long lastEngagedMs;
        private boolean armed;
        private boolean alerted;

        @SuppressWarnings("unused")
        TrackedTarget() {
        }

        TrackedTarget(String name) {
            this.name = name;
        }
    }

    @Getter
    public static class MiaAlert {
        private final String name;
        private final long lastSeenMs;
        private final String location;

        MiaAlert(String name, long lastSeenMs, String location) {
            this.name = name;
            this.lastSeenMs = lastSeenMs;
            this.location = location;
        }
    }

    public void recordSighting(String playerName, String location, long now) {
        String key = normalizeName(playerName);
        if (key.isEmpty()) return;
        String normalizedLocation = normalizeLocation(location);
        if (isMonitoredLocation(normalizedLocation)) return;

        TrackedTarget target = targets.computeIfAbsent(key, k -> new TrackedTarget(playerName));
        target.name = playerName;
        target.location = canonicalLocation(normalizedLocation);
        target.lastSeenMs = Math.max(target.lastSeenMs, now);
        target.alerted = false;
    }

    public void recordEngagement(String playerName, String targetLocation, String localLocation, long now) {
        String key = normalizeName(playerName);
        if (key.isEmpty()) return;
        String targetArea = normalizeLocation(targetLocation);
        String localArea = normalizeLocation(localLocation);
        if (isMonitoredLocation(targetArea) || isMonitoredLocation(localArea)) return;
        if (!targetArea.equals(localArea)) return;

        TrackedTarget target = targets.computeIfAbsent(key, k -> new TrackedTarget(playerName));
        target.name = playerName;
        target.location = canonicalLocation(targetArea);
        target.lastSeenMs = Math.max(target.lastSeenMs, now);
        target.lastEngagedMs = now;
        target.armed = true;
        target.alerted = false;
    }

    public List<MiaAlert> collectDueAlerts(long now, Set<String> activeTargets) {
        List<MiaAlert> alerts = new ArrayList<>();
        boolean changed = false;

        for (Iterator<Map.Entry<String, TrackedTarget>> it = targets.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, TrackedTarget> entry = it.next();
            String key = entry.getKey();
            TrackedTarget target = entry.getValue();

            if (target == null || !activeTargets.contains(key)) {
                it.remove();
                changed = true;
                continue;
            }
            if (target.lastSeenMs > 0L && now - target.lastSeenMs > STALE_ENTRY_MS) {
                it.remove();
                changed = true;
                continue;
            }
            if (!target.armed || target.alerted || target.lastSeenMs <= 0L) {
                continue;
            }
            if (now - target.lastSeenMs >= MIA_TIMEOUT_MS) {
                target.alerted = true;
                changed = true;
                alerts.add(new MiaAlert(
                        target.name != null ? target.name : key,
                        target.lastSeenMs,
                        canonicalLocation(normalizeLocation(target.location))
                ));
            }
        }

        if (changed) save();
        return alerts;
    }

    public void tickSave() {
        long now = System.currentTimeMillis();
        if (now >= nextSaveMs) {
            save();
            nextSaveMs = now + SAVE_INTERVAL_MS;
        }
    }

    public void saveTrackerInfo() {
        save();
    }

    public synchronized void save() {
        try {
            Path path = Paths.get(filePath);
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
            List<TrackedTarget> list = new ArrayList<>(targets.values());
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(list);
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(tmp, json);
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception moveEx) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            System.out.println("MiaTracker save error: " + e.getMessage());
        }
    }

    private void load() {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) return;
            String json = Files.readString(path);
            if (json.trim().isEmpty()) return;
            Type type = new TypeToken<List<TrackedTarget>>() {
            }.getType();
            List<TrackedTarget> loaded = new Gson().fromJson(json, type);
            if (loaded == null) return;
            for (TrackedTarget target : loaded) {
                if (target == null || target.name == null) continue;
                String key = normalizeName(target.name);
                if (key.isEmpty()) continue;
                TrackedTarget existing = targets.get(key);
                if (existing == null || target.lastSeenMs >= existing.lastSeenMs) {
                    targets.put(key, target);
                }
            }
        } catch (Exception e) {
            System.out.println("MiaTracker load error: " + e.getMessage());
        }
    }

    private static String normalizeName(String playerName) {
        if (playerName == null) return "";
        return playerName.toLowerCase(Locale.ROOT).trim();
    }

    private static String normalizeLocation(String location) {
        if (location == null) return "";
        String value = location.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("ffa - ")) {
            value = value.substring(6).trim();
        } else if (value.startsWith("ffa ")) {
            value = value.substring(4).trim();
        }
        if ("safezone".equals(value)) value = "safe zone";
        return value;
    }

    private static boolean isMonitoredLocation(String normalizedLocation) {
        return !MONITORED_LOCATIONS.contains(normalizedLocation);
    }

    private static String canonicalLocation(String normalizedLocation) {
        switch (normalizedLocation) {
            case "lobby":
                return "Lobby";
            case "safe zone":
                return "Safe Zone";
            case "portal":
                return "Portal";
            case "middle":
                return "Middle";
            default:
                return normalizedLocation;
        }
    }
}
