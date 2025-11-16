package ru.balance.GenAI.check;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.balance.GenAI.GenAI;
import ru.balance.GenAI.check.data.ActionData;
import ru.balance.GenAI.check.data.PacketData;
import ru.balance.GenAI.check.data.RotationData;
import ru.balance.GenAI.check.impl.aim.*;
import ru.balance.GenAI.check.impl.aura.*;
import ru.balance.GenAI.check.util.PacketUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CheckManager {
    private final GenAI plugin;
    private final Map<UUID, List<Check>> packetChecks = new ConcurrentHashMap<>();

    public CheckManager(GenAI plugin) {
        this.plugin = plugin;
        startTickTask();
    }

    public void registerChecks(Player player) {
        APlayer aPlayer = new APlayer(player);
        List<Check> playerChecks = new ArrayList<>();

        playerChecks.add(aPlayer.actionData);
        playerChecks.add(aPlayer.rotationData);
        playerChecks.add(aPlayer.packetData);

        playerChecks.add(new AimA(aPlayer));
        playerChecks.add(new AimB(aPlayer));
        playerChecks.add(new AimC(aPlayer));
        playerChecks.add(new AimD(aPlayer));
        playerChecks.add(new AimE(aPlayer));
        playerChecks.add(new AimF(aPlayer));
        playerChecks.add(new AimG(aPlayer));
        playerChecks.add(new AimH(aPlayer));
        playerChecks.add(new AimJ(aPlayer));
        playerChecks.add(new AimK(aPlayer));
        playerChecks.add(new AimL(aPlayer));
        playerChecks.add(new AimM(aPlayer));

        playerChecks.add(new AuraA(aPlayer));
        playerChecks.add(new AuraB(aPlayer));
        playerChecks.add(new AuraC(aPlayer));
        playerChecks.add(new AuraD(aPlayer));
        playerChecks.add(new AuraE(aPlayer));

        for (Check check : playerChecks) {
            check.resetViolations();
        }

        packetChecks.put(player.getUniqueId(), playerChecks);
    }

    public void unregisterChecks(Player player) {
        List<Check> playerChecks = packetChecks.remove(player.getUniqueId());
        if (playerChecks != null) {
            for (Check check : playerChecks) {
                check.resetViolations();
                check.cancelDecayTask();
            }
        }
    }

    public List<Check> getChecks(Player player) {
        return packetChecks.getOrDefault(player.getUniqueId(), Collections.emptyList());
    }

    public void handlePacketReceive(Player player, PacketReceiveEvent event) {
        List<Check> checks = packetChecks.get(player.getUniqueId());
        if (checks == null || checks.isEmpty()) return;

        for (Check check : checks) {
            if (check instanceof PacketCheck) {
                ((PacketCheck) check).onPacketReceive(event);
            }
        }

        if (PacketUtil.isAttack(event)) {
            for (Check check : checks) {
                if (check.hitTicksToCancel > 0) {
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }

    public void handlePacketSend(Player player, PacketSendEvent event) {
        List<Check> checks = packetChecks.get(player.getUniqueId());
        if (checks == null || checks.isEmpty()) return;

        for (Check check : checks) {
            if (check instanceof PacketCheck) {
                ((PacketCheck) check).onPacketSend(event);
            }
        }
    }

    private void startTickTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (List<Check> checks : packetChecks.values()) {
                for (Check check : checks) {
                    if (check.hitTicksToCancel > 0) {
                        check.hitTicksToCancel--;
                    }
                }
            }
        }, 1L, 1L);
    }
}
