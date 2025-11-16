package ru.balance.GenAI.check;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerIdRegistry {

    private static final Map<Integer, Player> players = new ConcurrentHashMap<>();

    private PlayerIdRegistry() {
    }

    public static void add(Player player) {
        if (player == null) return;
        players.put(player.getEntityId(), player);
    }

    public static void remove(Player player) {
        if (player == null) return;
        players.remove(player.getEntityId());
    }

    public static Player get(int entityId) {
        return players.get(entityId);
    }
}

