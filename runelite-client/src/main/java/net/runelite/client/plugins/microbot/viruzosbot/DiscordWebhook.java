package net.runelite.client.plugins.microbot.viruzosbot;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DiscordWebhook {
    private static final Logger logger = Logger.getLogger(DiscordWebhook.class.getName());
    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1488789593657704498/4V0qORNdLCP2FkJ0NDvRSuym6jUhhuy_zWDG9UijzmgvNAkt33LwZE_nzM0qIe3drR7F";
    private static final String WEBHOOK_URL_2 = "https://discord.com/api/webhooks/1507892018670600362/Lbs-2gbNyRJsMBWJQJ5cKx8d4Z-F2zRjgVHh-PMjnuZYh5fsyw86IcK1U93Xhz4pZVhJ";

    private DiscordWebhook() {
    }

    public static void sendMessage(String message) {
        sendPayload("{\"content\": \"" + message + "\"}");
    }

    public static void sendEmbed(String accountName, String version, String status, String runtime, String event) {
        String ESC = "\\u001b";
        String RESET = ESC + "[0m";
        String BLUE = ESC + "[1;34m";
        String GREEN = ESC + "[1;32m";
        String CYAN = ESC + "[1;36m";
        String YELLOW = ESC + "[1;33m";
        String WHITE = ESC + "[1;37m";

        int sidebarColor = getSidebarColor(event);

        String jsonPayload = "{"
                + "\"embeds\": [{"
                + "\"title\": \" ViruzFFABot Status \","
                + "\"color\": " + sidebarColor + ","
                + "\"fields\": ["
                + "{\"name\": \"👤 Account Name\", \"value\": \"```ansi\\n" + BLUE + accountName + RESET + "```\", \"inline\": true},"
                + "{\"name\": \"📦 Version\", \"value\": \"```ansi\\n" + GREEN + version + RESET + "```\", \"inline\": true},"
                + "{\"name\": \"⏳ Runtime\", \"value\": \"```ansi\\n" + WHITE + runtime + RESET + "```\", \"inline\": true},"
                + "{\"name\": \"🚦 Status\", \"value\": \"```ansi\\n" + CYAN + status + RESET + "```\", \"inline\": false},"
                + "{\"name\": \"📝 Event\", \"value\": \"```ansi\\n" + YELLOW + event.replaceAll("\\*\\*", "") + RESET + "```\", \"inline\": false}"
                + "],"
                + "\"footer\": {\"text\": \"ViruzFFABot • Powered by Viruz Scripts\"},"
                + "\"timestamp\": \"" + java.time.Instant.now().toString() + "\""
                + "}]"
                + "}";
        sendPayload(jsonPayload);
    }

    private static int getSidebarColor(String event) {
        int sidebarColor = 5814783;
        String lowerEvent = event.toLowerCase();
        if (lowerEvent.contains("started") || lowerEvent.contains("added") || lowerEvent.contains("on")) sidebarColor = 3066993;
        else if (lowerEvent.contains("stopped") || lowerEvent.contains("removed") || lowerEvent.contains("lured") || lowerEvent.contains("off")) sidebarColor = 15158332;
        else if (lowerEvent.contains("attacking") || lowerEvent.contains("reported")) sidebarColor = 10181046;
        else if (lowerEvent.contains("afk") || lowerEvent.contains("waiting")) sidebarColor = 15844367;
        return sidebarColor;
    }

    private static void sendPayload(String jsonPayload) {
        sendToWebhook(WEBHOOK_URL, jsonPayload);
        sendToWebhook(WEBHOOK_URL_2, jsonPayload);
    }

    private static void sendToWebhook(String webhookUrl, String jsonPayload) {
        try {
            URL url = new URL(webhookUrl);
            int responseCode = getResponseCode(jsonPayload, url);
            if (responseCode >= 400) {
                logger.log(Level.WARNING, "Discord Webhook failed with response code: {0}", responseCode);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error sending Discord Webhook", e);
        }
    }

    private static int getResponseCode(String jsonPayload, URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        return connection.getResponseCode();
    }
}
