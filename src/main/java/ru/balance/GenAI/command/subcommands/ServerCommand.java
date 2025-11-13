package ru.balance.GenAI.command.subcommands;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import ru.balance.GenAI.GenAI;
import ru.balance.GenAI.command.SubCommand;

import java.lang.reflect.Type;
import java.util.*;

public class ServerCommand extends SubCommand {
    
    private final GenAI plugin;
    private final Moshi moshi;
    private final JsonAdapter<Map<String, Object>> jsonAdapter;
    
    public ServerCommand(GenAI plugin) {
        super(plugin);
        this.plugin = plugin;
        this.moshi = new Moshi.Builder().build();
        Type mapType = Types.newParameterizedType(Map.class, String.class, Object.class);
        this.jsonAdapter = moshi.adapter(mapType);
    }
    
    @Override
    public String getName() {
        return "server";
    }
    
    @Override
    public String getDescription() {
        return "Управление локальным ИИ сервером";
    }
    
    @Override
    public String getUsage() {
        return "/genai server <подкоманда>";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return;
        }
        
        switch (args[0].toLowerCase()) {
            case "status":
                checkServerStatus(sender);
                break;
            case "stats":
                getServerStats(sender);
                break;
            default:
                sendHelp(sender);
                break;
        }
    }
    
    private void checkServerStatus(CommandSender sender) {
        sender.sendMessage("§6=== Статус ИИ сервера ===");
        sender.sendMessage("§7URL: §f" + plugin.getServerUrl());
        sender.sendMessage("§7Статус: " + (GenAI.isServerActive() ? "§aПодключен" : "§cОтключен"));
        
        if (GenAI.isServerActive()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    Request request = new Request.Builder()
                            .url(plugin.getServerUrl() + "/health")
                            .get()
                            .build();
                    
                    try (Response response = plugin.getHttpClient().newCall(request).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseBody = response.body().string();
                            Map<String, Object> result = jsonAdapter.fromJson(responseBody);
                            
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                if (result != null) {
                                    sender.sendMessage("§7Модель обучена: " + (result.get("model_trained") != null ? "§aДа" : "§cНет"));
                                    sender.sendMessage("§7Обучающих образцов: §f" + result.get("training_samples"));
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage("§cОшибка проверки статуса: " + e.getMessage());
                    });
                }
            });
        }
    }
    
    private void getServerStats(CommandSender sender) {
        sender.sendMessage("§6=== Статистика ИИ сервера ===");
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Request request = new Request.Builder()
                        .url(plugin.getServerUrl() + "/stats")
                        .get()
                        .build();
                
                try (Response response = plugin.getHttpClient().newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        Map<String, Object> result = jsonAdapter.fromJson(responseBody);
                        
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (result != null) {
                                sender.sendMessage("§7Всего датасетов: §f" + result.get("total_datasets"));
                                sender.sendMessage("§7Читеров: §c" + result.get("cheater_datasets"));
                                sender.sendMessage("§7Легитов: §a" + result.get("legit_datasets"));
                                sender.sendMessage("§7Точность модели: §f" + String.format("%.2f%%", 
                                    ((Number) result.get("model_accuracy")).doubleValue() * 100));
                            } else {
                                sender.sendMessage("§cНе удалось получить статистику");
                            }
                        });
                    } else {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            sender.sendMessage("§cОшибка получения статистики: HTTP " + response.code());
                        });
                    }
                }
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage("§cОшибка получения статистики: " + e.getMessage());
                });
            }
        });
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== Команды сервера ===");
        sender.sendMessage("§7/genai server status §f- Статус ИИ сервера");
        sender.sendMessage("§7/genai server stats §f- Статистика сервера");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> completions = Arrays.asList("status", "stats");
            List<String> result = new ArrayList<>();
            for (String completion : completions) {
                if (completion.toLowerCase().startsWith(args[0].toLowerCase())) {
                    result.add(completion);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }
}
