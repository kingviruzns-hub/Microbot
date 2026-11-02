package net.runelite.client.plugins.microbot.viruzosbot;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Config {
    private static final Logger logger = Logger.getLogger(Config.class.getName());
    private static final Properties properties = new Properties();

    private static final String DEFAULT_CONFIG_PATH;
    private static final String CONFIG_FILE_PATH;
    private static final String FILE_PATH;

    private static final Pattern BOT_TOKEN_PATTERN =
            Pattern.compile("\"(?:BOT_TOKEN|botToken|bot_token)\"\\s*[:=]\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHANNEL_ID_PATTERN =
            Pattern.compile("\"(?:CHANNEL_ID|channelId|channel_id)\"\\s*[:=]\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);

    static {
        String basePath = new File(System.getProperty("user.home"), ".microbot/viruzosbot").getAbsolutePath();
        DEFAULT_CONFIG_PATH = basePath + File.separator + "viruzffabot.json";
        CONFIG_FILE_PATH = System.getProperty("microbot.config.path", DEFAULT_CONFIG_PATH);
        FILE_PATH = basePath + File.separator + "nslist.txt";
        load();
    }

    public static void load() {
        logger.info("Loading local config.");
        File configFile = new File(CONFIG_FILE_PATH);
        ensureFileExists(configFile);
        try (FileInputStream fis = new FileInputStream(configFile)) {
            properties.load(fis);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to load config file as properties: {0}", e.getMessage());
        }
    }

    private static void ensureFileExists(File file) {
        if (!file.exists()) {
            logger.log(Level.WARNING, "{0} not found at: {1}", new Object[]{"config file", file.getAbsolutePath()});
            try {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    logger.log(Level.SEVERE, "Failed to create parent directories: {0}", parent.getAbsolutePath());
                }
                if (file.createNewFile()) {
                    logger.log(Level.INFO, "Created empty {0} at: {1}", new Object[]{"config file", file.getAbsolutePath()});
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to create " + "config file", e);
            }
        }
    }

    public static void save() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE_PATH)) {
            properties.store(fos, "ViruzFFABot Configuration");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save config file", e);
        }
    }

    private static String readConfigText() {
        try {
            File f = new File(CONFIG_FILE_PATH);
            if (!f.exists()) return null;
            return Files.readString(Paths.get(f.getAbsolutePath()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to read config file", e);
            return null;
        }
    }

    public static String getBotToken() {
        String text = readConfigText();
        if (text != null) {
            try {
                JsonObject obj = new Gson().fromJson(text, JsonObject.class);
                if (obj != null && obj.has("BOT_TOKEN")) {
                    return obj.get("BOT_TOKEN").getAsString();
                }
                if (obj != null && obj.has("botToken")) {
                    return obj.get("botToken").getAsString();
                }
                if (obj != null && obj.has("bot_token")) {
                    return obj.get("bot_token").getAsString();
                }
            } catch (Exception ignored) {
            }
            Matcher m = BOT_TOKEN_PATTERN.matcher(text);
            if (m.find()) {
                return m.group(1);
            }
        }
        String v = properties.getProperty("BOT_TOKEN");
        if (v != null && !v.trim().isEmpty()) return v.trim();
        return null;
    }

    public static String getChannelId() {
        String text = readConfigText();
        if (text != null) {
            try {
                JsonObject obj = new Gson().fromJson(text, JsonObject.class);
                if (obj != null) {
                    if (obj.has("CHANNEL_ID")) return obj.get("CHANNEL_ID").getAsString();
                    if (obj.has("channelId")) return obj.get("channelId").getAsString();
                    if (obj.has("channel_id")) return obj.get("channel_id").getAsString();
                }
            } catch (Exception ignored) {
            }
            Matcher m = CHANNEL_ID_PATTERN.matcher(text);
            if (m.find()) {
                return m.group(1);
            }
        }
        String v = properties.getProperty("CHANNEL_ID");
        if (v != null && !v.trim().isEmpty()) return v.trim();
        v = properties.getProperty("channelId");
        if (v != null && !v.trim().isEmpty()) return v.trim();
        v = properties.getProperty("channel_id");
        if (v != null && !v.trim().isEmpty()) return v.trim();
        return null;
    }

    public static String getNsListFilePath() {
        return FILE_PATH;
    }

    public static String getBluePhatsFilePath() {
        return FILE_PATH.replace("nslist.txt", "bluephats.json");
    }

    public static String getAiDataFilePath() {
        return FILE_PATH.replace("nslist.txt", "ai_data.json");
    }

    public static String getLureDragFilePath() {
        return FILE_PATH.replace("nslist.txt", "luredrag.txt");
    }

    public static String getLastSeenFilePath() {
        return FILE_PATH.replace("nslist.txt", "last_seen.json");
    }

    public static String getStatsFilePath() {
        return FILE_PATH.replace("nslist.txt", "stats.json");
    }

    public static String getTrackerInfoFilePath() {
        return FILE_PATH.replace("nslist.txt", "tracker_info.json");
    }

    public static String getSessionFilePath() {
        return FILE_PATH.replace("nslist.txt", "session.json");
    }

    public static String getHumanBehaviorFilePath() {
        return FILE_PATH.replace("nslist.txt", "human_behavior.json");
    }
}