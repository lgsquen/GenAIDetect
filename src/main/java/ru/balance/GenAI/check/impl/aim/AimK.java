package ru.balance.GenAI.check.impl.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import ru.balance.GenAI.check.APlayer;
import ru.balance.GenAI.check.Check;
import ru.balance.GenAI.check.PacketCheck;
import ru.balance.GenAI.check.util.MathUtil;
import ru.balance.GenAI.check.util.PacketUtil;

import java.util.ArrayList;
import java.util.List;

public class AimK extends Check implements PacketCheck {
    public AimK(APlayer aPlayer) {
        super("AimK", aPlayer);
    }

    private final List<Double> deltaYaws = new ArrayList<>();

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isEnabled() || !aPlayer.actionData.inCombat() || aPlayer.rotationData.isCinematicRotation())
            return;

        if (PacketUtil.isRotation(event)) {
            double deltaYaw = Math.abs(aPlayer.rotationData.deltaYaw);
            deltaYaws.add(deltaYaw);

            if (deltaYaws.size() >= 10) {
                double[] diff = MathUtil.diff(deltaYaws);
                double diffSymmetry = MathUtil.symmetry(diff);

                double threshold = 0.01;

                if (diffSymmetry < threshold) {
                    flag("Symmetric deltas");
                }

                deltaYaws.clear();
            }
        }
    }
}

