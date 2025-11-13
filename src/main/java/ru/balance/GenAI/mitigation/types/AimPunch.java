package ru.balance.GenAI.mitigation.types;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import ru.balance.GenAI.GenAI;
import java.util.concurrent.ThreadLocalRandom;

public class AimPunch implements PacketReceiveMitigation {
    private final GenAI plugin;
    private final double hStrength;
    private final double vStrength;

    public AimPunch(GenAI plugin) {
        this.plugin = plugin;
        this.hStrength = plugin.getConfig().getDouble("mitigations.types.aim-punch.horizontal-strength", 3.0);
        this.vStrength = plugin.getConfig().getDouble("mitigations.types.aim-punch.vertical-strength", 1.5);
    }

    @Override
    public void onPacket(Player player, PacketReceiveEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player == null || !player.isOnline()) {
                return;
            }
            Location currentLocation = player.getLocation();
            float yawOffset = (float) (ThreadLocalRandom.current().nextDouble(-hStrength, hStrength));
            float pitchOffset = (float) (ThreadLocalRandom.current().nextDouble(-vStrength, vStrength));
            currentLocation.setYaw(currentLocation.getYaw() + yawOffset);
            currentLocation.setPitch(currentLocation.getPitch() + pitchOffset);
            player.teleport(currentLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
        });
    }
}
