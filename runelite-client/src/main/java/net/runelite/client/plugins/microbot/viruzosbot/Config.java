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

public class Config {
    private static final Logger logger = Logger.getLogger(Config.class.getName());
    private static final String DEFAULT_CONFIG_PATH;
    private static final String CONFIG_FILE_PATH;
    private static final String FILE_PATH;
    private static final Properties properties = new Properties();

    static {
        String basePath = "C:" + File.separator + "Users" + File.separator + "4Twen" + File.separator + ".microbot" + File.separator + "viruzosbot";
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
            logger.log(Level.SEVERE, "Failed to load config file", e);
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

                    if (file.getAbsolutePath().equals(new File(CONFIG_FILE_PATH).getAbsolutePath())) {
                        save();
                    }
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

    public static String getBotToken() {
        try {
            String json = Files.readString(Paths.get(DEFAULT_CONFIG_PATH), StandardCharsets.UTF_8);
            JsonObject obj = new Gson().fromJson(json, JsonObject.class);
            if (obj != null && obj.has("BOT_TOKEN")) {
                return obj.get("BOT_TOKEN").getAsString();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to read BOT_TOKEN from config", e);
        }
        return null;
    }

    public static String getChannelId() {
        try {
            String json = Files.readString(Paths.get(DEFAULT_CONFIG_PATH), StandardCharsets.UTF_8);
            JsonObject obj = new Gson().fromJson(json, JsonObject.class);
            if (obj != null) {
                if (obj.has("CHANNEL_ID")) return obj.get("CHANNEL_ID").getAsString();
                if (obj.has("channelId")) return obj.get("channelId").getAsString();
                if (obj.has("channel_id")) return obj.get("channel_id").getAsString();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to read CHANNEL_ID from config", e);
        }
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