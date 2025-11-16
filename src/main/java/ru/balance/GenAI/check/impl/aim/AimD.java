package ru.balance.GenAI.check.impl.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import ru.balance.GenAI.check.APlayer;
import ru.balance.GenAI.check.Check;
import ru.balance.GenAI.check.PacketCheck;
import ru.balance.GenAI.check.stats.Statistics;
import ru.balance.GenAI.check.util.PacketUtil;

import java.util.ArrayList;
import java.util.List;

public class AimD extends Check implements PacketCheck {
    public AimD(APlayer aPlayer) {
        super("AimD", aPlayer);
    }

    private final List<Double> deltaYaws = new ArrayList<>();

    private final int maxHistory = 20;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isEnabled() || aPlayer.bukkitPlayer.isInsideVehicle() || !aPlayer.actionData.inCombat() || aPlayer.rotationData.isCinematicRotation())
            return;

        if (PacketUtil.isRotation(event)) {
            double deltaYaw = aPlayer.rotationData.deltaYaw;
            if (deltaYaw > 1.8 && aPlayer.rotationData.deltaPitch > 1.8) {
                deltaYaws.add(deltaYaw);
            }
            if (deltaYaws.size() > maxHistory) {
                List<Float> jiff = Statistics.getJiffDelta(deltaYaws, 1);
                float jiff1 = 95959;
                float jiff2 = 95795;
                for (float i : jiff) {
                    if (i == 0 && jiff1 == 0 && jiff2 == 0) {
                        flag();
                        break;
                    }
                    jiff2 = jiff1;
                    jiff1 = i;
                }
                deltaYaws.clear();
            }
        }
    }
}

