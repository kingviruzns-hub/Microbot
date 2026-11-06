package net.runelite.client.plugins.microbot.viruzosbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Discord bot control surface for the Viruz FFA bot.
 *
 * <p>Polls a configured Discord channel for text commands using {@link HttpClient},
 * {@link HttpRequest} and {@link HttpResponse}, performs the requested file
 * operations, and replies with fancy embeds. After {@code !add}/{@code !del} the
 * bot client's nslist is reloaded.</p>
 */
@Slf4j
public class ViruzBotControl extends Script {

    private static final String DISCORD_API = "https://discord.com/api/v10";
    private static final HttpClient HTTP_CLIENT =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private static final int COLOR_SUCCESS = 0x2ECC71;
    private static final int COLOR_ERROR = 0xE74C3C;
    private static final int COLOR_INFO = 0x3498DB;
    private static final int COLOR_STATS = 0x9B59B6;
    private static final int COLOR_SEEN = 0x1ABC9C;

    private volatile ViruzFFABotScript script;
    private volatile ViruzFFABotScript botScript;

    private volatile boolean running = false;
    private volatile boolean initialized = false;
    private volatile String lastMessageId = null;

    private String token;
    private String channelId;

    @SuppressWarnings("unused")
    public ViruzBotControl() {
    }

    public void setScript(ViruzFFABotScript script) {
        this.script = script;
        this.botScript = script;
    }

    @Override
    public boolean run() {
        token = Config.getBotToken();
        channelId = Config.getChannelId();
        if (token == null || token.trim().isEmpty()) {
            log.warn("ViruzBotControl: BOT_TOKEN not found in {}", Config.class.getSimpleName());
        }
        if (channelId == null || channelId.trim().isEmpty()) {
            log.warn("ViruzBotControl: CHANNEL_ID not found; bot will not receive commands");
        }
        running = true;
        initialized = false;
        lastMessageId = null;
        scheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(
                this::safePoll, 0, 2, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public void shutdown() {
        running = false;
        super.shutdown();
    }

    private void safePoll() {
        try {
            poll();
        } catch (Throwable t) {
            log.warn("ViruzBotControl poll error: {}", t.getMessage());
        }
    }

    private void poll() {
        if (!running) return;
        if (token == null || token.trim().isEmpty()) return;
        if (channelId == null || channelId.trim().isEmpty()) return;

        String url = DISCORD_API + "/channels/" + channelId + "/messages?limit=50";
        if (lastMessageId != null) {
            url += "&after=" + lastMessageId;
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bot " + token)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("ViruzBotControl HTTP error: {}", e.getMessage());
            return;
        }

        if (response.statusCode() == 429) {
            handleRateLimit(response.body());
            return;
        }
        if (response.statusCode() != 200) {
            log.warn("ViruzBotControl unexpected status {}: {}", response.statusCode(), response.body());
            return;
        }

        JsonElement parsed;
        try {
            parsed = new JsonParser().parse(response.body());
        } catch (Exception e) {
            log.warn("ViruzBotControl JSON parse error: {}", e.getMessage());
            return;
        }
        if (parsed == null || !parsed.isJsonArray()) return;

        List<JsonObject> messages = new ArrayList<>();
        for (JsonElement el : parsed.getAsJsonArray()) {
            if (el.isJsonObject()) messages.add(el.getAsJsonObject());
        }
        messages.sort((a, b) -> compareSnowflake(
                a.has("id") ? a.get("id").getAsString() : "0",
                b.has("id") ? b.get("id").getAsString() : "0"));

        if (!initialized) {
            // First poll: record cursor without replaying historical commands.
            initialized = true;
            for (JsonObject m : messages) {
                if (m.has("id")) {
                    String id = m.get("id").getAsString();
                    if (lastMessageId == null || compareSnowflake(id, lastMessageId) > 0) {
                        lastMessageId = id;
                    }
                }
            }
            return;
        }

        String maxId = lastMessageId;
        for (JsonObject m : messages) {
            if (m.has("id")) {
                String id = m.get("id").getAsString();
                if (maxId == null || compareSnowflake(id, maxId) > 0) maxId = id;
            }

            JsonObject author = m.has("author") && m.get("author").isJsonObject()
                    ? m.getAsJsonObject("author") : null;
            if (author != null && author.has("bot") && author.get("bot").getAsBoolean()) {
                continue;
            }

            String content = m.has("content") ? m.get("content").getAsString() : "";
            if (content.startsWith("!")) {
                handleCommand(content.trim());
            }
        }
        if (maxId != null) lastMessageId = maxId;
    }

    private void handleRateLimit(String body) {
        try {
            JsonObject obj = new JsonParser().parse(body).getAsJsonObject();
            if (obj.has("retry_after")) {
                double retry = obj.get("retry_after").getAsDouble();
                long ms = (long) (retry * 1000.0) + 250L;
                Thread.sleep(ms);
            }
        } catch (Exception ignored) {
        }
    }

    private void handleCommand(String content) {
        String[] parts = content.split("\\s+", 2);
        String cmd = parts[0].toLowerCase(Locale.ROOT);
        String arg = parts.length > 1 ? parts[1].trim() : "";

        switch (cmd) {
            case "!add":
                handleAdd(arg);
                break;
            case "!del":
                handleDel(arg);
                break;
            case "!stats":
                handleStats();
                break;
            case "!seen":
                handleSeen(arg);
                break;
            case "!addbphat":
                handleAddBphat(arg);
                break;
            case "!delbphat":
                handleDelBphat(arg);
                break;
            default:
                sendEmbed("❓ Unknown Command",
                        "I don't recognize `" + cmd + "`.\nTry `!add`, `!del`, `!stats`, `!seen`, `!addbphat` or `!delbphat`.",
                        COLOR_ERROR);
                break;
        }
    }

    private void handleAdd(String name) {
        if (name.isEmpty()) {
            sendEmbed("⚠️ Missing Name", "Usage: `!add name-here`", COLOR_ERROR);
            return;
        }
        boolean added = addToNsList(name);
        if (added) {
            reloadBotClientNsList();
            sendEmbed("✅ Name Added",
                    "✨ **" + name + "** has been added to `nslist.txt`.\n🔄 Bot client nslist reloaded.",
                    COLOR_SUCCESS);
        } else {
            sendEmbed("⚠️ Already Listed",
                    "🟡 **" + name + "** is already in `nslist.txt`. No changes made.",
                    COLOR_INFO);
        }
    }

    private void handleDel(String name) {
        if (name.isEmpty()) {
            sendEmbed("⚠️ Missing Name", "Usage: `!del name-here`", COLOR_ERROR);
            return;
        }
        boolean removed = removeFromNsList(name);
        if (removed) {
            reloadBotClientNsList();
            sendEmbed("🗑️ Name Removed",
                    "🧹 **" + name + "** has been removed from `nslist.txt`.\n🔄 Bot client nslist reloaded.",
                    COLOR_SUCCESS);
        } else {
            sendEmbed("❌ Not Found",
                    "🔍 **" + name + "** was not found in `nslist.txt`.",
                    COLOR_ERROR);
        }
    }

    private void handleStats() {
        Path path = Paths.get(Config.getStatsFilePath());
        JsonObject stats = readJsonObject(path);
        if (stats == null) {
            sendEmbed("❌ Stats Unavailable", "Could not read `stats.json`.", COLOR_ERROR);
            return;
        }

        JsonObject embed = new JsonObject();
        embed.addProperty("title", "📊 Viruz Bot Stats");
        embed.addProperty("color", COLOR_STATS);
        embed.addProperty("timestamp", Instant.now().toString());

        JsonArray fields = new JsonArray();
        for (Map.Entry<String, JsonElement> e : stats.entrySet()) {
            if (e.getValue() == null || !e.getValue().isJsonPrimitive()) continue;
            if ("Most Bullied".equalsIgnoreCase(e.getKey())) continue;
            // Internal raw-millisecond value; the formatted fields are shown instead.
            if ("RuntimeMs".equalsIgnoreCase(e.getKey())) continue;
            JsonObject field = new JsonObject();
            field.addProperty("name", "✨ " + e.getKey());
            field.addProperty("value", "```" + e.getValue().getAsString() + "```");
            field.addProperty("inline", true);
            fields.add(field);
        }

        if (stats.has("Most Bullied") && stats.get("Most Bullied").isJsonArray()) {
            StringBuilder sb = new StringBuilder();
            int rank = 1;
            for (JsonElement el : stats.getAsJsonArray("Most Bullied")) {
                if (!el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();
                String n = o.has("name") ? o.get("name").getAsString() : "?";
                int attacks = o.has("attacks") ? o.get("attacks").getAsInt() : 0;
                sb.append(String.format(Locale.ROOT, "%2d. %-20s  %d attacks%n", rank++, n, attacks));
                if (rank > 21) break;
            }
            JsonObject field = new JsonObject();
            field.addProperty("name", "🥊 Most Bullied");
            field.addProperty("value", "```\n" + sb.toString().trim() + "\n```");
            field.addProperty("inline", false);
            fields.add(field);
        }

        embed.add("fields", fields);

        JsonObject footer = new JsonObject();
        footer.addProperty("text", "ViruzFFABot • Powered by Viruz Scripts");
        embed.add("footer", footer);

        sendEmbedObject(embed);
    }

    private void handleSeen(String name) {
        if (name.isEmpty()) {
            sendEmbed("⚠️ Missing Name", "Usage: `!seen name-here`", COLOR_ERROR);
            return;
        }
        JsonObject entry = findTrackerEntry(name);
        if (entry == null) {
            sendEmbed("❌ Not Found",
                    "🔍 No tracker record found for **" + name + "**.",
                    COLOR_ERROR);
            return;
        }

        String displayName = entry.has("name") ? entry.get("name").getAsString() : name;
        long lastSeenMs = entry.has("lastSeenMs") ? entry.get("lastSeenMs").getAsLong() : 0L;
        String formatted = formatLastSeen(lastSeenMs);
        String since = formatDuration(Math.max(0L, System.currentTimeMillis() - lastSeenMs));

        JsonObject embed = new JsonObject();
        embed.addProperty("title", "👁️ Last Seen");
        embed.addProperty("color", COLOR_SEEN);
        embed.addProperty("timestamp", Instant.now().toString());

        JsonArray fields = new JsonArray();

        JsonObject nameField = new JsonObject();
        nameField.addProperty("name", "👤 Player");
        nameField.addProperty("value", "```" + displayName + "```");
        nameField.addProperty("inline", true);
        fields.add(nameField);

        JsonObject seenField = new JsonObject();
        seenField.addProperty("name", "⏱️ Last Seen");
        seenField.addProperty("value", "```" + formatted + "```");
        seenField.addProperty("inline", true);
        fields.add(seenField);

        if (entry.has("combatLevel")) {
            JsonObject cbField = new JsonObject();
            cbField.addProperty("name", "⚔️ Combat Level");
            cbField.addProperty("value", "```" + entry.get("combatLevel").getAsInt() + "```");
            cbField.addProperty("inline", true);
            fields.add(cbField);
        }
        if (entry.has("encounters")) {
            JsonObject encField = new JsonObject();
            encField.addProperty("name", "🔁 Encounters");
            encField.addProperty("value", "```" + entry.get("encounters").getAsInt() + "```");
            encField.addProperty("inline", true);
            fields.add(encField);
        }
        if (entry.has("timesAttackedUs")) {
            JsonObject atkField = new JsonObject();
            atkField.addProperty("name", "🎯 Times Attacked Us");
            atkField.addProperty("value", "```" + entry.get("timesAttackedUs").getAsInt() + "```");
            atkField.addProperty("inline", true);
            fields.add(atkField);
        }

        embed.add("fields", fields);

        JsonObject footer = new JsonObject();
        footer.addProperty("text", since + " since last seen");
        embed.add("footer", footer);

        sendEmbedObject(embed);
    }

    private void handleAddBphat(String name) {
        if (name.isEmpty()) {
            sendEmbed("⚠️ Missing Name", "Usage: `!addbphat name-here`", COLOR_ERROR);
            return;
        }
        boolean added = addToJsonList(Config.getBluePhatsFilePath(), name);
        if (added) {
            sendEmbed("💙 Blue Phat Added",
                    "💎 **" + name + "** has been added to `bluephats.json`.",
                    COLOR_SUCCESS);
        } else {
            sendEmbed("⚠️ Already Listed",
                    "🟡 **" + name + "** is already a Blue Phat.",
                    COLOR_INFO);
        }
    }

    private void handleDelBphat(String name) {
        if (name.isEmpty()) {
            sendEmbed("⚠️ Missing Name", "Usage: `!delbphat name-here`", COLOR_ERROR);
            return;
        }
        boolean removed = removeFromJsonList(Config.getBluePhatsFilePath(), name);
        if (removed) {
            sendEmbed("🗑️ Blue Phat Removed",
                    "🧹 **" + name + "** has been removed from `bluephats.json`.",
                    COLOR_SUCCESS);
        } else {
            sendEmbed("❌ Not Found",
                    "🔍 **" + name + "** was not found in `bluephats.json`.",
                    COLOR_ERROR);
        }
    }

    // ------------------------------------------------------------------ file helpers

    private boolean addToNsList(String name) {
        String key = name.trim().toLowerCase(Locale.ROOT);
        Path path = Paths.get(Config.getNsListFilePath());
        List<String> lines = readLines(path);
        List<String> normalized = new ArrayList<>();
        for (String line : lines) {
            String t = line.trim();
            if (!t.isEmpty()) normalized.add(t);
        }
        for (String line : normalized) {
            if (line.equalsIgnoreCase(key)) return false;
        }
        normalized.add(key);
        writeLines(path, normalized);
        return true;
    }

    private boolean removeFromNsList(String name) {
        String key = name.trim().toLowerCase(Locale.ROOT);
        Path path = Paths.get(Config.getNsListFilePath());
        List<String> lines = readLines(path);
        List<String> filtered = new ArrayList<>();
        boolean removed = false;
        for (String line : lines) {
            if (line.trim().equalsIgnoreCase(key)) {
                removed = true;
            } else {
                filtered.add(line);
            }
        }
        if (removed) writeLines(path, filtered);
        return removed;
    }

    private boolean addToJsonList(String filePath, String value) {
        String key = value.trim().toLowerCase(Locale.ROOT);
        Path path = Paths.get(filePath);
        List<String> list = readJsonStringList(path);
        for (String s : list) {
            if (s.equalsIgnoreCase(key)) return false;
        }
        list.add(key);
        writeJsonStringList(path, list);
        return true;
    }

    private boolean removeFromJsonList(String filePath, String value) {
        String key = value.trim().toLowerCase(Locale.ROOT);
        Path path = Paths.get(filePath);
        List<String> list = readJsonStringList(path);
        List<String> filtered = new ArrayList<>();
        boolean removed = false;
        for (String s : list) {
            if (s.equalsIgnoreCase(key)) {
                removed = true;
            } else {
                filtered.add(s);
            }
        }
        if (removed) writeJsonStringList(path, filtered);
        return removed;
    }

    private List<String> readLines(Path path) {
        List<String> out = new ArrayList<>();
        try {
            if (Files.exists(path)) {
                out.addAll(Files.readAllLines(path, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            log.warn("Failed to read {}: {}", path, e.getMessage());
        }
        return out;
    }

    private void writeLines(Path path, List<String> lines) {
        try {
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to write {}: {}", path, e.getMessage());
        }
    }

    private List<String> readJsonStringList(Path path) {
        List<String> out = new ArrayList<>();
        try {
            if (!Files.exists(path)) return out;
            String json = Files.readString(path, StandardCharsets.UTF_8);
            if (json.trim().isEmpty()) return out;
            List<String> parsed = new Gson().fromJson(json, new TypeToken<List<String>>() {
            }.getType());
            if (parsed != null) out.addAll(parsed);
        } catch (Exception e) {
            log.warn("Failed to read json list {}: {}", path, e.getMessage());
        }
        return out;
    }

    private void writeJsonStringList(Path path, List<String> list) {
        try {
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(list);
            Files.writeString(path, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to write json list {}: {}", path, e.getMessage());
        }
    }

    private JsonObject readJsonObject(Path path) {
        try {
            if (!Files.exists(path)) return null;
            String json = Files.readString(path, StandardCharsets.UTF_8);
            JsonElement parsed = new JsonParser().parse(json);
            if (parsed != null && parsed.isJsonObject()) return parsed.getAsJsonObject();
        } catch (Exception e) {
            log.warn("Failed to read json object {}: {}", path, e.getMessage());
        }
        return null;
    }

    private JsonObject findTrackerEntry(String name) {
        Path path = Paths.get(Config.getTrackerInfoFilePath());
        try {
            if (!Files.exists(path)) return null;
            String json = Files.readString(path, StandardCharsets.UTF_8);
            JsonElement parsed = new JsonParser().parse(json);
            if (parsed == null || !parsed.isJsonArray()) return null;
            String key = name.trim().toLowerCase(Locale.ROOT);
            for (JsonElement el : parsed.getAsJsonArray()) {
                if (!el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();
                String n = o.has("name") ? o.get("name").getAsString() : "";
                if (n.trim().toLowerCase(Locale.ROOT).equals(key)) return o;
            }
        } catch (Exception e) {
            log.warn("Failed to read tracker info: {}", e.getMessage());
        }
        return null;
    }

    private void reloadBotClientNsList() {
        try {
            ViruzFFABotScript s = botScript != null ? botScript : script;
            if (s != null) {
                s.reloadNsList();
                return;
            }
            ViruzFFABotPlugin plugin = Microbot.getPlugin(ViruzFFABotPlugin.class);
            if (plugin != null && plugin.getScript() != null) {
                plugin.getScript().reloadNsList();
            }
        } catch (Exception e) {
            log.warn("Failed to reload nslist in bot client: {}", e.getMessage());
        }
    }

    // ------------------------------------------------------------------ discord helpers

    private void sendEmbed(String title, String description, int color) {
        JsonObject embed = new JsonObject();
        embed.addProperty("title", title);
        if (description != null) embed.addProperty("description", description);
        embed.addProperty("color", color);
        embed.addProperty("timestamp", Instant.now().toString());
        JsonObject footer = new JsonObject();
        footer.addProperty("text", "Viruz Bot Control • Viruz Scripts");
        embed.add("footer", footer);
        sendEmbedObject(embed);
    }

    private void sendEmbedObject(JsonObject embed) {
        JsonObject payload = new JsonObject();
        JsonArray embeds = new JsonArray();
        embeds.add(embed);
        payload.add("embeds", embeds);
        postMessage(payload);
    }

    private void postMessage(JsonObject payload) {
        if (channelId == null || channelId.trim().isEmpty()) return;
        if (token == null || token.trim().isEmpty()) return;
        try {
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create(DISCORD_API + "/channels/" + channelId + "/messages"))
                    .header("Authorization", "Bot " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            new Gson().toJson(payload), StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("Discord sendMessage failed {}: {}", response.statusCode(), response.body());
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("Discord sendMessage error: {}", e.getMessage());
        }
    }

    private static int compareSnowflake(String a, String b) {
        if (a.length() != b.length()) return Integer.compare(a.length(), b.length());
        return a.compareTo(b);
    }

    private static String formatDuration(long ms) {
        long totalSeconds = Math.max(0L, ms / 1000L);
        long days = totalSeconds / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.ROOT, "%02d:%02d:%02d:%02d", days, hours, minutes, seconds);
    }

    private static String formatLastSeen(long ms) {
        if (ms <= 0L) return "Unknown";
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime zdt = Instant.ofEpochMilli(ms).atZone(zone);
        ZonedDateTime now = ZonedDateTime.now(zone);
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("h:mma", Locale.US);
        String time = zdt.format(timeFmt).toLowerCase(Locale.US);
        if (zdt.toLocalDate().equals(now.toLocalDate())) {
            return "Today at " + time;
        }
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US);
        return dateFmt.format(zdt) + " at " + time;
    }
}