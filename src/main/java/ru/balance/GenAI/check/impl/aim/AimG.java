package ru.balance.GenAI.check.impl.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import ru.balance.GenAI.check.APlayer;
import ru.balance.GenAI.check.Check;
import ru.balance.GenAI.check.PacketCheck;
import ru.balance.GenAI.check.util.Config;
import ru.balance.GenAI.check.util.PacketUtil;

public class AimG extends Check implements PacketCheck {
    public AimG(APlayer aPlayer) {
        super("AimG", aPlayer);
        this.maxBuffer = Config.getDouble(getConfigPath() + ".max-buffer", 2);
        this.bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 0.25);
    }

    private static final float minAmp = 0.009f;
    private static final float maxAmp = 20.0f;
    private static final double ampTol = 0.20;
    private static final int flipsNeed = 5;
    private float lastDeltaYawABS = 0f;
    private float lastDeltaYawSigned = 0f;
    private int altFlipYawStreak = 0;
    private float lastDeltaPitchABS = 0f;
    private float lastDeltaPitchSigned = 0f;
    private int altFlipPitchStreak = 0;
    private boolean hasPrev = false;

    private double buffer;
    private double maxBuffer;
    private double bufferDecrease;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isEnabled() || aPlayer.bukkitPlayer.isInsideVehicle() || !aPlayer.actionData.inCombat() || aPlayer.rotationData.isCinematicRotation()) {
            lastDeltaYawABS = 0f;
            lastDeltaYawSigned = 0f;
            altFlipYawStreak = 0;
            lastDeltaPitchABS = 0f;
            lastDeltaPitchSigned = 0f;
            altFlipPitchStreak = 0;
            hasPrev = false;
            return;
        }

        if (PacketUtil.isRotation(event)) {
            float deltaYaw = aPlayer.rotationData.deltaYaw;
            float deltaYawABS = Math.abs(deltaYaw);

            float deltaPitch = aPlayer.rotationData.deltaPitch;
            float deltaPitchABS = Math.abs(deltaPitch);

            if (!hasPrev) {
                lastDeltaYawSigned = deltaYaw;
                lastDeltaYawABS = deltaYawABS;
                lastDeltaPitchSigned = deltaPitch;
                lastDeltaPitchABS = deltaPitchABS;
                hasPrev = true;
                return;
            }

            boolean validYaw = deltaYawABS >= minAmp && deltaYawABS <= maxAmp;
            if (validYaw && Math.signum(deltaYaw) != Math.signum(lastDeltaYawSigned)) {
                double relYaw = Math.abs(deltaYawABS - lastDeltaYawABS) / Math.max(deltaYawABS, lastDeltaYawABS);
                if (relYaw <= ampTol) {
                    if (++altFlipYawStreak >= flipsNeed) {
                        flag();
                        altFlipYawStreak = flipsNeed / 2;
                    }
                } else {
                    altFlipYawStreak = Math.max(0, altFlipYawStreak - 1);
                }
            } else if (validYaw) {
                altFlipYawStreak = Math.max(0, altFlipYawStreak - 1);
            } else {
                altFlipYawStreak = Math.max(0, altFlipYawStreak - 1);
            }
            lastDeltaYawABS = deltaYawABS;
            lastDeltaYawSigned = deltaYaw;

            boolean validPitch = deltaPitchABS >= minAmp && deltaPitchABS <= maxAmp;
            if (validPitch && Math.signum(deltaPitch) != Math.signum(lastDeltaPitchSigned)) {
                double relPitch = Math.abs(deltaPitchABS - lastDeltaPitchABS) / Math.max(deltaPitchABS, lastDeltaPitchABS);
                if (relPitch <= ampTol) {
                    if (++altFlipPitchStreak >= flipsNeed) {
                        buffer++;
                        if (buffer > maxBuffer) {
                            flag();
                            buffer = 0;
                        }
                        altFlipPitchStreak = flipsNeed / 2;
                    } else {
                        buffer = Math.max(0, buffer - bufferDecrease);
                    }
                } else {
                    altFlipPitchStreak = Math.max(0, altFlipPitchStreak - 1);
                }
            } else if (validPitch) {
                altFlipPitchStreak = Math.max(0, altFlipPitchStreak - 1);
            } else {
                altFlipPitchStreak = Math.max(0, altFlipPitchStreak - 1);
            }
            lastDeltaPitchABS = deltaPitchABS;
            lastDeltaPitchSigned = deltaPitch;
        }
    }

    @Override
    public void onReload() {
        maxBuffer = Config.getDouble(getConfigPath() + ".max-buffer", 1);
        bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 0.05);
    }
}
