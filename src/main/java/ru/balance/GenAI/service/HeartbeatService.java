package ru.balance.GenAI.service;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import okhttp3.*;
import org.bukkit.Bukkit;
import ru.balance.GenAI.GenAI;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class HeartbeatService {

    public static void sendHeartbeat() {
        GenAI plugin = GenAI.getInstance();
        if (!GenAI.isServerActive()) {
            return;
        }
        
        String heartbeatUrl = plugin.getServerUrl() + "/health";
        
        int onlinePlayers = Bukkit.getOnlinePlayers().size();

        Map<String, Object> data = new HashMap<>();
        data.put("online_players", onlinePlayers);
        data.put("timestamp", System.currentTimeMillis());

        String json = LazyHolder.JSON_ADAPTER.toJson(data);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(heartbeatUrl).post(body).build();

        try (Response response = plugin.getHttpClient().newCall(request).execute()) {
        } catch (IOException ignored) {
        }
    }

    private static class LazyHolder {
        private static final Moshi MOSHI = new Moshi.Builder().build();
        private static final Type MAP_STRING_OBJECT_TYPE = Types.newParameterizedType(Map.class, String.class, Object.class);
        private static final JsonAdapter<Map<String, Object>> JSON_ADAPTER = MOSHI.adapter(MAP_STRING_OBJECT_TYPE);
    }
}
