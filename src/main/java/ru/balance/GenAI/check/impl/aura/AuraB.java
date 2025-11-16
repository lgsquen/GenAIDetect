package ru.balance.GenAI.check.impl.aura;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import ru.balance.GenAI.GenAI;
import ru.balance.GenAI.check.APlayer;
import ru.balance.GenAI.check.Check;
import ru.balance.GenAI.check.PacketCheck;
import ru.balance.GenAI.check.util.Config;

import java.util.ArrayList;
import java.util.List;

public class AuraB extends Check implements PacketCheck {
    public AuraB(APlayer player) {
        super("AuraB", player);
        this.maxBuffer = Config.getInt(getConfigPath() + ".max-buffer", 3);
        this.bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 0.5);
        this.hitboxExpandX = Config.getDouble(getConfigPath() + ".hitbox-expandX", 0.25);
        this.hitboxExpandY = Config.getDouble(getConfigPath() + ".hitbox-expandY", 0.35);
        this.hitboxExpandZ = Config.getDouble(getConfigPath() + ".hitbox-expandZ", 0.25);
    }

    private double buffer;
    private double maxBuffer;
    private double bufferDecrease;
    private double hitboxExpandX;
    private double hitboxExpandY;
    private double hitboxExpandZ;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isEnabled() || !aPlayer.actionData.inCombat()) return;

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
            if (wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                Player target = aPlayer.actionData.getPTarget();
                if (target == null) return;

                Bukkit.getScheduler().runTask(GenAI.getInstance(), () -> {
                    if (wallHit(aPlayer.bukkitPlayer, target)) {
                        buffer++;
                        if (buffer > maxBuffer) {
                            flag("Through-wall hit");
                            buffer = 0;
                        }
                    } else {
                        if (buffer > 0) buffer -= bufferDecrease;
                    }
                });
            }
        }
    }

    private boolean wallHit(Player from, Player target) {
        Location origin = from.getLocation().add(0, from.getEyeHeight(), 0);

        double widthX = target.getWidth() / 2.0 + hitboxExpandX;
        double widthZ = target.getWidth() / 2.0 + hitboxExpandZ;
        double minX = target.getLocation().getX() - widthX;
        double maxX = target.getLocation().getX() + widthX;
        double minY = target.getLocation().getY();
        double maxY = target.getLocation().getY() + target.getHeight() + hitboxExpandY;
        double minZ = target.getLocation().getZ() - widthZ;
        double maxZ = target.getLocation().getZ() + widthZ;

        List<Vector> points = new ArrayList<>();
        for (int i = 0; i <= 2; i++) {
            double x = minX + i * (maxX - minX) / 2.0;
            for (int j = 0; j <= 2; j++) {
                double y = minY + j * (maxY - minY) / 2.0;
                for (int k = 0; k <= 2; k++) {
                    double z = minZ + k * (maxZ - minZ) / 2.0;
                    points.add(new Vector(x, y, z));
                }
            }
        }

        for (Vector point : points) {
            Vector direction = point.clone().subtract(origin.toVector());
            RayTraceResult result = from.getWorld().rayTraceBlocks(
                    origin,
                    direction.normalize(),
                    direction.length(),
                    FluidCollisionMode.NEVER,
                    true
            );

            if (result == null || result.getHitPosition() == null) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void onReload() {
        this.maxBuffer = Config.getInt(getConfigPath() + ".max-buffer", 3);
        this.bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 0.5);
        this.hitboxExpandX = Config.getDouble(getConfigPath() + ".hitbox-expandX", 0.25);
        this.hitboxExpandY = Config.getDouble(getConfigPath() + ".hitbox-expandY", 0.35);
        this.hitboxExpandZ = Config.getDouble(getConfigPath() + ".hitbox-expandZ", 0.25);
    }
}

