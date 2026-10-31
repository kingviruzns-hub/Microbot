package net.runelite.client.plugins.microbot.viruzosbot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.Setter;

import static java.nio.file.Paths.get;

public class NonTargetTracker {
    private final String filePath;
    private final Map<String, LastSeenEntry> entries = new ConcurrentHashMap<>();
    private long nextSaveMs = 0L;
    private static final long SAVE_INTERVAL_MS = 5_000L;

    @Getter
    public static class LastSeenEntry {
        private String playerName;
        private int x;
        private int y;
        private int z;
        private long lastSeenMs;
        private String location;
        private int combatLevel;
        @Setter
        private String status;

        @SuppressWarnings("unused")
        public LastSeenEntry() {}

        public LastSeenEntry(String playerName, int x, int y, int z, long lastSeenMs, String location, int combatLevel) {
            this.playerName = playerName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.lastSeenMs = lastSeenMs;
            this.location = location;
            this.combatLevel = combatLevel;
        }
    }

    public NonTargetTracker() {
        this.filePath = Config.getLastSeenFilePath();
        load();
        mergeWithAiData();
    }

    public void record(String playerName, int tileX, int tileY, int tileZ, String location, int combatLevel) {
        if (playerName == null || playerName.isEmpty()) return;
        if (combatLevel > 0 && combatLevel < 30) return;
        String key = playerName.toLowerCase(Locale.ROOT).trim();
        LastSeenEntry existing = entries.get(key);
        long now = System.currentTimeMillis();
        if (existing != null && now - existing.lastSeenMs < 5000) {
            existing.lastSeenMs = now;
            existing.x = tileX;
            existing.y = tileY;
            existing.z = tileZ;
            if (location != null) existing.location = location;
            if (combatLevel > 0) existing.combatLevel = combatLevel;
            return;
        }
        entries.put(key, new LastSeenEntry(playerName, tileX, tileY, tileZ, now, location, combatLevel));
    }

    public void record(net.runelite.api.Player player, String location) {
        if (player instanceof net.runelite.client.plugins.microbot.util.player.Rs2PlayerModel) {
            player = ((net.runelite.client.plugins.microbot.util.player.Rs2PlayerModel) player).getPlayer();
        }
        if (player == null || !exists(player) || player.getName() == null) return;
        net.runelite.api.coords.WorldPoint tile = getWorldLocation(player);
        if (tile == null) return;
        record(player.getName(), tile.getX(), tile.getY(), tile.getPlane(), location, getCombatLevel(player));
    }

    private static boolean exists(net.runelite.api.Player player) {
        return net.runelite.client.plugins.microbot.Microbot.getClientThread()
                .runOnClientThreadOptional(() -> playerExistsOnClientThread(player)).orElse(false);
    }

    private static boolean playerExistsOnClientThread(net.runelite.api.Player player) {
        net.runelite.api.Client client = net.runelite.client.plugins.microbot.Microbot.getClient();
        net.runelite.api.WorldView worldView = client.getTopLevelWorldView();
        return client.getLocalPlayer() != null && worldView != null
                && worldView.players().stream().anyMatch(p -> p == player);
    }

    private static net.runelite.api.coords.WorldPoint getWorldLocation(net.runelite.api.Player player) {
        return net.runelite.client.plugins.microbot.Microbot.getClientThread()
                .runOnClientThreadOptional(player::getWorldLocation).orElse(null);
    }

    private static int getCombatLevel(net.runelite.api.Player player) {
        return net.runelite.client.plugins.microbot.Microbot.getClientThread()
                .runOnClientThreadOptional(player::getCombatLevel).orElse(0);
    }

    public void tickSave() {
        long now = System.currentTimeMillis();
        if (now >= nextSaveMs) {
            save();
            nextSaveMs = now + SAVE_INTERVAL_MS;
        }
    }

    public void save() {
        try {
            Path path = get(filePath);
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
            List<LastSeenEntry> list = new ArrayList<>(entries.values());
            list.sort(Comparator.comparingLong(e -> -e.lastSeenMs));
            String json = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(list);
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(tmp, json);
            try {
                Files.move(tmp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception moveEx) {
                Files.move(tmp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            System.out.println("NonTargetTracker save error: " + e.getMessage());
        }
    }

    public void saveTrackerInfo() {
        save();
    }

    private void load() {
        Path path = get(filePath);
        if (!Files.exists(path)) return;
        try {
            String json = Files.readString(path);
            com.google.gson.Gson gson = new com.google.gson.Gson();
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<List<LastSeenEntry>>(){}.getType();
            List<LastSeenEntry> loaded = gson.fromJson(json, type);
            if (loaded != null) {
                Map<String, LastSeenEntry> deduped = new LinkedHashMap<>();
                for (LastSeenEntry e : loaded) {
                    if (e == null || e.playerName == null) continue;
                    String key = e.playerName.toLowerCase(Locale.ROOT).trim();
                    LastSeenEntry existing = deduped.get(key);
                    if (existing == null || e.lastSeenMs > existing.lastSeenMs) {
                        deduped.put(key, e);
                    }
                }
                entries.putAll(deduped);
            }
        } catch (Exception e) {
            System.out.println("NonTargetTracker load error: " + e.getMessage());
        }
    }

    private void mergeWithAiData() {
        try {
            Path path = get(Config.getAiDataFilePath());
            if (!Files.exists(path)) return;
            String json = Files.readString(path);
            com.google.gson.Gson gson = new com.google.gson.Gson();
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> raw = gson.fromJson(json, type);
            if (raw == null) return;
            Object profilesObj = raw.get("profiles");
            if (!(profilesObj instanceof Map)) return;
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> profiles = (Map<String, Map<String, Object>>) profilesObj;
            for (Map.Entry<String, Map<String, Object>> entry : profiles.entrySet()) {
                String key = entry.getKey().toLowerCase(Locale.ROOT).trim();
                Map<String, Object> profile = entry.getValue();
                if (profile == null) continue;
                Object lastSeenObj = profile.get("lastSeenAt");
                Object combatObj = profile.get("combatLevel");
                long aiLastSeen = 0L;
                if (lastSeenObj instanceof Number) {
                    aiLastSeen = ((Number) lastSeenObj).longValue();
                }
                int aiCombat = 0;
                if (combatObj instanceof Number) {
                    aiCombat = ((Number) combatObj).intValue();
                }
                LastSeenEntry existing = entries.get(key);
                if (existing != null) {
                    if (aiLastSeen > existing.lastSeenMs) {
                        existing.lastSeenMs = aiLastSeen;
                    }
                    if (aiCombat > 0) {
                        existing.combatLevel = aiCombat;
                    }
                } else if (aiLastSeen > 0) {
                    entries.put(key, new LastSeenEntry(entry.getKey(), 0, 0, 0, aiLastSeen, "Unknown", aiCombat));
                }
            }
        } catch (Exception e) {
            System.out.println("NonTargetTracker mergeWithAiData error: " + e.getMessage());
        }
    }

    public void recordPlayer(String playerName, int combatLevel) {
        record(playerName, 0, 0, 0, "Unknown", combatLevel);
    }

    public void trackPlayer(String playerName, int combatLevel) {
        recordPlayer(playerName, combatLevel);
    }

    public void updateLastSeen(String playerName, int combatLevel) {
        recordPlayer(playerName, combatLevel);
    }

    public void recordSeen(String playerName, int combatLevel) {
        recordPlayer(playerName, combatLevel);
    }

    public int getTrackedCount() {
        return entries.size();
    }

    public Map<String, LastSeenEntry> getEntries() {
        return Collections.unmodifiableMap(entries);
    }
}