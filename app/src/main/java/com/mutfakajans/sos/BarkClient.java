package com.mutfakajans.sos;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Sends a Bark critical push without exposing the user's key in logs. */
final class BarkClient {
    private static final String ENDPOINT = "https://api.day.app/push";
    private BarkClient() {}

    static boolean send(String deviceKey, String senderName, int number, int total, boolean test) {
        HttpURLConnection connection = null;
        try {
            JSONObject message = new JSONObject();
            message.put("device_key", deviceKey);
            message.put("title", test ? "SOS · Kritik bildirim testi" : "SOS · ACİL YARDIM");
            message.put("body", test
                    ? "Test alarmı: kritik bildirimler çalışıyor mu kontrol et."
                    : senderName + " acil yardıma ihtiyaç duyuyor! Uyarı " + number + "/" + total
                      + ". Lütfen hemen iletişime geç.");
            message.put("level", "critical");
            message.put("call", "1");
            message.put("volume", "10");
            message.put("group", "sos-acil");

            URL url = new URL(ENDPOINT);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);
            connection.setInstanceFollowRedirects(false);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            byte[] bytes = message.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream stream = connection.getOutputStream()) {
                stream.write(bytes);
            }

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return false;
            StringBuilder response = new StringBuilder();
            try (InputStream stream = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null && response.length() < 16_384) {
                    response.append(line);
                }
            }
            return new JSONObject(response.toString()).optInt("code", -1) == 200;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
