package ru.balance.GenAI.check;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.npc.NPC;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCloseWindow;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import ru.balance.GenAI.GenAI;
import ru.balance.GenAI.check.data.ActionData;
import ru.balance.GenAI.check.data.PacketData;
import ru.balance.GenAI.check.data.RotationData;

import java.util.UUID;

public class APlayer {
    private final GenAI plugin;
    public final Player bukkitPlayer;
    public final User user;

    public final ActionData actionData;
    public final RotationData rotationData;
    public final PacketData packetData;

    public boolean hasInventoryOpened;

    public int windowId;
    public int globalVl;

    public NPC kaNpc;
    public int kaNpcVl;
    public boolean kaNpcSpawned;
    public BukkitRunnable kaNpcTask;

    public APlayer(Player bukkitPlayer) {
        this.bukkitPlayer = bukkitPlayer;
        this.user = PacketEvents.getAPI().getPlayerManager().getUser(bukkitPlayer);
        this.actionData = new ActionData(this);
        this.packetData = new PacketData(this);
        this.rotationData = new RotationData(this);
        this.plugin = GenAI.getInstance();
    }

    public Player getBukkitPlayer() {
        return bukkitPlayer;
    }

    public User getUser() {
        return user;
    }

    public void closeInventory() {
        if (user == null) return;

        user.writePacket(new WrapperPlayServerCloseWindow(windowId));
        PacketEvents.getAPI().getProtocolManager().receivePacket(
                user.getChannel(),
                new WrapperPlayClientCloseWindow(windowId)
        );
        user.flushPackets();
    }

    public void spawnNpc() {
        if (kaNpc != null || kaNpcSpawned) return;

        kaNpcSpawned = true;

        int entityId = (int) (Math.random() * Integer.MAX_VALUE);
        UUID uuid = UUID.randomUUID();
        UserProfile profile = new UserProfile(uuid, "Ashot");
        Component name = Component.text("Ashot");

        kaNpc = new NPC(profile, entityId, name);

        kaNpc.spawn(user.getChannel());

        kaNpcTask = new BukkitRunnable() {
            double angle = 0;
            final double radius = 3;
            final double speed = 0.45;

            @Override
            public void run() {
                if (!bukkitPlayer.isOnline()) {
                    removeNpc();
                    return;
                }

                double x = bukkitPlayer.getLocation().getX() + radius * Math.cos(angle);
                double y = bukkitPlayer.getLocation().getY() + 1;
                double z = bukkitPlayer.getLocation().getZ() + radius * Math.sin(angle);

                Vector3d pos = new Vector3d(x, y, z);
                float yaw = 0;
                float pitch = 0;

                angle += speed;

                kaNpc.teleport(new com.github.retrooper.packetevents.protocol.world.Location(pos, yaw, pitch));
            }
        };

        kaNpcTask.runTaskTimer(plugin, 0L, 1L);
    }

    public void removeNpc() {
        if (kaNpcTask != null) {
            kaNpcTask.cancel();
            kaNpcTask = null;
        }

        if (kaNpc != null) {
            kaNpc.despawn(user.getChannel());
            kaNpcSpawned = false;
            kaNpc = null;
        }
    }
}

