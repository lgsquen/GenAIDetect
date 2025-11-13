package ru.balance.GenAI.manager;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.balance.GenAI.GenAI;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class LocaleManager {

    private final GenAI plugin;
    private FileConfiguration langConfig;
    private String langCode;

    public LocaleManager(GenAI plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        this.langCode = plugin.getConfig().getString("language", "en").toLowerCase();
        File langFile = new File(plugin.getDataFolder(), "lang_" + langCode + ".yml");

        if (!langFile.exists()) {
            try {
                plugin.saveResource("lang_" + langCode + ".yml", false);
            } catch (IllegalArgumentException e) {
                langCode = "en";
                langFile = new File(plugin.getDataFolder(), "lang_en.yml");
                if (!langFile.exists()) {
                    plugin.saveResource("lang_en.yml", false);
                }
            }
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);

        try (Reader defConfigStream = new InputStreamReader(plugin.getResource("lang_" + langCode + ".yml"), StandardCharsets.UTF_8)) {
            if (defConfigStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
                langConfig.setDefaults(defConfig);
            }
        } catch (Exception ignored) {}
    }

    public String getMessage(String path) {
        String message = langConfig.getString(path);
        if (message == null) {
            return ChatColor.RED + "Error: Message not found for key '" + path + "'";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getLangCode() {
        return langCode;
    }
}
