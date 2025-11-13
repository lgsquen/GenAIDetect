package ru.balance.GenAI;

import com.github.retrooper.packetevents.PacketEvents;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.balance.GenAI.command.CommandManager;
import ru.balance.GenAI.listener.ConnectionListener;
import ru.balance.GenAI.listener.MitigationListener;
import ru.balance.GenAI.listener.PacketListener;
import ru.balance.GenAI.manager.LocaleManager;
import ru.balance.GenAI.mitigation.MitigationManager;
import ru.balance.GenAI.service.DatabaseService;
import ru.balance.GenAI.service.HeartbeatService;
import ru.balance.GenAI.service.TokenService;
import ru.balance.GenAI.service.ViolationManager;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class GenAI extends JavaPlugin {
    private static GenAI instance;
    private static boolean isServerConnected = false;
    private int heartbeatTaskID = -1;
    private String serverUrl = null;
    private final Set<UUID> alertsDisabledAdmins = ConcurrentHashMap.newKeySet();
    private DatabaseService databaseService;
    private TokenService tokenService;
    private OkHttpClient httpClient;
    private ViolationManager violationManager;
    private LocaleManager localeManager;
    private MitigationManager mitigationManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.serverUrl = getConfig().getString("server.url", "http://localhost:8000");

        getLogger().info("GenAI Anti-Cheat starting...");

        this.localeManager = new LocaleManager(this);

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();

        databaseService = new DatabaseService(this);
        databaseService.init();

        this.tokenService = new TokenService(this);
        this.violationManager = new ViolationManager(this);
        this.mitigationManager = new MitigationManager(this);
        isServerConnected = true;
        initializePluginServices();
        getLogger().info("GenAI Anti-Cheat enabled.");

        CommandManager commandManager = new CommandManager(this);
        if (getCommand("genai") != null) {
            getCommand("genai").setExecutor(commandManager);
            getCommand("genai").setTabCompleter(commandManager);
        }

        getServer().getPluginManager().registerEvents(new MitigationListener(this), this);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            Bukkit.getConsoleSender().sendMessage("[GenAI] Plugin enabled.");
        }, 20L);
    }

    private void checkServerConnection() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                Request healthRequest = new Request.Builder().url(serverUrl + "/health").build();
                try (Response healthResponse = this.httpClient.newCall(healthRequest).execute()) {
                    if (healthResponse.isSuccessful() && healthResponse.body() != null) {
                        String responseBody = healthResponse.body().string();
                        Map<String, Object> result = LazyHolder.RESPONSE_ADAPTER.fromJson(responseBody);
                        if (result != null && "healthy".equals(result.get("status"))) {
                            isServerConnected = true;
                            Bukkit.getScheduler().runTask(this, this::initializePluginServices);
                            getLogger().info("Connected to server: " + serverUrl);
                        } else {
                            disablePlugin();
                        }
                    } else {
                        disablePlugin();
                    }
                }
            } catch (Exception e) {
                getLogger().warning("Failed to connect to server: " + e.getMessage());
                disablePlugin();
            }
        });
    }

    private void disablePlugin() {
        Bukkit.getScheduler().runTask(this, () -> getServer().getPluginManager().disablePlugin(this));
    }

    private void initializePluginServices() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings().checkForUpdates(false).reEncodeByDefault(false);
        PacketEvents.getAPI().load();
        PacketEvents.getAPI().getEventManager().registerListeners(new ConnectionListener(), new PacketListener());
        PacketEvents.getAPI().init();
        long interval = 20L * 60 * 5;
        heartbeatTaskID = Bukkit.getScheduler().runTaskTimerAsynchronously(this, HeartbeatService::sendHeartbeat, 100L, interval).getTaskId();
    }

    @Override
    public void onDisable() {
        if (databaseService != null) {
            databaseService.close();
        }
        if (heartbeatTaskID != -1) {
            Bukkit.getScheduler().cancelTask(heartbeatTaskID);
        }
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
        try {
            if (PacketEvents.getAPI() != null && PacketEvents.getAPI().isLoaded()) {
                PacketEvents.getAPI().terminate();
            }
        } catch (NoClassDefFoundError ignored) {
        }
    }

    public void reloadPluginConfig() {
        reloadConfig();
        localeManager.loadMessages();
    }

    public boolean toggleAlerts(UUID uuid) {
        if (alertsDisabledAdmins.contains(uuid)) {
            alertsDisabledAdmins.remove(uuid);
            return true;
        } else {
            alertsDisabledAdmins.add(uuid);
            return false;
        }
    }

    public boolean areAlertsEnabledFor(UUID uuid) {
        return !alertsDisabledAdmins.contains(uuid);
    }

    public static GenAI getInstance() {
        return instance;
    }

    public static boolean isServerActive() {
        return isServerConnected;
    }

    public String getServerUrl() {
        return this.serverUrl;
    }

    public DatabaseService getDatabaseService() {
        return databaseService;
    }

    public TokenService getTokenService() {
        return tokenService;
    }

    public OkHttpClient getHttpClient() {
        return this.httpClient;
    }

    public ViolationManager getViolationManager() {
        return this.violationManager;
    }

    public LocaleManager getLocaleManager() {
        return this.localeManager;
    }

    public MitigationManager getMitigationManager() {
        return mitigationManager;
    }

    private static class LazyHolder {
        private static final Moshi MOSHI = new Moshi.Builder().build();
        private static final Type MAP_STRING_STRING_TYPE = Types.newParameterizedType(Map.class, String.class, String.class);
        private static final JsonAdapter<Map<String, String>> JSON_ADAPTER = MOSHI.adapter(MAP_STRING_STRING_TYPE);
        private static final Type MAP_STRING_OBJECT_TYPE = Types.newParameterizedType(Map.class, String.class, Object.class);
        private static final JsonAdapter<Map<String, Object>> RESPONSE_ADAPTER = MOSHI.adapter(MAP_STRING_OBJECT_TYPE);
    }
}

