package net.runelite.client.plugins.viruzosbot;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DiscordWebhook2 {
    private static final Logger logger = Logger.getLogger(DiscordWebhook2.class.getName());
    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1493138681781948517/QBpYdt7-p2BqOJa-6Iz8-AufMabjI9rr4TJFKarbWPt9ySLOuzIIfC8YGM27eEqJhEwR";
    private static final String WEBHOOK_URL_2 = "https://discord.com/api/webhooks/1507893724473196554/0nLfhWKnVtARQIC3YI_FOhGw4YKjLQrK0LaKwcms3HnWpGACnobK-P9dA57SFy95quEM";

    private DiscordWebhook2() {
    }

    public static void sendMessage(String message) {
        String jsonPayload = "{\"content\": \"" + message + "\"}";
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