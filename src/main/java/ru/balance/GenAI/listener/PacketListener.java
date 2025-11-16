package ru.balance.GenAI.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import ru.balance.GenAI.GenAI;
import ru.balance.GenAI.entity.Frame;
import ru.balance.GenAI.entity.PlayerEntity;
import ru.balance.GenAI.registry.PlayerRegistry;
import ru.balance.GenAI.service.AnalysisService;
import ru.balance.GenAI.util.MouseCalculator;
import java.util.List;

public class PacketListener extends PacketListenerAbstract {

    private final boolean isMlCheckEnabled;
    private final int framesToAnalyze;
    private final GenAI plugin;
    private final long combatTimerTicks;

    public PacketListener() {
        this.plugin = GenAI.getInstance();
        this.isMlCheckEnabled = plugin.getConfig().getBoolean("ml-check.enabled", false);
        this.framesToAnalyze = plugin.getConfig().getInt("ml-check.frames-to-analyze", 150);
        this.combatTimerTicks = plugin.getConfig().getLong("ml-check.combat-timer-seconds", 10) * 20;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        User user = event.getUser();
        if (user == null || user.getUUID() == null) return;

        Player player = Bukkit.getPlayer(user.getUUID());
        if (player != null && plugin.getCheckManager() != null) {
            plugin.getCheckManager().handlePacketReceive(player, event);
            if (event.isCancelled()) {
                return;
            }
        }

        if (!isMlCheckEnabled) return;

        PlayerEntity entity = PlayerRegistry.getPlayer(user.getUUID());
        if (entity == null) return;

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            if (entity.isInCombat()) {
                WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);

                if (flying.hasRotationChanged()) {
                    float currentYaw = MouseCalculator.normalizeAngle(flying.getLocation().getYaw());
                    float currentPitch = MouseCalculator.normalizeAngle(flying.getLocation().getPitch());
                    float lastYaw = MouseCalculator.normalizeAngle(entity.getLastYaw());
                    float lastPitch = MouseCalculator.normalizeAngle(entity.getLastPitch());

                    float deltaYaw = currentYaw - lastYaw;
                    float deltaPitch = currentPitch - lastPitch;

                    float accelYaw = MouseCalculator.calculateAcceleration(deltaYaw, entity.getLastDeltaYaw());
                    float accelPitch = MouseCalculator.calculateAcceleration(deltaPitch, entity.getLastDeltaPitch());

                    float jerkYaw = MouseCalculator.calculateJerk(accelYaw, entity.getLastAccelYaw());
                    float jerkPitch = MouseCalculator.calculateJerk(accelPitch, entity.getLastAccelPitch());

                    float gcdErrorYaw = MouseCalculator.calculateGCDError(deltaYaw);
                    float gcdErrorPitch = MouseCalculator.calculateGCDError(deltaPitch);

                    Frame frame = new Frame(
                            deltaYaw,
                            deltaPitch,
                            accelYaw,
                            accelPitch,
                            jerkYaw,
                            jerkPitch,
                            gcdErrorYaw,
                            gcdErrorPitch
                    );

                    entity.setLastDeltaYaw(deltaYaw);
                    entity.setLastDeltaPitch(deltaPitch);
                    entity.setLastAccelYaw(accelYaw);
                    entity.setLastAccelPitch(accelPitch);

                    List<Frame> frames = entity.getFrames();
                    frames.add(frame);
                    while (frames.size() > framesToAnalyze) {
                        frames.remove(0);
                    }
                }
                updateLastLocation(entity, flying.getLocation());
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {

                Player attackPlayer = Bukkit.getPlayer(user.getUUID());
                if (attackPlayer != null) {
                    plugin.getMitigationManager().handlePacketAttack(attackPlayer, event);
                    if (event.isCancelled()) return;

                    int targetId = interact.getEntityId();
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Entity targetEntity = null;
                        for (Entity worldEntity : attackPlayer.getWorld().getEntities()) {
                            if (worldEntity.getEntityId() == targetId) {
                                targetEntity = worldEntity;
                                break;
                            }
                        }

                        if (targetEntity instanceof Player) {
                            Player targetPlayer = (Player) targetEntity;
                            entity.tagCombat(combatTimerTicks);
                            PlayerEntity targetPlayerEntity = PlayerRegistry.getPlayer(targetPlayer.getUniqueId());
                            if (targetPlayerEntity != null) {
                                targetPlayerEntity.tagCombat(combatTimerTicks);
                            }

                            if (entity.getFrames().size() >= framesToAnalyze) {
                                AnalysisService.analyze(entity);
                                entity.getFrames().clear();
                            }
                        }
                    });
                }
            }
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        User user = event.getUser();
        if (user == null || user.getUUID() == null) return;

        Player player = Bukkit.getPlayer(user.getUUID());
        if (player != null && plugin.getCheckManager() != null) {
            plugin.getCheckManager().handlePacketSend(player, event);
            if (event.isCancelled()) {
                return;
            }
        }
    }

    private void updateLastLocation(PlayerEntity entity, com.github.retrooper.packetevents.protocol.world.Location location) {
        entity.setLastYaw(MouseCalculator.normalizeAngle(location.getYaw()));
        entity.setLastPitch(MouseCalculator.normalizeAngle(location.getPitch()));
    }
}
