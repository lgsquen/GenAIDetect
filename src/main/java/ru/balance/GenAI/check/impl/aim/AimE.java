package ru.balance.GenAI.check.impl.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import ru.balance.GenAI.check.APlayer;
import ru.balance.GenAI.check.Check;
import ru.balance.GenAI.check.PacketCheck;
import ru.balance.GenAI.check.util.Config;
import ru.balance.GenAI.check.util.EvictingList;
import ru.balance.GenAI.check.util.PacketUtil;

import java.util.List;

public class AimE extends Check implements PacketCheck {
    public AimE(APlayer aPlayer) {
        super("AimE", aPlayer);
        this.maxBuffer = Config.getInt(getConfigPath() + ".max-buffer", 9);
        this.bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 1);
    }

    private double buffer;
    private double maxBuffer;
    private double bufferDecrease;
    private final List<Double> yawSamples = new EvictingList<>(5);

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isEnabled() || !aPlayer.actionData.inCombat() || aPlayer.rotationData.isCinematicRotation())
            return;

        if (PacketUtil.isRotation(event)) {
            yawSamples.add((double) Math.abs(aPlayer.rotationData.deltaYaw));
            if (yawSamples.size() == 5) {
                double first = yawSamples.get(0);
                boolean allSame = true;
                for (double d : yawSamples) {
                    if (Math.abs(d - first) > 0.01) {
                        allSame = false;
                        break;
                    }
                }
                if (allSame && first > 1.5) {
                    buffer++;
                    if (buffer > maxBuffer) {
                        flag("Constant yaw pattern");
                        buffer = 0;
                    }
                } else {
                    if (buffer > 0) buffer -= bufferDecrease;
                }
                yawSamples.clear();
            }
        }
    }

    @Override
    public void onReload() {
        this.maxBuffer = Config.getInt(getConfigPath() + ".max-buffer", 9);
        this.bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 1);
    }
}

