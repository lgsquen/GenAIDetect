package ru.balance.GenAI.service;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import okhttp3.*;
import ru.balance.GenAI.GenAI;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class TokenService {
    private final GenAI plugin;
    private final OkHttpClient httpClient;
    private String storedToken;

    private static final Moshi MOSHI = new Moshi.Builder().build();
    private static final Type MAP_STRING_OBJECT_TYPE = Types.newParameterizedType(Map.class, String.class, Object.class);
    private static final JsonAdapter<Map<String, Object>> RESPONSE_ADAPTER = MOSHI.adapter(MAP_STRING_OBJECT_TYPE);

    public TokenService(GenAI plugin) {
        this.plugin = plugin;
        this.httpClient = plugin.getHttpClient();
        this.storedToken = plugin.getConfig().getString("auth.token", null);
    }

    public void setToken(String token) {
        this.storedToken = token;
        plugin.getConfig().set("auth.token", token);
        plugin.saveConfig();
        plugin.getLogger().info("Token updated successfully");
    }

    public String getToken() {
        return storedToken;
    }

    public boolean hasToken() {
        return storedToken != null && !storedToken.isEmpty();
    }

    public CompletableFuture<Boolean> validateToken() {
        if (!hasToken()) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String baseUrl = plugin.getServerUrl();
                if (baseUrl == null || baseUrl.isEmpty()) {
                    plugin.getLogger().warning("Server URL is not configured");
                    return false;
                }
                String url = baseUrl + "/subscription/" + storedToken;
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        Map<String, Object> result = RESPONSE_ADAPTER.fromJson(responseBody);
                        return result != null && result.containsKey("telegram_id");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Token validation failed: " + e.getMessage());
            }
            return false;
        });
    }

    public CompletableFuture<Map<String, Object>> getSubscriptionInfoAsync() {
        return CompletableFuture.supplyAsync(this::getSubscriptionInfo);
    }

    public Map<String, Object> getSubscriptionInfo() {
        if (!hasToken()) {
            return new HashMap<>();
        }

        try {
            String baseUrl = plugin.getServerUrl();
            if (baseUrl == null || baseUrl.isEmpty()) {
                plugin.getLogger().warning("Server URL is not configured");
                return new HashMap<>();
            }
            String url = baseUrl + "/subscription/" + storedToken;
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Map<String, Object> result = RESPONSE_ADAPTER.fromJson(responseBody);
                    return result != null ? result : new HashMap<>();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get subscription info: " + e.getMessage());
        }

        return new HashMap<>();
    }

    public void clearToken() {
        this.storedToken = null;
        plugin.getConfig().set("auth.token", null);
        plugin.saveConfig();
        plugin.getLogger().info("Token cleared");
    }

    public String addTokenToRequest(String jsonBody) {
        if (!hasToken()) {
            return jsonBody;
        }

        try {
            if (jsonBody.trim().startsWith("{")) {
                if (jsonBody.trim().endsWith("}")) {
                    return jsonBody.substring(0, jsonBody.length() - 1) +
                           ",\"token\":\"" + storedToken + "\"}";
                } else {
                    return jsonBody + ",\"token\":\"" + storedToken + "\"";
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to add token to request: " + e.getMessage());
        }

        return jsonBody;
    }

    public boolean isPremiumActive() {
        Map<String, Object> subInfo = getSubscriptionInfo();
        if (subInfo.isEmpty()) {
            return false;
        }

        Object remainingDays = subInfo.get("remaining_days");
        return remainingDays instanceof Number && ((Number) remainingDays).intValue() > 0;
    }

    public int getRemainingDays() {
        Map<String, Object> subInfo = getSubscriptionInfo();
        Object remainingDays = subInfo.get("remaining_days");
        return remainingDays instanceof Number ? ((Number) remainingDays).intValue() : 0;
    }

    public String getLastIp() {
        Map<String, Object> subInfo = getSubscriptionInfo();
        Object lastIp = subInfo.get("last_ip");
        return lastIp != null ? lastIp.toString() : "Not bound";
    }

    public boolean canRebindIp() {
        Map<String, Object> subInfo = getSubscriptionInfo();
        Object canRebind = subInfo.get("can_rebind");
        return Boolean.TRUE.equals(canRebind);
    }
}
