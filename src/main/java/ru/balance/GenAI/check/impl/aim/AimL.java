package ru.balance.GenAI.check.impl.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import ru.balance.GenAI.check.APlayer;
import ru.balance.GenAI.check.Check;
import ru.balance.GenAI.check.PacketCheck;
import ru.balance.GenAI.check.stats.Simplification;
import ru.balance.GenAI.check.stats.Statistics;
import ru.balance.GenAI.check.util.Config;
import ru.balance.GenAI.check.util.PacketUtil;

import java.util.ArrayList;
import java.util.List;

public class AimL extends Check implements PacketCheck {
    public AimL(APlayer aPlayer) {
        super("AimL", aPlayer);
        this.maxBuffer = Config.getDouble(getConfigPath() + ".max-buffer", 1);
        this.bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 0.25);
    }

    private final List<Double> deltaYaws = new ArrayList<>();
    private double maxBuffer;
    private double bufferDecrease;
    private double buffer;
    private int infinitives;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isEnabled() || !aPlayer.actionData.inCombat() || aPlayer.rotationData.isCinematicRotation()) return;

        if (PacketUtil.isRotation(event)) {
            double deltaYaw = Math.abs(aPlayer.rotationData.deltaYaw);
            deltaYaws.add(deltaYaw);

            if (deltaYaws.size() > 10) {
                double deltaYawFirst = Math.abs(deltaYaws.get(0) - deltaYaws.get(1));
                double robotized = Math.abs(deltaYaw - deltaYawFirst);
                double interpolation = Simplification.scaleVal(deltaYaw / robotized, 2);

                if (Double.isInfinite(interpolation) && deltaYaw > 0) {
                    infinitives++;
                    if (infinitives > 1 && deltaYaw < 0.4) {
                        infinitives--;
                    }
                }

                if (infinitives > 1 && Math.abs(Statistics.getAverage(deltaYaws)) > 3.2) {
                    buffer += bufferDecrease;
                    if (buffer > maxBuffer) {
                        flag();
                        buffer = 0;
                        infinitives = 0;
                    }
                } else {
                    if (buffer > 0) buffer -= bufferDecrease;
                }
                deltaYaws.clear();
            }
        }
    }

    @Override
    public void onReload() {
        maxBuffer = Config.getDouble(getConfigPath() + ".max-buffer", 1);
        bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 0.25);
    }
}

