package net.runelite.client.plugins.microbot.viruzosbot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
        String basePath = "C:" + File.separator + "Users" + File.separator + "4Twen" + File.separator + ".microbot";
        DEFAULT_CONFIG_PATH = basePath + File.separator + "config.properties";
        CONFIG_FILE_PATH = System.getProperty("config.path", DEFAULT_CONFIG_PATH);
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
}