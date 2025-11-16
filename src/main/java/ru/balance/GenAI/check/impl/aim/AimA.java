package ru.balance.GenAI.check.impl.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import ru.balance.GenAI.check.APlayer;
import ru.balance.GenAI.check.Check;
import ru.balance.GenAI.check.PacketCheck;
import ru.balance.GenAI.check.util.Config;
import ru.balance.GenAI.check.util.EvictingList;
import ru.balance.GenAI.check.util.PacketUtil;

import java.util.List;

public class AimA extends Check implements PacketCheck {
    public AimA(APlayer aPlayer) {
        super("AimA", aPlayer);
        this.maxBuffer = Config.getInt(getConfigPath() + ".max-buffer", 1);
        this.bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 0.05);
    }

    private double buffer;
    private double maxBuffer;
    private double bufferDecrease;
    private final List<Double> deltaYaws = new EvictingList<>(3);

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isEnabled() || aPlayer.bukkitPlayer.isInsideVehicle() || !aPlayer.actionData.inCombat())
            return;

        if (PacketUtil.isRotation(event)) {
            deltaYaws.add((double) Math.abs(aPlayer.rotationData.deltaYaw));
            if (deltaYaws.size() == 3) {
                double mid = deltaYaws.get(1);
                float min = 1.8f;

                if (deltaYaws.get(0) < min && deltaYaws.get(2) < min && mid > 35f && mid != 360) {
                    buffer++;

                    if (buffer > maxBuffer) {
                        flag("Patterned spike");
                        buffer = 0;
                    }
                } else {
                    if (buffer > 0) buffer -= bufferDecrease;
                }
            }
        }
    }

    @Override
    public void onReload() {
        this.maxBuffer = Config.getInt(getConfigPath() + ".max-buffer", 1);
        this.bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 0.05);
    }
}

