package ru.balance.GenAI.check.impl.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import ru.balance.GenAI.check.APlayer;
import ru.balance.GenAI.check.Check;
import ru.balance.GenAI.check.PacketCheck;
import ru.balance.GenAI.check.util.Config;
import ru.balance.GenAI.check.util.PacketUtil;

public class AimI extends Check implements PacketCheck {
    public AimI(APlayer aPlayer) {
        super("AimI", aPlayer);
        maxBuffer = Config.getDouble(getConfigPath() + ".max-buffer", 1);
        bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 0.05);
    }

    private int yawStreak;
    private int pitchStreak;
    private double buffer1;
    private double buffer2;
    private double maxBuffer;
    private double bufferDecrease;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isEnabled() || aPlayer.bukkitPlayer.isInsideVehicle() || !aPlayer.actionData.inCombat() || aPlayer.rotationData.isCinematicRotation()) return;

        if (PacketUtil.isRotation(event)) {
            float deltaYaw = Math.abs(aPlayer.rotationData.deltaYaw);
            float deltaPitch = Math.abs(aPlayer.rotationData.deltaPitch);
            float lastDeltaYaw = Math.abs(aPlayer.rotationData.lastDeltaYaw);
            float lastDeltaPitch = Math.abs(aPlayer.rotationData.lastDeltaPitch);

            if (deltaYaw == lastDeltaYaw && deltaYaw != 0) {
                yawStreak++;
                if (yawStreak > 2) {
                    buffer1++;
                    if (buffer1 > maxBuffer) {
                        flag();
                        buffer1 = 0;
                    }
                } else {
                    if (buffer1 > 0) buffer1 -= bufferDecrease;
                }
            } else {
                yawStreak = 0;
            }

            if (deltaPitch == lastDeltaPitch && deltaPitch != 0) {
                pitchStreak++;
                if (pitchStreak > 2) {
                    buffer2++;
                    if (buffer2 > maxBuffer) {
                        flag();
                        buffer2 = 0;
                    }
                    pitchStreak = 0;
                } else {
                    if (buffer2 > 0) buffer2 -= bufferDecrease;
                }
            } else {
                pitchStreak = 0;
            }
        }
    }

    @Override
    public void onReload() {
        maxBuffer = Config.getDouble(getConfigPath() + ".max-buffer", 1);
        bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 0.05);
    }
}
