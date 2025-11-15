package ru.balance.GenAI.service;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import okhttp3.*;
import org.bukkit.Bukkit;
import ru.balance.GenAI.GenAI;
import ru.balance.GenAI.entity.PlayerEntity;
import ru.balance.GenAI.util.MoshiFactory;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class AnalysisService {

    public static void analyze(final PlayerEntity entity) {
        if (!GenAI.isServerActive() || entity.getFrames().isEmpty()) {
            return;
        }
        entity.setLastAnalyzedFrames(entity.getFrames());

        String json;
        try {
            Map<String, Object> dataToSend = new HashMap<>();
            dataToSend.put("name", entity.getName());
            dataToSend.put("frames", entity.getLastAnalyzedFrames());

            Moshi moshi = MoshiFactory.getInstance();
            Type mapType = Types.newParameterizedType(Map.class, String.class, Object.class);
            JsonAdapter<Map<String, Object>> adapter = moshi.adapter(mapType);
            json = adapter.toJson(dataToSend);
        } catch (Exception e) {
            return;
        }

        try {
            TokenService tokenService = GenAI.getInstance().getTokenService();
            if (tokenService != null && tokenService.hasToken()) {
                json = tokenService.addTokenToRequest(json);
            }
        } catch (Exception ignored) {
        }

        String url = GenAI.getInstance().getServerUrl() + "/analyze";

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(url).post(body).build();

        GenAI.getInstance().getHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful() || responseBody == null) {
                        return;
                    }
                    String bodyString = responseBody.string();
                    Map<String, Double> result = LazyHolder.MAP_ADAPTER.fromJson(bodyString);
                    if (result != null && result.containsKey("cheat_probability")) {
                        double probability = result.get("cheat_probability");
                        Bukkit.getScheduler().runTask(GenAI.getInstance(), () -> {
                            handleAnalysisResult(entity, probability);
                        });
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private static void handleAnalysisResult(PlayerEntity entity, double probability) {
        GenAI plugin = GenAI.getInstance();
        double classificationThreshold = plugin.getConfig().getDouble("ml-check.classification-threshold");

        if (probability <= classificationThreshold) {
            return;
        }

        plugin.getViolationManager().handleViolation(entity, probability);
    }

    private static class LazyHolder {
        private static final Moshi MOSHI = MoshiFactory.getInstance();
        private static final Type MAP_TYPE = Types.newParameterizedType(Map.class, String.class, Double.class);
        private static final JsonAdapter<Map<String, Double>> MAP_ADAPTER = MOSHI.adapter(MAP_TYPE);
    }
}
