package ru.balance.GenAI.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.balance.GenAI.GenAI;
import ru.balance.GenAI.check.PlayerIdRegistry;

public class PlayerCheckListener implements Listener {

    private final GenAI plugin;

    public PlayerCheckListener(GenAI plugin) {
        this.plugin = plugin;
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerIdRegistry.add(player);
            plugin.getCheckManager().registerChecks(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerIdRegistry.add(player);
        plugin.getCheckManager().registerChecks(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getCheckManager().unregisterChecks(player);
        PlayerIdRegistry.remove(player);
    }
}

